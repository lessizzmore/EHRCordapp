package com.template.flows

import BroadcastTransaction
import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@InitiatingFlow
@StartableByRPC
class ShareEHRFlow(
        val patient: Party,
        val ehrId: UniqueIdentifier
): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // get input state
        // Get EHRShareAgreementState to clear.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToShare = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")

        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // build tx with 1 input and 0 output
        val shareCommand = Command(EHRShareAgreementContract.Commands.Share(), listOf(ourIdentity, patient).map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(ehrStateRefToShare)
        builder.addOutputState(ehrStateRefToShare.state.data.copy(status = EHRShareAgreementStateStatus.SHARED), EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(shareCommand)

        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        // Sends selfSignedTx to patient
        val targetSession = initiateFlow(patient)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))
        val ftx = subFlow(FinalityFlow(stx, listOf(targetSession)))

        // broadcast transaction to observer
        subFlow(BroadcastTransaction(ftx))

        return ftx

    }
}


@InitiatedBy(ShareEHRFlow::class)
class ShareEHRFlowResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an ERHState" using (output is EHRShareAgreementState)
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
