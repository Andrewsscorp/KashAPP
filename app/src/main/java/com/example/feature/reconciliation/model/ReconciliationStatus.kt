package com.example.feature.reconciliation.model

/**
 * Status resulting from comparing system balance vs real statement balance.
 */
enum class ReconciliationStatus(val label: String) {
    BALANCED("Cuadrada (Sin Discrepancia)"),
    ADJUSTED_SURPLUS("Ajuste por Superávit (+ Ingreso)"),
    ADJUSTED_DEFICIT("Ajuste por Déficit (- Gasto)");

    companion object {
        fun fromDiscrepancy(discrepancy: Double, epsilon: Double = 0.001): ReconciliationStatus {
            return when {
                kotlin.math.abs(discrepancy) < epsilon -> BALANCED
                discrepancy > 0 -> ADJUSTED_SURPLUS
                else -> ADJUSTED_DEFICIT
            }
        }
    }
}
