package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import com.template.states.EHRTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party


@StartableByRPC
@InitiatingFlow
class CreateAndIssueEHR(
        val owner: Party,
        val data: String,
        val symbol: String,
        val price: Amount<TokenType>) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        // create EHR Token state
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val ehrTokenState = EHRTokenState(
                ourIdentity,
                data,
                price,
                symbol,
                listOf(ourIdentity),
                linearId = UniqueIdentifier())
        subFlow(CreateEvolvableTokens(ehrTokenState withNotary notary))


        // issue non-fungible ehr token referencing ehrTokenState held by patient portal
        val patientPortal: Party = ourIdentity
        val issuerParty: Party = ourIdentity
        val ehrPtr = ehrTokenState.toPointer<EHRTokenState>()
        val ehrToken: NonFungibleToken = ehrPtr issuedBy issuerParty heldBy patientPortal
        val stx = subFlow(IssueTokens(listOf(ehrToken)))

        return """
            The non-fungible ehr token is created with UUID: ${ehrTokenState.linearId.id}.
            Transaction ID: ${stx.id}
            """.trimIndent()
    }
}