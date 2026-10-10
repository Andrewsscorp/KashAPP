package com.example.domain.usecase

import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountPayrollConfigEntity
import com.example.data.local.entity.TransactionEntity
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

/**
 * Result of daily payroll batch execution.
 */
data class PayrollBatchResult(
    val date: LocalDate,
    val totalChecked: Int,
    val processedCount: Int,
    val skippedCount: Int,
    val generatedTransactions: List<TransactionEntity>
)

/**
 * Clean Architecture UseCase for the Payroll Automation Engine.
 * 
 * Rules:
 * 1. Executes for all active auto_register = true payroll configs.
 * 2. Weekend Shift (Financial Banking Rule): If pay day is on Saturday or Sunday,
 *    banks pay on the nearest preceding Friday.
 * 3. Idempotency: Prevents duplicate income records if payroll was already registered this month.
 * 4. Atomic Transaction: Records INCOME and increments account balance.
 */
class PayrollAutomationUseCase(
    private val financialDao: FinancialDao
) {

    suspend fun executeDailyBatch(currentDate: LocalDate, userId: String = "default_user"): PayrollBatchResult {
        val allConfigs = financialDao.getActivePayrollConfigs()
        val yearMonth = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
        val generatedTxns = mutableListOf<TransactionEntity>()
        var processed = 0
        var skipped = 0

        for (config in allConfigs) {
            val isDue = isConfigDueOnDate(config, currentDate)
            if (!isDue) {
                continue
            }

            // Check idempotency: Was payroll already paid this month?
            val countThisMonth = financialDao.countPayrollTransactionsThisMonth(config.accountId, yearMonth)
            if (countThisMonth > 0) {
                skipped++
                continue
            }

            // Create automatic income transaction
            val txnId = "txn_auto_payroll_${UUID.randomUUID()}"
            val payrollTransaction = TransactionEntity(
                id = txnId,
                userId = userId,
                accountId = config.accountId,
                accountName = "Nómina (${config.employerName})",
                categoryId = "cat_payroll",
                categoryName = "Nómina",
                sourceId = null,
                sourceName = config.employerName,
                amount = config.expectedIncome,
                currency = "COP",
                type = "INCOME",
                date = currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE),
                description = "Auto: Nómina - Ingreso de ${config.employerName}",
                receiptUrl = null,
                idempotencyKey = "payroll_${config.accountId}_$yearMonth",
                isRecurring = true,
                recurrenceFrequency = config.periodicity,
                saveAsTemplate = false,
                tags = "Nómina,Automático,${config.employerName}",
                syncStatus = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            // Atomic execution in Room
            financialDao.executeIncomeTransaction(
                transaction = payrollTransaction,
                userId = userId
            )

            generatedTxns.add(payrollTransaction)
            processed++
        }

        return PayrollBatchResult(
            date = currentDate,
            totalChecked = allConfigs.size,
            processedCount = processed,
            skippedCount = skipped,
            generatedTransactions = generatedTxns
        )
    }

    /**
     * Determines whether a payroll configuration should execute on [date].
     * Takes into account month-end clamp (e.g. day 31 in 30-day month) and
     * weekend anticipation rule (Saturday/Sunday -> Friday).
     */
    fun isConfigDueOnDate(config: AccountPayrollConfigEntity, date: LocalDate): Boolean {
        val targetDay = config.expectedPayDay
        val maxDayThisMonth = date.lengthOfMonth()
        val effectivePayDay = targetDay.coerceAtMost(maxDayThisMonth)

        val scheduledDate = try {
            LocalDate.of(date.year, date.month, effectivePayDay)
        } catch (e: Exception) {
            date.withDayOfMonth(maxDayThisMonth)
        }

        // Adjust for weekend: if scheduledDate is Saturday, shift to Friday (scheduledDate - 1 day)
        // If scheduledDate is Sunday, shift to Friday (scheduledDate - 2 days)
        val adjustedDate = when (scheduledDate.dayOfWeek) {
            DayOfWeek.SATURDAY -> scheduledDate.minusDays(1)
            DayOfWeek.SUNDAY -> scheduledDate.minusDays(2)
            else -> scheduledDate
        }

        return date == adjustedDate
    }
}
