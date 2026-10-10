package com.example.data.datasource

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.example.data.local.KeystoreCryptoManager

/**
 * Biometric authentication data source leveraging AndroidX BiometricPrompt
 * and Android Keystore AES-256 CryptoObject for hardware security.
 */
class BiometricDataSource(
    private val context: Context,
    private val cryptoManager: KeystoreCryptoManager
) {

    sealed class BiometricAuthStatus {
        data class Success(val cryptoObject: BiometricPrompt.CryptoObject?) : BiometricAuthStatus()
        data class Error(val errorCode: Int, val errString: CharSequence) : BiometricAuthStatus()
        object Failed : BiometricAuthStatus()
        object Unavailable : BiometricAuthStatus()
    }

    fun canAuthenticate(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }

    fun authenticateWithPrompt(
        activity: FragmentActivity,
        title: String = "Autenticación Biométrica",
        subtitle: String = "Accede a KashApp con tu huella dactilar",
        negativeButtonText: String = "Usar PIN",
        onResult: (BiometricAuthStatus) -> Unit
    ) {
        val executor = ContextCompat.getMainExecutor(activity)
        val cryptoObject = cryptoManager.createBiometricCryptoObject()

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription("Verifica tu identidad para desbloquear tu clave de cifrado.")
            .setNegativeButtonText(negativeButtonText)
            .setConfirmationRequired(true)
            .build()

        val callback = object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                onResult(BiometricAuthStatus.Success(result.cryptoObject))
            }

            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                onResult(BiometricAuthStatus.Error(errorCode, errString))
            }

            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                onResult(BiometricAuthStatus.Failed)
            }
        }

        val biometricPrompt = BiometricPrompt(activity, executor, callback)

        if (cryptoObject != null) {
            biometricPrompt.authenticate(promptInfo, cryptoObject)
        } else {
            biometricPrompt.authenticate(promptInfo)
        }
    }
}
