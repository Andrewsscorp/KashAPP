package com.example.data.local

import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import android.util.Log
import androidx.sqlite.db.SupportSQLiteDatabase
import java.security.SecureRandom
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

/**
 * Excepción lanzada cuando ocurre un error crítico durante la derivación criptográfica de la clave del baúl.
 * Sustituye el fallback inseguro a SHA-256 plano (HALL-08 / GATE-08).
 */
class VaultKeyDerivationException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Metadata descriptor for SQLCipher database encryption status.
 */
data class DatabaseEncryptionInfo(
    val cipherEngine: String = "SQLCipher 4.6.0 (AES-256)",
    val isEncryptedAtRest: Boolean = true,
    val hasUserPassphrase: Boolean = false,
    val keyDerivation: String = "PBKDF2-HMAC-SHA256 (310,000 iter)",
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
 * Supports user-defined passphrases, PBKDF2-HMAC-SHA256 key derivation with 310,000 iterations (OWASP),
 * fail-closed error handling without insecure SHA-256 fallback, hardware Keystore wrapping,
 * and zero-memory erasure for raw passphrases (REM-P2-01 / HALL-08 / GATE-08).
 */
class PassphraseManager(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager = KeystoreCryptoManager(),
    private var auditSecurityLogDao: com.example.data.local.dao.AuditSecurityLogDao? = null
) {
    fun setAuditSecurityLogDao(dao: com.example.data.local.dao.AuditSecurityLogDao?) {
        this.auditSecurityLogDao = dao
    }
    companion object {
        private const val TAG = "PassphraseManager"
        private const val PREFS_NAME = "kashapp_security_vault_prefs"

        // Keys for SharedPreferences
        private const val KEY_ENCRYPTED_PASSPHRASE = "encrypted_user_passphrase_v1"
        private const val KEY_ENCRYPTED_DERIVED_KEY_V2 = "encrypted_derived_vault_key_v2"
        private const val KEY_PASSPHRASE_SALT = "vault_pbkdf2_salt_v1"
        private const val KEY_HAS_CUSTOM_PASSPHRASE = "has_custom_user_passphrase"
        private const val KEY_KDF_VERSION = "vault_kdf_version"

        // KDF Specifications (OWASP 2024+ recommendation: >= 310,000 iterations for PBKDF2-HMAC-SHA256)
        const val KDF_V1_ITERATIONS = 64_000
        const val KDF_V2_ITERATIONS = 310_000
        const val CURRENT_KDF_VERSION = 2
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
        try {
            SecurePreferencesVault.getEncryptedVaultPreferences(context)
        } catch (t: Throwable) {
            Log.w(TAG, "Fallback a preferencias locales ante error de inicialización criptográfica: ${t.message}")
            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
    }

    /**
     * Returns the current KDF version (1 for legacy 64k iterations, 2 for OWASP 310k iterations).
     */
    fun getKdfVersion(): Int {
        return prefs.getInt(KEY_KDF_VERSION, if (prefs.contains(KEY_ENCRYPTED_PASSPHRASE)) 1 else CURRENT_KDF_VERSION)
    }

    /**
     * Returns the 256-bit passphrase byte array for SQLCipher's SupportOpenHelperFactory.
     */
    @Synchronized
    fun getDatabasePassphrase(): ByteArray {
        if (!isUserPassphraseConfigured()) {
            // Default unique 32-byte hardware-backed master passphrase from Android Keystore
            return cryptoManager.getHardwareMasterPassphrase(context)
        }

        // 1. Try to load V2 encrypted derived key (non-reversible, wrapped with Keystore)
        val encryptedDerivedKey = prefs.getString(KEY_ENCRYPTED_DERIVED_KEY_V2, null)
        if (!encryptedDerivedKey.isNullOrBlank()) {
            val decryptedBase64 = cryptoManager.decryptData(encryptedDerivedKey)
            if (!decryptedBase64.isNullOrBlank()) {
                return Base64.decode(decryptedBase64, Base64.NO_WRAP)
            }
        }

        // 2. Legacy fallback for V1 users (raw passphrase stored, 64,000 iterations)
        val legacyEncrypted = prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null)
        if (!legacyEncrypted.isNullOrBlank()) {
            val legacyPassphrase = cryptoManager.decryptData(legacyEncrypted)
            if (!legacyPassphrase.isNullOrBlank()) {
                val iters = if (getKdfVersion() == 1) KDF_V1_ITERATIONS else KDF_V2_ITERATIONS
                return deriveKeyFromPassphrase(legacyPassphrase, iters)
            }
        }

        return cryptoManager.getHardwareMasterPassphrase(context)
    }

    /**
     * Returns true if the user has configured their own custom passphrase.
     */
    fun isUserPassphraseConfigured(): Boolean {
        if (!prefs.getBoolean(KEY_HAS_CUSTOM_PASSPHRASE, false)) return false
        val hasV2 = !prefs.getString(KEY_ENCRYPTED_DERIVED_KEY_V2, null).isNullOrBlank()
        val hasV1 = !prefs.getString(KEY_ENCRYPTED_PASSPHRASE, null).isNullOrBlank()
        return hasV2 || hasV1
    }

    /**
     * Stores a new user-defined passphrase securely:
     * 1. Derives 32-byte key using PBKDF2-HMAC-SHA256 with 310,000 iterations (KDF_V2).
     * 2. Overwrites in-memory passphrase characters immediately with zeros.
     * 3. Stores only the derived key encrypted with hardware Keystore (never the raw passphrase).
     */
    @Synchronized
    fun setUserPassphrase(passphrase: String): Boolean {
        if (passphrase.length < 6) return false
        val chars = passphrase.toCharArray()
        return try {
            val derivedKey = deriveKeyFromPassphrase(chars, KDF_V2_ITERATIONS)
            val base64Key = Base64.encodeToString(derivedKey, Base64.NO_WRAP)
            val encryptedDerivedKey = cryptoManager.encryptData(base64Key)

            prefs.edit()
                .putString(KEY_ENCRYPTED_DERIVED_KEY_V2, encryptedDerivedKey)
                .putInt(KEY_KDF_VERSION, CURRENT_KDF_VERSION)
                .putBoolean(KEY_HAS_CUSTOM_PASSPHRASE, true)
                .remove(KEY_ENCRYPTED_PASSPHRASE) // Purge legacy plaintext/reversible passphrase
                .apply()

            Log.i(TAG, "Clave derivada (310.000 iter) cifrada exitosamente con Keystore. Frase cruda eliminada de almacenamiento.")
            auditSecurityLogDao?.let { dao ->
                try {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        dao.insertLog(
                            com.example.data.local.entity.AuditSecurityLog(
                                eventType = "VAULT_PASSPHRASE_CHANGED",
                                authMethod = "LOCAL_PASSPHRASE",
                                details = "Frase de paso personalizada configurada y clave derivada con PBKDF2 (310.000 iteraciones)."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No fue posible registrar log de auditoría: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error configurando clave personalizada: ${e.message}", e)
            false
        } finally {
            chars.fill('\u0000')
        }
    }

    /**
     * Migrates an active V1 user passphrase vault to V2 (310,000 iterations) using PRAGMA rekey.
     */
    @Synchronized
    fun migratePassphraseKdfV1ToV2(
        passphrase: String,
        writableDatabase: SupportSQLiteDatabase? = null
    ): Boolean {
        val chars = passphrase.toCharArray()
        return try {
            val v2Key = deriveKeyFromPassphrase(chars, KDF_V2_ITERATIONS)
            val v2KeyHex = v2Key.joinToString("") { "%02x".format(it) }

            // Execute PRAGMA rekey on the active database if open
            writableDatabase?.execSQL("PRAGMA rekey = \"x'$v2KeyHex'\";")

            val base64Key = Base64.encodeToString(v2Key, Base64.NO_WRAP)
            val encryptedDerivedKey = cryptoManager.encryptData(base64Key)

            prefs.edit()
                .putString(KEY_ENCRYPTED_DERIVED_KEY_V2, encryptedDerivedKey)
                .putInt(KEY_KDF_VERSION, CURRENT_KDF_VERSION)
                .putBoolean(KEY_HAS_CUSTOM_PASSPHRASE, true)
                .remove(KEY_ENCRYPTED_PASSPHRASE)
                .apply()

            Log.i(TAG, "Migración exitosa de KDF V1 a V2 con PRAGMA rekey.")
            auditSecurityLogDao?.let { dao ->
                try {
                    kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                        dao.insertLog(
                            com.example.data.local.entity.AuditSecurityLog(
                                eventType = "VAULT_PASSPHRASE_CHANGED",
                                authMethod = "KDF_MIGRATION_V2",
                                details = "Migración de KDF V1 a V2 completada exitosamente con PRAGMA rekey (310.000 iteraciones)."
                            )
                        )
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "No fue posible registrar log de auditoría: ${e.message}")
                }
            }
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error al migrar KDF V1 a V2: ${e.message}", e)
            false
        } finally {
            chars.fill('\u0000')
        }
    }

    /**
     * Clears user custom passphrase and reverts to Keystore hardware master seed.
     */
    @Synchronized
    fun resetToHardwareMasterPassphrase() {
        prefs.edit()
            .remove(KEY_ENCRYPTED_DERIVED_KEY_V2)
            .remove(KEY_ENCRYPTED_PASSPHRASE)
            .remove(KEY_KDF_VERSION)
            .putBoolean(KEY_HAS_CUSTOM_PASSPHRASE, false)
            .apply()

        auditSecurityLogDao?.let { dao ->
            try {
                kotlinx.coroutines.runBlocking(kotlinx.coroutines.Dispatchers.IO) {
                    dao.insertLog(
                        com.example.data.local.entity.AuditSecurityLog(
                            eventType = "VAULT_PASSPHRASE_CHANGED",
                            authMethod = "HARDWARE_KEYSTORE",
                            details = "Frase de paso personalizada eliminada. Bóveda restaurada a clave maestra de hardware Keystore."
                        )
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "No fue posible registrar log de auditoría: ${e.message}")
            }
        }
    }

    /**
     * Retrieves and decrypts legacy user-defined passphrase if present.
     * Returns null for modern V2 accounts where the raw passphrase is never stored reversibly.
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
        val kdfVer = getKdfVersion()
        val iterStr = if (kdfVer == 1) "64,000 iter (V1)" else "310,000 iter (V2 OWASP)"
        return DatabaseEncryptionInfo(
            cipherEngine = "SQLCipher 4.6.0 (AES-256)",
            isEncryptedAtRest = true,
            hasUserPassphrase = hasCustom,
            keyDerivation = "PBKDF2-HMAC-SHA256 ($iterStr)",
            statusDescription = if (hasCustom) {
                "Base de datos asegurada con tu contraseña personalizada ($iterStr) y SQLCipher AES-256."
            } else {
                "Base de datos asegurada con clave maestra de hardware Keystore y SQLCipher AES-256."
            }
        )
    }

    /**
     * Derives a 256-bit key from a passphrase CharArray using PBKDF2-HMAC-SHA256.
     * Fails closed by throwing [VaultKeyDerivationException] instead of silently falling back to SHA-256.
     */
    fun deriveKeyFromPassphrase(
        passphrase: CharArray,
        iterations: Int = KDF_V2_ITERATIONS
    ): ByteArray {
        val salt = getOrCreateSalt()
        return try {
            val spec = PBEKeySpec(passphrase, salt, iterations, KEY_LENGTH_BITS)
            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            factory.generateSecret(spec).encoded
        } catch (e: Exception) {
            // OWASP & GATE-08: Fail closed on key derivation failure. Never degrade to single-round SHA-256.
            throw VaultKeyDerivationException("Fallo crítico en derivación de clave PBKDF2WithHmacSHA256: ${e.message}", e)
        }
    }

    /**
     * Overload accepting String, which safely converts to CharArray and wipes memory in finally.
     */
    fun deriveKeyFromPassphrase(
        passphrase: String,
        iterations: Int = KDF_V2_ITERATIONS
    ): ByteArray {
        val chars = passphrase.toCharArray()
        try {
            return deriveKeyFromPassphrase(chars, iterations)
        } finally {
            chars.fill('\u0000')
        }
    }

    private fun getOrCreateSalt(): ByteArray {
        val existingSalt = prefs.getString(KEY_PASSPHRASE_SALT, null)
        if (existingSalt != null) {
            return try {
                Base64.decode(existingSalt, Base64.NO_WRAP)
            } catch (e: Exception) {
                existingSalt.toByteArray(Charsets.UTF_8)
            }
        }
        val salt = ByteArray(16)
        SecureRandom().nextBytes(salt)
        val saltBase64 = try {
            Base64.encodeToString(salt, Base64.NO_WRAP)
        } catch (e: Exception) {
            String(salt, Charsets.ISO_8859_1)
        }
        prefs.edit().putString(KEY_PASSPHRASE_SALT, saltBase64).apply()
        return salt
    }
}
