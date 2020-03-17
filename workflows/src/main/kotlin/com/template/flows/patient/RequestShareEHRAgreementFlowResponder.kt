package com.template.flows.patient

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.flows.origindoctor.RequestShareEHRAgreementFlow
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap


@InitiatedBy(RequestShareEHRAgreementFlow::class)
open class RequestShareEHRAgreementFlowResponder(val counterpartySession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        val request = counterpartySession.receive<EHRShareAgreementState>().unwrap { it }
        val originDoctor = counterpartySession.counterparty
        val EHRState = EHRShareAgreementState(ourIdentity, originDoctor,request.targetDoctor, request.description,null, EHRShareAgreementStateStatus.PENDING)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // issuing PENDING EHRState onto the ledger
        val builder = TransactionBuilder(notary)
                .addOutputState(request, EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(EHRShareAgreementContract.Commands.Create(), originDoctor.owningKey, ourIdentity.owningKey)
        builder.verify(serviceHub)

        val selfSignedTx = serviceHub.signInitialTransaction(builder)
        return subFlow(ReceiveFinalityFlow(counterpartySession, selfSignedTx.id))
    }
}