package com.example.data.backup

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Suite de pruebas unitarias para el cortafuegos de sanitización de entidades de respaldo (Pilar 06).
 */
class BackupEntitySanitizerTest {

    @Test
    fun testNaNAndInfinityAmountsSanitizedToZero() {
        val rawAccount = AccountEntity(
            id = "acc_1",
            userId = "user_test",
            name = "Cuenta Invalida",
            balance = Double.NaN,
            availableBalance = Double.POSITIVE_INFINITY,
            overdraftLimit = Double.NEGATIVE_INFINITY,
            currency = "COP",
            type = "SAVINGS"
        )

        val (sanitized, repairs) = BackupEntitySanitizer.sanitizeAccount(rawAccount, "user_target")

        assertEquals(0.0, sanitized.balance, 0.0001)
        assertEquals(0.0, sanitized.availableBalance, 0.0001)
        assertEquals(0.0, sanitized.overdraftLimit, 0.0001)
        assertEquals("user_target", sanitized.userId)
        assertTrue(repairs >= 3)
    }

    @Test
    fun testOrphanTransactionsFilteredOut() {
        val validAccount = AccountEntity(
            id = "acc_valid",
            userId = "user_1",
            name = "Bancolombia",
            balance = 50000.0,
            currency = "COP",
            type = "SAVINGS"
        )

        val txValid = TransactionEntity(
            id = "tx_1",
            userId = "user_1",
            accountId = "acc_valid",
            accountName = "Bancolombia",
            categoryId = "cat_1",
            categoryName = "Comida",
            amount = 15000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-27",
            description = null,
            receiptUrl = null,
            idempotencyKey = "key_tx_1"
        )

        val txOrphan = TransactionEntity(
            id = "tx_orphan",
            userId = "user_1",
            accountId = "acc_inexistente",
            accountName = "Fantasma",
            categoryId = "cat_1",
            categoryName = "Comida",
            amount = 25000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-27",
            description = null,
            receiptUrl = null,
            idempotencyKey = "key_tx_orphan"
        )

        val batch = BackupEntitySanitizer.sanitizePortfolioBatch(
            targetUserId = "user_1",
            rawAccounts = listOf(validAccount),
            rawCategories = emptyList(),
            rawTransactions = listOf(txValid, txOrphan),
            rawFixedAssets = emptyList()
        )

        assertEquals(1, batch.accounts.size)
        assertEquals(1, batch.transactions.size)
        assertEquals("tx_1", batch.transactions[0].id)
        assertEquals(1, batch.discardedOrphanTransactions)
    }

    @Test
    fun testInvalidDatesAndColorsRepairedWithDefaults() {
        val (safeDate, repairedDate) = BackupEntitySanitizer.sanitizeIsoDate("fecha-totalmente-invalida")
        assertTrue(repairedDate)
        assertEquals(LocalDate.now().toString(), safeDate)

        val safeColor = BackupEntitySanitizer.sanitizeColorHex("no-es-color-hex", "#0284C7")
        assertEquals("#0284C7", safeColor)

        val validColor = BackupEntitySanitizer.sanitizeColorHex("#10B981")
        assertEquals("#10B981", validColor)
    }

    @Test
    fun testNegativeTransactionAmountsRejected() {
        val validAccount = AccountEntity(
            id = "acc_main",
            userId = "user_1",
            name = "Principal",
            balance = 100000.0,
            currency = "COP",
            type = "SAVINGS"
        )

        val txNegative = TransactionEntity(
            id = "tx_neg",
            userId = "user_1",
            accountId = "acc_main",
            accountName = "Principal",
            categoryId = "cat_1",
            categoryName = "Varios",
            amount = -5000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-27",
            description = null,
            receiptUrl = null,
            idempotencyKey = "key_tx_neg"
        )

        val (sanitized, repairs) = BackupEntitySanitizer.sanitizeTransaction(
            raw = txNegative,
            targetUserId = "user_1",
            validAccountIds = setOf("acc_main")
        )

        assertNull("Transacciones con monto negativo deben ser descartadas", sanitized)
        assertTrue(repairs > 0)
    }
}
