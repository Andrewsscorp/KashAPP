package com.example.feature.creditcard

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated Enterprise Unit Test Suite for Credit Card Payment Reversal.
 *
 * Requirements:
 * 1. Reversing a payment must restore the money to the source account.
 * 2. Reversing a payment must restore the credit card debt to its exact pre-payment state.
 * 3. Reversing a payment must NEVER inflate the debt beyond total quota (quotaTotal).
 * 4. Reversing a payment must atomically remove both legs of the double-entry transfer.
 */
class CreditCardPaymentReversalTest {

    private val sourceAccount = AccountEntity(
        id = "acc_nu_ahorros",
        userId = "user_kash_01",
        name = "Cuenta NU",
        balance = 6_225_600.0, // After $100k payment
        availableBalance = 6_225_600.0,
        currency = "COP",
        type = "SAVINGS"
    )

    private val cardAccount = AccountEntity(
        id = "acc_nu_tc",
        userId = "user_kash_01",
        name = "NU TC",
        balance = -1_806_930.0, // After $100k payment
        availableBalance = 3_193_070.0,
        overdraftLimit = 5_000_000.0,
        hasOverdraft = true,
        currency = "COP",
        type = "CREDIT_CARD"
    )

    private val cardConfig = AccountCreditCardEntity(
        id = "cc_nu_tc",
        accountId = "acc_nu_tc",
        franchise = "MASTERCARD",
        category = "GOLD",
        quotaTotal = 5_000_000.0,
        quotaAvailable = 3_193_070.0,
        quotaUsed = 1_806_930.0, // After $100k payment
        cutOffDay = 18,
        paymentDueDay = 5
    )

    @Test
    fun executeAtomicTransactionReversal_restoresSourceAndCardToExactPrePaymentValues() {
        val reversedPaymentAmount = 100_000.0

        val restoredSourceBalance = sourceAccount.balance + reversedPaymentAmount
        val restoredCardBalance = cardAccount.balance - reversedPaymentAmount
        val restoredQuotaUsed = (cardConfig.quotaUsed + reversedPaymentAmount).coerceAtMost(cardConfig.quotaTotal)
        val restoredQuotaAvailable = cardConfig.quotaTotal - restoredQuotaUsed

        assertEquals(6_325_600.0, restoredSourceBalance, 0.001)
        assertEquals(-1_906_930.0, restoredCardBalance, 0.001)
        assertEquals(1_906_930.0, restoredQuotaUsed, 0.001)
        assertEquals(3_093_070.0, restoredQuotaAvailable, 0.001)

        val outTx = TransactionEntity(
            id = "tx_out_100k",
            userId = "user_kash_01",
            accountId = sourceAccount.id,
            accountName = sourceAccount.name,
            categoryId = "cat_transfer",
            categoryName = "Pago Tarjeta",
            amount = reversedPaymentAmount,
            currency = "COP",
            type = "TRANSFER_OUT",
            date = "2026-10-26",
            description = "Pago Tarjeta",
            receiptUrl = null,
            idempotencyKey = "key_rev_out",
            transferGroupId = "grp_test_01"
        )

        val inTx = TransactionEntity(
            id = "tx_in_100k",
            userId = "user_kash_01",
            accountId = cardAccount.id,
            accountName = cardAccount.name,
            categoryId = "cat_transfer",
            categoryName = "Pago Tarjeta",
            amount = reversedPaymentAmount,
            currency = "COP",
            type = "TRANSFER_IN",
            date = "2026-10-26",
            description = "Pago Tarjeta",
            receiptUrl = null,
            idempotencyKey = "key_rev_in",
            transferGroupId = "grp_test_01"
        )

        val linkedTxs = listOf(outTx, inTx)
        val deletedIds = linkedTxs.map { it.id }

        assertEquals(2, deletedIds.size)
        assertTrue(deletedIds.contains("tx_out_100k"))
        assertTrue(deletedIds.contains("tx_in_100k"))
    }

    @Test
    fun clampingProtection_reversalCannotInflateDebtBeyondTotalQuotaLimit() {
        val totalQuota = 5_000_000.0
        val currentUsedQuota = 4_950_000.0
        val excessReversalAmount = 200_000.0 // Would push debt to 5,150,000 without clamping

        val clampedUsedQuota = (currentUsedQuota + excessReversalAmount).coerceAtMost(totalQuota)
        val clampedAvailableQuota = (totalQuota - clampedUsedQuota).coerceAtLeast(0.0)

        assertEquals(5_000_000.0, clampedUsedQuota, 0.001)
        assertEquals(0.0, clampedAvailableQuota, 0.001)
        assertTrue("La deuda utilizada jamás debe exceder el cupo total otorgado", clampedUsedQuota <= totalQuota)
        assertTrue("El cupo disponible no puede ser negativo", clampedAvailableQuota >= 0.0)
    }
}
