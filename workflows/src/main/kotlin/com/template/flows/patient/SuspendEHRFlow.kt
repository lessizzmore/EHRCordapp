package com.template.flows.patient

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder

@StartableByRPC
@InitiatingFlow
open class SuspendEHRFlow(val EHR: StateAndRef<EHRShareAgreementState>) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        // build suspension transaction
        val builder = TransactionBuilder(notary)
                .addInputState(EHR)
                .addOutputState(EHR.state.data.copy(status = EHRShareAgreementStateStatus.SUSPENDED))
                .addCommand(EHRShareAgreementContract.Commands.Suspend(), ourIdentity.owningKey)

        builder.verify(serviceHub)
        val selfSignedTx = serviceHub.signInitialTransaction(builder)

        val doctorOriginSession = initiateFlow(EHR.state.data.originDoctor)

        val finalisedTx = subFlow(FinalityFlow(selfSignedTx, doctorOriginSession))

        // sending notification to the origin doctor about suspension
        subFlow(NotifyOriginDoctorFlow("EHR suspended by patient", EHR.state.data.originDoctor))
        return finalisedTx
    }
}