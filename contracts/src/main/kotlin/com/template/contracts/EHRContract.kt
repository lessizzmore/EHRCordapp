package com.template.contracts

import com.template.states.EHRState
import com.template.states.EHRStateStatus
import net.corda.core.contracts.*
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.LedgerTransaction


class EHRContract : Contract {
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
        val output = tx.outputs.single {it.data is EHRState}
        val outputEHR = output.data as EHRState
        val inputEHR = tx.inputsOfType<EHRState>().single()
        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when issuing a EHRState." using (tx.inputs.isEmpty())
                "Only one output state should be created when creating a EHRState." using (tx.outputs.size == 1)
                "Creating a EHRState should contain an output in PENDING status." using (outputEHR.status == EHRStateStatus.PENDING)
            }
            is Commands.Suspend -> requireThat {
                "Only patient should sign a suspension transaction" using (command.signers.toSet() == setOf(outputEHR.patient.owningKey))
                "Input state of a suspension transaction shouldn't be already suspended" using (inputEHR.status != EHRStateStatus.SUSPENDED)
                "Output state of a suspension transaction should be suspended" using (outputEHR.status != EHRStateStatus.PENDING)
            }
            is Commands.Activate -> requireThat {
                "Only patient should sign a EHR activation transaction" using (command.signers.toSet() == setOf(outputEHR.patient.owningKey))
                "Input state of a EHR activation transaction shouldn't be already active" using (inputEHR.status != EHRStateStatus.ACTIVE)
                "Output state of a EHR activation transaction should be active" using (outputEHR.status == EHRStateStatus.ACTIVE)
            }
            is Commands.Share -> requireThat {
                val output = tx.outputsOfType<EHRState>().single()
                "Patient is a required signer" using (command.signers.contains(output.patient.owningKey))
                "Only one output state should be created when issuing a EHRState." using (tx.outputs.size == 1)
            }

        }
    }
}