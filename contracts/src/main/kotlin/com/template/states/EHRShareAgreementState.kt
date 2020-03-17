package com.template.states

import com.template.contracts.EHRShareAgreementContract
import net.corda.core.contracts.*
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.Party
import net.corda.core.serialization.CordaSerializable
import net.corda.core.contracts.LinearState as LinearState

@BelongsToContract(EHRShareAgreementContract::class)
data class EHRShareAgreementState (val patient: Party,
                     val originDoctor: Party,
                     val targetDoctor: Party,
                     val note: String? = null,
                     val attachmentHash: SecureHash? = null,
                     val status: EHRShareAgreementStateStatus = EHRShareAgreementStateStatus.PENDING,
                     override val linearId: UniqueIdentifier = UniqueIdentifier()) : LinearState {

    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf(originDoctor, patient)


}

/**
 * Statuses that a EHRAgreementState can go through.
 *
 * [PENDING] - newly submitted state, haven't been approved yet. Pending originDoctor can't share EHRAgreementStates with others
 * [ACTIVE] - active EHRAgreementStates can be shared
 * [SUSPENDED] - Pending originDoctor can't share suspended EHRAgreementStates with others
 */
@CordaSerializable
enum class EHRShareAgreementStateStatus {
    PENDING, ACTIVE, SUSPENDED
}

