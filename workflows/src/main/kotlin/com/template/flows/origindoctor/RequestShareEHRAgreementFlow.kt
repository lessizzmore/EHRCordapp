package com.template.flows.origindoctor

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RequestShareEHRAgreementFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {


    companion object {
        object SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT : ProgressTracker.Step("Sending EHR share agreement to patient")

        fun tracker() = ProgressTracker(
                SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val createCommand = Command(EHRShareAgreementContract.Commands.Create(), listOf(ourIdentity, patient).map { it.owningKey })
        val initialEHRShareAgreementState = EHRShareAgreementState(patient, ourIdentity, targetDoctor, "blood test result", null)
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(initialEHRShareAgreementState, EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)
        builder.verify(serviceHub)

        val ptx = serviceHub.signInitialTransaction(builder)
        // origin doctor sends share agreement to patient
        val targetSession = initiateFlow(patient)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))
        return subFlow(FinalityFlow(stx, targetSession))
    }
}

