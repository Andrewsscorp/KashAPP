package com.example

import com.example.data.local.entity.TransactionEntity
import com.example.domain.currency.CurrencyConversionEngine
import com.example.domain.security.TransactionProtectionEngine
import com.example.domain.validation.FormValidator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainLogicCoverageTest {

    @Test
    fun formValidator_sanitizesTextTagsCurrencyAmountsAndColors() {
        assertEquals("Hola mundo", FormValidator.sanitizeText("\u0000  Hola   mundo  "))
        assertEquals("1234.56", FormValidator.sanitizeConcept(" 1234.56 "))
        assertEquals(listOf("viaje", "hogar"), FormValidator.sanitizeTags(listOf(" Viaje ", "viaje", "hogar", "bad tag", "")))
        assertEquals("USD", FormValidator.sanitizeCurrency(" usd "))
        assertEquals("BAD", FormValidator.sanitizeCurrency("bad"))
        assertEquals(1_234_567.89, FormValidator.sanitizeAmount("$ 1.234.567,89") ?: 0.0, 0.001)
        assertEquals(12.5, FormValidator.sanitizeRate("12,5%") ?: 0.0, 0.001)
        assertEquals("#AABBCC", FormValidator.sanitizeColorHex("aabbcc"))
        assertEquals("#0284C7", FormValidator.sanitizeColorHex("invalid"))
        assertEquals("123456", FormValidator.sanitizeAccountNumber(" 123 / 456 "))
    }

    @Test
    fun formValidator_rejectsInvalidFieldsAndAcceptsValidFlows() {
        assertTrue(FormValidator.validateName("A") is FormValidator.ValidationResult.Invalid)
        assertTrue(FormValidator.validateName("Cuenta válida") is FormValidator.ValidationResult.Valid)
        assertTrue(FormValidator.validateAmount(Double.NaN) is FormValidator.ValidationResult.Invalid)
        assertTrue(FormValidator.validateAmount(10.0) is FormValidator.ValidationResult.Valid)
        assertTrue(FormValidator.validateAccountSelection("") is FormValidator.ValidationResult.Invalid)
        assertTrue(FormValidator.validateCategorySelection("cat-1") is FormValidator.ValidationResult.Valid)
        assertTrue(FormValidator.validateExpense(100.0, "acc", "cat", "Compra") is FormValidator.ValidationResult.Valid)
        assertTrue(FormValidator.validateExpense(-1.0, "", "", null) is FormValidator.ValidationResult.Invalid)
        assertTrue(FormValidator.validateIncome(100.0, "acc", "cat", "Empresa") is FormValidator.ValidationResult.Valid)
        assertTrue(FormValidator.validateIncome(100.0, "acc", "cat", "") is FormValidator.ValidationResult.Invalid)
    }

    @Test
    fun currencyEngine_parsesConvertsAndFormatsLocalizedAmounts() {
        assertEquals(1_500_000.0, CurrencyConversionEngine.parseUserInputAmount("1.500.000"), 0.001)
        assertEquals(1500.75, CurrencyConversionEngine.parseUserInputAmount("1.500,75"), 0.001)
        assertEquals(1500.75, CurrencyConversionEngine.parseUserInputAmount("1,500.75"), 0.001)
        assertEquals(0.0, CurrencyConversionEngine.parseUserInputAmount("invalid"), 0.001)
        assertEquals(200.0, CurrencyConversionEngine.convert(100.0, 2.0), 0.001)
        assertEquals(100.0, CurrencyConversionEngine.convert(100.0, 0.0), 0.001)
        assertEquals("$ 1.234.567 COP", CurrencyConversionEngine.formatLocalized(1_234_567.0, "COP"))
        assertEquals("1,25", CurrencyConversionEngine.formatRateValue(1.25))
    }

    @Test
    fun transactionProtection_detectsTamperingAndAuditsBalances() {
        val transaction = transaction(amount = 100.0, type = "EXPENSE")
        val hash = TransactionProtectionEngine.computeTransactionHash(transaction)
        assertEquals(64, hash.length)
        assertTrue(TransactionProtectionEngine.verifyTransactionIntegrity(transaction, hash))
        assertTrue(TransactionProtectionEngine.verifyTransactionIntegrity(transaction, hash.uppercase()))
        assertFalse(TransactionProtectionEngine.verifyTransactionIntegrity(transaction.copy(amount = 101.0), hash))
        assertTrue(TransactionProtectionEngine.verifyTransactionIntegrity(transaction, null))

        val report = TransactionProtectionEngine.auditAccountBalanceIntegrity(
            initialBalance = 1_000.0,
            transactions = listOf(
                transaction.copy(id = "income", amount = 250.0, type = "INCOME"),
                transaction.copy(id = "expense", amount = 100.0, type = "EXPENSE"),
                transaction.copy(id = "transfer", amount = 50.0, type = "TRANSFER"),
                transaction.copy(id = "transfer-out", amount = -25.0, type = "TRANSFER")
            )
        )
        assertEquals(4, report.transactionCount)
        assertEquals(300.0, report.totalInflow, 0.001)
        assertEquals(125.0, report.totalOutflow, 0.001)
        assertEquals(1_175.0, report.computedNetBalance, 0.001)
        assertTrue(report.isLockedAgainstDeletion)
    }

    private fun transaction(amount: Double, type: String) = TransactionEntity(
        id = "tx-1",
        userId = "user-1",
        accountId = "account-1",
        accountName = "Account",
        categoryId = "category-1",
        categoryName = "Category",
        amount = amount,
        currency = "COP",
        type = type,
        date = "2026-10-12",
        description = "Test",
        receiptUrl = null,
        idempotencyKey = "key-$amount-$type"
    )
}
