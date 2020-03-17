package com.template.contracts

import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class DoctorContract : Contract {
    companion object {
        const val EHR_CONTRACT_ID = "com.template.contracts.EHRContract"
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Share : TypeOnlyCommandData(), Commands
    }

    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when issuing a EHRState." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a EHRState." using (tx.outputs.size == 1)
            }

            is Commands.Share -> requireThat {
                val output = tx.outputsOfType<EHRShareAgreementState>().single()
                "Patient is a required signer" using (command.signers.contains(output.patient.owningKey))
                "Only one output state should be created when issuing a EHRState." using (tx.outputs.size == 1)
            }

        }
    }
}