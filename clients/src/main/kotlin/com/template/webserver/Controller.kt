package com.template.webserver

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.template.flows.*
import com.template.flows.token.CreateAndIssueEHR
import com.template.flows.token.EHRSale
import com.template.flows.token.FiatCurrencyIssue
import com.template.states.EHRShareAgreementState
import com.template.states.EHRTokenState
import net.corda.core.contracts.Amount.Companion.parseCurrency
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.util.*
import javax.servlet.http.HttpServletRequest
import kotlin.collections.ArrayList


/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */
@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy: CordaRPCOps = rpc.proxy



    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["status"])
    private fun isAlive() = "Up and running!"


    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["my-accounts"])
    private fun getMyAccounts(): List<String> {
        val result = proxy.startFlowDynamic(ViewMyAccounts::class.java).returnValue.get()
        return result
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["create-account"])
    private fun createNewAccount( request: HttpServletRequest): ResponseEntity<String> {
        val accountName = request.getParameter("accountName")
                ?: return ResponseEntity.badRequest().body("Query parameter 'accountName' must not be null.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::CreateNewAccount,
                    accountName
                    ).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["share-account"])
    private fun shareAccount(
            request: HttpServletRequest): ResponseEntity<String> {
        val accountName = request.getParameter("accountName")
        val shareTo = request.getParameter("shareTo")

        if(accountName == null){
            return ResponseEntity.badRequest().body("Query parameter 'accountName' must not be null.\n")
        }
        if(shareTo == null){
            return ResponseEntity.badRequest().body("Query parameter 'shareTo' must not be null.\n")
        }

        val sharedTo = proxy.partiesFromName(shareTo, false).singleOrNull()
                ?:throw  IllegalArgumentException("No exact match found for Party name ${shareTo}.")
        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::ShareAccount,
                    accountName,
                    sharedTo).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["view-account/{acctName}"])
    private fun viewAccount(@PathVariable acctName: String): List<EHRShareAgreementState>? {
        val result = proxy.startFlowDynamic(ViewByAccount::class.java, acctName).returnValue.get()
        return result
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["ehrs"])
    private fun getEHRs(): ResponseEntity<Any?> {
        return try {
            val stateRefs = proxy.vaultQueryBy<EHRShareAgreementState>().states
            val states = ArrayList<EHRShareAgreementState>()
            stateRefs.forEach {
                states.add(it.state.data)
            }
            ResponseEntity.status(HttpStatus.OK).body(states)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["tokens"])
    private fun getEHRTokens(): ResponseEntity<Any?> {
        return try {
            val stateRefs = proxy.vaultQueryBy<EHRTokenState>().states
            val states = ArrayList<EHRTokenState>()
            stateRefs.forEach {
                states.add(it.state.data)
            }
            ResponseEntity.status(HttpStatus.OK).body(states)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["/ehrs"])
    private fun getEHRsByAccount(request: HttpServletRequest): ResponseEntity<Any?> {
            val accountName = request.getParameter("acctname")
            return try {
                val signedTx = proxy.startTrackedFlow(
                        ::ViewByAccount,
                        accountName).returnValue.getOrThrow()
                ResponseEntity
                        .status(HttpStatus.OK)
                        .body(signedTx)
            }catch (ex: Throwable) {
                logger.error(ex.message, ex)
                ResponseEntity.badRequest().body(ex.message!!)
            }
    }

    @PostMapping(value = ["request"])
    private fun request(request: HttpServletRequest): ResponseEntity<String> {
        val d1 = request.getParameter("whoIam")
        val d2 = request.getParameter("whereTo")
        val patient = request.getParameter("aboutWho")
        val note = request.getParameter("note")
        val attachmentId = request.getParameter("attachmentId")


        if(patient == null){
            return ResponseEntity.badRequest().body("Query parameter 'aboutWho' must not be null.\n")
        }
        if(d1 == null){
            return ResponseEntity.badRequest().body("Query parameter 'whoIam' must not be null.\n")
        }
        if(d2 == null){
            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
        }

        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::RequestShareEHRAgreementFlow,
                    d1, d2, patient, note, attachmentId).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["approve"])
    fun approvePendingEHR (request: HttpServletRequest): ResponseEntity<String> {
        val whoIam = request.getParameter("whoIam")
        val whereTo = request.getParameter("whereTo")
        val ehrId = request.getParameter("ehrId")
        val ehrState = UniqueIdentifier.fromString(ehrId)

        if(whoIam == null){
            return ResponseEntity.badRequest().body("Query parameter 'whoIam' must not be null.\n")
        }
        if(whereTo == null){
            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
        }
        if(ehrId == null){
            return ResponseEntity.badRequest().body("Query parameter 'ehrId' must not be null.\n")
        }
        return try {
            val signedTx = proxy.startTrackedFlow(
                ::ApproveEHRFlow,
                whoIam,
                whereTo,
                ehrState).returnValue.getOrThrow()
        ResponseEntity
                .status(HttpStatus.OK)
                .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["reject"])
    fun rejectPendingEHR (request: HttpServletRequest): ResponseEntity<String> {
        val whoIam = request.getParameter("whoIam")
        val whereTo = request.getParameter("whereTo")
        val ehrId = request.getParameter("ehrId")
        val ehrState = UniqueIdentifier.fromString(ehrId)

        if(whoIam == null){
            return ResponseEntity.badRequest().body("Query parameter 'whoIam' must not be null.\n")
        }
        if(whereTo == null){
            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
        }
        if(ehrId == null){
            return ResponseEntity.badRequest().body("Query parameter 'ehrId' must not be null.\n")
        }
        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::RejectEHRFlow,
                    whoIam,
                    whereTo,
                    ehrState).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["share"])
    fun shareEHR (request: HttpServletRequest): ResponseEntity<String> {
        val whoIam = request.getParameter("whoIam")
        val observer = request.getParameter("observer")
        val ehrId = request.getParameter("ehrId")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::ShareEHRFlow,
                    whoIam,
                    observer,
                    ehrState).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["issue-cash"])
    fun issueCash (request: HttpServletRequest): ResponseEntity<String> {
        val currency = request.getParameter("currency")
        val amount = request.getParameter("amount").toLong()
        val recipient = request.getParameter("recipient")


        if(currency == null){
            return ResponseEntity.badRequest().body("Query parameter 'currency' must not be null.\n")
        }
        if(amount == null){
            return ResponseEntity.badRequest().body("Query parameter 'whoIam' must not be null.\n")
        }

        if(recipient == null){
            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
        }


        val partyX500Name = CordaX500Name.parse(recipient)
        val partyParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $recipient cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::FiatCurrencyIssue,
                    currency,
                    amount,
                    partyParty).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["issue-ehr-token"])
    fun issueEHR (request: HttpServletRequest): ResponseEntity<String> {
        val owner = request.getParameter("owner")
        val data = request.getParameter("data")
        val price = request.getParameter("price")

        if(owner == null){
            return ResponseEntity.badRequest().body("Query parameter 'currency' must not be null.\n")
        }

        if(price == null){
            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
        }

        val parsedPrice = parseCurrency(price)

        val partyX500Name = CordaX500Name.parse(owner)
        val partyParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $owner cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::CreateAndIssueEHR,
                    partyParty,
                    data,
                    parsedPrice).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }


    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["sell-ehr"])
    fun sellEHR (request: HttpServletRequest): ResponseEntity<String> {
        val buyer = request.getParameter("buyer")
        val tokenId = request.getParameter("tokenId")
        val ehrState = UniqueIdentifier.fromString(tokenId)
        if(buyer == null){
            return ResponseEntity.badRequest().body("Query parameter 'buyer' must not be null.\n")
        }

        if(tokenId == null){
            return ResponseEntity.badRequest().body("Query parameter 'tokenId' must not be null.\n")
        }

        val partyX500Name = CordaX500Name.parse(buyer)
        val partyParty = proxy.wellKnownPartyFromX500Name(partyX500Name) ?: return ResponseEntity.badRequest().body("Party named $buyer cannot be found.\n")

        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::EHRSale,
                    ehrState,
                    partyParty).returnValue.getOrThrow()
            ResponseEntity
                    .status(HttpStatus.OK)
                    .body(signedTx)
        }catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @GetMapping(value= ["fungible-tokens"])
    fun getFungibleTokens(request: HttpServletRequest): ResponseEntity<Any?> {
        return try {
            val stateRefs = proxy.vaultQueryBy<FungibleToken>().states
            val states = ArrayList<FungibleToken>()
            stateRefs.forEach {
                states.add(it.state.data)
            }
            ResponseEntity.status(HttpStatus.OK).body(states)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @GetMapping(value= ["nonfungible-tokens"])
    fun getnonFungibleTokens(request: HttpServletRequest): ResponseEntity<Any?> {
        return try {
            val stateRefs = proxy.vaultQueryBy<NonFungibleToken>().states
            val states = ArrayList<NonFungibleToken>()
            stateRefs.forEach {
                states.add(it.state.data)
            }
            ResponseEntity.status(HttpStatus.OK).body(states)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @GetMapping(value= ["ehr-tokens"])
    fun getEhrTokens(request: HttpServletRequest): ResponseEntity<Any?> {
        return try {
            val stateRefs = proxy.vaultQueryBy<EHRTokenState>().states
            val states = ArrayList<EHRTokenState>()
            stateRefs.forEach {
                states.add(it.state.data)
            }
            ResponseEntity.status(HttpStatus.OK).body(states)
        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}