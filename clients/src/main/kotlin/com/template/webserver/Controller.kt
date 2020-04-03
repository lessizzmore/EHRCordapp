package com.template.webserver

import com.template.flows.ActivateEHRFlow
import com.template.flows.DeleteShareEHRAgreementFlow
import com.template.flows.RequestShareEHRAgreementFlow
import com.template.flows.SuspendEHRFlow
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.identity.CordaX500Name
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.messaging.startTrackedFlow
import net.corda.core.messaging.vaultQueryBy
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.MediaType.APPLICATION_JSON_VALUE
import org.springframework.http.ResponseEntity
import org.springframework.http.ResponseEntity.*
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


/**
 *  A Spring Boot Server API controller for interacting with the node via RPC.
 */
val SERVICE_NAMES = listOf("Notary", "Network Map Service")

@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }
    private val myLegalName = rpc.proxy.nodeInfo().legalIdentities.first().name
    private val proxy: CordaRPCOps = rpc.proxy


    /**
     * Returns the node's name.
     */
    @GetMapping(value = [ "me" ], produces = [ APPLICATION_JSON_VALUE ])
    fun whoami() = mapOf("me" to myLegalName)

    /**
     * Returns all parties registered with the network map service. These names can be used to look up identities using
     * the identity service.
     */
    @GetMapping(value = [ "peers" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPeers(): Map<String, List<CordaX500Name>> {
        val nodeInfo = proxy.networkMapSnapshot()
        return mapOf("peers" to nodeInfo
                .map { it.legalIdentities.first().name }
                //filter out myself, notary and eventual network map started by driver
                .filter { it.organisation !in (SERVICE_NAMES + myLegalName.organisation) })
    }



    @PostMapping
    fun upload(@RequestParam file: MultipartFile, @RequestParam uploader: String): ResponseEntity<String> {
        val filename = file.originalFilename
        require(filename != null) { "File name must be set" }
        val hash: SecureHash = if (!(file.contentType == "zip" || file.contentType == "jar")) {
            uploadZip(file.inputStream, uploader, filename!!)
        } else {
            proxy.uploadAttachmentWithMetadata(
                    jar = file.inputStream,
                    uploader = uploader,
                    filename = filename!!
            )
        }
        return created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
    }

    private fun uploadZip(inputStream: InputStream, uploader: String, filename: String): AttachmentId {
        val zipName = "$filename-${UUID.randomUUID()}.zip"
        FileOutputStream(zipName).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                val zipEntry = ZipEntry(filename)
                zipOutputStream.putNextEntry(zipEntry)
                inputStream.copyTo(zipOutputStream, 1024)
            }
        }
        return FileInputStream(zipName).use { fileInputStream ->
            val hash = proxy.uploadAttachmentWithMetadata(
                    jar = fileInputStream,
                    uploader = uploader,
                    filename = filename
            )
            Files.deleteIfExists(Paths.get(zipName))
            hash
        }
    }

    @GetMapping("/{hash}")
    fun downloadByHash(@PathVariable hash: String): ResponseEntity<Resource> {
        val inputStream = InputStreamResource(proxy.openAttachment(SecureHash.parse(hash)))
        return ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$hash.zip\""
        ).body(inputStream)
    }

    @GetMapping
    fun downloadByName(@RequestParam name: String): ResponseEntity<Resource> {
        val attachmentIds: List<AttachmentId> = proxy.queryAttachments(
                AttachmentQueryCriteria.AttachmentsQueryCriteria(filenameCondition = Builder.equal(name)),
                null
        )
        val inputStreams = attachmentIds.map { proxy.openAttachment(it) }
        val zipToReturn = if (inputStreams.size == 1) {
            inputStreams.single()
        } else {
            combineZips(inputStreams, name)
        }
        return ok().header(
                HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"$name.zip\""
        ).body(InputStreamResource(zipToReturn))
    }

    private fun combineZips(inputStreams: List<InputStream>, filename: String): InputStream {
        val zipName = "$filename-${UUID.randomUUID()}.zip"
        FileOutputStream(zipName).use { fileOutputStream ->
            ZipOutputStream(fileOutputStream).use { zipOutputStream ->
                inputStreams.forEachIndexed { index, inputStream ->
                    val zipEntry = ZipEntry("$filename-$index.zip")
                    zipOutputStream.putNextEntry(zipEntry)
                    inputStream.copyTo(zipOutputStream, 1024)
                }
            }
        }
        return try {
            FileInputStream(zipName)
        } finally {
            Files.deleteIfExists(Paths.get(zipName))
        }
    }

    /**
     * Displays all EHR states that exist in the node's vault with pagination.
     */
    @GetMapping(value = ["ehrs"], produces = [MediaType.APPLICATION_JSON_VALUE])
    fun getEHRs(): MutableList<StateAndRef<EHRShareAgreementState>> {

        var pageNumber = DEFAULT_PAGE_NUM
        val pageSize = 200
        val states = mutableListOf<StateAndRef<EHRShareAgreementState>>()
        val sorting = Sort(setOf(Sort.SortColumn(SortAttribute.Standard(Sort.VaultStateAttribute.RECORDED_TIME), Sort.Direction.ASC)))

        do {
            val pageSpec = PageSpecification(pageNumber = pageNumber, pageSize = pageSize)
            val results = proxy.vaultQueryBy(
                    QueryCriteria.VaultQueryCriteria(),
                    pageSpec,
                    sorting,
                    EHRShareAgreementState::class.java)
            states.addAll(results.states)
            pageNumber++
        } while ((pageSpec.pageSize * (pageNumber - 1)) <= results.totalStatesAvailable)

        return states
    }


    /**
     * Initiates a flow to agree an EHR share between two parties.
     *
     * Once the flow finishes it will have written the EHR to ledger. Both the patient and the origin doctor will be able to
     * see it on their respective nodes.
     *
     * This end-point takes a patient and a target-doctor name parameter as part of the path. If the serving node can't find the other party
     * in its network map cache, it will return an HTTP bad request.
     *
     * The flow is invoked asynchronously. It returns a future when the flow's call() method returns.
     */
    @PostMapping(value = ["create-ehr"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun sendEHRShareRequest (request: HttpServletRequest): ResponseEntity<String> {

        val patient = request.getParameter("patient")
        val targetD = request.getParameter("target-doctor")

        if(patient == null){
            return ResponseEntity.badRequest().body("Query parameter 'patient' must not be null.\n")
        }
        if(targetD == null){
            return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")
        }

        val patientX500Name = CordaX500Name.parse(patient)
        val patientParty = proxy.wellKnownPartyFromX500Name(patientX500Name) ?: return ResponseEntity.badRequest().body("Party named $patient cannot be found.\n")
        val targetDX500Name = CordaX500Name.parse(targetD)
        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")


        return try {
            val signedTx = proxy.startTrackedFlow(::RequestShareEHRAgreementFlow, patientParty, targetDParty).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }

    @GetMapping(value = [ "patient-ehrs" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getPatientEHRs(): ResponseEntity<List<StateAndRef<EHRShareAgreementState>>>  {
        val myehrs = proxy.vaultQueryBy<EHRShareAgreementState>().states.filter { it.state.data.patient.equals(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myehrs)
    }

    @GetMapping(value = [ "originD-ehrs" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getOriginDoctorEHRs(): ResponseEntity<List<StateAndRef<EHRShareAgreementState>>>  {
        val myehrs = proxy.vaultQueryBy<EHRShareAgreementState>().states.filter { it.state.data.originDoctor.equals(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myehrs)
    }

    @GetMapping(value = [ "targetD-ehrs" ], produces = [ APPLICATION_JSON_VALUE ])
    fun getTargetDoctorEHRs(): ResponseEntity<List<StateAndRef<EHRShareAgreementState>>>  {
        val myehrs = proxy.vaultQueryBy<EHRShareAgreementState>().states.filter { it.state.data.targetDoctor.equals(proxy.nodeInfo().legalIdentities.first()) }
        return ResponseEntity.ok(myehrs)
    }

    @PostMapping(value = ["activate-ehr"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun activatePendingEHR (request: HttpServletRequest): ResponseEntity<String> {
        val targetD = request.getParameter("target-doctor")
                ?: return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")

        val targetDX500Name = CordaX500Name.parse(targetD)
        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        return try {
            val signedTx = proxy.startTrackedFlow(::ActivateEHRFlow, targetDParty, ehrState).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState activated")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }

    }

    @PostMapping(value = ["suspend-ehr"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun suspendPendingEHR (request: HttpServletRequest): ResponseEntity<String> {
        val targetD = request.getParameter("target-doctor")
                ?: return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")

        val targetDX500Name = CordaX500Name.parse(targetD)
        val targetDParty = proxy.wellKnownPartyFromX500Name(targetDX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        return try {
            val signedTx = proxy.startTrackedFlow(::SuspendEHRFlow, targetDParty, ehrState).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState suspended")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }

    @PostMapping(value = ["delete-ehr"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun deletePendingEHR (request: HttpServletRequest): ResponseEntity<String> {

        val targetD = request.getParameter("counter-party")
                ?: return ResponseEntity.badRequest().body("Query parameter 'targetD' must not be null.\n")

        val counterPartyX500Name = CordaX500Name.parse(targetD)
        val counterParty = proxy.wellKnownPartyFromX500Name(counterPartyX500Name) ?: return ResponseEntity.badRequest().body("Party named $targetD cannot be found.\n")

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)
        return try {
            val signedTx = proxy.startTrackedFlow(::DeleteShareEHRAgreementFlow, counterParty, ehrState).returnValue.getOrThrow()
            ResponseEntity.status(HttpStatus.CREATED).body("Transaction id ${signedTx.id} committed to ledger.\n EHR $ehrState deleted")

        } catch (ex: Throwable) {
            logger.error(ex.message, ex)
            ResponseEntity.badRequest().body(ex.message!!)
        }
    }
}