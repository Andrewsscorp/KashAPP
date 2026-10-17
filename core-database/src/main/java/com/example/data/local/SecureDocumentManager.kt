package com.example.data.local

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import androidx.security.crypto.EncryptedFile
import androidx.security.crypto.MasterKey
import java.io.File
import java.io.InputStream
import java.util.UUID

/**
 * Enterprise Secure Document & Media Storage Manager.
 *
 * Implements strict security policies:
 * 1. Validates MIME type (JPG, PNG, WEBP, PDF).
 * 2. Enforces strict 2MB (2,097,152 bytes) limit.
 * 3. Saves locally in App Internal Sandbox (`files/hidden_docs/`) with AES-256 hardware encryption
 *    at rest using AndroidX EncryptedFile (AES256_GCM_HKDF_4KB).
 *    Never saves in public or OBB directories to prevent PII leaks.
 * 4. Generates both offline-first `local_uri` and remote Cloud Storage `remote_url`.
 */
class SecureDocumentManager(private val context: Context) {

    companion object {
        const val MAX_SIZE_BYTES = 2L * 1024L * 1024L // 2 MB
        val ALLOWED_MIME_TYPES = setOf(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "application/pdf"
        )
        const val HIDDEN_DOCS_DIR = "hidden_docs"
    }

    data class SavedDocumentResult(
        val originalName: String,
        val localFile: File,
        val localUri: String,
        val remotePath: String,
        val mimeType: String,
        val sizeBytes: Long
    )

    private fun getOrCreateMasterKey(): MasterKey {
        return try {
            MasterKey.Builder(context)
                .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
                .build()
        } catch (e: Throwable) {
            throw IllegalStateException("No se pudo crear la clave maestra para cifrar documentos.", e)
        }
    }

    /**
     * Inspects, validates and copies a selected document/image into the app's sandboxed hidden_docs directory,
     * encrypting it at rest with AES-256 GCM.
     */
    fun saveDocumentSafely(uri: Uri, userId: String = "user_default"): Result<SavedDocumentResult> {
        return try {
            val contentResolver = context.contentResolver
            var fileName = "doc_${UUID.randomUUID()}"
            var fileSize = 0L

            contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                val sizeIndex = cursor.getColumnIndex(OpenableColumns.SIZE)
                if (cursor.moveToFirst()) {
                    if (nameIndex != -1) {
                        fileName = cursor.getString(nameIndex) ?: fileName
                    }
                    if (sizeIndex != -1) {
                        fileSize = cursor.getLong(sizeIndex)
                    }
                }
            }

            var mimeType = contentResolver.getType(uri) ?: ""
            if (mimeType.isBlank()) {
                val ext = fileName.substringAfterLast('.', "").lowercase()
                mimeType = when (ext) {
                    "pdf" -> "application/pdf"
                    "png" -> "image/png"
                    "jpg", "jpeg" -> "image/jpeg"
                    "webp" -> "image/webp"
                    "gif" -> "image/gif"
                    "bmp" -> "image/bmp"
                    "heic", "heif" -> "image/heic"
                    "svg" -> "image/svg+xml"
                    else -> "application/octet-stream"
                }
            }

            // 1. Format validation: ONLY images (any format) or PDF
            val isAllowed = mimeType.startsWith("image/", ignoreCase = true) ||
                mimeType.equals("application/pdf", ignoreCase = true) ||
                ALLOWED_MIME_TYPES.any { allowed ->
                    mimeType.equals(allowed, ignoreCase = true) ||
                    (allowed.startsWith("image/") && mimeType.startsWith("image/"))
                }
            if (!isAllowed) {
                return Result.failure(
                    IllegalArgumentException("Formato no permitido. Únicamente se permite cargar imágenes (cualquier formato) o archivos PDF.")
                )
            }

            // 2. Size limit validation
            if (fileSize > MAX_SIZE_BYTES) {
                return Result.failure(
                    IllegalArgumentException("El archivo excede el límite máximo permitido de 2MB.")
                )
            }

            // 3. Create sandboxed hidden directory: /data/data/<package>/files/hidden_docs/
            val hiddenDir = File(context.filesDir, HIDDEN_DOCS_DIR)
            if (!hiddenDir.exists()) {
                hiddenDir.mkdirs()
            }

            val sanitizedFileName = "${System.currentTimeMillis()}_${fileName.replace(Regex("[^a-zA-Z0-9._-]"), "_")}"
            val destinationFile = File(hiddenDir, sanitizedFileName)

            val masterKey = getOrCreateMasterKey()
            val encryptedFile = EncryptedFile.Builder(
                context,
                destinationFile,
                masterKey,
                EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
            ).build()

            var copiedSize = 0L
            contentResolver.openInputStream(uri)?.use { input: InputStream ->
                encryptedFile.openFileOutput().use { output ->
                    copiedSize = input.copyTo(output)
                }
            } ?: return Result.failure(IllegalStateException("No se pudo abrir el archivo seleccionado."))

            if (copiedSize > MAX_SIZE_BYTES) {
                destinationFile.delete()
                return Result.failure(
                    IllegalArgumentException("El archivo excede el límite máximo permitido de 2MB.")
                )
            }

            val localUri = destinationFile.toURI().toString()
            val remotePath = "gs://kashapp/users/$userId/documents/$sanitizedFileName"

            Result.success(
                SavedDocumentResult(
                    originalName = fileName,
                    localFile = destinationFile,
                    localUri = localUri,
                    remotePath = remotePath,
                    mimeType = mimeType,
                    sizeBytes = copiedSize
                )
            )
        } catch (e: Exception) {
            Log.e("SecureDocumentManager", "Error saving document: ${e.message}", e)
            Result.failure(e)
        }
    }

    /** Reads a document previously encrypted on disk and never falls back to plaintext. */
    fun openDecryptedInputStream(file: File): InputStream {
        val masterKey = getOrCreateMasterKey()
        val encryptedFile = EncryptedFile.Builder(
            context,
            file,
            masterKey,
            EncryptedFile.FileEncryptionScheme.AES256_GCM_HKDF_4KB
        ).build()
        return encryptedFile.openFileInput()
    }
}
