package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
import net.corda.core.contracts.Command
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.Party
import net.corda.core.internal.notary.isConsumedByTheSameTx
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicReference


/**
 * The flow changes status of a PENDING EHRAgreementState to ACTIVE. The flow can be started only by patient.
 *
 */
@StartableByRPC
@InitiatingFlow
class ActivateEHRFlow(
        val whereTo: String,
        val ehrId: UniqueIdentifier
) : FlowLogic<SignedTransaction>() {

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
    override fun call(): SignedTransaction {
        // create a key for tx
        progressTracker.currentStep = GENERATING_KEYS
        val targetAccount = accountService.accountInfo(whereTo).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))


        // get input state
        progressTracker.currentStep = GET_STATE
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToActivate = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")


        val activateCommand = Command(
                EHRShareAgreementContract.Commands.Activate(),
                listOf(ourIdentity.owningKey, targetAcctAnonymousParty.owningKey))

        // Create activation tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        progressTracker.currentStep = GENERATING_TRANSACTION
        val builder = TransactionBuilder(notary)
                .addInputState(ehrStateRefToActivate)
                .addOutputState(ehrStateRefToActivate.state.data.copy(status = EHRShareAgreementStateStatus.ACTIVE), EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(activateCommand)

        // sign tx locally
        progressTracker.currentStep = SIGNING_TRANSACTION
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey))

        progressTracker.currentStep =GATHERING_SIGS
        val sessionForAccountToSentTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSentTo, targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        // finalize
        progressTracker.currentStep =FINALISING_TRANSACTION
        val fullySignedTx =  subFlow(FinalityFlow(
                signedByCounterParty,
                listOf(sessionForAccountToSentTo)
                        .filter { it.counterparty != ourIdentity }))

        return fullySignedTx

    }
}

@InitiatedBy(ActivateEHRFlow::class)
class ActivateEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
//        val accountTransferredTo = AtomicReference<AccountInfo>()
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
//                val keyStateTransferredTo = stx
//                        .coreTransaction
//                        .outRefsOfType(EHRShareAgreementState::class.java)
//                        .first().state.data.originDoctor.owningKey
//                keyStateTransferredTo?.let {
//                    accountTransferredTo.set(accountService.accountInfo(keyStateTransferredTo)?.state?.data)
//                }
//
//                if(accountTransferredTo.get() == null) {
//                    throw IllegalArgumentException("Account to transferred to was not found on this node")
//                }
            }
        }

        val transaction = subFlow(transactionSigner)
        if(otherSession.counterparty != serviceHub.myInfo.legalIdentities.first()) {
            val receivedTx = subFlow(
                    ReceiveFinalityFlow(
                            otherSession,
                            expectedTxId = transaction.id,
                            statesToRecord = StatesToRecord.ALL_VISIBLE
                    )
            )
        }

    }
}