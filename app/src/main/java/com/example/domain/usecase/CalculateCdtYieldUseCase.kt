package com.example.domain.usecase

import com.example.domain.model.CdtCalculation
import java.time.LocalDate
import kotlin.math.pow

/**
 * Mathematical engine for CDT investments.
 * Calculates term rate equivalence and expected yield based on Effective Annual Rate (EA):
 *
 * Tasa_Plazo = (1 + EA)^(dias / 365) - 1
 * Rendimiento_Estimado = Capital * Tasa_Plazo
 * Fecha_Vencimiento = Fecha_Constitucion + Plazo_Dias
 */
class CalculateCdtYieldUseCase {

    operator fun invoke(
        initialAmount: Double,
        interestRateEa: Double,
        termDays: Int,
        issueDate: LocalDate = LocalDate.now()
    ): CdtCalculation {
        val safeAmount = if (initialAmount < 0.0) 0.0 else initialAmount
        val safeEa = if (interestRateEa < 0.0) 0.0 else interestRateEa
        val safeDays = if (termDays <= 0) 1 else termDays

        val eaDecimal = safeEa / 100.0
        val termFraction = safeDays.toDouble() / 365.0
        // Equivalent compound rate for the term
        val termRateDecimal = (1.0 + eaDecimal).pow(termFraction) - 1.0
        val termRatePercentage = termRateDecimal * 100.0

        val estimatedYield = safeAmount * termRateDecimal
        val maturityDate = issueDate.plusDays(safeDays.toLong())

        return CdtCalculation(
            initialAmount = safeAmount,
            interestRateEa = safeEa,
            termDays = safeDays,
            issueDate = issueDate,
            maturityDate = maturityDate,
            termRatePercentage = termRatePercentage,
            estimatedYield = estimatedYield,
            totalEstimatedValue = safeAmount + estimatedYield
        )
    }
}
