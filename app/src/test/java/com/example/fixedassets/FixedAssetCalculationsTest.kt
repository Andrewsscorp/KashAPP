package com.example.fixedassets

import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.AssetType
import com.example.domain.model.fixedassets.DepreciationMethod
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase
import com.example.domain.usecase.fixedassets.ProjectAssetValuesUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class FixedAssetCalculationsTest {

    @Test
    fun testStraightLineMonthlyDepreciation() {
        val cost = 12_000_000.0
        val residual = 0.0
        val months = 60

        val monthlyDep = AssetFinancialCalculations.calculateMonthlyDepreciation(
            acquisitionValue = cost,
            residualValue = residual,
            usefulLifeMonths = months,
            method = DepreciationMethod.STRAIGHT_LINE
        )

        // 12M / 60 months = 200,000 COP per month
        assertEquals(200_000.0, monthlyDep, 0.01)
    }

    @Test
    fun testBookValueCalculation() {
        val cost = 10_000_000.0
        val accumulatedDep = 4_000_000.0
        val residual = 1_000_000.0

        val bookValue = AssetFinancialCalculations.calculateCurrentBookValue(
            acquisitionValue = cost,
            accumulatedDepreciation = accumulatedDep,
            residualValue = residual
        )

        assertEquals(6_000_000.0, bookValue, 0.01)
    }

    @Test
    fun testBookValueNeverDropsBelowResidual() {
        val cost = 10_000_000.0
        val accumulatedDep = 9_500_000.0
        val residual = 1_000_000.0

        val bookValue = AssetFinancialCalculations.calculateCurrentBookValue(
            acquisitionValue = cost,
            accumulatedDepreciation = accumulatedDep,
            residualValue = residual
        )

        assertEquals(1_000_000.0, bookValue, 0.01)
    }

    @Test
    fun testDepreciationProgress() {
        val cost = 10_000_000.0
        val residual = 2_000_000.0 // Depreciable base = 8,000,000
        val accumulatedDep = 4_000_000.0

        val progress = AssetFinancialCalculations.calculateDepreciationProgress(
            acquisitionValue = cost,
            residualValue = residual,
            accumulatedDepreciation = accumulatedDep
        )

        // 4M / 8M = 50%
        assertEquals(0.5f, progress, 0.001f)
    }

    @Test
    fun testPendingDepreciationUseCase() {
        val useCase = CalculateAssetDepreciationUseCase()
        val asset = FixedAssetEntity(
            id = "asset-test-1",
            userId = "user-1",
            name = "Laptop Dell XPS",
            type = AssetType.TANGIBLE.name,
            category = AssetCategory.TECHNOLOGY.name,
            acquisitionDate = LocalDate.now().minusMonths(3).toString(),
            lastDepreciationDate = LocalDate.now().minusMonths(2).toString(),
            acquisitionValue = 6_000_000.0,
            residualValue = 0.0,
            depreciationMethod = DepreciationMethod.STRAIGHT_LINE.name,
            usefulLifeMonths = 36,
            accumulatedDepreciation = 166_666.67,
            hasDepreciation = true
        )

        val pending = useCase.calculatePendingForAsset(asset, LocalDate.now().toString())
        assertNotNull(pending)
        assertTrue(pending!!.monthsToDepreciate >= 1)
        assertTrue(pending.totalPendingAmount > 0)
    }

    @Test
    fun testProjectionsUseCaseAppreciationAndDepreciation() {
        val useCase = ProjectAssetValuesUseCase()
        val realEstate = FixedAssetEntity(
            id = "asset-apt",
            userId = "user-1",
            name = "Apartamento Rosales",
            type = AssetType.TANGIBLE.name,
            category = AssetCategory.REAL_ESTATE.name,
            acquisitionDate = LocalDate.now().minusYears(1).toString(),
            acquisitionValue = 500_000_000.0,
            residualValue = 500_000_000.0,
            depreciationMethod = DepreciationMethod.STRAIGHT_LINE.name,
            usefulLifeMonths = 240,
            accumulatedDepreciation = 0.0,
            currentMarketValue = 550_000_000.0,
            hasDepreciation = false
        )

        val projection = useCase.execute(listOf(realEstate), horizonYears = 5)
        // 6 points: baseline Today (year 0) + 5 projected years
        assertEquals(6, projection.points.size)
        // Market value should appreciate over time
        val lastPoint = projection.points.last()
        assertTrue(lastPoint.projectedMarketValue > 550_000_000.0)
    }
}
