package com.template.states

import com.template.contracts.EHRContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party
import java.util.*

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
data class EHRState(val id: String,
                    val patient: Party,
                    val originDoctor: Party,
                    val targetDoctor: Party,
                    val description: String): ContractState {

    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf(originDoctor, patient, targetDoctor)
    /**
     * Helper methods for when building transactions for settling and transferring IOUs.
     * - [pay] adds an amount to the paid property. It does no validation.
     * - [withNewMember] creates a copy of the current state with a newly specified lender. For use when transferring.
     */

//    fun pay(amountToPay: Amount<Currency>) = copy(amount * percentage /100 = amount * percentage.plus(amountToPay))
//    fun withNewMember(newMember: Party) = copy(member1 = newMember)

}

