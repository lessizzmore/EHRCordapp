//package com.template.schemas
//
//import com.template.states.EHRShareAgreementStateStatus
//import net.corda.core.crypto.SecureHash
//import net.corda.core.identity.Party
//import net.corda.core.schemas.MappedSchema
//import net.corda.core.schemas.PersistentState
//import org.hibernate.annotations.Type
//import java.util.*
//import javax.persistence.Column
//import javax.persistence.Entity
//import javax.persistence.Table
//
//object EHRShareAgreementSchema
//object EHRShareAgreementSchemaV1 : MappedSchema(schemaFamily = EHRShareAgreementSchema.javaClass, version = 1, mappedTypes = listOf(PersistentEHRShareAgreementState::class.java)) {
//    @Entity
//    @Table(name = "ehr_share_agreement_states")
//    class PersistentEHRShareAgreementState(
//            @Column(name = "patient")
//            var patient: Party,
//
//            @Column(name = "originDoctor")
//            var originDoctor: Party,
//
//            @Column(name = "targetDoctor")
//            var targetDoctor: Party,
//
//            @Column(name = "board")
//            var notes: String,
//
//            @Column(name = "attachmentId")
//            var attachmentId: SecureHash,
//
//            @Column(name = "status")
//            var status: EHRShareAgreementStateStatus,
//
//            @Column(name = "linearId")
//            @Type(type = "uuid-char")
//            var linearId: UUID
//    ) : PersistentState()
//
//    override val migrationResource = "eHRShareAgreementSchema.changelog.init"
//}
