package com.template.flows

import com.template.contracts.EHRShareAgreementContract
import com.template.states.EHRShareAgreementState
import com.template.states.EHRShareAgreementStateStatus
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

class DeleteShareEHRAgreementFlowTests {

    private val mockNetwork = MockNetwork(MockNetworkParameters(cordappsForAllNodes = listOf(
            TestCordapp.findCordapp("com.template.contracts"),
            TestCordapp.findCordapp("com.template.flows")
    )))

    private lateinit var nodeA: StartedMockNode
    private lateinit var nodeB: StartedMockNode
    private lateinit var nodeC: StartedMockNode
    private lateinit var partyA: Party
    private lateinit var partyB: Party
    private lateinit var partyC: Party
    private lateinit var ehrStateToDelete: EHRShareAgreementState


    @Before
    fun setup() {
        nodeA = mockNetwork.createNode()
        nodeB = mockNetwork.createNode()
        nodeC = mockNetwork.createNode()
        partyA = nodeA.info.chooseIdentityAndCert().party
        partyB = nodeB.info.chooseIdentityAndCert().party
        partyC = nodeC.info.chooseIdentityAndCert().party
        listOf(nodeA, nodeB, nodeC).forEach {
            it.registerInitiatedFlow(DeleteShareEHRAgreementFlowResponder::class.java)
        }

        ehrStateToDelete = EHRShareAgreementState(
                partyA,
                partyB,
                partyC,
                "EKG",
                null,
                EHRShareAgreementStateStatus.PENDING
        )
    }

    @After
    fun tearDown() = mockNetwork.stopNodes()

    @Test
    fun flowReturnsCorrectlyFormedTransaction() {
        val future = nodeA.startFlow(DeleteShareEHRAgreementFlow(partyB, ehrStateToDelete.linearId))
        mockNetwork.runNetwork()
        val ptx: SignedTransaction = future.getOrThrow()

        assert(ptx.tx.inputs.size == 1)
        assert(ptx.tx.outputs.isEmpty())
        assert(ptx.tx.outputs[0].data is EHRShareAgreementState)
        assert(ptx.tx.commands.singleOrNull() != null)
        assert(ptx.tx.commands.single().value is EHRShareAgreementContract.Commands.Delete)
        assert(ptx.tx.requiredSigningKeys.equals(setOf(partyA.owningKey, partyB.owningKey)))
    }

    @Test
    fun flowReturnsTransactionSignedByBothParties() {
        val future = nodeA.startFlow(DeleteShareEHRAgreementFlow(partyB, ehrStateToDelete.linearId))
        mockNetwork.runNetwork()
        val stx = future.getOrThrow()
        stx.verifyRequiredSignatures()
    }

    @Test
    fun flowRecordsTheSameTransactionInBothPartyVaults() {
        val future = nodeA.startFlow(DeleteShareEHRAgreementFlow(partyB, ehrStateToDelete.linearId))
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