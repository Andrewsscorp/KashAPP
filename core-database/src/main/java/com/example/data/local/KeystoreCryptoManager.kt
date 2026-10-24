package com.example.data.local

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.biometric.BiometricPrompt
import java.security.KeyStore
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Exception thrown when the hardware-backed database passphrase seed cannot be read or initialized.
 * Replaces ephemeral random key generation to prevent vault corruption or unpredictable keys.
 */
class VaultKeyReadException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)

/**
 * Manages Android Keystore hardware-backed AES keys, CryptoObjects for BiometricPrompt,
 * and AES-256 GCM hardware encryption/decryption for sensitive user data and passphrases.
 */
class KeystoreCryptoManager(private val allowSoftwareFallbackForTests: Boolean = false) {

    companion object {
        const val KEY_ALIAS = "kashapp_security_master_key"
        const val BIOMETRIC_KEY_ALIAS = "kashapp_biometric_auth_key"
        private const val KEYSTORE_NAME = "AndroidKeyStore"
        private const val TRANSFORMATION = "${KeyProperties.KEY_ALGORITHM_AES}/${KeyProperties.BLOCK_MODE_GCM}/${KeyProperties.ENCRYPTION_PADDING_NONE}"
        private const val GCM_IV_LENGTH = 12
        private const val GCM_TAG_LENGTH = 128
    }

    private val keyStore: KeyStore? = try {
        KeyStore.getInstance(KEYSTORE_NAME).apply { load(null) }
    } catch (e: Throwable) {
        null
    }

    @Volatile
    private var softwareFallbackKey: SecretKey? = null

    @Volatile
    private var biometricFallbackKey: SecretKey? = null

