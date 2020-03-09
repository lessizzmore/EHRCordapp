package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PaymentContract
import com.template.states.ResponsibilityState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndContract
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.finance.POUNDS

@InitiatingFlow
@StartableByRPC
class RequestPaymentFlow(val totalAmount: Int,
                         val otherParty: Party) : FlowLogic<SignedTransaction>() {

    // TODO: progressTracker
    override val progressTracker = ProgressTracker()

    @Suspendable
    override fun call(): SignedTransaction {

        // Obtain a reference to the notary we want to use.
        val notary = serviceHub.networkMapCache.notaryIdentities[0]

        // Stage 1.
        // progressTracker.currentStep = GENERATING_TRANSACTION
        // Generate an unsigned transaction.
        val responsibilityState = ResponsibilityState(otherParty, serviceHub.myInfo.legalIdentities.first(), 50, totalAmount.POUNDS)
        val txCommand = Command(PaymentContract.Commands.Create(), responsibilityState.participants.map { it.owningKey })
        val txBuilder = TransactionBuilder(notary)
                .addOutputState(responsibilityState, PaymentContract.PAYMENT_CONTRACT_ID)
                .addCommand(txCommand)

        // Stage 2.
        // progressTracker.currentStep = VERIFYING_TRANSACTION
        // Verify that the transaction is valid.
        txBuilder.verify(serviceHub)

        // Stage 3.
        // progressTracker.currentStep = SIGNING_TRANSACTION
        // Sign the transaction.
        val partSignedTx = serviceHub.signInitialTransaction(txBuilder)

        // Stage 4.
        // progressTracker.currentStep = GATHERING_SIGS
        // Send the state to the counterparty, and receive it back with their signature.
        val otherPartySession = initiateFlow(otherParty)
        val fullySignedTx = subFlow(CollectSignaturesFlow(partSignedTx, setOf(otherPartySession), Flow.Initiator.Companion.GATHERING_SIGS.childProgressTracker()))

        // Stage 5.
        // progressTracker.currentStep = FINALISING_TRANSACTION
        // Notarise and record the transaction in both parties' vaults.
        return subFlow(FinalityFlow(fullySignedTx, setOf(otherPartySession)))

    }
}

@InitiatedBy(RequestPaymentFlow::class)
class RequestPaymentFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(counterpartySession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                // If this node is already participating in an active game, decline the request to start a new one
                val criteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
                val results = serviceHub.vaultService.queryBy<ResponsibilityState>(criteria)
                if (results.states.isNotEmpty()) throw FlowException("A node can only play one game at a time!")
            }
        }
        val txWeJustSigned = subFlow(signedTransactionFlow)
        return subFlow(ReceiveFinalityFlow(counterpartySession, txWeJustSigned.id))
    }
}
