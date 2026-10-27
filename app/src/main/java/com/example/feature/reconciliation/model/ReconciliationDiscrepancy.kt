package com.example.feature.reconciliation.model

import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Pure mathematical model calculating discrepancy between system balance and physical statement balance.
 * Uses exact scale rounding to eliminate floating point imprecision.
 */
data class ReconciliationDiscrepancy(
    val previousSystemBalance: Double,
    val reconciledRealBalance: Double,
    val discrepancyAmount: Double,
    val status: ReconciliationStatus,
    val requiresAdjustment: Boolean
) {
    companion object {
        fun calculate(previousSystemBalance: Double, reconciledRealBalance: Double): ReconciliationDiscrepancy {
            val prevBd = BigDecimal.valueOf(previousSystemBalance).setScale(2, RoundingMode.HALF_EVEN)
            val realBd = BigDecimal.valueOf(reconciledRealBalance).setScale(2, RoundingMode.HALF_EVEN)
            val deltaBd = realBd.subtract(prevBd).setScale(2, RoundingMode.HALF_EVEN)
            val discrepancy = deltaBd.toDouble()

            val status = ReconciliationStatus.fromDiscrepancy(discrepancy)
            val requiresAdjustment = status != ReconciliationStatus.BALANCED

            return ReconciliationDiscrepancy(
                previousSystemBalance = prevBd.toDouble(),
                reconciledRealBalance = realBd.toDouble(),
                discrepancyAmount = discrepancy,
                status = status,
                requiresAdjustment = requiresAdjustment
            )
        }
    }
}
