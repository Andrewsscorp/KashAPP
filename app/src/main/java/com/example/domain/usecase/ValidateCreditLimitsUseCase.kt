package com.example.domain.usecase

import com.example.domain.models.CreditLimitValidationResult
import kotlin.math.abs
import kotlin.math.max

/**
 * UseCase implementing the Master Equation Rule for revolving credit cards:
 * Cupo Total = Cupo Disponible + Cupo Utilizado + Cupo Retenido
 *
 * Mathematically validates and auto-balances limits:
 * - If user edits Total ($12.000.000) and Used ($2.850.000), Available is auto-calculated to $9.150.000.
 * - Prevents equation mismatch and calculates real-time usage and amortization ratios.
 */
class ValidateCreditLimitsUseCase {

    operator fun invoke(
        totalQuota: Double,
        availableQuota: Double,
        usedQuota: Double,
        retainedQuota: Double = 0.0
    ): CreditLimitValidationResult {
        if (totalQuota < 0 || availableQuota < 0 || usedQuota < 0 || retainedQuota < 0) {
            return CreditLimitValidationResult(
                isValid = false,
                totalQuota = totalQuota,
                availableQuota = availableQuota,
                usedQuota = usedQuota,
                retainedQuota = retainedQuota,
                usagePercentage = 0f,
                paidPercentage = 0f,
                errorMessage = "Los montos de cupo no pueden ser negativos."
            )
        }

        val calculatedSum = availableQuota + usedQuota + retainedQuota
        val diff = abs(totalQuota - calculatedSum)
        val isValid = totalQuota > 0 && diff <= 1.0 // 1 unit tolerance for floating point rounding

        val usagePercent = if (totalQuota > 0) {
            ((usedQuota / totalQuota) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        val paidPercent = if (totalQuota > 0) {
            ((availableQuota / totalQuota) * 100.0).toFloat().coerceIn(0f, 100f)
        } else 0f

        return CreditLimitValidationResult(
            isValid = isValid,
            totalQuota = totalQuota,
            availableQuota = availableQuota,
            usedQuota = usedQuota,
            retainedQuota = retainedQuota,
            usagePercentage = usagePercent,
            paidPercentage = paidPercent,
            errorMessage = if (!isValid && totalQuota > 0) {
                "Descuadre en cupo: Total ($totalQuota) ≠ Disponible ($availableQuota) + Utilizado ($usedQuota) + Retenido ($retainedQuota)"
            } else null
        )
    }

    /**
     * Helper to auto-calculate Available Quota from Total and Used.
     */
    fun calculateAvailableFromTotalAndUsed(
        totalQuota: Double,
        usedQuota: Double,
        retainedQuota: Double = 0.0
    ): Double {
        return max(0.0, totalQuota - usedQuota - retainedQuota)
    }

    /**
     * Helper to auto-calculate Used Quota from Total and Available.
     */
    fun calculateUsedFromTotalAndAvailable(
        totalQuota: Double,
        availableQuota: Double,
        retainedQuota: Double = 0.0
    ): Double {
        return max(0.0, totalQuota - availableQuota - retainedQuota)
    }
}
