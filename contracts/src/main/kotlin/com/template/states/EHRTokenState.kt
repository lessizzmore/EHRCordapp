package com.template.states

import com.r3.corda.lib.tokens.contracts.states.EvolvableTokenType
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.template.contracts.EHRContract
import net.corda.core.contracts.Amount
import net.corda.core.contracts.BelongsToContract
import net.corda.core.contracts.LinearState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.Party
import java.math.BigDecimal
import java.util.*

// A token representing a EHR on ledger.
@BelongsToContract(EHRContract::class)
data class EHRTokenState(
        val issuer: Party,
        val data: String?="",
        val price: Amount<Currency>,
        override val maintainers: List<Party>,
        override val fractionDigits: Int = 0,
        override val linearId: UniqueIdentifier
) : EvolvableTokenType() {
    companion object {
        val contractId = this::class.java.enclosingClass.canonicalName
    }
}