package com.example.domain.currency

import com.example.domain.validation.FormValidator
import org.junit.Assert.*
import org.junit.Test

class CurrencyConversionEngineTest {

    @Test
    fun testEmptyInputYieldsEmptyParsedInput() {
        val emptyNull = CurrencyConversionEngine.parseMonetaryInput(null)
        val emptyString = CurrencyConversionEngine.parseMonetaryInput("")
        val emptySpaces = CurrencyConversionEngine.parseMonetaryInput("   ")

        assertTrue(emptyNull is ParsedMonetaryInput.Empty)
        assertEquals(0.0, emptyNull.amountOrZero, 0.0001)

        assertTrue(emptyString is ParsedMonetaryInput.Empty)
        assertEquals(0.0, emptyString.amountOrZero, 0.0001)

        assertTrue(emptySpaces is ParsedMonetaryInput.Empty)
        assertEquals(0.0, emptySpaces.amountOrZero, 0.0001)
    }

    @Test
    fun testExplicitZeroYieldsValidZero() {
        val zeroInput = CurrencyConversionEngine.parseMonetaryInput("0")
        val zeroFormatted = CurrencyConversionEngine.parseMonetaryInput("$ 0")

        assertTrue(zeroInput is ParsedMonetaryInput.Valid)
        assertEquals(0.0, (zeroInput as ParsedMonetaryInput.Valid).amount, 0.0001)

        assertTrue(zeroFormatted is ParsedMonetaryInput.Valid)
        assertEquals(0.0, (zeroFormatted as ParsedMonetaryInput.Valid).amount, 0.0001)
    }

    @Test
    fun testColombianThousandDotsParsing() {
        val parsed = CurrencyConversionEngine.parseMonetaryInput("1.000.000")
        assertTrue(parsed is ParsedMonetaryInput.Valid)
        assertEquals(1000000.0, (parsed as ParsedMonetaryInput.Valid).amount, 0.0001)

        val withCurrency = CurrencyConversionEngine.parseMonetaryInput("$ 4.500.000 COP")
        assertTrue(withCurrency is ParsedMonetaryInput.Valid)
        assertEquals(4500000.0, (withCurrency as ParsedMonetaryInput.Valid).amount, 0.0001)
    }

    @Test
    fun testDecimalsWithCommaOrDot() {
        val europeanDecimal = CurrencyConversionEngine.parseMonetaryInput("1.250,75")
        assertTrue(europeanDecimal is ParsedMonetaryInput.Valid)
        assertEquals(1250.75, (europeanDecimal as ParsedMonetaryInput.Valid).amount, 0.0001)

        val usDecimal = CurrencyConversionEngine.parseMonetaryInput("1,250.75")
        assertTrue(usDecimal is ParsedMonetaryInput.Valid)
        assertEquals(1250.75, (usDecimal as ParsedMonetaryInput.Valid).amount, 0.0001)
    }

    @Test
    fun testNegativeBalanceEnforcement() {
        val negativeNotAllowed = CurrencyConversionEngine.parseMonetaryInput("-250000", allowNegative = false)
        assertTrue(negativeNotAllowed is ParsedMonetaryInput.Invalid)

        val negativeAllowed = CurrencyConversionEngine.parseMonetaryInput("-250000", allowNegative = true)
        assertTrue(negativeAllowed is ParsedMonetaryInput.Valid)
        assertEquals(-250000.0, (negativeAllowed as ParsedMonetaryInput.Valid).amount, 0.0001)
    }

    @Test
    fun testInvalidFormatDetection() {
        val invalidChars = CurrencyConversionEngine.parseMonetaryInput("abc123")
        assertTrue(invalidChars is ParsedMonetaryInput.Invalid)

        val multipleCommas = CurrencyConversionEngine.parseMonetaryInput("1,2,3")
        assertTrue(multipleCommas is ParsedMonetaryInput.Invalid)
    }

    @Test
    fun testFormValidatorAccountValidation() {
        // Zero balance allowed for any account type
        val validZeroSavings = FormValidator.validateAccount(
            name = "Ahorros Bancolombia",
            type = "SAVINGS",
            initialBalance = 0.0,
            currency = "COP"
        )
        assertTrue(validZeroSavings.isValid)

        // Negative balance allowed for CHECKING (overdraft) and CREDIT_CARD
        val validCheckingOverdraft = FormValidator.validateAccount(
            name = "Corriente Principal",
            type = "CHECKING",
            initialBalance = -500000.0,
            currency = "COP"
        )
        assertTrue(validCheckingOverdraft.isValid)

        val validCreditCardDebt = FormValidator.validateAccount(
            name = "Visa Gold",
            type = "CREDIT_CARD",
            initialBalance = -1200000.0,
            currency = "COP"
        )
        assertTrue(validCreditCardDebt.isValid)

        // Negative balance disallowed for SAVINGS, PAYROLL, WALLET
        val invalidSavingsNegative = FormValidator.validateAccount(
            name = "Ahorros",
            type = "SAVINGS",
            initialBalance = -100.0,
            currency = "COP"
        )
        assertFalse(invalidSavingsNegative.isValid)

        val invalidPayrollNegative = FormValidator.validateAccount(
            name = "Nómina",
            type = "PAYROLL",
            initialBalance = -500.0,
            currency = "COP"
        )
        assertFalse(invalidPayrollNegative.isValid)
    }
}
