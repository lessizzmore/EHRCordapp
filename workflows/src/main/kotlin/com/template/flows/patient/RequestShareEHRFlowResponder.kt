package com.template.flows.patient

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRContract
import com.template.flows.doctor.RequestShareEHRFlow
import com.template.flows.doctor.SharingRequest
import com.template.states.EHRState
import net.corda.core.flows.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.unwrap
import javax.persistence.PersistenceException

/**
 * The flow issues a PENDING membership state onto the ledger. After the state is issued, the BNO is supposed to perform
 * required governance / KYC checks / paperwork and etc. After all of the required activities are completed, the BNO can activate membership
 * via [ActivateMembershipFlow].
 *
 * The flow supports automatic membership activation via [MembershipAutoAcceptor].
 *
 * TODO: remove MembershipAutoAcceptor in favour of flow overrides when Corda 4 is released
 */
@InitiatedBy(RequestShareEHRFlow::class)
open class RequestShareEHRFlowResponder(val session: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call() {
        val request = session.receive<EHRState>().unwrap { it }
        val originDoctor = session.counterparty
        val EHRState = EHRState(ourIdentity, originDoctor,request.targetDoctor, request.description, request.status)
        val notary = serviceHub.networkMapCache.notaryIdentities.first()

        // issuing PENDING membership state onto the ledger
        val builder = TransactionBuilder(notary)
                .addOutputState(request, EHRContract.EHR_CONTRACT_ID)
                .addCommand(MembershipContract.Commands.Request(), counterparty.owningKey, ourIdentity.owningKey)



    }

    /**
     * Override this method to automatically accept memberships
     * See: https://docs.corda.net/head/flow-overriding.html
     */
    @Suspendable
    protected open fun activateRightAway(membershipState: MembershipState<Any>, configuration: BNOConfigurationService): Boolean {
        return false
    }

    /**
     * Override this method to add custom verification membership metadata verifications.
     * See: https://docs.corda.net/head/flow-overriding.html
     */
    @Suspendable
    protected open fun verifyTransaction(builder: TransactionBuilder) {
        builder.verify(serviceHub)
    }
}