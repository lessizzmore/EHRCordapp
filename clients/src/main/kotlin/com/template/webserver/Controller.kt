package com.template.webserver

import com.template.flows.ActivateEHRFlow
import com.template.flows.DeleteShareEHRAgreementFlow
import com.template.flows.RequestShareEHRAgreementFlow
import com.template.flows.SuspendEHRFlow
import com.template.states.EHRShareAgreementState
import net.corda.core.contracts.StateAndRef
import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.crypto.SecureHash
import net.corda.core.messaging.CordaRPCOps
import net.corda.core.node.services.AttachmentId
import net.corda.core.node.services.vault.*
import net.corda.core.utilities.getOrThrow
import org.slf4j.LoggerFactory
import org.springframework.core.io.InputStreamResource
import org.springframework.core.io.Resource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
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


@RestController
@RequestMapping("/") // The paths for HTTP requests are relative to this base path.
class Controller(rpc: NodeRPCConnection) {

    companion object {
        private val logger = LoggerFactory.getLogger(RestController::class.java)
    }

    private val proxy: CordaRPCOps = rpc.proxy

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
        return ResponseEntity.created(URI.create("attachments/$hash")).body("Attachment uploaded with hash - $hash")
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
        return ResponseEntity.ok().header(
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
        return ResponseEntity.ok().header(
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

    @GetMapping(value = ["ehr-states"], produces = [MediaType.APPLICATION_JSON_VALUE])
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

    @PostMapping(value = ["send-request"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun sendEHRShareRequest (request: HttpServletRequest): ResponseEntity<String> {

        val patient = request.getParameter("patient")
        val originD = request.getParameter("origin-doctor")
        val targetD = request.getParameter("target-doctor")

        val (status, message) = try {
            val flowHandle = proxy.startFlowDynamic(
                    RequestShareEHRAgreementFlow::class.java,
                    patient,
                    originD,
                    targetD
            )

            flowHandle.use { it.returnValue.getOrThrow() }
            HttpStatus.CREATED to "EHR state committed to ledger for $patient $targetD from $originD."
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }

        return ResponseEntity.status(status).body(message)
    }


    @PostMapping(value = ["activate-ehr-state"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun activatePendingEHR (request: HttpServletRequest): ResponseEntity<String> {

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)

        val (status, message) = try {
            val flowHandle = proxy.startFlowDynamic(
                    ActivateEHRFlow::class.java,
                    ehrState
            )

            flowHandle.use { it.returnValue.getOrThrow() }
            HttpStatus.CREATED to "EHR state $ehrState activated."
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }

        return ResponseEntity.status(status).body(message)
    }

    @PostMapping(value = ["suspend-ehr-state"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun suspendPendingEHR (request: HttpServletRequest): ResponseEntity<String> {

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)

        val (status, message) = try {
            val flowHandle = proxy.startFlowDynamic(
                    SuspendEHRFlow::class.java,
                    ehrState
            )

            flowHandle.use { it.returnValue.getOrThrow() }
            HttpStatus.CREATED to "EHR state $ehrState suspended."
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }

        return ResponseEntity.status(status).body(message)
    }

    @PostMapping(value = ["delete-ehr-state"], produces = [MediaType.APPLICATION_JSON_VALUE], headers = ["Content-Type=application/x-www-form-urlencoded"])
    fun deletePendingEHR (request: HttpServletRequest): ResponseEntity<String> {

        val ehrId = request.getParameter("ehr-id")
        val ehrState = UniqueIdentifier.fromString(ehrId)

        val (status, message) = try {
            val flowHandle = proxy.startFlowDynamic(
                    DeleteShareEHRAgreementFlow::class.java,
                    ehrState
            )

            flowHandle.use { it.returnValue.getOrThrow() }
            HttpStatus.CREATED to "EHR state $ehrState deleted."
        } catch (e: Exception) {
            HttpStatus.BAD_REQUEST to e.message
        }

        return ResponseEntity.status(status).body(message)
    }
}