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
import net.corda.core.internal.notary.isConsumedByTheSameTx
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicReference


/**
 * The flow changes status of a PENDING EHRAgreementState to ACTIVE. The flow can be started only by patient.
 *
 */
@StartableByRPC
@InitiatingFlow
class ActivateEHRFlow(
        val sendFromPatient: String,
        val sendToDoctor: String,
        val ehrId: UniqueIdentifier
) : FlowLogic<String>() {

    @Suspendable
    override fun call(): String {
        // create a key for tx
        val myAccount = accountService.accountInfo(sendFromPatient).single().state.data
        val myKey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey
        val targetAccount = accountService.accountInfo(sendToDoctor).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))


        // get input state
        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
                null,
                listOf(ehrId),
                Vault.StateStatus.UNCONSUMED, null)
        val ehrStateRefToActivate = serviceHub.vaultService.queryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")


        val activateCommand = Command(
                EHRShareAgreementContract.Commands.Activate(),
                listOf(ourIdentity, targetAcctAnonymousParty).map { it.owningKey })

        // Create activation tx
        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO
        val builder = TransactionBuilder(notary)
                .addInputState(ehrStateRefToActivate)
                .addOutputState(ehrStateRefToActivate.state.data.copy(status = EHRShareAgreementStateStatus.ACTIVE), EHRShareAgreementContract.EHR_CONTRACT_ID)
                .addCommand(activateCommand)

        // sign tx locally
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey))

        val sessionForAccountToSentTo = initiateFlow(targetAccount.host)
        val accountToMoveToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAccountToSentTo, targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToMoveToSignature)

        // finalize
        val fullySignedTx =  subFlow(FinalityFlow(
                signedByCounterParty,
                listOf(sessionForAccountToSentTo)
                        .filter { it.counterparty != ourIdentity }))

        return "send activated EHR to " + targetAccount.host.name.organisation + "'s "+ targetAccount.name

    }
}

@InitiatedBy(ActivateEHRFlow::class)
class ActivateEHRFlowResponder (private val otherSession: FlowSession) : FlowLogic<Unit>() {

    @Suspendable
    override fun call() {
        val accountTransferredTo = AtomicReference<AccountInfo>()
        val transactionSigner = object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                val keyStateTransferredTo = stx
                        .coreTransaction
                        .outRefsOfType(EHRShareAgreementState::class.java)
                        .first().state.data.targetDoctor.owningKey
                keyStateTransferredTo?.let {
                    accountTransferredTo.set(accountService.accountInfo(keyStateTransferredTo)?.state?.data)
                }

                if(accountTransferredTo.get() == null) {
                    throw IllegalArgumentException("Account to transferred to was not found on this node")
                }
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

            //TODO broadcast to receivers
//            val accountInfo = accountTransferredTo.get()
//            if (accountInfo != null) {
//                subFlow()
//            }
        }

    }
}