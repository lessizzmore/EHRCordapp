package com.template.flows.patient

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRContract
import com.template.flows.origindoctor.RequestShareEHRFlow
import com.template.states.EHRState
import com.template.states.EHRStateStatus
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatedBy(RequestShareEHRFlow::class)
open class RequestShareEHRFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val request = counterpartySession.receive<EHRState>().unwrap { it }
        val originDoctor = counterpartySession.counterparty
        val EHRState = EHRState(ourIdentity, originDoctor,request.targetDoctor, request.description, EHRStateStatus.PENDING)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // issuing PENDING EHRState onto the ledger
        val builder = TransactionBuilder(notary)
                .addOutputState(request, EHRContract.EHR_CONTRACT_ID)
                .addCommand(EHRContract.Commands.Create(), originDoctor.owningKey, ourIdentity.owningKey)
        builder.verify(serviceHub)

        val selfSignedTx = serviceHub.signInitialTransaction(builder)
        return subFlow(ReceiveFinalityFlow(counterpartySession, selfSignedTx.id))
    }
}