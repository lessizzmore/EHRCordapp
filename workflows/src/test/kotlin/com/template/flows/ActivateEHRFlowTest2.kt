//package com.template.flows
//
//import com.r3.corda.lib.accounts.workflows.flows.RequestKeyForAccount
//import com.r3.corda.lib.accounts.workflows.services.KeyManagementBackedAccountService
//import com.template.contracts.EHRShareAgreementContract
//import com.template.states.EHRShareAgreementState
//import com.template.states.EHRShareAgreementStateStatus
//import net.corda.core.identity.Party
//import net.corda.core.node.services.queryBy
//import net.corda.core.serialization.SerializeAsToken
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.common.internal.testNetworkParameters
//import net.corda.testing.internal.chooseIdentityAndCert
//import net.corda.testing.node.MockNetwork
//import net.corda.testing.node.MockNetworkParameters
//import net.corda.testing.node.StartedMockNode
//import net.corda.testing.node.TestCordapp
//import org.apache.commons.lang3.Validate.notEmpty
//import org.junit.After
//import org.junit.Assert
//import org.junit.Before
//import org.junit.Test
//import org.junit.platform.commons.util.Preconditions.notEmpty
//import java.text.SimpleDateFormat
//import kotlin.test.assertEquals
//
//class ActivateEHRFlowTest2 {
//    lateinit var mockNetwork: MockNetwork
//    private lateinit var nodeA: StartedMockNode
//    private lateinit var nodeB: StartedMockNode
//    private lateinit var partyA: Party
//    private lateinit var partyB: Party
//    private lateinit var ehrStateToActivate: EHRShareAgreementState
//
//
//
//    val REQUIRED_CORDAPP_PACKAGES = listOf(
//            TestCordapp.findCordapp("com.template.flows"),
//            TestCordapp.findCordapp("com.template.contracts"),
//            TestCordapp.findCordapp("com.r3.corda.lib.accounts.workflows"),
//            TestCordapp.findCordapp("com.r3.corda.lib.accounts.contracts"),
//            TestCordapp.findCordapp("com.r3.corda.lib.ci")
//    )
//    @Before
//    fun setup() {
//
//        mockNetwork = MockNetwork(
//                parameters = MockNetworkParameters(
//                cordappsForAllNodes = REQUIRED_CORDAPP_PACKAGES,
//                networkParameters = testNetworkParameters(
//                        minimumPlatformVersion = 4
//                )
//                )
//        )
//        nodeA = mockNetwork.createNode()
//        nodeB = mockNetwork.createNode()
//
//        partyA = nodeA.info.chooseIdentityAndCert().party
//        partyB = nodeB.info.chooseIdentityAndCert().party
//        listOf(nodeB).forEach {
//            it.registerInitiatedFlow(ActivateEHRFlowResponder::class.java)
//        }
//        listOf(nodeA).forEach {
//            it.registerInitiatedFlow(RequestShareEHRAgreementFlowResponder::class.java)
//        }
//        ehrStateToActivate = createEHRShareAgreementState()
//    }
//
//    @After
//    fun tearDown() = mockNetwork.stopNodes()
//
//    // helper functions
//    private fun createEHRShareAgreementState(): EHRShareAgreementState {
//
//        val patientAccountService = nodeA.services.cordaService(KeyManagementBackedAccountService::class.java)
//        val doctor1AccountService = nodeB.services.cordaService(KeyManagementBackedAccountService::class.java)
//        val doctor2AccountService = nodeB.services.cordaService(KeyManagementBackedAccountService::class.java)
//
//        val patientAccount = patientAccountService.createAccount("Patient-Account").getOrThrow().state.data
//        val doctor1Account = doctor1AccountService.createAccount("Doctor1-Account").getOrThrow().state.data
//        val doctor2Account = doctor2AccountService.createAccount("Doctor2-Account").getOrThrow().state.data
//
//        mockNetwork.runNetwork()
//
//
//        patientAccountService.shareAccountInfoWithParty(patientAccount.identifier.id, nodeB.info.legalIdentities[0])
//        doctor1AccountService.shareAccountInfoWithParty(doctor1Account.identifier.id, nodeA.info.legalIdentities[0])
//        doctor2AccountService.shareAccountInfoWithParty(doctor2Account.identifier.id, nodeA.info.legalIdentities[0])
//
//        mockNetwork.runNetwork()
//
//        val NodeBPartyFuture = nodeB
//                .startFlow(RequestShareEHRAgreementFlow(
//                        "Doctor1-Account",
//                        "Patient-Account",
//                        "Doctor2-Account",
//                        "happy",
//                        "11"))
//        mockNetwork.runNetwork()
//
//        val NodeBParty = NodeBPartyFuture.getOrThrow()
//
//        val states = nodeA.transaction {
//            nodeB.services.vaultService.queryBy<EHRShareAgreementState>().states
//        }
//        return states.last().state.data
//    }
//
//
//    @Test
//    fun activate() {
//        val newEHRFuture = nodeA.startFlow(ActivateEHRFlow("Patient-Account", "Doctor1-Account", ehrStateToActivate.linearId))
//        mockNetwork.runNetwork()
//
//        val savedEHR = newEHRFuture.getOrThrow()
//    }
//
//}