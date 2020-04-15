package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria


@StartableByRPC
@StartableByService
@InitiatingFlow
class ShareStateSyncAcct(
        val ehrId: UniqueIdentifier,
        private val shareTo: Party
) : FlowLogic<String>(){

    @Suspendable
    override fun call(): String {

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToActivate = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
        subFlow(ShareStateAndSyncAccounts(ehrStateRefToActivate, shareTo))

        return "Shared state and sync accounts with$shareTo"

    }
}



