package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RequestShareEHRAgreementFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {


    companion object {
        object CREATING_EHR_SHARE_AGREEMENT : ProgressTracker.Step("Creating EHR share agreement")
        object SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT : ProgressTracker.Step("Sending EHR share agreement to patient")

        fun tracker() = ProgressTracker(
                CREATING_EHR_SHARE_AGREEMENT,
                SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = CREATING_EHR_SHARE_AGREEMENT
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO code into config instead of getting the first()
        val createCommand = Command(EHRShareAgreementContract.Commands.Create(), listOf(ourIdentity, patient).map { it.owningKey })


        // create output EHRShareAgreementState
        val initialEHRShareAgreementState = EHRShareAgreementState(patient, ourIdentity, targetDoctor,"blood test result")


        // create tx builder. add output state and command
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(initialEHRShareAgreementState, EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)


        // verify and sign the tx
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)


        // origin doctor sends share agreement to patient
        progressTracker.currentStep = SENDING_EHR_SHARE_AGREEMENT_TO_PATIENT
        val targetSession = initiateFlow(patient)


        // collect signature from patient and finalize the tx
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))
        return subFlow(FinalityFlow(stx, targetSession))
    }
}


@InitiatedBy(RequestShareEHRAgreementFlow::class)
class RequestShareEHRAgreementFlowResponder (

        private val otherSession: FlowSession

) : FlowLogic<SignedTransaction>() {

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
