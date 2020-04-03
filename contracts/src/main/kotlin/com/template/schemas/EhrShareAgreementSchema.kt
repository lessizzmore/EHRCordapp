package com.template.schemas

import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import org.hibernate.annotations.Type
import java.util.*
import javax.persistence.Column
import javax.persistence.Entity
import javax.persistence.Table

object EhrShareAgreementSchema
object EhrShareAgreementSchemaV1 : MappedSchema(
        schemaFamily = EhrShareAgreementSchema.javaClass,
        version = 1,
        mappedTypes = listOf(PersistentEhrShareAgreementState::class.java)) {
    @Entity
    @Table(name = "ehr_share_agreement_states")
    class PersistentEhrShareAgreementState(
            @Column(name = "patient")
            var patient: Party,

            @Column(name = "originDoctor")
            var originDoctor: Party,

            @Column(name = "targetDoctor")
            var targetDoctor: Party,

            @Column(name = "note")
            var note: String?,

            @Column(name = "attachmentId")
            var attachmentId: String?,

            @Column(name = "status")
            var status: EHRShareAgreementStateStatus,

            @Column(name = "linearId")
            @Type(type = "uuid-char")
            var linearId: UUID
    ) : PersistentState()

    override val migrationResource = "ehr-share-agreement-schema.changelog-init"
}
