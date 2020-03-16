package com.template.flows.patient
import co.paralleluniverse.fibers.Suspendable
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.InitiatingFlow
import net.corda.core.identity.Party


@InitiatingFlow
class NotifyOriginDoctorFlow(private val notification: Any, val originDoctor: Party) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        initiateFlow(originDoctor).send(notification)
    }
}





