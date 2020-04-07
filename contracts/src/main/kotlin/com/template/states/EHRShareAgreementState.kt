package com.template.states

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.template.contracts.EHRShareAgreementContract
import com.template.schemas.EhrShareAgreementSchemaV1
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import net.corda.core.utilities.toBase58String
import net.corda.core.contracts.LinearState as LinearState


// target doctor is an observer
@BelongsToContract(EHRShareAgreementContract::class)
data class EHRShareAgreementState(val patient: Party,
                                  val originDoctor: Party,
                                  val targetDoctor: Party,
                                  val note: String? = "",
                                  val attachmentId: String? = "",
                                  val status: EHRShareAgreementStateStatus = EHRShareAgreementStateStatus.PENDING,
                                  override val linearId: UniqueIdentifier = UniqueIdentifier()
) : LinearState, QueryableState {

    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOfNotNull(originDoctor, patient)
    fun getPatientParty(): Party { return patient }
    fun getOriginDoctorParty() : Party {return originDoctor}
    override fun supportedSchemas(): Iterable<MappedSchema> = listOf(EhrShareAgreementSchemaV1)
    override fun generateMappedObject(schema: MappedSchema): PersistentState {
        if (!(schema is EhrShareAgreementSchemaV1)) throw Exception()
        return EhrShareAgreementSchemaV1
                .PersistentEhrShareAgreementState(
                        patient,
                        originDoctor,
                        targetDoctor,
                        note= note,
                        attachmentId = attachmentId,
                        status = status,
                        linearId = linearId.id
        )
    }
    override fun toString(): String {
        return "EHRShareAgreementState(patient=$patient, originDoctor=$originDoctor, targetDoctor=$targetDoctor, note=$note, attachmentId=$attachmentId, status=$status, linearId=$linearId)"
    }
}

/**
 * Statuses that a EHRAgreementState can go through.
 *
 * [PENDING] - newly submitted state, haven't been approved yet. Pending originDoctor can't share EHRAgreementStates with others
 * [ACTIVE] - active EHRAgreementStates can be shared
 * [SUSPENDED] - Pending originDoctor can't share suspended EHRAgreementStates with others
 */
@CordaSerializable
enum class EHRShareAgreementStateStatus(s: String) {
    PENDING ("pending"),
    ACTIVE("active"),
    SUSPENDED("suspended"),
    SHARED("shared")
}


