package com.template.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party


@StartableByRPC
@StartableByService
@InitiatingFlow
class RequestKeyForAccounName(
        private val acctName: String
) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        //Create a new account
        val accountInfoStateAndRef = accountService.accountInfo(acctName).single()
        val result = subFlow(RequestKeyForAccount(accountInfoStateAndRef.state.data))

        return result.toString()
    }
}



