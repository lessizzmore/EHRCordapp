package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


/**
 * The flow changes status of a PENDING EHRAgreementState to ACTIVE. The flow can be started only by patient.
 *
 * @param EHR EHRShareAgreement state to be activated
 */
@StartableByRPC
@InitiatingFlow
class ActivateEHRFlow(val EHR: StateAndRef<EHRShareAgreementState>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        // Ensure we are the patient.
        check(EHR.state.data.patient != ourIdentity) {
            throw FlowException("Activate EHRShareAgreementState flow must be initiated by patient.")
        }


        // Create activation tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO
        val builder = TransactionBuilder(notary)
                .addInputState(EHR)
                .addOutputState(EHR.state.data.copy(status = EHRShareAgreementStateStatus.ACTIVE), EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(EHRShareAgreementContract.Commands.Activate(), ourIdentity.owningKey)



        // Verify and sign the tx
        builder.verify(serviceHub)
        val selfSignedTx = serviceHub.signInitialTransaction(builder)


        // Sends selfSignedTx to origin doctor
        val targetSession = initiateFlow(EHR.state.data.originDoctor)


        // Finalize transaction, origin doctor doesn't need to sign
        return subFlow(FinalityFlow(selfSignedTx, listOf(targetSession)))
    }
}

@InitiatedBy(ActivateEHRFlow::class)
class ActivateEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val ehrShareAgreementState =
                        stx.coreTransaction.outputStates.single() as EHRShareAgreementState
                check(ehrShareAgreementState.originDoctor == ourIdentity) { "Activated EHRShareAgreement sent to the wrong person"}
            }
        }

        val stx = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSession, expectedTxId = stx.id))
    }
}