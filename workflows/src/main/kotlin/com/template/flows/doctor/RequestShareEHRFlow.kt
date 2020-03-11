package com.template.flows.doctor

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRContract
import com.template.states.EHRState
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.ProgressTracker

@CordaSerializable
data class SharingRequest(val patient: Party, val targetDoctor: Party)

@InitiatingFlow
@StartableByRPC
class RequestShareEHRFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {


    companion object {
        object SENDING_EHR_DATA_TO_PATIENT : ProgressTracker.Step("Sending EHR data to patient")
        object ACCEPTING_INCOMING_PENDING_EHR : ProgressTracker.Step("Accepting incoming pending EHR")

        fun tracker() = ProgressTracker(
                SENDING_EHR_DATA_TO_PATIENT,
                ACCEPTING_INCOMING_PENDING_EHR
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = SENDING_EHR_DATA_TO_PATIENT
        val patientSession = initiateFlow(patient)
        patientSession.send(SharingRequest(patient, targetDoctor))
        val signResponder = object : SignTransactionFlow(patientSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val command = stx.tx.commands.single()
                if (command.value !is EHRContract.Commands.Create) {
                    throw FlowException("Only create command is allowed")
                }

                val output = stx.tx.outputs.single()

                val EHRState = output.data as EHRState
                if (patient != EHRState.patient) {
                    throw IllegalArgumentException("Wrong patient identity")
                }
                if (ourIdentity != EHRState.originDoctor) {
                    throw IllegalArgumentException("We have to be originDoctor")
                }

                stx.toLedgerTransaction(serviceHub, false).verify()
            }
        }

        progressTracker.currentStep = ACCEPTING_INCOMING_PENDING_EHR
        if (ourIdentity != patient) {
            val selfSignedTx = subFlow(signResponder)

            return if (patientSession.getCounterpartyFlowInfo().flowVersion != 1) {
                subFlow(ReceiveFinalityFlow(patientSession, selfSignedTx.id))
            } else {
                selfSignedTx
            }
        } else {
            return patientSession.receive<SignedTransaction>().unwrap { it }
        }

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
