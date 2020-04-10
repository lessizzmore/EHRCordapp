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
        val whoIam: String,
        val whereTo: String,
        val aboutWho: Party,
        val note: String? = "",
        val attachmentId: String? = ""
): FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        // generate key for tx
        // doctor
        val myAccount = accountService.accountInfo(whoIam).single().state.data
        val myKey = subFlow(NewKeyForAccount(myAccount.identifier.id)).owningKey

        // target doctor
        val targetDAccount = accountService.accountInfo(whereTo).single().state.data
        val targetDAcctAnonymousParty = subFlow(RequestKeyForAccount(targetDAccount))

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val createCommand = Command(EHRShareAgreementContract.Commands.Create(), listOf(myKey, aboutWho.owningKey))


        // create state to transfer
        val initialEHRShareAgreementState = EHRShareAgreementState(AnonymousParty(myKey), targetDAcctAnonymousParty, aboutWho, note, attachmentId)


        // create tx, state + command
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(initialEHRShareAgreementState, EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)

        // locally sign tx
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey, myKey))

        // collect signatures
        val sessionForAcctToSendTo = initiateFlow(aboutWho)
        val accountToSendToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAcctToSendTo, aboutWho.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToSendToSignature)

        // finalize
        val fullySignedTx =  subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAcctToSendTo).filter { it.counterparty != ourIdentity }))

        return whoIam + " wants to share "+ aboutWho.name +"'s record to " + targetDAccount.host.name.organisation + "'s "+ targetDAccount.name
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
