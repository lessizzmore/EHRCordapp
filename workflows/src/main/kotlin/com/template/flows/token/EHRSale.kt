package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.template.states.EHRTokenState
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap

@StartableByRPC
@InitiatingFlow
class EHRSale(
        val tokenId: UniqueIdentifier,
        val buyer: Party
        ) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(tokenId),
                Vault.StateStatus.UNCONSUMED,
                null)
        val ehrStateAndRef =
                serviceHub.vaultService.queryBy<EHRTokenState>(queryCriteria).states.last()
        val ehrState = ehrStateAndRef.state.data
        val builder = TransactionBuilder(notary)

        addMoveNonFungibleTokens(builder, serviceHub, ehrState.toPointer<EHRTokenState>(), buyer)

        val buyerSession = initiateFlow(buyer)
        buyerSession.send(ehrState.price)


        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))
        val outputs = buyerSession.receive<List<FungibleToken>>().unwrap { it }
        addMoveTokens(builder, inputs, outputs)

        // Because states on the transaction can have confidential identities on them, we need to sign them with corresponding keys.

        /* Sign the transaction with your private */
        val initialStx = serviceHub.signInitialTransaction(builder, ourIdentity.owningKey)
        // Collect signatures from the new house owner.
        val stx = subFlow(CollectSignaturesFlow(initialStx, listOf(buyerSession)))
        //Update distribution list.
        subFlow(UpdateDistributionListFlow(stx))
        // Finalise transaction! If you want to have observers notified, you can pass optional observers sessions.
        subFlow(FinalityFlow(stx, listOf(buyerSession)))

        return """
            The ehr is sold to ${buyer.name.organisation}
            Transaction ID: ${stx.id}
            """.trimIndent()

    }
}