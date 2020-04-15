package com.template.flows



import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import java.security.PublicKey


@StartableByRPC
@StartableByService
@InitiatingFlow
class ViewAllAccountsHosted() : FlowLogic<List<String>>() {

    @Suspendable
    override fun call(): List<String> {
        /** Performs a vault query which returns all accounts hosted by the calling node. */
        // returns List<StateAndRef<AccountInfo>>
        val results = accountService.allAccounts()
//        val accountKeys = results.map { accountService.accountKeys(it.state.data.identifier.id)}
        val print = results.map { it.state.data.toString() to accountService.accountKeys(it.state.data.identifier.id).toString()}.toString()
        return listOf(print)
    }
}



