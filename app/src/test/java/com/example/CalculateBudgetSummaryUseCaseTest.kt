package com.example

import com.example.domain.usecase.CalculateBudgetSummaryUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying real-time reactive calculations and Zero-Based Budgeting rules
 * for the Create Budget module.
 */
class CalculateBudgetSummaryUseCaseTest {

    private lateinit var useCase: CalculateBudgetSummaryUseCase

    @Before
    fun setUp() {
        useCase = CalculateBudgetSummaryUseCase()
    }

    @Test
    fun `when allocations match ceiling exactly, budget is zero-based balanced`() {
        val result = useCase(
            name = "Presupuesto mensual",
            totalAmount = 3500000.0,
            envelopeAmounts = listOf(1500000.0, 1000000.0, 1000000.0)
        )

        assertEquals(3500000.0, result.totalAmount, 0.001)
        assertEquals(3500000.0, result.allocatedAmount, 0.001)
        assertEquals(0.0, result.unassignedAmount, 0.001)
        assertEquals(100f, result.progressPercentage, 0.001f)
        assertFalse(result.isOverbudget)
        assertTrue(result.isZeroBasedBalanced)
        assertTrue(result.isFormSubmittable)
        assertEquals("#10B981", result.ruleBannerColorHex)
    }

    @Test
    fun `when allocations exceed ceiling, overbudget rule triggers and blocks submission`() {
        val result = useCase(
            name = "Presupuesto mensual",
            totalAmount = 3500000.0,
            envelopeAmounts = listOf(2000000.0, 1500000.0, 500000.0) // 4,000,000 > 3,500,000
        )

        assertEquals(3500000.0, result.totalAmount, 0.001)
        assertEquals(4000000.0, result.allocatedAmount, 0.001)
        assertEquals(-500000.0, result.unassignedAmount, 0.001)
        assertTrue(result.progressPercentage > 100f)
        assertTrue(result.isOverbudget)
        assertFalse(result.isZeroBasedBalanced)
        assertFalse(result.isFormSubmittable) // Zero-based budgeting block!
        assertEquals("#EF4444", result.ruleBannerColorHex)
        assertTrue(result.ruleMessage.contains("500.000 más que tu techo total"))
    }

    @Test
    fun `when partial allocations exist, unassigned amount is positive and submittable`() {
        val result = useCase(
            name = "Presupuesto mensual familiar",
            totalAmount = 3500000.0,
            envelopeAmounts = listOf(500000.0, 300000.0, 700000.0) // 1,500,000 of 3,500,000
        )

        assertEquals(3500000.0, result.totalAmount, 0.001)
        assertEquals(1500000.0, result.allocatedAmount, 0.001)
        assertEquals(2000000.0, result.unassignedAmount, 0.001)
        assertEquals(42.857f, result.progressPercentage, 0.1f)
        assertFalse(result.isOverbudget)
        assertFalse(result.isZeroBasedBalanced)
        assertTrue(result.isFormSubmittable)
        assertEquals("#0284C7", result.ruleBannerColorHex)
    }

    @Test
    fun `when budget name is blank or total is zero, form cannot be submitted`() {
        val emptyNameResult = useCase(
            name = "   ",
            totalAmount = 3500000.0,
            envelopeAmounts = listOf(1000000.0)
        )
        assertFalse(emptyNameResult.isFormSubmittable)

        val zeroTotalResult = useCase(
            name = "Presupuesto",
            totalAmount = 0.0,
            envelopeAmounts = emptyList()
        )
        assertFalse(zeroTotalResult.isFormSubmittable)
    }
}
