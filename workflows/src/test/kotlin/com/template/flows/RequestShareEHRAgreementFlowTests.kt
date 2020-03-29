package com.template.flows

import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import net.corda.core.identity.Party
import net.corda.core.transactions.SignedTransaction
import net.corda.core.utilities.getOrThrow
import net.corda.testing.internal.chooseIdentityAndCert
import net.corda.testing.node.MockNetwork
import net.corda.testing.node.MockNetworkParameters
import net.corda.testing.node.StartedMockNode
import net.corda.testing.node.TestCordapp
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.test.assertEquals

class RequestShareEHRAgreementFlowTests {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))

    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode
    private lateinit var partyA: Party
    private lateinit var partyB: Party

    @Before
    fun setup() {
        nodeA = mockNetwork.createNode()
        nodeB = mockNetwork.createNode()
        partyA = nodeA.info.chooseIdentityAndCert().party
        partyB = nodeB.info.chooseIdentityAndCert().party
        listOf(nodeA).forEach {
            it.registerInitiatedFlow(RequestShareEHRAgreementFlowResponder::class.java)
        }
    }

    @After
    fun tearDown() = mockNetwork.stopNodes()

    @Test
    fun flowReturnsCorrectlyFormedTransaction() {
        val future = nodeB.startFlow(RequestShareEHRAgreementFlow(partyA, partyB))
        mockNetwork.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()

        assert(ptx.tx.inputs.isEmpty())
        assert(ptx.tx.outputs.size == 1)
        assert(ptx.tx.outputs[0].data is EHRShareAgreementState)
        assert(ptx.tx.commands.singleOrNull() != null)
        assert(ptx.tx.commands.single().value is EHRShareAgreementContract.Commands.Create)
        assert(ptx.tx.requiredSigningKeys.equals(setOf(partyA.owningKey, partyB.owningKey)))
    }

    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val future = nodeB.startFlow(RequestShareEHRAgreementFlow(partyA, partyB))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val future = nodeB.startFlow(RequestShareEHRAgreementFlow(partyA, partyB))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()

        listOf(nodeA, nodeB).map {
            it.services.validatedTransactions.getTransaction(stx.id)
        }.forEach {
            val txHash = (it as SignedTransaction).id
            assertEquals(txHash, stx.id)
        }
    }
}