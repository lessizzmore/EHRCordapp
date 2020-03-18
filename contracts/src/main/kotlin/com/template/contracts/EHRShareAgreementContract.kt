package com.template.contracts

import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import java.security.PublicKey


class EHRShareAgreementContract : Contract {
    companion object {
        const val EHR_CONTRACT_ID = "com.template.contracts.EHRContract"
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Suspend : TypeOnlyCommandData(), Commands
        class Activate : TypeOnlyCommandData(), Commands
        class Share : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {

        val command = tx.commands.requireSingleCommand<Commands>()
        val setOfSigners = command.signers.toSet()

        when (command.value) {

            is Commands.Create -> verifyCreate(tx, setOfSigners)
            is Commands.Suspend -> verifySuspend(tx, setOfSigners)
            is Commands.Activate -> verifyActive(tx, setOfSigners)
            is Commands.Share -> verifyShare(tx, setOfSigners)
            else -> throw IllegalArgumentException("Unrecognized command.")
        }
    }

    private fun verifyCreate(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputs.single {it.data is EHRShareAgreementState }
        val outputEHR = output.data as EHRShareAgreementState
        val inputEHR = tx.inputsOfType<EHRShareAgreementState>().single()
        "No inputs should be consumed when issuing a EHRAgreementState." using (tx.inputs.isEmpty())
        "Only one output state should be created when creating a EHRState." using (tx.outputs.size == 1)
        "Creating a EHRAgreementState should contain an output in PENDING status." using (outputEHR.status == EHRShareAgreementStateStatus.PENDING)
    }

    private fun verifySuspend(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputs.single {it.data is EHRShareAgreementState }
        val outputEHR = output.data as EHRShareAgreementState
        val inputEHR = tx.inputsOfType<EHRShareAgreementState>().single()
        "Only patient should sign a suspension transaction" using (command.signers.toSet() == setOf(outputEHR.patient.owningKey))
        "Input state of a suspension transaction shouldn't be already suspended" using (inputEHR.status != EHRShareAgreementStateStatus.SUSPENDED)
        "Output state of a suspension transaction should be suspended" using (outputEHR.status != EHRShareAgreementStateStatus.PENDING)
    }

    private fun verifyActive(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputs.single {it.data is EHRShareAgreementState }
        val outputEHR = output.data as EHRShareAgreementState
        val inputEHR = tx.inputsOfType<EHRShareAgreementState>().single()
        "Only patient should sign a EHR activation transaction" using (command.signers.toSet() == setOf(outputEHR.patient.owningKey))
        "Input state of a EHR activation transaction shouldn't be already active" using (inputEHR.status != EHRShareAgreementStateStatus.ACTIVE)
        "Output state of a EHR activation transaction should be active" using (outputEHR.status == EHRShareAgreementStateStatus.ACTIVE)
    }

    private fun verifyShare(tx: LedgerTransaction, signers: Set<PublicKey>) = requireThat {
        val command = tx.commands.requireSingleCommand<Commands>()
        val output = tx.outputs.single {it.data is EHRShareAgreementState }
        val outputEHR = output.data as EHRShareAgreementState
        "Patient is a required signer" using (command.signers.contains(outputEHR.patient.owningKey))
        "Only one output state should be created when issuing a EHRAgreementState." using (tx.outputs.size == 1)
    }
}