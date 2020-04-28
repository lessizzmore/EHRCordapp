package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction


/**
 * Flow class to issue fiat currency. FiatCurrency is defined in the TokenSDK and is issued as a Fungible Token. This constructor takes the currecy code
 * for the currency to be issued, the amount of the currency to be issued and the recipient as input parameters.
 */
@InitiatingFlow
@StartableByRPC
class FiatCurrencyIssue(
        private val currency: String,
        private val amount: Long,
        private val recipient: Party) : FlowLogic<String>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {
        /* Create an instance of the fiat currency token */
        val token = getInstance(currency)

        /* Create an instance of IssuedTokenType for the fiat currency */
        val issuedTokenType = IssuedTokenType(ourIdentity, token)

        /* Create an instance of FungibleToken for the fiat currency to be issued */
        val fungibleToken = FungibleToken(Amount(amount, issuedTokenType), recipient, null)

        subFlow(IssueTokens(listOf(fungibleToken), listOf(recipient)))

        val realAmount = amount / 100

        return "$ourIdentity issued $realAmount $currency to $recipient."
    }

}