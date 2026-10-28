package com.example.data.local

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.zip.GZIPInputStream
import java.util.zip.GZIPOutputStream
import javax.crypto.Cipher
import javax.crypto.CipherInputStream
import javax.crypto.CipherOutputStream
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * Resultado de una operación de empaquetado cifrado por streaming.
 */
data class EncryptedBackupMetadata(
    val outputFile: File,
    val plainSizeBytes: Long,
    val encryptedSizeBytes: Long,
    val plaintextSha256Hex: String,
    val kdfIterations: Int = KashBackupEncryptionEngine.PBKDF2_ITERATIONS
)

/**
 * Motor de cifrado E2EE por streaming (AES-256-GCM + GZIP + PBKDF2 310.000 iteraciones + AAD).
 * Opera con memoria constante O(1) usando buffers de 8 KB para evitar OutOfMemoryError.
 */
class KashBackupEncryptionEngine(
    private val secureRandom: SecureRandom = SecureRandom()
) {
    companion object {
        val MAGIC_HEADER = "KASH_E2E".toByteArray(Charsets.US_ASCII) // 8 bytes
        const val CURRENT_VERSION: Byte = 0x02
        const val KDF_MODE_PBKDF2_310K: Byte = 0x01
        const val SALT_LENGTH_BYTES = 16
        const val IV_LENGTH_BYTES = 12
        const val HEADER_TOTAL_BYTES = 8 + 1 + 1 + SALT_LENGTH_BYTES + IV_LENGTH_BYTES // 38 bytes
        const val GCM_TAG_LENGTH_BITS = 128
        const val PBKDF2_ITERATIONS = 310_000
        const val KEY_LENGTH_BITS = 256
        private const val BUFFER_SIZE = 8192
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }

    /**
     * Comprime con GZIP y cifra por streaming un archivo fuente hacia un archivo `.kash_enc`.
     * Limpia el arreglo de caracteres de la contraseña en el bloque finally.
     */
    fun encryptBackupFile(
        plainInputFile: File,
        encryptedOutputFile: File,
        passphrase: String
    ): Result<EncryptedBackupMetadata> {
        val passChars = passphrase.toCharArray()
        return try {
            require(plainInputFile.exists() && plainInputFile.length() > 0L) {
                "El archivo de respaldo origen no existe o está vacío."
            }
            require(passChars.size >= 6) {
                "La clave de cifrado del respaldo debe tener al menos 6 caracteres."
            }

            val salt = ByteArray(SALT_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val iv = ByteArray(IV_LENGTH_BYTES).also { secureRandom.nextBytes(it) }
            val secretKey = deriveAesKey(passChars, salt, PBKDF2_ITERATIONS)

            val headerBytes = buildHeader(CURRENT_VERSION, KDF_MODE_PBKDF2_310K, salt, iv)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
            cipher.updateAAD(headerBytes)

            val sha256Digest = MessageDigest.getInstance("SHA-256")
            var plainBytesReadTotal = 0L

            FileOutputStream(encryptedOutputFile).use { fos ->
                // 1. Escribir cabecera de 38 bytes autenticada con AAD
                fos.write(headerBytes)

                // 2. Canal de cifrado por bloques -> Compresión GZIP
                CipherOutputStream(fos, cipher).use { cos ->
                    GZIPOutputStream(cos, BUFFER_SIZE).use { gzipOut ->
                        FileInputStream(plainInputFile).use { fis ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (fis.read(buffer).also { read = it } != -1) {
                                gzipOut.write(buffer, 0, read)
                                sha256Digest.update(buffer, 0, read)
                                plainBytesReadTotal += read
                            }
                            gzipOut.finish()
                        }
                    }
                }
            }

            val sha256Hex = sha256Digest.digest().joinToString("") { "%02x".format(it) }
            Result.success(
                EncryptedBackupMetadata(
                    outputFile = encryptedOutputFile,
                    plainSizeBytes = plainBytesReadTotal,
                    encryptedSizeBytes = encryptedOutputFile.length(),
                    plaintextSha256Hex = sha256Hex
                )
            )
        } catch (e: Exception) {
            if (encryptedOutputFile.exists()) encryptedOutputFile.delete()
            Result.failure(e)
        } finally {
            passChars.fill('\u0000')
        }
    }

    /**
     * Descifra y descomprime por streaming un archivo `.kash_enc`, verificando el tag GCM (128-bit)
     * y opcionalmente el checksum SHA-256 del contenido original.
     */
    fun decryptBackupFile(
        encryptedInputFile: File,
        plainOutputFile: File,
        passphrase: String,
        expectedPlaintextSha256Hex: String? = null
    ): Result<File> {
        val passChars = passphrase.toCharArray()
        return try {
            if (!isEncryptedKashBackup(encryptedInputFile)) {
                return Result.failure(IllegalArgumentException("Cabecera inválida: el archivo no es un respaldo cifrado de KashAPP (.kash_enc)."))
            }

            val sha256Digest = MessageDigest.getInstance("SHA-256")

            FileInputStream(encryptedInputFile).use { fis ->
                val headerBytes = ByteArray(HEADER_TOTAL_BYTES)
                val readHeader = fis.read(headerBytes)
                if (readHeader != HEADER_TOTAL_BYTES) {
                    return Result.failure(IllegalArgumentException("Cabecera de respaldo truncada o corrupta."))
                }

                val version = headerBytes[8]
                if (version != CURRENT_VERSION) {
                    return Result.failure(IllegalArgumentException("Versión de formato de respaldo no soportada: $version"))
                }

                val salt = headerBytes.copyOfRange(10, 10 + SALT_LENGTH_BYTES)
                val iv = headerBytes.copyOfRange(10 + SALT_LENGTH_BYTES, HEADER_TOTAL_BYTES)

                val secretKey = deriveAesKey(passChars, salt, PBKDF2_ITERATIONS)
                val cipher = Cipher.getInstance(TRANSFORMATION)
                cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(GCM_TAG_LENGTH_BITS, iv))
                cipher.updateAAD(headerBytes)

                CipherInputStream(fis, cipher).use { cis ->
                    GZIPInputStream(cis, BUFFER_SIZE).use { gzipIn ->
                        FileOutputStream(plainOutputFile).use { fos ->
                            val buffer = ByteArray(BUFFER_SIZE)
                            var read: Int
                            while (gzipIn.read(buffer).also { read = it } != -1) {
                                fos.write(buffer, 0, read)
                                sha256Digest.update(buffer, 0, read)
                            }
                        }
                    }
                }
            }

            if (!expectedPlaintextSha256Hex.isNullOrBlank()) {
                val computedSha256 = sha256Digest.digest().joinToString("") { "%02x".format(it) }
                if (!computedSha256.equals(expectedPlaintextSha256Hex.trim(), ignoreCase = true)) {
                    plainOutputFile.delete()
                    return Result.failure(SecurityException("Fallo de integridad SHA-256 post-descifrado."))
                }
            }

            Result.success(plainOutputFile)
        } catch (e: Exception) {
            if (plainOutputFile.exists()) plainOutputFile.delete()
            Result.failure(SecurityException("No fue posible descifrar el respaldo. Contraseña incorrecta o archivo adulterado.", e))
        } finally {
            passChars.fill('\u0000')
        }
    }

    /**
     * Verifica si un archivo posee la firma mágica "KASH_E2E" y el tamaño mínimo requerido.
     */
    fun isEncryptedKashBackup(file: File): Boolean {
        // Mínimo: 38 bytes cabecera + 16 bytes GCM Tag
        if (!file.exists() || file.length() < HEADER_TOTAL_BYTES + 16) return false
        return try {
            FileInputStream(file).use { fis ->
                val magic = ByteArray(MAGIC_HEADER.size)
                fis.read(magic) == MAGIC_HEADER.size && magic.contentEquals(MAGIC_HEADER)
            }
        } catch (_: Exception) {
            false
        }
    }

    private fun buildHeader(version: Byte, kdfMode: Byte, salt: ByteArray, iv: ByteArray): ByteArray {
        val header = ByteArray(HEADER_TOTAL_BYTES)
        System.arraycopy(MAGIC_HEADER, 0, header, 0, 8)
        header[8] = version
        header[9] = kdfMode
        System.arraycopy(salt, 0, header, 10, SALT_LENGTH_BYTES)
        System.arraycopy(iv, 0, header, 10 + SALT_LENGTH_BYTES, IV_LENGTH_BYTES)
        return header
    }

    private fun deriveAesKey(passChars: CharArray, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passChars, salt, iterations, KEY_LENGTH_BITS)
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val encoded = factory.generateSecret(spec).encoded
        return try {
            SecretKeySpec(encoded, "AES")
        } finally {
            encoded.fill(0)
        }
    }
}
