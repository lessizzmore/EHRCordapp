package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.*


@InitiatingFlow
@StartableByRPC
class DeleteShareEHRAgreementFlow (
        // counterParty can be patient who wants to delete suspended request
        // counterParty can also be origin doctor who wants to delete pending request
        val whoIam: String,
        val whereTo: String,
        val ehrId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val myAccountStateAndRef = accountService.accountInfo(whoIam).single()
        val myKeyToUse = if (myAccountStateAndRef.state.data.host == ourIdentity) {
            serviceHub.createKeyForAccount(myAccountStateAndRef.state.data).owningKey
        } else {
            subFlow(RequestKeyForAccount(myAccountStateAndRef.state.data)).owningKey
        }

        val counterPartyAccountStateAndRef = accountService.accountInfo(whereTo).single()

        val counterParty = if (counterPartyAccountStateAndRef.state.data.host == ourIdentity) {
            serviceHub.createKeyForAccount(counterPartyAccountStateAndRef.state.data)
        } else {
            subFlow(RequestKeyForAccount(counterPartyAccountStateAndRef.state.data))
        }

        // Get EHRShareAgreementState to clear.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)

        val stateToDelete = serviceHub.vaultService
                .queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")

        val inputEHRShareAgreementState = stateToDelete.state.data


        check( inputEHRShareAgreementState.status != EHRShareAgreementStateStatus.ACTIVE) {
            throw FlowException("Cannot delete state that has been activated.")
        }


        // Create the list of signers and the delete command.
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO code into config instead of getting the first()

        val deleteCommand = Command(EHRShareAgreementContract.Commands.Delete(), listOf(ourIdentity.owningKey, myKeyToUse))

        // Create the tx. Add delete command and input.
        val txBuilder = TransactionBuilder(notary)
                .addInputState(stateToDelete)
                .addCommand(deleteCommand)


        // Verify and sign the tx, only the origin doctor needs to sign it
        txBuilder.verify(serviceHub)
        // sign tx locally
        val locallySignedTx = serviceHub.signInitialTransaction(txBuilder, listOfNotNull(ourIdentity.owningKey, myKeyToUse))

        val sessionForAccountToSentTo = initiateFlow(counterParty)

        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSentTo, counterParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        // finalize
        return subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSentTo).filter { it.counterparty != ourIdentity }))
    }
}

@InitiatedBy(DeleteShareEHRAgreementFlow::class)
class DeleteShareEHRAgreementFlowResponder (private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        val transaction = subFlow(transactionSigner)
        subFlow(
                ReceiveFinalityFlow(
                        otherSession,
                        expectedTxId = transaction.id,
                        statesToRecord = StatesToRecord.ALL_VISIBLE
                )
        )
    }
}