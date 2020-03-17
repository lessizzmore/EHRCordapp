package com.template.states

import com.template.contracts.EHRShareAgreementContract
import net.corda.core.contracts.*
import net.corda.core.identity.Party

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
@BelongsToContract(EHRShareAgreementContract::class)
data class DoctorState(val id: String,
                    val firstName: String,
                    val lastName: String,
                    val patients: List<Party>,
                    val EHRs: List<EHRShareAgreementState>
                    ): ContractState {

    /**
     *  This property holds a list of the nodes which can "use" this state in a valid transaction. In this case, the
     *  lender or the borrower.
     */
    override val participants: List<Party> get() = listOf()

}

