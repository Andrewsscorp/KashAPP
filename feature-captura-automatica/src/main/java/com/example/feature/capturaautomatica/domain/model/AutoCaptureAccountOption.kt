package com.example.feature.capturaautomatica.domain.model

/**
 * Modelo ligero y desacoplado para representar cuentas bancarias disponibles en el selector
 * de la bandeja de captura automática.
 */
data class AutoCaptureAccountOption(
    val id: String,
    val name: String,
    val bankName: String,
    val balance: Double = 0.0,
    val type: String = "SAVINGS",
    val colorHex: String = "#0284C7"
)
