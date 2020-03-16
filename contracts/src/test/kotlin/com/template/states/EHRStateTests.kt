package com.template.states

import net.corda.core.contracts.UniqueIdentifier
import net.corda.core.identity.CordaX500Name
import net.corda.core.identity.Party
import net.corda.testing.core.TestIdentity
import org.junit.Test
import kotlin.test.assertEquals

class EHRStateTests {

    @Test
    fun hasPatientFieldOfCorrectType() {
        assertEquals(EHRAgreementState::class.java.getDeclaredField("patient").type, Party::class.java)
    }

    @Test
    fun hasOriginDoctorFieldOfCorrectType() {
        assertEquals(EHRAgreementState::class.java.getDeclaredField("originDoctor").type, Party::class.java)
    }

    @Test
    fun hasTargetDoctorFieldOfCorrectType() {
        assertEquals(EHRAgreementState::class.java.getDeclaredField("targetDoctor").type, Boolean::class.java)
    }

    @Test
    fun hasStatusFieldOfCorrectType() {
        assertEquals(EHRAgreementState::class.java.getDeclaredField("status").type, EHRAgreementStateStatus::class.java)
    }

    @Test
    fun hasLinearIdFieldOfCorrectType() {
        assertEquals(EHRAgreementState::class.java.getDeclaredField("linearId").type, UniqueIdentifier::class.java)
    }

    @Test
    fun isStatusFieldSetToPending() {
        val partyA = TestIdentity(CordaX500Name("patient","New York","US")).party
        val partyB = TestIdentity(CordaX500Name("originDoc","New York","US")).party
        val partyC = TestIdentity(CordaX500Name("targetDoc","New York","US")).party
        val eHRAgreementState = EHRAgreementState(partyA, partyB, partyC)
        assert(eHRAgreementState.status == EHRAgreementStateStatus.PENDING)
    }

    @Test
    fun checkEHRAgreementStateParameterOrdering() {
        val fields = EHRAgreementState::class.java.declaredFields
        val patientIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("patient"))
        val originDoctorIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("originDoctor"))
        val targetDoctorIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("targetDoctor"))
        val descriptionIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("description"))
        val attachmentHashIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("attachmentHash"))
        val statusIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("status"))
        val linearIdIdx = fields.indexOf(EHRAgreementState::class.java.getDeclaredField("linearId"))

        assert(patientIdx < originDoctorIdx)
        assert(originDoctorIdx < targetDoctorIdx)
        assert(targetDoctorIdx < descriptionIdx)
        assert(descriptionIdx < attachmentHashIdx)
        assert(attachmentHashIdx < statusIdx)
        assert(statusIdx < linearIdIdx)
    }

    @Test
    fun checkParticipants() {
        val partyA = TestIdentity(CordaX500Name("patient","New York","US")).party
        val partyB = TestIdentity(CordaX500Name("originDoc","New York","US")).party
        val partyC = TestIdentity(CordaX500Name("targetDoc","New York","US")).party
        val eHRAgreementState = EHRAgreementState(partyA, partyB, partyC)
        assert(eHRAgreementState.participants.size == 2)
        assert(eHRAgreementState.participants.contains(partyA))
        assert(eHRAgreementState.participants.contains(partyB))
    }

}


