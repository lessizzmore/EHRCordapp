package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.utilities.issuedBy
import com.r3.corda.lib.tokens.contracts.utilities.withNotary
import com.r3.corda.lib.tokens.workflows.flows.rpc.CreateEvolvableTokens
import com.r3.corda.lib.tokens.workflows.flows.rpc.IssueTokens
import com.r3.corda.lib.tokens.workflows.utilities.heldBy
import com.template.states.EHRTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.UniqueIdentifier.Companion.fromString
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import java.util.*


@StartableByRPC
@InitiatingFlow
class CreateAndIssueEHR(
        val owner: Party,
        val data: String,
        val price: Amount<Currency>) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        // create EHR Token state
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        /* Construct the output state */
        val uuid = fromString(UUID.randomUUID().toString())

        val ehrTokenState = EHRTokenState(
                ourIdentity,
                data,
                price,
                listOf(ourIdentity),
                linearId = uuid)
        subFlow(CreateEvolvableTokens(ehrTokenState withNotary notary))


        // issue non-fungible ehr token referencing ehrTokenState held by patient portal
        val issuerParty: Party = ourIdentity
        val ehrPtr = ehrTokenState.toPointer<EHRTokenState>()
        val ehrToken = ehrPtr issuedBy issuerParty heldBy owner
        val stx = subFlow(IssueTokens(listOf(ehrToken)))

        return """
            The non-fungible ehr token is created with UUID: ${uuid}.
            Transaction ID: ${stx.id}
            """.trimIndent()
    }
}