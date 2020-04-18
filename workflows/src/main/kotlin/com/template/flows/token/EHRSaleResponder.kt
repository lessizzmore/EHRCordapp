package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.ci.workflows.SyncKeyMappingFlowHandler
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import com.r3.corda.lib.tokens.workflows.internal.flows.finality.ObserverAwareFinalityFlowHandler
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.unwrap

@InitiatedBy(EHRSale::class)
class EHRSaleResponder(private val otherSession: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call() {
        // Receive notification with ehr price.
        val priceNotification = otherSession.receive<PriceNotification>().unwrap { it }
        // Generate fresh key, possible change outputs will belong to this key.
        val changeHolder = serviceHub.keyManagementService.freshKeyAndCert(ourIdentityAndCert, false).party.anonymise()
        // Chose state and refs to send back.
        val partyAndAmount = Pair<AbstractParty, Amount<TokenType>>(otherSession.counterparty, priceNotification.amount)
        val (inputs, outputs) =
                DatabaseTokenSelection(serviceHub).generateMove(
                lockId = runId.uuid,
                partiesAndAmounts = listOf(partyAndAmount),
                changeHolder = changeHolder
        )
        subFlow(SendStateAndRefFlow(otherSession, inputs))
        otherSession.send(outputs)
        subFlow(SyncKeyMappingFlowHandler(otherSession))
        subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // We should perform some basic sanity checks before signing the transaction. This step was omitted for simplicity.
            }
        })
        subFlow(ObserverAwareFinalityFlowHandler(otherSession))
    }

}