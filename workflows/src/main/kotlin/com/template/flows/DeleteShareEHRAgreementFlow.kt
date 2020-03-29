package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.*


@InitiatingFlow
@StartableByRPC
class DeleteShareEHRAgreementFlow (
        // counterParty can be patient who wants to delete suspended request
        // counterParty can also be origin doctor who wants to delete pending request
        val counterParty: Party,
        val ehrId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

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

        val deleteCommand = Command(EHRShareAgreementContract.Commands.Delete(), listOf(ourIdentity, counterParty).map { it.owningKey })

        // Create the tx. Add delete command and input.
        val txBuilder = TransactionBuilder(notary)
                .addInputState(stateToDelete)
                .addCommand(deleteCommand)


        // Verify and sign the tx, only the origin doctor needs to sign it
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)
        val targetSession = initiateFlow(counterParty)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))

        // Finalize the tx.
        return subFlow(FinalityFlow(stx, listOf(targetSession)))
    }
}

@InitiatedBy(DeleteShareEHRAgreementFlow::class)
class DeleteShareEHRAgreementFlowResponder (private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {

            }
        }

        val stx = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSession, expectedTxId = stx.id))
    }
}