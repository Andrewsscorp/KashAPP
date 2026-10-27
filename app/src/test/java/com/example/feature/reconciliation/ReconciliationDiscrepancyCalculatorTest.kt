package com.example.feature.reconciliation

import com.example.feature.reconciliation.model.ReconciliationDiscrepancy
import com.example.feature.reconciliation.model.ReconciliationStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated Unit Test: Discrepancy Calculation Engine.
 * Verifies exact floating point rounding and status determination.
 */
class ReconciliationDiscrepancyCalculatorTest {

    @Test
    fun calculate_withIdenticalBalances_yieldsBalancedStatusAndNoAdjustment() {
        val result = ReconciliationDiscrepancy.calculate(
            previousSystemBalance = 2_500_000.0,
            reconciledRealBalance = 2_500_000.0
        )

        assertEquals(ReconciliationStatus.BALANCED, result.status)
        assertEquals(0.0, result.discrepancyAmount, 0.0001)
        assertFalse(result.requiresAdjustment)
    }

    @Test
    fun calculate_withRealBalanceHigher_yieldsSurplusStatusAndPositiveDiscrepancy() {
        val result = ReconciliationDiscrepancy.calculate(
            previousSystemBalance = 1_000_000.0,
            reconciledRealBalance = 1_250_000.0
        )

        assertEquals(ReconciliationStatus.ADJUSTED_SURPLUS, result.status)
        assertEquals(250_000.0, result.discrepancyAmount, 0.0001)
        assertTrue(result.requiresAdjustment)
    }

    @Test
    fun calculate_withRealBalanceLower_yieldsDeficitStatusAndNegativeDiscrepancy() {
        val result = ReconciliationDiscrepancy.calculate(
            previousSystemBalance = 1_000_000.0,
            reconciledRealBalance = 850_000.0
        )

        assertEquals(ReconciliationStatus.ADJUSTED_DEFICIT, result.status)
        assertEquals(-150_000.0, result.discrepancyAmount, 0.0001)
        assertTrue(result.requiresAdjustment)
    }

    @Test
    fun calculate_handlesComplexFloatingDecimalsWithoutDrift() {
        val result = ReconciliationDiscrepancy.calculate(
            previousSystemBalance = 100.10,
            reconciledRealBalance = 100.35
        )

        assertEquals(ReconciliationStatus.ADJUSTED_SURPLUS, result.status)
        assertEquals(0.25, result.discrepancyAmount, 0.0001)
    }
}
