package com.example.domain.usecase

/**
 * Domain service to validate and compute local/remote storage paths for uploaded financial documents.
 */
class DocumentUploader {

    companion object {
        val ALLOWED_TYPES = listOf("application/pdf", "image/jpeg", "image/png", "image/webp")
        const val MAX_SIZE_BYTES = 2L * 1024L * 1024L // 2MB
    }

    /**
     * Validates file properties and returns local and remote storage URLs.
     */
    fun upload(userId: String, fileName: String, fileType: String, fileSize: Double): Map<String, String> {
        return upload(userId, fileName, fileType, fileSize.toLong())
    }

    fun upload(userId: String, fileName: String, fileType: String, fileSize: Long): Map<String, String> {
        if (fileSize > MAX_SIZE_BYTES) {
            throw IllegalArgumentException("El archivo excede el límite máximo de 2MB.")
        }
        if (fileType !in ALLOWED_TYPES) {
            throw IllegalArgumentException("Formato no soportado. Solo JPG, PNG o PDF.")
        }

        // Sandboxed secure internal directory (encrypted at rest by Android OS)
        val localPath = "/data/data/com.kashapp/files/hidden_docs/$fileName"
        val remotePath = "gs://kashapp/users/$userId/$fileName"

        return mapOf(
            "local" to localPath,
            "remote" to remotePath
        )
    }
}
