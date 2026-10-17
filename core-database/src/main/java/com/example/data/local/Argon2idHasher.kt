package com.example.data.local

import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec
import android.util.Base64

/**
 * PBKDF2-HMAC-SHA256 PIN hasher with an explicit, honest storage format.
 * Uses a random salt and a work factor suitable for PIN protection at rest.
 */
object Pbkdf2PinHasher {
    private const val SALT_LENGTH = 16
    const val ITERATIONS = 600000
    private const val KEY_LENGTH = 256
    private const val ALGORITHM = "PBKDF2WithHmacSHA256"

    data class HashResult(
        val saltBase64: String,
        val hashFormatted: String
    )

    private fun encodeBase64(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (e: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }

    private fun decodeBase64(str: String): ByteArray {
        return try {
            java.util.Base64.getDecoder().decode(str)
        } catch (e: Throwable) {
            android.util.Base64.decode(str, android.util.Base64.NO_WRAP)
        }
    }

    fun generateSalt(): String {
        val random = SecureRandom()
        val salt = ByteArray(SALT_LENGTH)
        random.nextBytes(salt)
        return encodeBase64(salt)
    }

    fun hashPin(pin: String, saltBase64: String): String {
        val salt = decodeBase64(saltBase64)
        val spec = PBEKeySpec(pin.toCharArray(), salt, ITERATIONS, KEY_LENGTH)
        val factory = SecretKeyFactory.getInstance(ALGORITHM)
        val hashBytes = factory.generateSecret(spec).encoded
        val hashEncoded = encodeBase64(hashBytes)
        return "\$pbkdf2-sha256\$i=$ITERATIONS\$$saltBase64\$$hashEncoded"
    }

    fun createNewPinHash(pin: String): HashResult {
        val salt = generateSalt()
        val formatted = hashPin(pin, salt)
        return HashResult(saltBase64 = salt, hashFormatted = formatted)
    }

    fun verifyPin(pin: String, storedHash: String, storedSalt: String): Boolean {
        return try {
            val computed = hashPin(pin, storedSalt)
            // Constant-time comparison to prevent side-channel timing attacks
            MessageDigest.isEqual(
                computed.toByteArray(Charsets.UTF_8),
                storedHash.toByteArray(Charsets.UTF_8)
            )
        } catch (e: Exception) {
            false
        }
    }
}
