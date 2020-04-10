package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.template.states.EHRShareAgreementState
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.node.services.vault.QueryCriteria


@StartableByRPC
@InitiatingFlow
class ViewByAccount(
        val acctname : String
) : FlowLogic<List<EHRShareAgreementState>>() {
    @Suspendable
    override fun call(): List<EHRShareAgreementState> {
        val myAccount = accountService.accountInfo(acctname).single().state.data
        val criteria = QueryCriteria.VaultQueryCriteria(
                externalIds = listOf(myAccount.identifier.id)
        )
        val states = serviceHub.vaultService.queryBy(
                contractStateType = EHRShareAgreementState::class.java,
                criteria = criteria
        ).states.map { it.state.data }
        return states
    }
}