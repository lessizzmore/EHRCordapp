package com.template.flows.origindoctor

import co.paralleluniverse.fibers.Suspendable
import com.template.flows.patient.SuspendEHRFlow
import net.corda.core.flows.FlowLogic
import net.corda.core.flows.FlowSession
import net.corda.core.flows.InitiatedBy
import net.corda.core.flows.ReceiveFinalityFlow

@InitiatedBy(SuspendEHRFlow::class)
open class SuspendEHRFlowResponder(val session: FlowSession) : FlowLogic<Unit>() {
    @Suspendable
    override fun call() {
        subFlow(ReceiveFinalityFlow(session))
    }
}