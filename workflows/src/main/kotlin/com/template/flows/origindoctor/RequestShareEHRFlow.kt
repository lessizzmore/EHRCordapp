package com.template.flows.origindoctor

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRContract
import com.template.states.EHRState
import net.corda.core.contracts.Command
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

@InitiatingFlow
@StartableByRPC
class RequestShareEHRFlow(val patient: Party, val targetDoctor: Party): FlowLogic<SignedTransaction>() {


    companion object {
        object SENDING_EHR_DATA_TO_PATIENT : ProgressTracker.Step("Sending EHR agreement to patient")
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
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val createCommand = Command(EHRContract.Commands.Create(), listOf(ourIdentity, targetDoctor, patient).map { it.owningKey })
        val EHRState = EHRState(patient, ourIdentity, targetDoctor, "Blood Test")
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(EHRState, EHRContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)
        patientSession.send(builder)
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
        val selfSignedTx = subFlow(signResponder)
        return selfSignedTx
    }
}

