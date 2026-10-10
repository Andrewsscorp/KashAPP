package com.example.feature.activos.calculator

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DepreciationEngineTest {

    @Test
    fun calculateStraightLine_standardAsset_computesCorrectSchedule() {
        val acquisitionValue = 60_000_000.0 // 60M COP vehicle
        val salvageValue = 10_000_000.0 // 10M residual
        val usefulLifeYears = 5 // 5 years

        val summary = DepreciationEngine.calculateStraightLine(
            assetId = "AF-001",
            assetName = "Camioneta D-Max",
            acquisitionValue = acquisitionValue,
            salvageValue = salvageValue,
            usefulLifeYears = usefulLifeYears,
            startYear = 2026
        )

        assertNotNull(summary)
        assertEquals(50_000_000.0, summary.depreciableBase, 0.01)
        assertEquals(20.0, summary.annualRatePercentage, 0.01) // 1/5 = 20%
        val expectedAnnualDep = 10_000_000.0
        assertEquals(expectedAnnualDep / 12.0, summary.monthlyDepreciation, 0.01)
        assertEquals(5, summary.schedule.size)

        // Final book value must exactly equal salvage value
        val lastEntry = summary.schedule.last()
        assertEquals(salvageValue, lastEntry.closingBookValue, 0.01)
        assertEquals(50_000_000.0, lastEntry.accumulatedDepreciation, 0.01)
    }

    @Test
    fun calculateDecliningBalance_acceleratedMethod_depreciatesFasterInEarlyYears() {
        val acquisitionValue = 10_000_000.0
        val salvageValue = 1_000_000.0
        val usefulLifeYears = 5

        val summary = DepreciationEngine.calculateDecliningBalance(
            assetId = "AF-002",
            assetName = "MacBook Pro M3",
            acquisitionValue = acquisitionValue,
            salvageValue = salvageValue,
            usefulLifeYears = usefulLifeYears,
            factor = 2.0,
            startYear = 2026
        )

        assertNotNull(summary)
        assertTrue(summary.schedule.isNotEmpty())
        val firstYearDep = summary.schedule.first().depreciationAmount
        assertTrue("First year depreciation should be substantial", firstYearDep > 0)
        assertTrue(summary.schedule.last().closingBookValue >= salvageValue)
    }

    @Test
    fun calculateStraightLine_zeroSalvageValue_depreciatesFullAmount() {
        val summary = DepreciationEngine.calculateStraightLine(
            assetId = "AF-003",
            assetName = "Licencia Software",
            acquisitionValue = 12_000_000.0,
            salvageValue = 0.0,
            usefulLifeYears = 3,
            startYear = 2026
        )

        assertEquals(12_000_000.0, summary.depreciableBase, 0.01)
        assertEquals(4_000_000.0 / 12.0, summary.monthlyDepreciation, 0.01)
        assertEquals(0.0, summary.schedule.last().closingBookValue, 0.01)
        assertEquals(12_000_000.0, summary.schedule.last().accumulatedDepreciation, 0.01)
    }

    @Test
    fun calculateStraightLine_singleYearLife_handlesCorrectly() {
        val summary = DepreciationEngine.calculateStraightLine(
            assetId = "AF-004",
            assetName = "Herramienta menor",
            acquisitionValue = 500_000.0,
            salvageValue = 50_000.0,
            usefulLifeYears = 1,
            startYear = 2026
        )

        assertEquals(1, summary.schedule.size)
        assertEquals(50_000.0, summary.schedule.first().closingBookValue, 0.01)
        assertEquals(450_000.0, summary.schedule.first().depreciationAmount, 0.01)
    }
}
