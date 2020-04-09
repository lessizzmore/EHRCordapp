//package com.template.flows
//
//import com.template.contracts.EHRShareAgreementContract
//import com.template.states.EHRShareAgreementState
//import net.corda.core.identity.Party
//import net.corda.core.node.services.queryBy
//import net.corda.core.transactions.SignedTransaction
//import net.corda.core.utilities.getOrThrow
//import net.corda.testing.internal.chooseIdentityAndCert
//import net.corda.testing.node.MockNetwork
//import net.corda.testing.node.MockNetworkParameters
//import net.corda.testing.node.StartedMockNode
//import net.corda.testing.node.TestCordapp
//import org.junit.After
//import org.junit.Before
//import org.junit.Test
//import kotlin.test.assertEquals
//
//class SuspendEHRFlowTests {
//
//    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
//            TestCordapp.findCordapp("com.template.contracts"),
//            TestCordapp.findCordapp("com.template.flows")
//    )))
//
//    private lateinit var nodeA: StartedMockNode
//    private lateinit var nodeB: StartedMockNode
//    private lateinit var nodeC: StartedMockNode
//    private lateinit var partyA: Party
//    private lateinit var partyB: Party
//    private lateinit var partyC: Party
//    private lateinit var ehrStateToSuspend: EHRShareAgreementState
//
//    @Before
//    fun setup() {
//        nodeA = mockNetwork.createNode()
//        nodeB = mockNetwork.createNode()
//        nodeC = mockNetwork.createNode()
//        partyA = nodeA.info.chooseIdentityAndCert().party
//        partyB = nodeB.info.chooseIdentityAndCert().party
//        partyC = nodeC.info.chooseIdentityAndCert().party
//        listOf(nodeB).forEach {
//            it.registerInitiatedFlow(SuspendEHRFlowResponder::class.java)
//        }
//        listOf(nodeA).forEach {
//            it.registerInitiatedFlow(RequestShareEHRAgreementFlowResponder::class.java)
//        }
//        ehrStateToSuspend = createEHRShareAgreementState()
//    }
//
//    @After
//    fun tearDown() = mockNetwork.stopNodes()
//
//    // helper functions
//    private fun createEHRShareAgreementState(): EHRShareAgreementState {
//
//        val flow = RequestShareEHRAgreementFlow(partyA, partyB)
//        val future = nodeB.startFlow(flow)
//        mockNetwork.runNetwork()
//        future.getOrThrow()
//
//        val states = nodeB.transaction {
//            nodeB.services.vaultService.queryBy<EHRShareAgreementState>().states
//        }
//        return states.last().state.data
//    }
//
//    @Test
//    fun flowReturnsCorrectlyFormedTransaction() {
//        val future2 = nodeA.startFlow(ActivateEHRFlow(partyB, ehrStateToSuspend.linearId))
//        mockNetwork.runNetwork()
//        val ptx: SignedTransaction = future2.getOrThrow()
//
//        assert(ptx.tx.outputs.size == 1)
//        assert(ptx.tx.outputs[0].data is EHRShareAgreementState)
//        assert(ptx.tx.commands.singleOrNull() != null)
//        assert(ptx.tx.commands.single().value is EHRShareAgreementContract.Commands.Activate)
//        assert(ptx.tx.commands[0].signers == listOf(partyA.owningKey, partyB.owningKey))
//    }
//
//    @Test
//    fun flowReturnsTransactionSignedByBothParties() {
//        val future = nodeA.startFlow(ActivateEHRFlow(partyB, ehrStateToSuspend.linearId))
//        mockNetwork.runNetwork()
//        val stx = future.getOrThrow()
//        stx.verifyRequiredSignatures()
//    }
//
//    @Test
//    fun flowRecordsTheSameTransactionInBothPartyVaults() {
//        val future = nodeA.startFlow(ActivateEHRFlow(partyB, ehrStateToSuspend.linearId))
//        mockNetwork.runNetwork()
//        val stx = future.getOrThrow()
//
//        listOf(nodeA, nodeB).map {
//            it.services.validatedTransactions.getTransaction(stx.id)
//        }.forEach {
//            val txHash = (it as SignedTransaction).id
//            assertEquals(txHash, stx.id)
//        }
//    }
//}