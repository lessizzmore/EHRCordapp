package com.template.states

import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.serialization.CordaSerializable
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

@CordaSerializable
object EHRStateSchema : MappedSchema(schemaFamily = EHRState::class.java, version = 1, mappedTypes = listOf(PersistentEHRState::class.java)) {
    @Entity
    @Table(name = "ehr_states")
    class PersistentEHRState(
            @Column(name = "patient_name")
            var patient: Party,
            @Column(name = "doctor_name")
            var originDoctor: Party,
            @Column(name = "status")
            var status: EHRStateStatus) : PersistentState()
}

