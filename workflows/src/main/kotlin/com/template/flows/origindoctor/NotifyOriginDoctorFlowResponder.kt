package com.template.flows.origindoctor

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.patient.NotifyOriginDoctorFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.utilities.unwrap


@InitiatedBy(NotifyOriginDoctorFlow::class)
open class NotifyOriginDoctorFlowResponder(val session: FlowSession) : FlowLogic<Any>() {

    @Suspendable
    override fun call(): Any {
        val notification = session.receive<Any>().unwrap { it }
        return notification
    }
}