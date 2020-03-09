package com.template.contracts

import com.template.states.ResponsibilityState
import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction


class EHRContract : Contract {
    companion object {
        const val PAYMENT_CONTRACT_ID = "com.template.contracts.PaymentContract"
    }

    interface Commands : CommandData {
        class Create : TypeOnlyCommandData(), Commands
        class Pay : TypeOnlyCommandData(), Commands
    }

    // A transaction is valid if the verify() function of the contract of all the transaction's input and output states
    // does not throw an exception.
    override fun verify(tx: LedgerTransaction) {
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Create -> requireThat {
                "No inputs should be consumed when issuing a payment request." using (tx.inputs.isEmpty())
                "Only one output state should be created when issuing a payment request." using (tx.outputs.size == 1)
                val request = tx.outputsOfType<ResponsibilityState>().single()
                "There must be at least one member in the pool" using (request.participants.isNotEmpty())
            }

            is Commands.Pay -> requireThat {
            }

        }
    }
}