package com.example.domain.usecase

import java.util.Locale

data class InvestmentRoiResult(
    val netInvestedCapital: Double,
    val capitalGainLoss: Double,
    val roiPercentage: Double,
    val isProfit: Boolean,
    val isLoss: Boolean,
    val statusLabel: String,
    val statusColorHex: String,
    val formattedGainLoss: String,
    val formattedRoi: String
)

/**
 * Mathematical engine for calculating Capital Gains (Plusvalía), Net Invested Capital, and ROI %.
 */
class CalculateInvestmentRoiUseCase {

    operator fun invoke(
        initialAmount: Double,
        currentValue: Double,
        contributions: Double = 0.0,
        withdrawals: Double = 0.0
    ): InvestmentRoiResult {
        val netInvested = initialAmount + contributions - withdrawals
        val effectiveCurrentValue = if (currentValue > 0.0) currentValue else initialAmount
        val gainLoss = effectiveCurrentValue - netInvested
        val roi = if (netInvested > 0.0) {
            (gainLoss / netInvested) * 100.0
        } else {
            0.0
        }

        val isProfit = gainLoss > 0.01
        val isLoss = gainLoss < -0.01

        val statusLabel = when {
            isProfit -> "Plusvalía (Ganancia)"
            isLoss -> "Minusvalía (Pérdida)"
            else -> "Punto de equilibrio"
        }

        val statusColor = when {
            isProfit -> "#10B981"
            isLoss -> "#EF4444"
            else -> "#64748B"
        }

        val sign = if (isProfit) "+" else if (isLoss) "-" else ""
        val formattedGain = "$sign$ ${String.format(Locale.GERMANY, "%,.0f", Math.abs(gainLoss))}"
        val formattedRoi = "$sign${String.format(Locale.GERMANY, "%.2f", Math.abs(roi))}%"

        return InvestmentRoiResult(
            netInvestedCapital = netInvested,
            capitalGainLoss = gainLoss,
            roiPercentage = roi,
            isProfit = isProfit,
            isLoss = isLoss,
            statusLabel = statusLabel,
            statusColorHex = statusColor,
            formattedGainLoss = formattedGain,
            formattedRoi = formattedRoi
        )
    }
}
