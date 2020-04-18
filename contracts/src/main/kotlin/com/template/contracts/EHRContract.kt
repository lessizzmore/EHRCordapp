package com.template.contracts

import com.r3.corda.lib.tokens.contracts.EvolvableTokenContract
import com.template.states.EHRTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Contract
import net.corda.core.transactions.LedgerTransaction

class EHRContract : EvolvableTokenContract(), Contract  {

    override fun additionalCreateChecks(tx: LedgerTransaction) {
        // Not much to do for this example token.
        val newEHR = tx.outputStates.single() as EHRTokenState
        newEHR.apply {
            require(price > Amount.zero(price.token)) { "Price must be greater than zero." }
        }
    }

    override fun additionalUpdateChecks(tx: LedgerTransaction) {
        val oldEHR = tx.inputStates.single() as EHRTokenState
        val newEHR = tx.outputStates.single() as EHRTokenState
        require(newEHR.price > Amount.zero(newEHR.price.token)) { "price must be greater than zero." }
    }
}