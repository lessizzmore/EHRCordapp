package com.template.flows

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.accounts.contracts.states.AccountInfo
import com.r3.corda.lib.accounts.workflows.accountService
import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
import com.r3.corda.lib.accounts.workflows.flows.ShareStateAndSyncAccounts
import com.r3.corda.lib.accounts.workflows.internal.flows.createKeyForAccount
import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.Command
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.requireThat
import net.corda.core.flows.*
import net.corda.core.identity.AnonymousParty
import net.corda.core.identity.Party
import net.corda.core.node.StatesToRecord
import net.corda.core.node.services.Vault
import net.corda.core.node.services.queryBy
import net.corda.core.node.services.vault.QueryCriteria
import net.corda.core.serialization.CordaSerializable
import net.corda.core.transactions.SignedTransaction
import net.corda.core.transactions.TransactionBuilder
import net.corda.core.utilities.ProgressTracker
import net.corda.core.utilities.unwrap
import java.lang.IllegalArgumentException
import java.security.PublicKey
import java.util.*
import java.util.concurrent.atomic.AtomicReference

@InitiatingFlow
@StartableByRPC
class RequestShareEHRAgreementFlow(
        val whoIam: String,
        val whereTo: String,
        val aboutWho: String,
        val note: String? = "",
        val attachmentId: String? = ""
): FlowLogic<String>() {

    @Suspendable
    override fun call(): String {

        // generate key for tx
        // doctor1
        val myAccount = accountService.accountInfo(whoIam).single().state.data
        val myAccountKey = serviceHub.createKeyForAccount(myAccount).owningKey

        // doctor2
        val targetDAccount = accountService.accountInfo(whereTo).single().state.data
        val targetDKey = serviceHub.createKeyForAccount(targetDAccount).owningKey

        // patient
        val patientAccount = accountService.accountInfo(aboutWho).single().state.data
        val patientAcctAnonParty = subFlow(RequestKeyForAccount(patientAccount))

        val notary = serviceHub.networkMapCache.notaryIdentities.first()
        val createCommand = Command(
                EHRShareAgreementContract.Commands.Create(),
                listOf(myAccountKey, patientAcctAnonParty.owningKey))


        // create state to transfer
        val initialEHRShareAgreementState =
                EHRShareAgreementState(AnonymousParty(myAccountKey), AnonymousParty(targetDKey), patientAcctAnonParty, note, attachmentId)

        // create tx, state + command
        val builder = TransactionBuilder(notary = notary)
        builder.addOutputState(initialEHRShareAgreementState, EHRShareAgreementContract.EHR_CONTRACT_ID)
        builder.addCommand(createCommand)
        builder.verify(serviceHub)


        // locally sign tx
        val locallySignedTx = serviceHub.signInitialTransaction(builder, listOfNotNull(ourIdentity.owningKey, myAccountKey))

        // collect signatures
        val sessionForAcctToSendTo = initiateFlow(patientAcctAnonParty)
        val accountToSendToSignature = subFlow(CollectSignatureFlow(locallySignedTx, sessionForAcctToSendTo, patientAcctAnonParty.owningKey))
        val signedByCounterParty = locallySignedTx.withAdditionalSignatures(accountToSendToSignature)

        // finalize
        val fullySignedTx =  subFlow(FinalityFlow(signedByCounterParty, listOf(sessionForAcctToSendTo).filter { it.counterparty != ourIdentity }))

        return whoIam + " wants to share "+ patientAccount.host.name.organisation + " - "+ patientAccount.name +"'s record to " + targetDAccount.host.name.organisation + "- "+ targetDAccount.name
    }
}


@InitiatedBy(RequestShareEHRAgreementFlow::class)
class RequestShareEHRAgreementFlowResponder (

        private val otherSession: FlowSession

) : FlowLogic<Unit>() {



    @Suspendable
    override fun call() {
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
