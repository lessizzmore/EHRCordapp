package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.IssuedTokenType
import com.r3.corda.lib.tokens.contracts.utilities.heldBy
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.money.GBP
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import net.corda.core.contracts.Amount
import net.corda.core.flows.FlowException
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction


@StartableByRPC
@InitiatingFlow
class CashIssue(
        val issuer: Party,
        val recipient: Party) : FlowLogic<String>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): String {
        subFlow(IssueTokens(listOf(1_000_00.GBP issuedBy issuer heldBy recipient))) // Initiating version of IssueFlow
        return "success"
    }

}