package com.template.flows.patient

import co.paralleluniverse.fibers.Suspendable
import net.corda.core.contracts.StateAndRef
import net.corda.core.flows.FinalityFlow
import net.corda.core.flows.InitiatingFlow
import net.corda.core.flows.StartableByRPC
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker

/**
 * The flow changes status of a PENDING or SUSPENDED membership to ACTIVE. The flow can be started only by BNO. BNO can unilaterally
 * activate memberships and no member's signature is required. After a membership is activated, the flow
 * fires-and-forgets [OnMembershipChanged] notification to all business network members.
 *
 * @param membership membership state to be activated
 */
@StartableByRPC
@InitiatingFlow(version = 2)
open class ActivateEHRFlow(val membership: StateAndRef<MembershipState<Any>>) : BusinessNetworkOperatorFlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {
        verifyThatWeAreBNO(membership.state.data)

        val configuration = serviceHub.cordaService(BNOConfigurationService::class.java)

        // create membership activation transaction
        val notary = configuration.notaryParty()
        val builder = TransactionBuilder(notary)
                .addInputState(membership)
                .addOutputState(membership.state.data.copy(status = MembershipStatus.ACTIVE, modified = serviceHub.clock.instant()), MembershipContract.CONTRACT_NAME)
                .addCommand(MembershipContract.Commands.Activate(), ourIdentity.owningKey)

        if (membership.isAttachmentRequired()) builder.addAttachment(membership.getAttachmentIdForGenericParam())

        builder.verify(serviceHub)
        val selfSignedTx = serviceHub.signInitialTransaction(builder)

        val memberSession = initiateFlow(membership.state.data.member)
        val stx = if (memberSession.getCounterpartyFlowInfo().flowVersion == 1) {
            subFlow(FinalityFlow(selfSignedTx))
        } else {
            if (membership.state.data.bno == membership.state.data.member)
                subFlow(FinalityFlow(selfSignedTx, listOf()))
            else
                subFlow(FinalityFlow(selfSignedTx, listOf(memberSession)))
        }

        // We should notify members about changes with the ACTIVATED membership
        val databaseService = serviceHub.cordaService(DatabaseService::class.java)
        val activatedMembership = databaseService.getMembershipOnNetwork(membership.state.data.member, ourIdentity, membership.state.data.networkID)!!
        subFlow(NotifyActiveMembersFlow(OnMembershipChanged(activatedMembership)))

        return stx
    }
}

/**
 * This is a convenience flow that can be easily used from a command line
 *
 * @param party whose membership state to be activated
 */
@InitiatingFlow
@StartableByRPC
open class ActivateMembershipForPartyFlow(val party: Party, val networkID: String?) : BusinessNetworkOperatorFlowLogic<SignedTransaction>() {

    companion object {
        object LOOKING_FOR_MEMBERSHIP_STATE : ProgressTracker.Step("Looking for party's membership state")
        object ACTIVATING_THE_MEMBERSHIP_STATE : ProgressTracker.Step("Activating the membership state")

        fun tracker() = ProgressTracker(
                LOOKING_FOR_MEMBERSHIP_STATE,
                ACTIVATING_THE_MEMBERSHIP_STATE
        )
    }

    override val progressTracker = tracker()

    @Suspendable
    override fun call(): SignedTransaction {
        progressTracker.currentStep = LOOKING_FOR_MEMBERSHIP_STATE
        val stateToActivate = findMembershipStateForParty(party, networkID)

        progressTracker.currentStep = ACTIVATING_THE_MEMBERSHIP_STATE
        return subFlow(ActivateMembershipFlow(stateToActivate))
    }

}