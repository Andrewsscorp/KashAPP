package com.example.core.id

import java.security.MessageDigest
import java.util.Locale

/**
 * Universal Deterministic UID Generator derived strictly from the user's normalized email.
 *
 * Guarantees:
 * 1. Hardware Independent: Does NOT rely on ANDROID_ID, device hardware serial, or local Keystore.
 * 2. Device Agnostic: Identical UID across mobile, tablet, desktop, or web ingress.
 * 3. Reinstall Resilient: Uninstallation of the app wipes local caches, but logging back in
 *    with the same email produces the mathematically identical UID every time.
 */
object EmailDeterministicUidGenerator {

    private const val PREFIX = "usr_"
    private val EMAIL_REGEX = Regex("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$")

    /**
     * Normalizes the email address by trimming surrounding whitespace and converting to lower case.
     */
    fun normalizeEmail(email: String): String {
        return email.trim().lowercase(Locale.ROOT)
    }

    /**
     * Validates if the string has a valid email structure.
     */
    fun isValidEmail(email: String): Boolean {
        val normalized = normalizeEmail(email)
        return normalized.isNotEmpty() && EMAIL_REGEX.matches(normalized)
    }

    /**
     * Computes the deterministic, cryptographically collision-resistant UID from an email.
     *
     * @param email User email (e.g., "Andres.Suarez@gmail.com")
     * @return Deterministic UID formatted as "usr_" + 32-char SHA-256 hex (e.g., "usr_e3b0c44298fc1c149afbf4c8996fb924")
     * @throws IllegalArgumentException if the email is blank
     */
    fun fromEmail(email: String): String {
        val normalized = normalizeEmail(email)
        require(normalized.isNotBlank()) { "Email cannot be blank for deterministic UID generation" }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(normalized.toByteArray(Charsets.UTF_8))
        val hexString = hashBytes.joinToString("") { "%02x".format(it) }

        // Take first 32 characters (128-bit security equivalent to UUID) with "usr_" prefix (total 36 chars)
        return "$PREFIX${hexString.take(32)}"
    }

    /**
     * Safely computes deterministic UID or returns null if email is null, blank or malformed.
     */
    fun fromEmailOrNull(email: String?): String? {
        if (email.isNullOrBlank()) return null
        val normalized = normalizeEmail(email)
        if (!normalized.contains('@') || normalized.length < 5) return null
        return try {
            fromEmail(normalized)
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Computes the full 64-character SHA-256 hash representation of the email.
     */
    fun fullHashFromEmail(email: String): String {
        val normalized = normalizeEmail(email)
        require(normalized.isNotBlank()) { "Email cannot be blank for deterministic UID generation" }

        val digest = MessageDigest.getInstance("SHA-256")
        val hashBytes = digest.digest(normalized.toByteArray(Charsets.UTF_8))
        return hashBytes.joinToString("") { "%02x".format(it) }
    }

    /**
     * Verifies if a given string is a valid deterministic email UID.
     */
    fun isDeterministicEmailUid(uid: String): Boolean {
        if (!uid.startsWith(PREFIX)) return false
        val hexPart = uid.removePrefix(PREFIX)
        return hexPart.length == 32 && hexPart.all { it in '0'..'9' || it in 'a'..'f' }
    }
}
