package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.RoomLocalDb
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountPayrollConfigEntity
import com.example.data.local.entity.TransactionEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.concurrent.TimeUnit

/**
 * WorkManager CoroutineWorker and Domain Engine for scheduled and recurring payroll income generation.
 * 
 * Key Financial Business Logic:
 * 1. Idempotency Protection: Checks both transaction month history and uniqueness key to prevent duplicate payments.
 * 2. Banking Weekend Shift: If scheduled pay day falls on Saturday or Sunday, shifts to the nearest preceding Friday.
 * 3. End-of-Month Clamping: Adjusts for shorter months (e.g., February 28/29, April 30).
 * 4. ACID Compliance: Atomically creates the INCOME transaction and updates account balance in Room.
 */
class PayrollAutomationWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting PayrollAutomationWorker daily execution...")
        return@withContext try {
            val database = RoomLocalDb.getDatabase(appContext)
            val financialDao = database.financialDao()
            val currentDate = LocalDate.now()
            val userId = inputData.getString(KEY_USER_ID) ?: "default_user"

            val executionResult = executeDailyBatch(
                financialDao = financialDao,
                currentDate = currentDate,
                userId = userId
            )

            Log.i(
                TAG,
                "Payroll batch completed successfully: " +
                "${executionResult.processedCount} processed, " +
                "${executionResult.skippedCount} skipped out of " +
                "${executionResult.totalChecked} configs."
            )

            val outputData = workDataOf(
                KEY_PROCESSED_COUNT to executionResult.processedCount,
                KEY_SKIPPED_COUNT to executionResult.skippedCount,
                KEY_TOTAL_CHECKED to executionResult.totalChecked,
                KEY_EXECUTION_DATE to currentDate.format(DateTimeFormatter.ISO_LOCAL_DATE)
            )

            Result.success(outputData)
        } catch (e: Exception) {
            Log.e(TAG, "PayrollAutomationWorker encountered error during execution: ${e.message}", e)
            if (runAttemptCount < MAX_RETRIES) {
                Result.retry()
            } else {
                Result.failure(workDataOf(KEY_ERROR_MESSAGE to (e.message ?: "Unknown error")))
            }
        }
    }

    companion object {
        const val TAG = "PayrollAutoWorker"
        const val UNIQUE_PERIODIC_WORK_NAME = "kashapp_daily_payroll_automation"
        const val KEY_USER_ID = "payroll_user_id"
        const val KEY_PROCESSED_COUNT = "payroll_processed_count"
        const val KEY_SKIPPED_COUNT = "payroll_skipped_count"
        const val KEY_TOTAL_CHECKED = "payroll_total_checked"
        const val KEY_EXECUTION_DATE = "payroll_execution_date"
        const val KEY_ERROR_MESSAGE = "payroll_error_message"
        private const val MAX_RETRIES = 3

        /**
         * Schedules the daily recurring background worker via WorkManager.
         * Runs every 24 hours with battery and storage health constraints.
         */
        fun schedulePeriodic(
            context: Context,
            userId: String = "default_user",
            repeatIntervalHours: Long = 24
        ) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
                .setRequiresBatteryNotLow(true)
                .build()

            val workRequest = PeriodicWorkRequestBuilder<PayrollAutomationWorker>(
                repeatInterval = repeatIntervalHours,
                repeatIntervalTimeUnit = TimeUnit.HOURS,
                flexTimeInterval = 1,
                flexTimeIntervalUnit = TimeUnit.HOURS
            )
                .setConstraints(constraints)
                .setInputData(workDataOf(KEY_USER_ID to userId))
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniquePeriodicWork(
                UNIQUE_PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                workRequest
            )
            Log.i(TAG, "Periodic payroll automation enqueued (every $repeatIntervalHours hrs).")
        }

        /**
         * Triggers an immediate one-time execution (useful for on-demand sync, testing, or initial boot check).
         */
        fun runImmediateOnce(context: Context, userId: String = "default_user") {
            val workRequest = OneTimeWorkRequestBuilder<PayrollAutomationWorker>()
                .setInputData(workDataOf(KEY_USER_ID to userId))
                .addTag(TAG)
                .build()

            WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
                "${UNIQUE_PERIODIC_WORK_NAME}_immediate",
                ExistingWorkPolicy.REPLACE,
                workRequest
            )
            Log.i(TAG, "Immediate payroll automation work requested.")
        }

        /**
         * Direct execution engine: Evaluates all active payroll configs against the given [currentDate]
         * and atomically registers the income transactions in Room.
         */
        suspend fun executeDailyBatch(
            financialDao: FinancialDao,
            currentDate: LocalDate,
            userId: String = "default_user"
        ): PayrollAutomationResult {
            val allConfigs = financialDao.getActivePayrollConfigs()
            val yearMonth = currentDate.format(DateTimeFormatter.ofPattern("yyyy-MM"))
            val generatedTxns = mutableListOf<TransactionEntity>()
            var processed = 0
            var skipped = 0

            for (config in allConfigs) {
                // 1. Check Weekend Shift & Due Date Rule
                val isDue = isConfigDueOnDate(config, currentDate)
                if (!isDue) {
                    continue
                }

                // 2. Idempotency Check: Was payroll already credited this month for this account?
                val countThisMonth = financialDao.countPayrollTransactionsThisMonth(
                    accountId = config.accountId,
                    yearMonth = yearMonth
                )
                if (countThisMonth > 0) {
                    Log.d(TAG, "Idempotency skip: Payroll already paid for account ${config.accountId} in $yearMonth")
                    skipped++
                    continue
                }

                // Check specific unique idempotency key
                val idempotencyKey = "payroll_${config.accountId}_$yearMonth"
                val existingTxn = financialDao.getTransactionByIdempotencyKey(idempotencyKey)
                if (existingTxn != null) {
                    Log.d(TAG, "Idempotency key hit: $idempotencyKey already exists.")
                    skipped++
                    continue
                }

                // 3. Construct the ACID Payroll Income Transaction
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
                    idempotencyKey = idempotencyKey,
                    isRecurring = true,
                    recurrenceFrequency = config.periodicity,
                    saveAsTemplate = false,
                    tags = "Nómina,Automático,${config.employerName}",
                    syncStatus = "PENDING",
                    createdAt = System.currentTimeMillis()
                )

                // 4. Atomic Room Execution: Insert Transaction & Update Account Balance
                financialDao.executeIncomeTransaction(
                    transaction = payrollTransaction,
                    userId = userId
                )

                generatedTxns.add(payrollTransaction)
                processed++
                Log.i(TAG, "Generated payroll income: $txnId for account ${config.accountId}, amount: ${config.expectedIncome}")
            }

            return PayrollAutomationResult(
                date = currentDate,
                totalChecked = allConfigs.size,
                processedCount = processed,
                skippedCount = skipped,
                generatedTransactions = generatedTxns
            )
        }

        /**
         * Determines if a payroll configuration is due on [date], applying:
         * 1. Clamping to the current month's max days.
         * 2. Banking weekend anticipation (Saturday -> Friday -1 day, Sunday -> Friday -2 days).
         */
        fun isConfigDueOnDate(config: AccountPayrollConfigEntity, date: LocalDate): Boolean {
            val targetDay = config.expectedPayDay
            val maxDayThisMonth = date.lengthOfMonth()
            val effectiveDay = targetDay.coerceIn(1, maxDayThisMonth)

            val scheduledDate = try {
                LocalDate.of(date.year, date.month, effectiveDay)
            } catch (e: Exception) {
                date.withDayOfMonth(maxDayThisMonth)
            }

            val adjustedDate = calculateEffectivePayDate(scheduledDate)
            return date == adjustedDate
        }

        /**
         * Calculates the effective pay date for any target date by shifting weekend days
         * (Saturday / Sunday) backwards to the preceding business Friday.
         */
        fun calculateEffectivePayDate(scheduledDate: LocalDate): LocalDate {
            return when (scheduledDate.dayOfWeek) {
                DayOfWeek.SATURDAY -> scheduledDate.minusDays(1)
                DayOfWeek.SUNDAY -> scheduledDate.minusDays(2)
                else -> scheduledDate
            }
        }

        /**
         * Overloaded calculation for YearMonth and integer day of month.
         */
        fun calculateEffectivePayDate(yearMonth: YearMonth, scheduledDay: Int): LocalDate {
            val clampedDay = scheduledDay.coerceIn(1, yearMonth.lengthOfMonth())
            val targetDate = yearMonth.atDay(clampedDay)
            return calculateEffectivePayDate(targetDate)
        }
    }
}

/**
 * Result model summarizing the execution batch.
 */
data class PayrollAutomationResult(
    val date: LocalDate,
    val totalChecked: Int,
    val processedCount: Int,
    val skippedCount: Int,
    val generatedTransactions: List<TransactionEntity>
)
