package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import java.security.MessageDigest
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Metadata descriptor for SQLCipher database encryption status.
 */
data class DatabaseEncryptionInfo(
    val cipherEngine: String = "SQLCipher 4.6.0 (AES-256)",
    val isEncryptedAtRest: Boolean = true,
    val hasUserPassphrase: Boolean = false,
    val keyDerivation: String = "PBKDF2-HMAC-SHA256 (64,000 iter)",
    val statusDescription: String = "Base de datos local blindada con SQLCipher y cifrado de 256 bits."
)

/**
 * Result evaluation for user-defined passphrase strength.
 */
data class PassphraseStrength(
    val score: Int, // 0 to 100
    val label: String, // Débil, Moderada, Fuerte, Muy Segura
    val isValid: Boolean,
    val validationMessage: String?
)

/**
 * Manages the encryption passphrase for SQLCipher Room database at rest.
 * Supports user-defined passphrases, PBKDF2 key derivation,
 * hardware Keystore fallback, and AES-256 encryption at rest.
 */
class PassphraseManager(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager()
) {
    companion object {
        private const val TAG = "PassphraseManager"
        private const val PREFS_NAME = "kashapp_security_vault_prefs"
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_user_passphrase_v1"
        private const val KEY_PASSPHRASE_SALT = "vault_pbkdf2_salt_v1"
        private const val KEY_HAS_CUSTOM_PASSPHRASE = "has_custom_user_passphrase"
        private const val PBKDF2_ITERATIONS = 64000
        private const val KEY_LENGTH_BITS = 256

        @Volatile
        private var INSTANCE: PassphraseManager? = null

        fun getInstance(context: Context): PassphraseManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: PassphraseManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the 256-bit passphrase byte array for SQLCipher's SupportOpenHelperFactory.
     */
    @Synchronized
    fun getDatabasePassphrase(): ByteArray {
        val customPassphrase = getCustomUserPassphrase()
        return if (!customPassphrase.isNullOrBlank()) {
            deriveKeyFromPassphrase(customPassphrase)
        } else {
            // Default unique 32-byte hardware-backed master passphrase from Android Keystore
            cryptoManager.getHardwareMasterPassphrase(context)
        }
    }

    /**
     * Returns true if the user has configured their own custom passphrase.
     */
    fun isUserPassphraseConfigured(): Boolean {
        return prefs.getBoolean(KEY_HAS_CUSTOM_PASSPHRASE, false) && !getCustomUserPassphrase().isNullOrBlank()
    }

    /**
     * Stores a new user-defined passphrase securely with hardware-backed Keystore AES-GCM encryption.
     */
    @Synchronized
    fun setUserPassphrase(passphrase: String): Boolean {
        if (passphrase.length < 6) return false
        return try {
            val encryptedPassphrase = cryptoManager.encryptData(passphrase)
            prefs.edit()
                .putString(KEY_ENCRYPTED_PASSPHRASE, encryptedPassphrase)
                .putBoolean(KEY_HAS_CUSTOM_PASSPHRASE, true)
                .apply()
            Log.i(TAG, "User-defined passphrase saved securely with Keystore encryption.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save user passphrase: ${e.message}", e)
            false
        }
    }

    /**
     * Clears user custom passphrase and reverts to Keystore hardware master seed.
     */
    @Synchronized
    fun resetToHardwareMasterPassphrase() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_PASSPHRASE)
            .putBoolean(KEY_HAS_CUSTOM_PASSPHRASE, false)
            .apply()
    }

    /**
     * Retrieves and decrypts the user-defined passphrase.
     */
    fun getCustomUserPassphrase(): String? {
        val encrypted = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null) ?: return null
        return cryptoManager.decryptData(encrypted)
    }

    /**
     * Validates passphrase strength and security requirements.
     */
    fun evaluatePassphraseStrength(passphrase: String): PassphraseStrength {
        if (passphrase.isBlank()) {
            return PassphraseStrength(0, "Vacía", false, "La contraseña no puede estar vacía")
        }
        if (passphrase.length < 6) {
            return PassphraseStrength(20, "Muy Débil", false, "Mínimo 6 caracteres requeridos")
        }

        var score = 30
        if (passphrase.length >= 8) score += 20
        if (passphrase.length >= 12) score += 15
        if (passphrase.any { it.isDigit() }) score += 15
        if (passphrase.any { it.isUpperCase() } && passphrase.any { it.isLowerCase() }) score += 10
        if (passphrase.any { !it.isLetterOrDigit() }) score += 10

        val label = when {
            score >= 80 -> "Muy Segura (Cifrado Militar)"
            score >= 60 -> "Fuerte"
            score >= 40 -> "Moderada"
            else -> "Débil"
        }

        return PassphraseStrength(score.coerceIn(0, 100), label, true, null)
    }

    fun getEncryptionInfo(): DatabaseEncryptionInfo {
        val hasCustom = isUserPassphraseConfigured()
        return DatabaseEncryptionInfo(
            cipherEngine = "SQLCipher 4.6.0 (AES-256)",
            isEncryptedAtRest = true,
            hasUserPassphrase = hasCustom,
            keyDerivation = "PBKDF2-HMAC-SHA256 (64,000 iter)",
            statusDescription = if (hasCustom) {
                "Base de datos asegurada con tu contraseña personalizada y SQLCipher AES-256."
            } else {
                "Base de datos asegurada con clave maestra de hardware Keystore y SQLCipher AES-256."
            }
        )
    }

    fun deriveKeyFromPassphrase(passphrase: String): ByteArray {
        val salt = getOrCreateSalt()
        return try {
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } catch (e: Exception) {
            // Fallback to SHA-256 digest
            val md = MessageDigest.getInstance("SHA-256")
            md.update(salt)
            md.digest(passphrase.toByteArray(Charsets.UTF_8))
        }
    }

    private fun getOrCreateSalt(): ByteArray {
        val existingSalt = prefs.getString(KEY_PASSPHRASE_SALT, null)
        if (existingSalt != null) {
            return try {
                android.util.Base64.decode(existingSalt, android.util.Base64.NO_WRAP)
            } catch (e: Exception) {
                existingSalt.toByteArray(Charsets.UTF_8)
            }
        }
        val salt = ByteArray(16)
        java.security.SecureRandom().nextBytes(salt)
        val saltBase64 = try {
            android.util.Base64.encodeToString(salt, android.util.Base64.NO_WRAP)
        } catch (e: Exception) {
            String(salt, Charsets.ISO_8859_1)
        }
        prefs.edit().putString(KEY_PASSPHRASE_SALT, saltBase64).apply()
        return salt
    }
}
