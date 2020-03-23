package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.*
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.*


@InitiatingFlow
@StartableByRPC
class DeleteShareEHEAgreementFlow (
        val patient: Party,
        private val linearId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // Get EHRShareAgreementState to clear.
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(linearId),
                Vault.StateStatus.UNCONSUMED, null)

        val stateToDelete = serviceHub.vaultService
                .queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $linearId not found.")

        val inputEHRShareAgreementState = stateToDelete.state.data


        // Ensure origin doctor is initiator.
        check(inputEHRShareAgreementState.originDoctor != ourIdentity) {
            throw FlowException("Delete EHRShareAgreementState flow must be initiated by origin doctor.")
        }
        check( inputEHRShareAgreementState.status != EHRShareAgreementStateStatus.PENDING) {
            throw FlowException("Cannot delete state that has status other than PENDING.")
        }


        // Create the list of signers and the delete command.
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO code into config instead of getting the first()

        val allSigningKeys = inputEHRShareAgreementState.originDoctor.owningKey


        // Create the tx. Add delete command and input.
        val txBuilder = TransactionBuilder(notary)
                .addInputState(stateToDelete)
                .addCommand(EHRShareAgreementContract.Commands.Delete(), allSigningKeys)


        // Verify and sign the tx, only the origin doctor needs to sign it
        txBuilder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(txBuilder, ourIdentity.owningKey)

        // Finalize the tx.
        return subFlow(FinalityFlow(ptx, emptyList()))
    }
}