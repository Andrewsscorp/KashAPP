package com.example

import com.example.data.local.Pbkdf2PinHasher
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class Argon2idHasherTest {

    @Test
    fun `createNewPinHash produces honest pbkdf2 formatted hash and unique salt`() {
        val pin = "1234"
        val result1 = Pbkdf2PinHasher.createNewPinHash(pin)
        val result2 = Pbkdf2PinHasher.createNewPinHash(pin)

        assertTrue(result1.hashFormatted.startsWith("\$pbkdf2-sha256\$i=600000\$"))
        assertTrue(result1.saltBase64.isNotEmpty())

        // Different salts must result in different hashes for identical PIN
        assertNotEquals(result1.saltBase64, result2.saltBase64)
        assertNotEquals(result1.hashFormatted, result2.hashFormatted)
    }

    @Test
    fun `verifyPin returns true for matching PIN and false for wrong PIN`() {
        val pin = "8976"
        val hashResult = Pbkdf2PinHasher.createNewPinHash(pin)

        val isValid = Pbkdf2PinHasher.verifyPin(pin, hashResult.hashFormatted, hashResult.saltBase64)
        assertTrue("PIN verification should succeed for correct PIN", isValid)

        val isInvalid = Pbkdf2PinHasher.verifyPin("0000", hashResult.hashFormatted, hashResult.saltBase64)
        assertFalse("PIN verification must fail for wrong PIN", isInvalid)
    }

    @Test
    fun `legacy fake argon2 prefix is not accepted as a valid modern hash`() {
        val result = Pbkdf2PinHasher.createNewPinHash("8976")
        val legacyLabelledHash = result.hashFormatted.replace("\$pbkdf2-sha256", "\$argon2id")

        assertFalse(Pbkdf2PinHasher.verifyPin("8976", legacyLabelledHash, result.saltBase64))
    }
}
