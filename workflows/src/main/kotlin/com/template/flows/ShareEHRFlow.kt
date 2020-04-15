package com.template.flows

import BroadcastTransactionToRecipients
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ShareEHRFlow(
        val whoIam: String,
        val whereTo: String,
        val observer: String,
        val ehrId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // generate keys for tx
        // doctor 1
        val myAccountStateAndRef = accountService.accountInfo(whoIam).single()
        val myAccount = serviceHub.createKeyForAccount(myAccountStateAndRef.state.data)
        // doctor2 : observer
        val observerAccount = accountService.accountInfo(observer).single().state.data
//        val observerAccountAnonParty = serviceHub.createKeyForAccount(observerAccount)
        val observerAccountAnonParty = subFlow(RequestKeyForAccount(observerAccount))
        // patient
        val targetPAccountStateAndRef = accountService.accountInfo(whereTo).single()
        val targetPAnonParty =  subFlow(RequestKeyForAccount(targetPAccountStateAndRef.state.data))



        // get input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToShare = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.single()
        val ehrState = ehrStateRefToShare.state.data
        val notary = serviceHub.networkMapCache.notaryIdentities.first()



        val spiedOnState = ehrState.copy( participants= ehrState.participants + observerAccountAnonParty)
        val shareCommand =
                Command(EHRShareAgreementContract.Commands.Share(), listOf(ourIdentity.owningKey))
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(ehrStateRefToShare)
        builder.addOutputState(spiedOnState.copy(status = EHRShareAgreementStateStatus.SHARED), EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(shareCommand)

        builder.verify(serviceHub)

        // sign tx locallys
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey))

        // finalize
        return subFlow(FinalityFlow(locallySignedTx))

    }
}


@InitiatedBy(ShareEHRFlow::class)
class ShareEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<Unit>() {

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