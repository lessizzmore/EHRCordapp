package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.*
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker


/**
 * The flow changes status of a PENDING EHRAgreementState to ACTIVE. The flow can be started only by patient.
 *
 */
@StartableByRPC
@InitiatingFlow
class ApproveEHRFlow(
        val whoIam: String,
        val whereTo: String,
        val ehrId: UniqueIdentifier
) : FlowLogic<String>() {

    companion object {
        object GENERATING_KEYS : ProgressTracker.Step("Generating Keys for transactions.")
        object GET_STATE : ProgressTracker.Step("Retrieving state.")
        object GENERATING_TRANSACTION : ProgressTracker.Step("Generating tx.")
        object VERIFYING_TRANSACTION : ProgressTracker.Step("Verifying contract constraints.")
        object SIGNING_TRANSACTION : ProgressTracker.Step("Signing transaction with our private key.")
        object GATHERING_SIGS : ProgressTracker.Step("Gathering the counterparty's signature.") {
            override fun childProgressTracker() = CollectSignaturesFlow.tracker()
        }

        object FINALISING_TRANSACTION : ProgressTracker.Step("Obtaining notary signature and recording transaction.") {
            override fun childProgressTracker() = FinalityFlow.tracker()
        }

        fun tracker() = ProgressTracker(
                GENERATING_KEYS,
                GET_STATE,
                GENERATING_TRANSACTION,
                VERIFYING_TRANSACTION,
                SIGNING_TRANSACTION,
                GATHERING_SIGS,
                FINALISING_TRANSACTION
        )
    }

    override val progressTracker = tracker()


    @Suspendable
    override fun call(): String {
        // create a key for tx
        progressTracker.currentStep = GENERATING_KEYS

        // patient
        val myAccountStateAndRef = accountService.accountInfo(whoIam).single()
        val myAccountKey = serviceHub.createKeyForAccount(myAccountStateAndRef.state.data).owningKey

        // doctor 1
        val targetDAccountStateAndRef = accountService.accountInfo(whereTo).single()
        val targetDAnonParty =  subFlow(RequestKeyForAccount(targetDAccountStateAndRef.state.data))


        // get input state
        progressTracker.currentStep = GET_STATE
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToActivate = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")

        val approveCommand = Command(
                EHRShareAgreementContract.Commands.Approve(),
                listOf(ourIdentity.owningKey, targetDAnonParty.owningKey))

        // Create activation tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        progressTracker.currentStep = GENERATING_TRANSACTION
        val builder = TransactionBuilder(notary)
                .addInputState(ehrStateRefToActivate)
                .addOutputState(ehrStateRefToActivate.state.data.copy(status = EHRShareAgreementStateStatus.APPROVED), EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(approveCommand)
        builder.verify(serviceHub)


        // sign tx locally
        progressTracker.currentStep = SIGNING_TRANSACTION
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey, myAccountKey))

        progressTracker.currentStep =GATHERING_SIGS
        val sessionForAccountToSentTo = initiateFlow(targetDAnonParty)

        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSentTo, targetDAnonParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        // finalize
        progressTracker.currentStep =FINALISING_TRANSACTION
        subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAccountToSentTo).filter { it.counterparty != ourIdentity }))
        subFlow(ShareStateSyncAcct(ehrStateRefToActivate.state.data.linearId, targetDAccountStateAndRef.state.data.host))
        return "$whoIam approves EHR sharing request. \n ehrId: ${ehrStateRefToActivate.state.data.linearId.id}"

    }
}

@InitiatedBy(ApproveEHRFlow::class)
class ApproveEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {

        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
            }
        }
        val transaction = subFlow(transactionSigner)
            subFlow(
                    ReceiveFinalityFlow(
                            otherSession,
                            expectedTxId = transaction.id,
                            statesToRecord = StatesToRecord.ALL_VISIBLE
                    )
            )
        }
}