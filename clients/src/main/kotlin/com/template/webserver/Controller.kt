package com.template.webserver

import com.r3.corda.lib.tokens.contracts.states.FungibleToken
import com.r3.corda.lib.tokens.contracts.states.NonFungibleToken
import com.template.flows.*
import com.template.flows.token.CreateAndIssueEHR
import com.template.flows.token.EHRSale
import com.template.flows.token.FiatCurrencyIssue
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.Amount
import net.corda.core.contracts.Amount.Companion.parseCurrency
import net.corda.core.contracts.ContractState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.flows.FlowException
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.*
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.boot.context.properties.bind.Bindable.mapOf
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.created
import org.springframework.http.ResponseEntity.ok
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.InputStream
import java.net.URI
import java.nio.file.Files
import java.nio.file.Paths
import java.util.*
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.servlet.http.HttpServletRequest
import javax.xml.ws.Response
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


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
    @GetMapping(value = ["/my-accounts"])
    private fun getMyAccounts(): List<String> {
        val result = proxy.startFlowDynamic(ViewMyAccounts::class.java).returnValue.get()
        return result
    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["/create-account"])
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
    @PostMapping(value = ["/share-account"])
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

//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @RequestMapping(value = ["share-with-account-request"])
//    fun sendEHRShareRequest (request: HttpServletRequest): ResponseEntity<String> {
//
//        val doctorA = request.getParameter("whoIam")
//        val doctorB = request.getParameter("whereTo")
//        val patient = request.getParameter("aboutWho")
//        val note = request.getParameter("note")
//        val attachmentId = request.getParameter("attachmentId")
//
//
//        if(patient == null){
//            return ResponseEntity.badRequest().body("Query parameter 'aboutWho' must not be null.\n")
//        }
//        if(doctorA == null){
//            return ResponseEntity.badRequest().body("Query parameter 'whoIam' must not be null.\n")
//        }
//
//        if(doctorB == null){
//            return ResponseEntity.badRequest().body("Query parameter 'whereTo' must not be null.\n")
//        }
//
//
//        val patientX500Name = CordaX500Name.parse(patient)
//        val patientParty = proxy.wellKnownPartyFromX500Name(patientX500Name) ?: return ResponseEntity.badRequest().body("Party named $patient cannot be found.\n")
//
//        return try {
//            val signedTx = proxy.startTrackedFlow(::RequestShareEHRAgreementFlow, doctorA, doctorB, patientParty, note, attachmentId).returnValue.getOrThrow()
//            ResponseEntity
//                    .status(HttpStatus.OK)
//                    .body(signedTx)
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//
//    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @PostMapping(value = ["/view-account/{acctName}"])
    private fun viewAccount(@PathVariable acctName: String): List<EHRShareAgreementState>? {
        val result = proxy.startFlowDynamic(ViewByAccount::class.java, acctName).returnValue.get()
        return result
    }

