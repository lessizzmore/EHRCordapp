package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder


/**
 * The flow changes status of a PENDING EHRAgreementState to ACTIVE. The flow can be started only by patient.
 *
 */
@StartableByRPC
@InitiatingFlow
class ActivateEHRFlow(val targetDoctor: Party) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        // get input state
        val queryCriteria = QueryCriteria.VaultQueryCriteria(Vault.StateStatus.UNCONSUMED)
        val ehrStateRefToActivate = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.single()

//        // Ensure we are the patient.
//        check(ehrStateRefToActivate.state.data.patient != ourIdentity) {
//            throw FlowException("Activate EHRShareAgreementState flow Party must be initiated by patient.")
//        }

        val activateCommand = Command(EHRShareAgreementContract.Commands.Activate(), listOf(ourIdentity, targetDoctor).map { it.owningKey })

        // Create activation tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO
        val builder = TransactionBuilder(notary)
                .addInputState(ehrStateRefToActivate)
                .addOutputState(ehrStateRefToActivate.state.data.copy(status = EHRShareAgreementStateStatus.ACTIVE), EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(activateCommand)



        // Verify and sign the tx
        builder.verify(serviceHub)
        val ptx = serviceHub.signInitialTransaction(builder)


        // Sends selfSignedTx to origin doctor
        val targetSession = initiateFlow(targetDoctor)
        val stx = subFlow(CollectSignaturesFlow(ptx, listOf(targetSession)))


        // collect signature from originDoctor and finalize the tx
        return subFlow(FinalityFlow(stx, listOf(targetSession)))
    }
}

@InitiatedBy(ActivateEHRFlow::class)
class ActivateEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {

    @Suspendable
    override fun call(): SignedTransaction {

        val signTransactionFlow = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) = requireThat {
                val ehrShareAgreementState =
                        stx.coreTransaction.outputStates.single() as EHRShareAgreementState
                check(ehrShareAgreementState.originDoctor == ourIdentity) { "Activated EHRShareAgreement sent to the wrong person"}
            }
        }

        val stx = subFlow(signTransactionFlow)
        return subFlow(ReceiveFinalityFlow(otherSession, expectedTxId = stx.id))
    }
}