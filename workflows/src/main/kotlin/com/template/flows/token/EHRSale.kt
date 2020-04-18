package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveNonFungibleTokens
import com.r3.corda.lib.tokens.workflows.flows.move.addMoveTokens
import com.r3.corda.lib.tokens.workflows.internal.flows.distribution.UpdateDistributionListFlow
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlow
import com.r3.corda.lib.tokens.workflows.utilities.ourSigningKeys
import com.template.states.EHRTokenState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import java.util.*

@StartableByRPC
@InitiatingFlow
class EHRSale(
        val ehrId: UniqueIdentifier,
        val buyer: Party
        ) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateAndRef =
                serviceHub.vaultService.queryBy<EHRTokenState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
        val ehrState = ehrStateAndRef.state.data
        val builder = TransactionBuilder(notary)

        addMoveNonFungibleTokens(builder, serviceHub, ehrState.toPointer<EHRTokenState>(), buyer)

        val buyerSession = initiateFlow(buyer)
        buyerSession.send(PriceNotification(ehrStateAndRef.state.data.price))


        val inputs = subFlow(ReceiveStateAndRefFlow<FungibleToken>(buyerSession))
        val outputs = buyerSession.receive<List<FungibleToken>>().unwrap { it }
        addMoveTokens(builder, inputs, outputs)

        // subFlow(SyncKeyMappingFlow(session, txBuilder.toWireTransaction(serviceHub)))

        // Because states on the transaction can have confidential identities on them, we need to sign them with corresponding keys.
        val ourSigningKeys = builder.toLedgerTransaction(serviceHub).ourSigningKeys(serviceHub)
        val initialStx = serviceHub.signInitialTransaction(builder, signingPubKeys = ourSigningKeys)
        // Collect signatures from the new house owner.
        val stx = subFlow(CollectSignaturesFlow(initialStx, listOf(buyerSession), ourSigningKeys))
        //Update distribution list.
        subFlow(UpdateDistributionListFlow(stx))
        // Finalise transaction! If you want to have observers notified, you can pass optional observers sessions.
        subFlow(ObserverAwareFinalityFlow(stx, listOf(buyerSession)))

        return  "success"

    }
}

@CordaSerializable
data class PriceNotification(val amount: Amount<TokenType>)