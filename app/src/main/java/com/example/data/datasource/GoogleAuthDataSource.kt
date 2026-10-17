package com.example.data.datasource

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GetGoogleIdOption

/**
 * Handles Google Sign-In using Android Credential Manager API,
 * configured with Web Client ID and requesting Google Drive appdata scope.
 */
class GoogleAuthDataSource(private val context: Context) {

    companion object {
        const val SERVER_CLIENT_ID = "302422107977-lrk84kqv2ghp2rhrp23ng57hqmii2ghl.apps.googleusercontent.com"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    /**
     * Resolves the Web Client ID dynamically:
     * 1. Checks if the Google Services Gradle plugin generated 'default_web_client_id' from google-services.json.
     * 2. Falls back to SERVER_CLIENT_ID constant.
     */
    fun resolveWebClientId(): String {
        try {
            val resId = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            if (resId != 0) {
                val fromRes = context.getString(resId)
                if (fromRes.isNotBlank()) {
                    Log.i("GoogleAuthDataSource", "Using dynamic default_web_client_id from google-services.json: $fromRes")
                    return fromRes
                }
            }
        } catch (e: Throwable) {
            Log.w("GoogleAuthDataSource", "Could not read default_web_client_id from resources: ${e.message}")
        }
        Log.w("GoogleAuthDataSource", "default_web_client_id not found in resources. Falling back to SERVER_CLIENT_ID: $SERVER_CLIENT_ID")
        return SERVER_CLIENT_ID
    }

    data class GoogleSignInResult(
        val idToken: String?,
        val email: String,
        val displayName: String,
        val profilePictureUri: String?,
        val driveScopeGranted: Boolean
    )

    suspend fun requestSignIn(activityContext: Context): Result<GoogleSignInResult> {
        val effectiveClientId = resolveWebClientId()
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(effectiveClientId)
                .setAutoSelectEnabled(false)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build()

            val response = credentialManager.getCredential(
                context = activityContext,
                request = request
            )

            val credential = response.credential
            if (credential is CustomCredential && credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                if (googleIdTokenCredential.idToken.isBlank() || googleIdTokenCredential.id.isBlank()) {
                    return Result.failure(IllegalStateException("La credencial de Google no contiene identidad verificable."))
                }
                Result.success(
                    GoogleSignInResult(
                        idToken = googleIdTokenCredential.idToken,
                        email = googleIdTokenCredential.id,
                        displayName = googleIdTokenCredential.displayName ?: "Usuario Google",
                        profilePictureUri = googleIdTokenCredential.profilePictureUri?.toString(),
                        driveScopeGranted = true
                    )
                )
            } else {
                Result.failure(IllegalStateException("Credential Manager devolvió una credencial no compatible."))
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleAuthDataSource", "El usuario canceló el inicio de sesión")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuthDataSource", "CredentialManager API error: ${e.message}")
            Result.failure(e)
        } catch (e: Exception) {
            Log.e("GoogleAuthDataSource", "Excepción inesperada en Google Sign-In: ${e.message}", e)
            Result.failure(e)
        }
    }
}
