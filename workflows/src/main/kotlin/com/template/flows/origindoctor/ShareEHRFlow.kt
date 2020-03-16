package com.template.flows.origindoctor

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRContract
import com.template.states.EHRState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class ShareEHRFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {

    override val progressTracker = ProgressTracker()

    companion object {
        object CREATING : ProgressTracker.Step("Creating a new EHR")
        object VERIFYING : ProgressTracker.Step("Verifying EHR")
        object SIGNING : ProgressTracker.Step("Signing EHR")
        object FINALISING : ProgressTracker.Step("Sending EHR") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(CREATING, SIGNING, VERIFYING, FINALISING)
    }

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING
        val originDoctor = serviceHub.myInfo.legalIdentities.first()
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val createCommand = Command(EHRContract.Commands.Create(), listOf(originDoctor, targetDoctor, patient).map { it.owningKey })
        val EHRState = EHRState(patient, originDoctor, targetDoctor, "Blood Test")
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(EHRState, EHRContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)

        progressTracker.currentStep = VERIFYING
        builder.verify(serviceHub)

        progressTracker.currentStep = SIGNING
        val ptx = serviceHub.signInitialTransaction(builder)

        progressTracker.currentStep = FINALISING
        val targetSessions = listOf(patient, targetDoctor).map{ initiateFlow(it as Party) }
        val fullytx = subFlow(CollectSignaturesFlow(ptx, targetSessions))
        return subFlow(FinalityFlow(fullytx, targetSessions))
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(ShareEHRFlow::class)
class ShareEHRFlowResponder(val counterpartySession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an ERHState" using (output is EHRState)
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
