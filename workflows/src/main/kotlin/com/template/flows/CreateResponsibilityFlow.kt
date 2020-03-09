package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.PaymentContract
import com.template.states.ResponsibilityState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

/**
 * This is the flow which handles issuance of new IOUs on the ledger.
 */
@InitiatingFlow
@StartableByRPC
class CreateResponsibilityFlow(val state: ResponsibilityState): FlowLogic<SignedTransaction>() {
    @Suspendable
    override fun call(): SignedTransaction {
        // Step 1. Get a reference to the notary service on our network and our key pair.
        // Note: ongoing work to support multiple notary identities is still in progress.
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // Step 2. Create a new issue command.
        // Remember that a command is a CommandData object and a list of CompositeKeys
        val createCommand = Command(PaymentContract.Commands.Create(), state.participants.map { it.owningKey })

        // Step 3. Create a new TransactionBuilder object.
        val builder = TransactionBuilder(notary = notary)

        // Step 4. Add the iou as an output state, as well as a command to the transaction builder.
        builder.addOutputState(state, PaymentContract.PAYMENT_CONTRACT_ID)
        builder.addCommand(createCommand)

        // Step 5. Verify and sign it with our KeyPair.
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)

        val sessions = (state.participants - ourIdentity).map { initiateFlow(it) }.toSet()
        // Step 6. Collect the other party's signature using the SignTransactionFlow.
        val stx = subFlow(CollectSignaturesFlow(ptx, sessions))

        // Step 7. Assuming no exceptions, we can now finalise the transaction.
        return subFlow(FinalityFlow(stx, sessions))
    }
}

/**
 * This is the flow which signs IOU issuances.
 * The signing is handled by the [SignTransactionFlow].
 */
@InitiatedBy(CreateResponsibilityFlow::class)
class CreateResponsibilityFlowResponder(val flowSession: FlowSession): FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val signedTransactionFlow = object : SignTransactionFlow(flowSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val output = stx.tx.outputs.single().data
                "This must be an IOU transaction" using (output is ResponsibilityState)
            }
        }

        val txWeJustSignedId = subFlow(signedTransactionFlow)

        return subFlow(ReceiveFinalityFlow(otherSideSession = flowSession, expectedTxId = txWeJustSignedId.id))
    }
}