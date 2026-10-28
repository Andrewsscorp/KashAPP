package com.example.core.id

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated unit test for deterministic email UID generation.
 * Ensures strict invariance under whitespace, uppercase/lowercase variations,
 * and confirms collision resistance.
 */
class DeterministicEmailUidGeneratorTest {

    @Test
    fun testSameEmailProducesIdenticalUidRegardlessOfCasingAndWhitespace() {
        val email1 = "andres.suarez@gmail.com"
        val email2 = "  Andres.Suarez@GMAIL.COM  "
        val email3 = "ANDRES.SUAREZ@GMAIL.COM"

        val uid1 = EmailDeterministicUidGenerator.fromEmail(email1)
        val uid2 = EmailDeterministicUidGenerator.fromEmail(email2)
        val uid3 = EmailDeterministicUidGenerator.fromEmail(email3)

        assertEquals("UID must match exactly across all variations", uid1, uid2)
        assertEquals("UID must match exactly across all variations", uid2, uid3)
        assertTrue(EmailDeterministicUidGenerator.isDeterministicEmailUid(uid1))
        assertEquals(36, uid1.length) // "usr_" (4) + 32 hex = 36 chars
    }

    @Test
    fun testDifferentEmailsProduceDifferentUids() {
        val uidA = EmailDeterministicUidGenerator.fromEmail("andres@example.com")
        val uidB = EmailDeterministicUidGenerator.fromEmail("hawk@example.com")

        assertNotEquals(uidA, uidB)
    }

    @Test
    fun testValidationOfEmailFormat() {
        assertTrue(EmailDeterministicUidGenerator.isValidEmail("usuario@kashapp.co"))
        assertTrue(EmailDeterministicUidGenerator.isValidEmail("  test.user+tag@domain.com.co "))
        assertFalse(EmailDeterministicUidGenerator.isValidEmail("not-an-email"))
        assertFalse(EmailDeterministicUidGenerator.isValidEmail("@missinguser.com"))
        assertFalse(EmailDeterministicUidGenerator.isValidEmail("missingdomain@.com"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun testBlankEmailThrowsException() {
        EmailDeterministicUidGenerator.fromEmail("   ")
    }
}
