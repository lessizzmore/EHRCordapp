package com.template.flows


import net.corda.core.flows.*
import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.ShareAccountInfo
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party


@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareAccount(
        private val acctNameShared: String,
        private val shareTo: Party
        ) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        val allMyAccounts = accountService.ourAccounts()
//        val sharedAccount = allMyAccounts.single { it.state.data.name == acctNameShared }.state.data.identifier.id
        val sharedAccountStateAndRef = allMyAccounts.single { it.state.data.name == acctNameShared }

//        accountService.shareAccountInfoWithParty(sharedAccount,shareTo)
        subFlow(ShareAccountInfo(sharedAccountStateAndRef, listOf(shareTo)))

        return "Shared " + sharedAccountStateAndRef.state.data.name + sharedAccountStateAndRef.state.data.identifier
    }
}



