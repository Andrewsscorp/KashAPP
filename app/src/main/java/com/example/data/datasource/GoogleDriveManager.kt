package com.example.data.datasource

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Manages secure financial backup synchronization to Google Drive's hidden appData folder.
 * Uses official Google Drive v3 REST API with scope: https://www.googleapis.com/auth/drive.appdata
 */
class GoogleDriveManager(private val context: Context) {

    companion object {
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
        private const val BACKUP_FILE_NAME = "kashapp_encrypted_backup.json"
        private const val DRIVE_UPLOAD_URL = "https://www.googleapis.com/upload/drive/v3/files?uploadType=multipart"
        private const val DRIVE_LIST_URL = "https://www.googleapis.com/drive/v3/files?spaces=appDataFolder&fields=files(id,name,modifiedTime)"
    }

    data class DriveSyncStatus(
        val isConnected: Boolean,
        val lastSyncTimestamp: Long,
        val syncedFilesCount: Int,
        val fileId: String? = null,
        val storageFolderName: String = "appDataFolder (Cifrado E2E)",
        val lastError: String? = null
    )

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var currentStatus = DriveSyncStatus(
        isConnected = false,
        lastSyncTimestamp = 0L,
        syncedFilesCount = 0
    )

    /**
     * Uploads or updates the encrypted financial backup in the user's hidden Google Drive appDataFolder.
     */
    suspend fun syncEncryptedBackupToDrive(
        accessTokenOrToken: String,
        encryptedPayload: String
    ): Result<DriveSyncStatus> = withContext(Dispatchers.IO) {
        try {
            Log.d("GoogleDriveManager", "Iniciando sincronización real con Google Drive (appDataFolder)...")

            // Real Google Drive API v3 Multipart Request
            val metadataJson = JSONObject().apply {
                put("name", BACKUP_FILE_NAME)
                put("parents", org.json.JSONArray().apply { put("appDataFolder") })
                put("mimeType", "application/json")
                put("description", "KashApp End-to-End Encrypted Financial Vault")
            }.toString()

            val multipartBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart(
                    "metadata",
                    null,
                    metadataJson.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .addFormDataPart(
                    "file",
                    BACKUP_FILE_NAME,
                    encryptedPayload.toRequestBody("application/json; charset=UTF-8".toMediaType())
                )
                .build()

            val request = Request.Builder()
                .url(DRIVE_UPLOAD_URL)
                .addHeader("Authorization", "Bearer $accessTokenOrToken")
                .addHeader("X-Goog-Upload-Protocol", "multipart")
                .post(multipartBody)
                .build()

            var driveFileId: String? = null
            var isSuccessfulUpload = false
            var errorMessage: String? = null

            try {
                httpClient.newCall(request).execute().use { response ->
                    val responseBody = response.body?.string().orEmpty()
                    if (response.isSuccessful) {
                        isSuccessfulUpload = true
                        val jsonResp = JSONObject(responseBody)
                        driveFileId = jsonResp.optString("id", "drive_${System.currentTimeMillis()}")
                        Log.i("GoogleDriveManager", "Respaldo sincronizado exitosamente en Google Drive. FileId: $driveFileId")
                    } else {
                        if (response.code == 401) {
                            errorMessage = "HTTP 401 Unauthorized: Google Drive requiere un AccessToken OAuth2 con scope '${DRIVE_APPDATA_SCOPE}' en vez de un OpenID idToken."
                            Log.w("GoogleDriveManager", errorMessage!!)
                        } else {
                            errorMessage = "HTTP ${response.code}: $responseBody"
                            Log.w("GoogleDriveManager", "Google Drive API returned non-200: $errorMessage")
                        }
                        // Always save secure local copy in app private storage as fallback
                        try {
                            val backupDir = java.io.File(context.filesDir, "drive_backup").apply { mkdirs() }
                            java.io.File(backupDir, BACKUP_FILE_NAME).writeText(encryptedPayload)
                            Log.i("GoogleDriveManager", "Copia de seguridad local protegida en ${backupDir.absolutePath}")
                        } catch (_: Exception) {}
                    }
                }
            } catch (networkEx: Exception) {
                errorMessage = networkEx.message
                Log.w("GoogleDriveManager", "Red o token no autenticado en emulador: ${networkEx.message}")
            }

            val now = System.currentTimeMillis()
            currentStatus = DriveSyncStatus(
                isConnected = isSuccessfulUpload,
                lastSyncTimestamp = now,
                syncedFilesCount = if (isSuccessfulUpload) 1 else currentStatus.syncedFilesCount,
                fileId = driveFileId ?: currentStatus.fileId,
                storageFolderName = "appDataFolder (Cifrado E2E)",
                lastError = errorMessage
            )

            if (isSuccessfulUpload) {
                Result.success(currentStatus)
            } else {
                Result.failure(IllegalStateException(errorMessage ?: "No se pudo sincronizar el respaldo con Google Drive."))
            }
        } catch (e: Exception) {
            Log.e("GoogleDriveManager", "Error en sincronización con Drive: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Checks files in Google Drive's appDataFolder
     */
    suspend fun queryAppDataFiles(accessToken: String): Result<List<String>> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(DRIVE_LIST_URL)
                .addHeader("Authorization", "Bearer $accessToken")
                .get()
                .build()

            httpClient.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    val json = JSONObject(body)
                    val filesArray = json.optJSONArray("files") ?: org.json.JSONArray()
                    val result = mutableListOf<String>()
                    for (i in 0 until filesArray.length()) {
                        val fileObj = filesArray.getJSONObject(i)
                        result.add(fileObj.optString("name", "unnamed"))
                    }
                    Result.success(result)
                } else {
                    Result.failure(Exception("HTTP ${response.code} querying appDataFolder"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun getSyncStatus(): DriveSyncStatus = currentStatus
}
