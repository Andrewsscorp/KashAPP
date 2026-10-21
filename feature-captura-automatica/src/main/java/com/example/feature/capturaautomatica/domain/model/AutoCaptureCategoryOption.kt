package com.example.feature.capturaautomatica.domain.model

/**
 * Representación de categorías disponibles creadas en la base de datos de KashApp
 * para su selección en la bandeja de captura automática.
 */
data class AutoCaptureCategoryOption(
    val id: String,
    val name: String,
    val type: String = "EXPENSE",
    val colorHex: String = "#EF4444",
    val icon: String = "category"
)

/**
 * Representación de etiquetas (tags) creadas en la base de datos de KashApp
 * para asignación rápida en transacciones capturadas.
 */
data class AutoCaptureTagOption(
    val id: String,
    val name: String,
    val colorHex: String = "#0284C7"
)
