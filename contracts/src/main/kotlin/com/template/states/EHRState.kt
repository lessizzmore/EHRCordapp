package com.template.states

import com.template.contracts.EHRContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import net.corda.core.schemas.MappedSchema
import net.corda.core.schemas.PersistentState
import net.corda.core.schemas.QueryableState
import net.corda.core.serialization.CordaSerializable
import java.lang.IllegalArgumentException

/**
 * The IOU State object, with the following properties:
 * - [pool] The initiating party.
 * - [member] The participating party.
 * - [contract] Holds a reference to the [PaymentContract]
 * - [percentage] determines what percentage of the [amount] is supposed to pay.
 * - [amount] The total amount owed by the [pool] to the [members]
 * - [linearId] A unique id shared by all LinearState states representing the same agreement throughout history within
 *   the vaults of all parties. Verify methods should check that one input and one output share the id in a transaction,
 *   except at issuance/termination.
 */
@BelongsToContract(EHRContract::class)
data class EHRState (val patient: Party,
                    val originDoctor: Party,
                    val targetDoctor: Party,
                    val description: String,
                    val status: EHRStateStatus = EHRStateStatus.PENDING,
                    val linearId: UniqueIdentifier = UniqueIdentifier()) : ContractState {

    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf(originDoctor)
}

/**
 * Statuses that a EHRState can go through.
 *
 * [PENDING] - newly submitted state, haven't been approved yet. Pending originDoctor can't share EHRState with others
 * [ACTIVE] - active EHRStates can be shared
 * [SUSPENDED] - Pending originDoctor can't share suspended EHRState with others
 */
@CordaSerializable
enum class EHRStateStatus {
    PENDING, ACTIVE, SUSPENDED
}

