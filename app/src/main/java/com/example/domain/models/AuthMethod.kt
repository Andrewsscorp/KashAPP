package com.example.domain.models

/**
 * Supported authentication methods in KashApp.
 * Pure domain model with zero Android framework dependencies.
 */
enum class AuthMethod(val displayName: String) {
    GOOGLE("Google Sign-In"),
    BIOMETRIC("Huella Dactilar"),
    PIN("Código PIN"),
    FIREBASE_EMAIL("Correo y Contraseña"),
    FIREBASE_ANONYMOUS("Sesión Invitado"),
    OTHER("Sistema / Sesión")
}

