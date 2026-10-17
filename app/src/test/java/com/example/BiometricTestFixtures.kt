package com.example

import androidx.biometric.BiometricPrompt

/** Creates AuthenticationResult in tests without depending on AndroidX package-private constructors. */
fun biometricAuthenticationResult(
    cryptoObject: BiometricPrompt.CryptoObject?
): BiometricPrompt.AuthenticationResult {
    val constructor = BiometricPrompt.AuthenticationResult::class.java.declaredConstructors.first()
    constructor.isAccessible = true
    return constructor.newInstance(
        cryptoObject,
        BiometricPrompt.AUTHENTICATION_RESULT_TYPE_BIOMETRIC
    ) as BiometricPrompt.AuthenticationResult
}
