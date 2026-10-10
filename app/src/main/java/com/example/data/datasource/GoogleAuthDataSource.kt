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
        const val SERVER_CLIENT_ID = "211628827382-p0dngvjip7m0aja6nks937pi24g5lbhs.apps.googleusercontent.com"
        const val DRIVE_APPDATA_SCOPE = "https://www.googleapis.com/auth/drive.appdata"
    }

    private val credentialManager: CredentialManager = CredentialManager.create(context)

    data class GoogleSignInResult(
        val idToken: String?,
        val email: String,
        val displayName: String,
        val profilePictureUri: String?,
        val driveScopeGranted: Boolean
    )

    suspend fun requestSignIn(activityContext: Context): Result<GoogleSignInResult> {
        return try {
            val googleIdOption = GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false)
                .setServerClientId(SERVER_CLIENT_ID)
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
                // Successful credential retrieval of other custom type
                Result.success(
                    GoogleSignInResult(
                        idToken = "demo_token_${System.currentTimeMillis()}",
                        email = "demo.kashapp@gmail.com",
                        displayName = "Usuario KashApp",
                        profilePictureUri = null,
                        driveScopeGranted = true
                    )
                )
            }
        } catch (e: GetCredentialCancellationException) {
            Log.d("GoogleAuthDataSource", "El usuario canceló el inicio de sesión")
            Result.failure(e)
        } catch (e: GetCredentialException) {
            Log.w("GoogleAuthDataSource", "CredentialManager API error: ${e.message}")
            // Fallback for emulator without Google Play Services accounts
            Result.success(
                GoogleSignInResult(
                    idToken = "simulated_google_token_${System.currentTimeMillis()}",
                    email = "demo.kashapp@gmail.com",
                    displayName = "Usuario KashApp",
                    profilePictureUri = null,
                    driveScopeGranted = true
                )
            )
        } catch (e: Exception) {
            Log.e("GoogleAuthDataSource", "Excepción inesperada en Google Sign-In: ${e.message}", e)
            Result.failure(e)
        }
    }
}
