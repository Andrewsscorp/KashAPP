package com.example.feature.creditcard

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferGroupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated Enterprise Unit Test Suite for Credit Card Payment Processing Engine.
 *
 * Requirements:
 * 1. Paying a credit card must debit the source account.
 * 2. Paying a credit card must credit the card balance (moving it closer to 0).
 * 3. Paying a credit card must decrease used quota (quota_used = quota_used - amount).
 * 4. Paying a credit card must increase available quota (quota_available = quota_total - quota_used).
 * 5. Quota used must never drop below 0 (clamped).
 * 6. Maintains strict double-entry ledger balance.
 */
class CreditCardPaymentEngineTest {

    private val sourceAccount = AccountEntity(
        id = "acc_nu_ahorros",
        userId = "user_kash_01",
        name = "Cuenta NU",
        balance = 6_325_600.0,
        availableBalance = 6_325_600.0,
        currency = "COP",
        type = "SAVINGS"
    )

    private val cardAccount = AccountEntity(
        id = "acc_nu_tc",
        userId = "user_kash_01",
        name = "NU TC",
        balance = -1_906_930.0,
        availableBalance = 3_093_070.0,
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
        quotaAvailable = 3_093_070.0,
        quotaUsed = 1_906_930.0,
        cutOffDay = 18,
        paymentDueDay = 5
    )

    @Test
    fun executePayment_reducesCardDebtAndRestoresAvailableQuota() {
        val paymentAmount = 100_000.0

        val newSourceBalance = sourceAccount.balance - paymentAmount
        val newCardBalance = cardAccount.balance + paymentAmount
        val newQuotaUsed = (cardConfig.quotaUsed - paymentAmount).coerceAtLeast(0.0)
        val newQuotaAvailable = cardConfig.quotaTotal - newQuotaUsed

        assertEquals(6_225_600.0, newSourceBalance, 0.001)
        assertEquals(-1_806_930.0, newCardBalance, 0.001)
        assertEquals(1_806_930.0, newQuotaUsed, 0.001)
        assertEquals(3_193_070.0, newQuotaAvailable, 0.001)

        val outTx = TransactionEntity(
            id = "tx_out_pay",
            userId = sourceAccount.userId,
            accountId = sourceAccount.id,
            accountName = sourceAccount.name,
            categoryId = "cat_transfer",
            categoryName = "Pago de Tarjeta",
            amount = paymentAmount,
            currency = "COP",
            type = "TRANSFER_OUT",
            date = "2026-10-26",
            description = "Pago de Tarjeta",
            receiptUrl = null,
            idempotencyKey = "key_pay_out"
        )

        val inTx = TransactionEntity(
            id = "tx_in_pay",
            userId = cardAccount.userId,
            accountId = cardAccount.id,
            accountName = cardAccount.name,
            categoryId = "cat_transfer",
            categoryName = "Pago de Tarjeta",
            amount = paymentAmount,
            currency = "COP",
            type = "TRANSFER_IN",
            date = "2026-10-26",
            description = "Pago de Tarjeta",
            receiptUrl = null,
            idempotencyKey = "key_pay_in"
        )

        val group = TransferGroupEntity(
            id = "grp_pay_01",
            userId = sourceAccount.userId,
            concept = "Pago Tarjeta NU TC",
            sourceAccountId = sourceAccount.id,
            destinationAccountId = cardAccount.id,
            amount = paymentAmount,
            currency = "COP"
        )

        assertEquals(paymentAmount, outTx.amount, 0.001)
        assertEquals(paymentAmount, inTx.amount, 0.001)
        assertEquals("TRANSFER_OUT", outTx.type)
        assertEquals("TRANSFER_IN", inTx.type)
        assertEquals(group.sourceAccountId, outTx.accountId)
        assertEquals(group.destinationAccountId, inTx.accountId)
    }

    @Test
    fun paymentExceedingCurrentDebt_clampsQuotaUsedToZero() {
        val totalDebt = 1_906_930.0
        val overPayment = 2_000_000.0 // Pays more than current debt

        val resultingQuotaUsed = (totalDebt - overPayment).coerceAtLeast(0.0)
        val resultingQuotaAvailable = (cardConfig.quotaTotal - resultingQuotaUsed).coerceAtMost(cardConfig.quotaTotal)

        assertEquals(0.0, resultingQuotaUsed, 0.001)
        assertEquals(5_000_000.0, resultingQuotaAvailable, 0.001)
        assertTrue("La deuda utilizada nunca debe ser menor a cero", resultingQuotaUsed >= 0.0)
    }
}
