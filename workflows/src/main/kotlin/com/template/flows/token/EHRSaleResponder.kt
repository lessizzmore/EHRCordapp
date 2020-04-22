package com.template.flows.token

import co.paralleluniverse.fibers.Suspendable
import com.r3.corda.lib.tokens.contracts.types.TokenType
import com.r3.corda.lib.tokens.money.FiatCurrency.Companion.getInstance
import com.r3.corda.lib.tokens.selection.TokenQueryBy
import com.r3.corda.lib.tokens.selection.database.config.MAX_RETRIES_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.PAGE_SIZE_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.RETRY_CAP_DEFAULT
import com.r3.corda.lib.tokens.selection.database.config.RETRY_SLEEP_DEFAULT
import com.r3.corda.lib.tokens.selection.database.selector.DatabaseTokenSelection
import net.corda.core.contracts.Amount
import net.corda.core.flows.*
import net.corda.core.identity.AbstractParty
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.UntrustworthyData
import net.corda.core.utilities.unwrap
import java.util.*

@InitiatedBy(EHRSale::class)
class EHRSaleResponder(private val otherSession: FlowSession) : FlowLogic<SignedTransaction>() {
    @Suspendable
    @Throws(FlowException::class)
    override fun call(): SignedTransaction {
        // Receive notification with ehr price.


        /* Recieve the valuation of the house */
        val priceInfo = otherSession.receive<Amount<Currency>>().unwrap { it }
        // Chose state and refs to send back.
        val amountTokenType = Amount(priceInfo.quantity, getInstance(priceInfo.token.currencyCode))
        val partyAndAmount = Pair<AbstractParty, Amount<TokenType>>(otherSession.counterparty, amountTokenType)

        /* Create an instance of the TokenSelection object, it is used to select the token from the vault and generate the proposal for the movement of the token
        *  The constructor takes the service hub to perform vault query, the max-number of retries, the retry sleep interval, and the retry sleep cap interval. This
        *  is a temporary solution till in-memory token selection in implemented.
        * */
        val tokenSelection = DatabaseTokenSelection(
                serviceHub,
                MAX_RETRIES_DEFAULT,
                RETRY_SLEEP_DEFAULT,
                RETRY_CAP_DEFAULT,
                PAGE_SIZE_DEFAULT
        )
        val inputsAndOutputs =
                tokenSelection.generateMove(listOf(partyAndAmount), ourIdentity, TokenQueryBy(), lockId = runId.uuid)


        subFlow(SendStateAndRefFlow(otherSession, inputsAndOutputs.first))
        otherSession.send(inputsAndOutputs.second)
        subFlow(object : SignTransactionFlow(otherSession) {
            override fun checkTransaction(stx: SignedTransaction) {
                // We should perform some basic sanity checks before signing the transaction. This step was omitted for simplicity.
            }
        })
        return subFlow(ReceiveFinalityFlow(otherSession))
    }

}