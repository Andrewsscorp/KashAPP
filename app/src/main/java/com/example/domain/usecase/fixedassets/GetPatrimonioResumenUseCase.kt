package com.example.domain.usecase.fixedassets

import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import kotlin.math.max

/**
 * Domain Use Case representing the IFRS / NIC 16 aggregated read model for Fixed Assets equity.
 * Computes the exact accounting balance:
 * Costo de Adquisición ($ 90.500.000) - Depreciación Acumulada ($ 24.740.000) = Valor en Libros Neto ($ 65.760.000),
 * with exact amortization percentage (27,3%) and remaining percentage (72,7%).
 */
class GetPatrimonioResumenUseCase {

    data class PatrimonioResumen(
        val totalAssetsCount: Int,
        val totalAcquisitionCost: Double,
        val totalAccumulatedDepreciation: Double,
        val totalNetBookValue: Double,
        val totalMarketValue: Double,
        val amortizedPercent: Double, // 27.3%
        val remainingPercent: Double, // 72.7%
        val monthlyDepreciationQuota: Double,
        val activeCount: Int,
        val maintenanceCount: Int,
        val writtenOffCount: Int
    )

    fun execute(assets: List<FixedAssetEntity>): PatrimonioResumen {
        if (assets.isEmpty()) {
            return PatrimonioResumen(
                totalAssetsCount = 0,
                totalAcquisitionCost = 0.0,
                totalAccumulatedDepreciation = 0.0,
                totalNetBookValue = 0.0,
                totalMarketValue = 0.0,
                amortizedPercent = 0.0,
                remainingPercent = 100.0,
                monthlyDepreciationQuota = 0.0,
                activeCount = 0,
                maintenanceCount = 0,
                writtenOffCount = 0
            )
        }

        val totalAcq = assets.sumOf { it.acquisitionValue }
        val totalAccumDep = assets.sumOf { it.accumulatedDepreciation }
        val netBookValue = max(0.0, totalAcq - totalAccumDep)
        val totalMarket = assets.sumOf { it.currentMarketValue }

        val amortizedPct = if (totalAcq > 0.0) {
            val raw = (totalAccumDep / totalAcq) * 100.0
            kotlin.math.round(raw * 10.0) / 10.0
        } else 0.0

        val remainingPct = if (totalAcq > 0.0) {
            val raw = (netBookValue / totalAcq) * 100.0
            kotlin.math.round(raw * 10.0) / 10.0
        } else 100.0

        val monthlyQuota = assets.filter { it.hasDepreciation }.sumOf {
            val s = AssetStateMachine.resolveState(it.status)
            if (s.allowsDepreciation) {
                AssetFinancialCalculations.calculateMonthlyDepreciation(
                    acquisitionValue = it.acquisitionValue,
                    residualValue = it.residualValue,
                    usefulLifeMonths = it.usefulLifeMonths
                )
            } else 0.0
        }

        var active = 0
        var maintenance = 0
        var writtenOff = 0

        for (asset in assets) {
            val state = AssetStateMachine.resolveState(asset.status)
            when {
                state.isTerminal || asset.deletedAt != null -> writtenOff++
                state.code == "EN_MANTENIMIENTO" || asset.status == "MAINTENANCE" -> maintenance++
                else -> active++
            }
        }

        return PatrimonioResumen(
            totalAssetsCount = assets.size,
            totalAcquisitionCost = totalAcq,
            totalAccumulatedDepreciation = totalAccumDep,
            totalNetBookValue = netBookValue,
            totalMarketValue = totalMarket,
            amortizedPercent = amortizedPct,
            remainingPercent = remainingPct,
            monthlyDepreciationQuota = monthlyQuota,
            activeCount = active,
            maintenanceCount = maintenance,
            writtenOffCount = writtenOff
        )
    }
}
