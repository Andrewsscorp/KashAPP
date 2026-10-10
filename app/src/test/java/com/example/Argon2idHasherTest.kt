package com.example

import com.example.data.local.Argon2idHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Argon2idHasherTest {

    @Test
    fun `createNewPinHash produces valid argon2id formatted hash and unique salt`() {
        val pin = "1234"
        val result1 = Argon2idHasher.createNewPinHash(pin)
        val result2 = Argon2idHasher.createNewPinHash(pin)

        assertTrue(result1.hashFormatted.startsWith("\$argon2id\$v=19\$m=65536,t=3,p=4\$"))
        assertTrue(result1.saltBase64.isNotEmpty())

        // Different salts must result in different hashes for identical PIN
        assertNotEquals(result1.saltBase64, result2.saltBase64)
        assertNotEquals(result1.hashFormatted, result2.hashFormatted)
    }

    @Test
    fun `verifyPin returns true for matching PIN and false for wrong PIN`() {
        val pin = "8976"
        val hashResult = Argon2idHasher.createNewPinHash(pin)

        val isValid = Argon2idHasher.verifyPin(pin, hashResult.hashFormatted, hashResult.saltBase64)
        assertTrue("PIN verification should succeed for correct PIN", isValid)

        val isInvalid = Argon2idHasher.verifyPin("0000", hashResult.hashFormatted, hashResult.saltBase64)
        assertFalse("PIN verification must fail for wrong PIN", isInvalid)
    }
}
