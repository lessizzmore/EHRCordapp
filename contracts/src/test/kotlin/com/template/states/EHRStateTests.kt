//package com.template.states
//
//import net.corda.core.contracts.UniqueIdentifier
//import net.corda.core.identity.CordaX500Name
//import net.corda.core.identity.Party
//import net.corda.testing.core.TestIdentity
//import org.junit.Test
//import kotlin.test.assertEquals
//
//class EHRStateTests {
//
//    @Test
//    fun hasPatientFieldOfCorrectType() {
//        assertEquals(EHRShareAgreementState::class.java.getDeclaredField("patient").type, Party::class.java)
//    }
//
//    @Test
//    fun hasOriginDoctorFieldOfCorrectType() {
//        assertEquals(EHRShareAgreementState::class.java.getDeclaredField("originDoctor").type, Party::class.java)
//    }
//
//    @Test
//    fun hasTargetDoctorFieldOfCorrectType() {
//        assertEquals(EHRShareAgreementState::class.java.getDeclaredField("targetDoctor").type, Boolean::class.java)
//    }
//
//    @Test
//    fun hasStatusFieldOfCorrectType() {
//        assertEquals(EHRShareAgreementState::class.java.getDeclaredField("status").type, EHRShareAgreementStateStatus::class.java)
//    }
//
//    @Test
//    fun hasLinearIdFieldOfCorrectType() {
//        assertEquals(EHRShareAgreementState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
//    }
//
//    @Test
//    fun isStatusFieldSetToPending() {
//        val partyA = TestIdentity(CordaX500Name("patient","New York","US")).party
//        val partyB = TestIdentity(CordaX500Name("originDoc","New York","US")).party
//        val partyC = TestIdentity(CordaX500Name("targetDoc","New York","US")).party
//        val eHRAgreementState = EHRShareAgreementState(partyA, partyB, partyC)
//        assert(eHRAgreementState.status == EHRShareAgreementStateStatus.PENDING)
//    }
//
//    @Test
//    fun checkEHRAgreementStateParameterOrdering() {
//        val fields = EHRShareAgreementState::class.java.declaredFields
//        val patientIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("patient"))
//        val originDoctorIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("originDoctor"))
//        val targetDoctorIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("targetDoctor"))
//        val descriptionIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("description"))
//        val attachmentHashIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("attachmentHash"))
//        val statusIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("status"))
//        val linearIdIdx = fields.indexOf(EHRShareAgreementState::class.java.getDeclaredField("linearId"))
//
//        assert(patientIdx < originDoctorIdx)
//        assert(originDoctorIdx < targetDoctorIdx)
//        assert(targetDoctorIdx < descriptionIdx)
//        assert(descriptionIdx < attachmentHashIdx)
//        assert(attachmentHashIdx < statusIdx)
//        assert(statusIdx < linearIdIdx)
//    }
//
//    @Test
//    fun checkParticipants() {
//        val partyA = TestIdentity(CordaX500Name("patient","New York","US")).party
//        val partyB = TestIdentity(CordaX500Name("originDoc","New York","US")).party
//        val partyC = TestIdentity(CordaX500Name("targetDoc","New York","US")).party
//        val eHRAgreementState = EHRShareAgreementState(partyA, partyB, partyC)
//        assert(eHRAgreementState.participants.size == 2)
//        assert(eHRAgreementState.participants.contains(partyA))
//        assert(eHRAgreementState.participants.contains(partyB))
//    }
//}
//
//