//    @PostMapping
//    fun upload(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String> {
//        val filename = file.originalFilename
//        require(filename != null) { "File name must be set" }
//        val hash: SecureHash = if (!(file.contentType == "zip" || file.contentType == "jar")) {
//            uploadZip(file.inputStream, uploader, filename!!)
//        } else {
//            proxy.uploadAttachmentWithMetadata(
//                    jar = file.inputStream,
//                    uploader = uploader,
//                    filename = filename!!
//            )
//        }
//        return created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
//    }
//
//    private fun uploadZip(inputStream: InputStream, uploader: String, filename: String): AttachmentId {
//        val zipName = "$filename-${UUID.randomUUID()}.zip"
//        FileOutputStream(zipName).use { fileOutputStream ->
//            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
//                val zipEntry = ZipEntry(filename)
//                zipOutputStream.putNextEntry(zipEntry)
//                inputStream.copyTo(zipOutputStream, 1024)
//            }
//        }
//        return FileInputStream(zipName).use { fileInputStream ->
//            val hash = proxy.uploadAttachmentWithMetadata(
//                    jar = fileInputStream,
//                    uploader = uploader,
//                    filename = filename
//            )
//            Files.deleteIfExists(Paths.get(zipName))
//            hash
//        }
//    }
//
//    @GetMapping("{hash}")
//    fun downloadByHash(@PathVariable hash: String): ResponseEntity<Resource> {
//        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
//        return ok().header(
//                HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"$hash.zip\""
//        ).body(inputStream)
//    }
//
//    @GetMapping
//    fun downloadByName(@RequestParam name: String): ResponseEntity<Resource> {
//        val attachmentIds: List<AttachmentId> = proxy.queryAttachments(
//                AttachmentQueryCriteria.AttachmentsQueryCriteria(filenameCondition = Builder.equal(name)),
//                null
//        )
//        val inputStreams = attachmentIds.map { proxy.openAttachment(it) }
//        val zipToReturn = if (inputStreams.size == 1) {
//            inputStreams.single()
//        } else {
//            combineZips(inputStreams, name)
//        }
//        return ok().header(
//                HttpHeaders.CONTENT_DISPOSITION,
//                "attachment; filename=\"$name.zip\""
//        ).body(InputStreamResource(zipToReturn))
//    }
//
//    private fun combineZips(inputStreams: List<InputStream>, filename: String): InputStream {
//        val zipName = "$filename-${UUID.randomUUID()}.zip"
//        FileOutputStream(zipName).use { fileOutputStream ->
//            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
//                inputStreams.forEachIndexed { index, inputStream ->
//                    val zipEntry = ZipEntry("$filename-$index.zip")
//                    zipOutputStream.putNextEntry(zipEntry)
//                    inputStream.copyTo(zipOutputStream, 1024)
//                }
//            }
//        }
//        return try {
//            FileInputStream(zipName)
//        } finally {
//            Files.deleteIfExists(Paths.get(zipName))
//        }
//    }

    @CrossOrigin(origins = ["http://localhost:4200"])
    @GetMapping(value = ["/ehrs"])
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
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @GetMapping(value = ["ehr/{ehrId}"])
//    private fun getEHR(@PathVariable ehrId: UUID): ResponseEntity<Any?> {
//            return try {
//                val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
//                        linearId = listOf(UniqueIdentifier(null, ehrId)))
//                val ehrStateRef =
//                        proxy.vaultQueryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()
//                                ?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
//                ResponseEntity.status(HttpStatus.OK).body(ehrStateRef.state.data)
//            }catch (ex: Throwable) {
//                logger.error(ex.message, ex)
//                ResponseEntity.badRequest().body(ex.message!!)
//            }
//    }
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @GetMapping(value = ["ehr/{ehrId}/patient"], produces = [APPLICATION_JSON_VALUE])
//    private fun getPatient(@PathVariable ehrId: UUID): ResponseEntity<Any?> {
//        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
//                linearId = listOf(UniqueIdentifier(null,ehrId)))
//        val ehrStateRef =
//            proxy.vaultQueryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
//        return ok(ehrStateRef.state.data.patient.toString())
//}
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @GetMapping(value = ["ehr/{ehrId}/origin"], produces = [APPLICATION_JSON_VALUE])
//    private fun getOrigin(@PathVariable ehrId: UUID): ResponseEntity<Any?> {
//        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
//                linearId = listOf(UniqueIdentifier(null,ehrId)))
//        val ehrStateRef =
//                proxy.vaultQueryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
//        return ok(ehrStateRef.state.data.originDoctor.toString())
//    }
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @GetMapping(value = ["ehr/{ehrId}/target"], produces = [APPLICATION_JSON_VALUE])
//    private fun getTarget(@PathVariable ehrId: UUID): ResponseEntity<Any?> {
//        val queryCriteria = QueryCriteria.LinearStateQueryCriteria(
//                linearId = listOf(UniqueIdentifier(null,ehrId)))
//        val ehrStateRef =
//                proxy.vaultQueryBy<EHRShareAgreementState>(queryCriteria).states.singleOrNull()?: throw FlowException("EHRShareAgreementState with id $ehrId not found.")
//        return ok(ehrStateRef.state.data.targetDoctor.toString())
//    }
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @RequestMapping(value = ["request"], headers = ["Content-Type=application/json"])
//    fun sendEHRShareRequest (request: HttpServletRequest): ResponseEntity<String> {
//
//        val patient = request.getParameter("patient")
//        val targetD = request.getParameter("targetD")
//        val note = request.getParameter("note")
//        val attachmentId = request.getParameter("attachmentId")
//
//
//        if(patient == null){
//            return ResponseEntity.badRequest().body("Query parameter 'patient' must not be null.\n")
//        }
//        if(targetD == null){
//            return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")
//        }
//
//        val patientX500Name = CordaX500Name.parse(patient)
//        val patientParty = proxy.wellKnownPartyFromX500Name(patientX500Name) ?: return ResponseEntity.badRequest().body("Party named $patient cannot be found.\n")
//        val targetDX500Name = CordaX500Name.parse(targetD)
//        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")
//
//
//        return try {
//            val signedTx = proxy.startTrackedFlow(::RequestShareEHRAgreementFlow, patientParty, targetDParty, note, attachmentId).returnValue.getOrThrow()
//            ResponseEntity
//                    .status(HttpStatus.OK)
//                    .body("Transaction id ${signedTx.id} committed to ledger.\n")
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//
//    }


    @RequestMapping(value = ["/request"], headers = ["Content-Type=application/json"])
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
    @PostMapping(value = ["/approve"])
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
    @PostMapping(value = ["/reject"])
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
    @PostMapping(value = ["/share"])
    fun shareEHR (request: HttpServletRequest): ResponseEntity<String> {
        val whoIam = request.getParameter("whoIam")
        val whereTo = request.getParameter("whereTo")
        val observer = request.getParameter("observer")
        val ehrId = request.getParameter("ehrId")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        return try {
            val signedTx = proxy.startTrackedFlow(
                    ::ShareEHRFlow,
                    whoIam,
                    whereTo,
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
    @PostMapping(value = ["/issue-cash"])
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
    @PostMapping(value = ["/issue-ehr"])
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
    @PostMapping(value = ["/sell-ehr"])
    fun sellEHR (request: HttpServletRequest): ResponseEntity<String> {
        val buyer = request.getParameter("buyer")
        val ehrId = request.getParameter("ehrId")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        if(buyer == null){
            return ResponseEntity.badRequest().body("Query parameter 'buyer' must not be null.\n")
        }

        if(ehrId == null){
            return ResponseEntity.badRequest().body("Query parameter 'ehrId' must not be null.\n")
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

    @GetMapping(value= ["/fungible-tokens"])
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

    @GetMapping(value= ["/nonfungible-tokens"])
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

//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @PostMapping(value = ["suspend"])
//    fun suspendPendingEHR (request: HttpServletRequest): ResponseEntity<String> {
//        val targetD = request.getParameter("targetD")
//                ?: return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")
//
//        val targetDX500Name = CordaX500Name.parse(targetD)
//        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")
//
//        val ehrId = request.getParameter("ehrId")
//        val ehrState = UniqueIdentifier.fromString(ehrId)
//        return try {
//            val signedTx = proxy.startTrackedFlow(::SuspendEHRFlow, targetDParty, ehrState).returnValue.getOrThrow()
//            ResponseEntity
//                    .status(HttpStatus.OK)
//                    .body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState suspended")
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//    }
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @PostMapping(value = ["delete"])
//    fun deletePendingEHR (request: HttpServletRequest): ResponseEntity<String> {
//
//        val targetD = request.getParameter("counterParty")
//                ?: return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")
//
//        val counterPartyX500Name = CordaX500Name.parse(targetD)
//        val counterParty = proxy.wellKnownPartyFromX500Name(counterPartyX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")
//
//        val ehrId = request.getParameter("ehrId")
//        val ehrState = UniqueIdentifier.fromString(ehrId)
//        return try {
//            val signedTx = proxy.startTrackedFlow(::DeleteShareEHRAgreementFlow, counterParty, ehrState).returnValue.getOrThrow()
//            ResponseEntity.status(HttpStatus.OK).body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState deleted")
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//    }
//
//    @CrossOrigin(origins = ["http://localhost:4200"])
//    @PostMapping(value = ["share"])
//    fun shareActivatedEHR (request: HttpServletRequest): ResponseEntity<String> {
//        val patient = request.getParameter("patient")
//        val targetD = request.getParameter("targetD")
//
//        if(patient == null){
//            return ResponseEntity.badRequest().body("Query parameter 'patient' must not be null.\n")
//        }
//        if(targetD == null){
//            return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")
//        }
//
//        val patientX500Name = CordaX500Name.parse(patient)
//        val patientParty = proxy.wellKnownPartyFromX500Name(patientX500Name) ?: return ResponseEntity.badRequest().body("Party named $patient cannot be found.\n")
//        val targetDX500Name = CordaX500Name.parse(targetD)
//        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")
//
//
//        val ehrId = request.getParameter("ehrId")
//        val ehrState = UniqueIdentifier.fromString(ehrId)
//        return try {
//            val signedTx = proxy.startTrackedFlow(::ShareEHRFlow, patientParty,  targetDParty, ehrState).returnValue.getOrThrow()
//            ResponseEntity
//                    .status(HttpStatus.OK)
//                    .body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState shared")
//
//        } catch (ex: Throwable) {
//            logger.error(ex.message, ex)
//            ResponseEntity.badRequest().body(ex.message!!)
//        }
//    }
}