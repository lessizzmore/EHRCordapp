package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
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
class ShareEHRFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // get input state
        val queryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val ehrStateRefToShare = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.single()

        val originDoctor = serviceHub.myInfo.legalIdentities.first()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // build tx with 1 input and 0 output
        val shareCommand = Command(EHRShareAgreementContract.Commands.Share(), listOf(originDoctor, targetDoctor, patient).map { it.owningKey })
        val builder = TransactionBuilder(notary = notary)
        builder.addInputState(ehrStateRefToShare)
        builder.addCommand(shareCommand)

        builder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(builder)
        val targetSession = initiateFlow(targetDoctor)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))
        return subFlow(FinalityFlow(stx, targetSession))
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
                val ehrShareAgreementState =
                        stx.coreTransaction.outputStates.single() as EHRShareAgreementState
                "EHRShareAgreement sent to the wrong person" using (ehrShareAgreementState.originDoctor == ourIdentity)
                "Only Activated EHRShareAgreement will be accepted" using (ehrShareAgreementState.status.equals(EHRShareAgreementStateStatus.ACTIVE))
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