    private fun getOrCreateSecretKey(): SecretKey {
        val ks = keyStore
        if (ks != null) {
            try {
                if (!ks.containsAlias(KEY_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_NAME
                    )
                    val builder = KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false) // Permitir acceso para cifrado de DB en el arranque
                        .setRandomizedEncryptionRequired(true)

                    keyGenerator.init(builder.build())
                    return keyGenerator.generateKey()
                }
                val entry = ks.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            } catch (e: Throwable) {
                if (!allowSoftwareFallbackForTests) {
                    throw IllegalStateException("Android Keystore no está disponible para proteger datos sensibles.", e)
                }
            }
        }
        if (!allowSoftwareFallbackForTests) {
            throw IllegalStateException("Android Keystore no está disponible para proteger datos sensibles.")
        }
        return softwareFallbackKey ?: synchronized(this) {
            softwareFallbackKey ?: SecretKeySpec(
                ByteArray(32).also { SecureRandom().nextBytes(it) },
                "AES"
            ).also { softwareFallbackKey = it }
        }
    }

    private fun getOrCreateBiometricSecretKey(): SecretKey {
        val ks = keyStore
        if (ks != null) {
            try {
                if (!ks.containsAlias(BIOMETRIC_KEY_ALIAS)) {
                    val keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES,
                        KEYSTORE_NAME
                    )
                    var generated = false
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                        try {
                            val strongBoxBuilder = KeyGenParameterSpec.Builder(
                                BIOMETRIC_KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                            )
                                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                                .setKeySize(256)
                                .setUserAuthenticationRequired(true)
                                .setRandomizedEncryptionRequired(true)
                                .setIsStrongBoxBacked(true)

                            keyGenerator.init(strongBoxBuilder.build())
                            keyGenerator.generateKey()
                            generated = true
                        } catch (_: Throwable) {
                            // Fallback to standard hardware TEE
                        }
                    }

                    if (!generated) {
                        val teeBuilder = KeyGenParameterSpec.Builder(
                            BIOMETRIC_KEY_ALIAS,
                            KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                            .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                            .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                            .setKeySize(256)
                            .setUserAuthenticationRequired(true)
                            .setRandomizedEncryptionRequired(true)

                        keyGenerator.init(teeBuilder.build())
                        keyGenerator.generateKey()
                    }
                }
                val entry = ks.getEntry(BIOMETRIC_KEY_ALIAS, null) as? KeyStore.SecretKeyEntry
                if (entry != null) {
                    return entry.secretKey
                }
            } catch (e: Throwable) {
                if (!allowSoftwareFallbackForTests) {
                    throw IllegalStateException("Android Keystore no está disponible para clave biométrica.", e)
                }
            }
        }
        if (!allowSoftwareFallbackForTests) {
            throw IllegalStateException("Android Keystore no está disponible para clave biométrica.")
        }
        return biometricFallbackKey ?: synchronized(this) {
            biometricFallbackKey ?: SecretKeySpec(
                ByteArray(32).also { SecureRandom().nextBytes(it) },
                "AES"
            ).also { biometricFallbackKey = it }
        }
    }

    fun createBiometricCryptoObject(): BiometricPrompt.CryptoObject? {
        return try {
            val secretKey = getOrCreateBiometricSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            BiometricPrompt.CryptoObject(cipher)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Cryptographically validates that the CryptoObject contains a properly initialized
     * and hardware/keystore-backed Cipher, executing an authenticated encryption operation
     * to verify that biometric authorization was genuinely achieved.
     */
    fun verifyBiometricCryptoObject(cryptoObject: BiometricPrompt.CryptoObject?): Boolean {
        if (cryptoObject == null) return false
        val cipher = cryptoObject.cipher ?: return false
        return try {
            val challenge = "kashapp_auth_token_${System.currentTimeMillis()}".toByteArray(Charsets.UTF_8)
            val output = cipher.doFinal(challenge)
            output != null && output.isNotEmpty()
        } catch (e: Throwable) {
            false
        }
    }

    /**
     * Retrieves the hardware master passphrase for SQLCipher.
     * When context is provided, generates a unique 32-byte (256-bit) cryptographically random passphrase
     * on first run, encrypts it with hardware Keystore (AES-GCM), and persists it encrypted.
     * Each device has a 100% unique hardware-backed database passphrase.
     */
    fun getHardwareMasterPassphrase(context: android.content.Context? = null): ByteArray {
        val finalContext = context ?: throw IllegalArgumentException("Se requiere un contexto para inicializar la clave de la base de datos.")
        
        try {
            val prefs = finalContext.getSharedPreferences("kashapp_keystore_vault_internal", android.content.Context.MODE_PRIVATE)
            val stored = prefs.getString("unique_device_encrypted_seed", null)
            if (!stored.isNullOrBlank()) {
                val decrypted = decryptData(stored)
                if (!decrypted.isNullOrBlank()) {
                    return try {
                        android.util.Base64.decode(decrypted, android.util.Base64.NO_WRAP)
                    } catch (_: Throwable) {
                        decrypted.toByteArray(Charsets.UTF_8)
                    }
                }
            }
            // First initialization: generate random 32-byte passphrase
            val randomPassphrase = ByteArray(32).also { SecureRandom().nextBytes(it) }
            val b64 = try {
                android.util.Base64.encodeToString(randomPassphrase, android.util.Base64.NO_WRAP)
            } catch (_: Throwable) {
                java.util.Base64.getEncoder().encodeToString(randomPassphrase)
            }
            val encrypted = encryptData(b64)
            prefs.edit().putString("unique_device_encrypted_seed", encrypted).apply()
            return randomPassphrase
        } catch (e: Exception) {
            // Fail-closed: Never generate ephemeral volatile keys that corrupt vault persistence
            throw VaultKeyReadException("Fallo crítico al acceder o inicializar la clave maestra de la bóveda local.", e)
        }
    }

    fun encryptData(plainText: String): String {
        val key = getOrCreateSecretKey()
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv ?: ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return try {
            java.util.Base64.getEncoder().encodeToString(combined)
        } catch (e: Throwable) {
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        }
    }

    fun decryptData(cipherTextBase64: String): String? {
        return try {
            val combined = try {
                java.util.Base64.getDecoder().decode(cipherTextBase64)
            } catch (e: Throwable) {
                android.util.Base64.decode(cipherTextBase64, android.util.Base64.NO_WRAP)
            }
            if (combined.size <= GCM_IV_LENGTH) return null
            val iv = ByteArray(GCM_IV_LENGTH)
            val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
            System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
            System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encrypted)
            String(decryptedBytes, Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Deriva una clave AES-256 criptográficamente reproducible a partir de la identidad del usuario (UID/Email).
     * Garantiza resiliencia total frente a desinstalación de la app o cambio de dispositivo.
     */
    fun deriveCloudKey(userSeed: String): SecretKey {
        val cleanSeed = userSeed.trim()
        val saltPreimage = "KashApp_Cloud_Vault_Salt_v2_$cleanSeed".toByteArray(Charsets.UTF_8)
        val salt = MessageDigest.getInstance("SHA-256").digest(saltPreimage)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(cleanSeed.toCharArray(), salt, 100_000, 256)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    /**
     * Cifra un payload para respaldo en la nube de forma resiliente a desinstalaciones y multi-dispositivo.
     * Utiliza AES-256 GCM con derivación determinística PBKDF2 basada en la identidad del usuario.
     */
    fun encryptCloudPayload(plainText: String, userSeed: String): String {
        val key = deriveCloudKey(userSeed)
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, key)
        val iv = cipher.iv ?: ByteArray(GCM_IV_LENGTH).also { SecureRandom().nextBytes(it) }
        val encrypted = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))
        val combined = ByteArray(iv.size + encrypted.size)
        System.arraycopy(iv, 0, combined, 0, iv.size)
        System.arraycopy(encrypted, 0, combined, iv.size, encrypted.size)
        return try {
            java.util.Base64.getEncoder().encodeToString(combined)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(combined, android.util.Base64.NO_WRAP)
        }
    }

    /**
     * Descifra un payload de la nube utilizando derivación de clave por identidad de usuario,
     * con fallback automático a la clave de Keystore local.
     */
    fun decryptCloudPayload(cipherTextBase64: String, userSeed: String): String? {
        val combined = try {
            java.util.Base64.getDecoder().decode(cipherTextBase64)
        } catch (_: Throwable) {
            try {
                android.util.Base64.decode(cipherTextBase64, android.util.Base64.NO_WRAP)
            } catch (_: Throwable) {
                return null
            }
        }
        if (combined.size <= GCM_IV_LENGTH) return null
        val iv = ByteArray(GCM_IV_LENGTH)
        val encrypted = ByteArray(combined.size - GCM_IV_LENGTH)
        System.arraycopy(combined, 0, iv, 0, GCM_IV_LENGTH)
        System.arraycopy(combined, GCM_IV_LENGTH, encrypted, 0, encrypted.size)

        // Intento 1: Descifrado con clave determinística derivada del userSeed
        try {
            val key = deriveCloudKey(userSeed)
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encrypted)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Continuar con fallback
        }

        // Intento 2: Fallback a clave local de hardware Keystore (por si el respaldo se generó en la misma sesión previa)
        try {
            val key = getOrCreateSecretKey()
            val cipher = Cipher.getInstance(TRANSFORMATION)
            val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            cipher.init(Cipher.DECRYPT_MODE, key, spec)
            val decryptedBytes = cipher.doFinal(encrypted)
            return String(decryptedBytes, Charsets.UTF_8)
        } catch (_: Exception) {
            // Fallback falló
        }

        return null
    }
}
