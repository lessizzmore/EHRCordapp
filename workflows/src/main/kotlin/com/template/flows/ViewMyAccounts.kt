package com.template.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewMyAccounts() : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        /** Performs a vault query which returns all accounts hosted by the calling node. */
        // returns List<StateAndRef<AccountInfo>>
        val results = accountService.ourAccounts().map { it.state.data.name }
        return results
    }
}



