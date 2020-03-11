package com.template.flows.patient
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party


/**
 * Flow that is used by BNO to notify active BN members about changes to the membership list.
 */
class NotifyDoctorFlow(private val notification: Any) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
       subFlow(NotifyDoctorFlow(notification))
    }
}

/**
 * Flow that is used by BNO to notify a BN member about changes to the membership list.
 */
@InitiatingFlow
class NotifyMemberFlow(private val notification: Any, private val originDoctor: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        initiateFlow(originDoctor).send(notification)
    }
}



