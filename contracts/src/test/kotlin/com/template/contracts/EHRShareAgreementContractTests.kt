//package com.template.contracts
//
//import com.template.states.EHRShareAgreementState
//import net.corda.testing.node.MockServices
//import org.junit.Test
//
//
//import net.corda.core.contracts.TypeOnlyCommandData
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.identity.Party
//import net.corda.testing.contracts.DummyState
//import net.corda.testing.core.TestIdentity
//import net.corda.testing.node.ledger
//import org.junit.Before
//import java.security.PublicKey
//
//class EHRShareAgreementContractTests {
//    private val ledgerServices = MockServices()
//
//    class DummyCommand : TypeOnlyCommandData()
//
//    lateinit var ehrAgreementState: EHRShareAgreementState
//    lateinit var publicKeys: List<PublicKey>
//    lateinit var partyA: Party
//    lateinit var partyB: Party
//    lateinit var partyC: Party
//
//    @Before
//    fun setup() {
//        partyA = TestIdentity(CordaX500Name("PartyA","New York","US")).party
//        partyB = TestIdentity(CordaX500Name("PartyB","New York","US")).party
//        partyC = TestIdentity(CordaX500Name("PartyC","New York","US")).party
//        ehrAgreementState = EHRShareAgreementState(partyA, partyB, partyC)
//        publicKeys = ehrAgreementState.participants.map {it.owningKey}
//    }
//
//    @Test
//    fun mustIncludeCreateEHRAgreementCommand() {
//        ledgerServices.ledger {
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(publicKeys, EHRShareAgreementContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(publicKeys, DummyCommand())
//                this.fails()
//            }
//        }
//    }
//
//    @Test
//    fun createEHRAgreementTransactionMustHaveNoInputs() {
//        ledgerServices.ledger {
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(publicKeys, EHRShareAgreementContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                input(EHRShareAgreementContract.EHR_CONTRACT_ID, DummyState())
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(publicKeys, EHRShareAgreementContract.Commands.Create())
//                this `fails with` "There should be no input state."
//            }
//        }
//    }
//
//    @Test
//    fun createEHRAgreementTransactionMustHaveOneOutput() {
//        ledgerServices.ledger {
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(publicKeys, EHRShareAgreementContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, DummyState())
//                command(publicKeys, EHRShareAgreementContract.Commands.Create())
//                this `fails with` "There should be one output state."
//            }
//        }
//    }
//
//    @Test
//    fun cannotCreateEHRAgreementWithYourself() {
//        val boardStateSameParty = EHRShareAgreementState(partyA, partyA, partyC)
//        ledgerServices.ledger {
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                command(listOf(partyA.owningKey, partyB.owningKey), EHRShareAgreementContract.Commands.Create())
//                this.verifies()
//            }
//            transaction {
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, boardStateSameParty)
//                command(listOf(partyA.owningKey, partyA.owningKey), EHRShareAgreementContract.Commands.Create())
//                this `fails with` "You cannot share an EHR with yourself."
//            }
//        }
//    }
//
//    @Test
//    fun patientMustSignEHRShareAgreementTransaction() {
//        val partyC = TestIdentity(CordaX500Name("PartyC","New York","US")).party
//        ledgerServices.ledger {
//            transaction {
//                command(listOf(partyA.owningKey, partyB.owningKey), EHRShareAgreementContract.Commands.Create())
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                this.verifies()
//            }
//            transaction {
//                command(partyA.owningKey, EHRShareAgreementContract.Commands.Create())
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                this `fails with` "Both participants must sign a StartGame transaction."
//            }
//            transaction {
//                command(partyC.owningKey, EHRShareAgreementContract.Commands.Create())
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                this `fails with` "Both participants must sign a StartGame transaction."
//            }
//            transaction {
//                command(listOf(partyC.owningKey, partyA.owningKey, partyB.owningKey), EHRShareAgreementContract.Commands.Create())
//                output(EHRShareAgreementContract.EHR_CONTRACT_ID, ehrAgreementState)
//                this `fails with` "Both participants must sign a StartGame transaction."
//            }
//        }
//    }
//}