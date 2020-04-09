package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.Command
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import java.lang.IllegalArgumentException
import java.util.concurrent.atomic.AtomicReference

@InitiatingFlow
@StartableByRPC
class RequestShareEHRAgreementFlow(
        val sendFromDoctor: String,
        val sendToPatient: String,
        val targetDoctor: String,
        val note: String? = "",
        val attachmentId: String? = ""
): FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        // generate key for tx
        // patient
        val myAccount = accountService.accountInfo(sendFromDoctor).single().state.data
        val myKey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        // origin doctor
        val targetAccount = accountService.accountInfo(sendToPatient).single().state.data
        val targetAcctAnonymousParty = subFlow(RequestKeyForAccount(targetAccount))

        // target doctor
        val targetDAccount = accountService.accountInfo(targetDoctor).single().state.data
        val targetDAcctAnonymousParty = subFlow(RequestKeyForAccount(targetDAccount))

        val notary = serviceHub.networkMapCache.notaryIdentities.first() //TODO code into config instead of getting the first()
        val createCommand = Command(EHRShareAgreementContract.Commands.Create(), listOf(myKey, targetAcctAnonymousParty.owningKey))


        // create state to transfer
        val initialEHRShareAgreementState = EHRShareAgreementState(targetAcctAnonymousParty, AnonymousParty(myKey), targetDAcctAnonymousParty, note, attachmentId)


        // create tx, state + command
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(initialEHRShareAgreementState, EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)

        // locally sign tx
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey, myKey))

        // collect signatures
        val sessionForAcctToSendTo = initiateFlow(targetAccount.host)
        val accountToSendToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAcctToSendTo, targetAcctAnonymousParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToSendToSignature)

        // finalize
        val fullySignedTx =  subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAcctToSendTo).filter { it.counterparty != ourIdentity }))

        return "EHR state send to " + targetAccount.host.name.organisation + "'s " + targetAccount.name
    }
}


@InitiatedBy(RequestShareEHRAgreementFlow::class)
class RequestShareEHRAgreementFlowResponder (

        private val otherSession: FlowSession

) : FlowLogic<Unit>() {

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
