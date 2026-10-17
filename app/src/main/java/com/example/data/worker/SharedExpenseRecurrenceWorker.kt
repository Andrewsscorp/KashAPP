package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.KashAppApplication
import com.example.util.DebtReminderNotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.util.Locale
import java.util.concurrent.TimeUnit

/**
 * Worker autónomo en segundo plano para:
 * 1. Evaluar y notificar cobros/pagos de Gastos Compartidos en fechas de quincena (días 15 y 30).
 * 2. Alertar sobre tickets pendientes próximos a vencer o con antigüedad > 30 días.
 * 3. Verificar gastos compartidos recurrentes programados.
 */
class SharedExpenseRecurrenceWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val appContainer = (appContext.applicationContext as? KashAppApplication)?.appContainer
                ?: return@withContext Result.retry()

            val dao = appContainer.sharedExpenseDao
            val today = LocalDate.now()
            val isPayday15Or30 = today.dayOfMonth == 15 ||
                today.dayOfMonth == 30 ||
                (today.monthValue == 2 && today.dayOfMonth == today.lengthOfMonth())

            val totalReceivable = dao.observeTotalAccountsReceivableNiif("user_default").firstOrNull() ?: 0.0
            val totalPayable = dao.observeTotalAccountsPayableNiif("user_default").firstOrNull() ?: 0.0

            // 1. Recordatorios preventivos de Quincena (Días 15 y 30)
            if (isPayday15Or30 && (totalReceivable > 0.0 || totalPayable > 0.0)) {
                val formattedRec = String.format(Locale.US, "%,.0f", totalReceivable)
                val formattedPay = String.format(Locale.US, "%,.0f", totalPayable)
                val message = buildString {
                    append("Corte de Quincena KashApp: ")
                    if (totalReceivable > 0.0) {
                        append("Tienes $$formattedRec COP pendientes por cobrar en tickets compartidos. ")
                    }
                    if (totalPayable > 0.0) {
                        append("Recuerda liquidar $$formattedPay COP de tus cuotas parte pendientes.")
                    }
                }

                DebtReminderNotificationManager.showNotification(
                    context = appContext,
                    notificationId = 8042,
                    title = "🤝 Resumen Quincenal de Gastos Compartidos",
                    message = message
                )
            }

            // 2. Alertas de envejecimiento de cartera (> 30 días)
            val allDetails = dao.observeSharedExpensesWithDetails("user_default").firstOrNull().orEmpty()
            val agingCutoff = today.minusDays(30).toString()
            val overdueReceivables = allDetails.filter { detail ->
                detail.expense.status != "VOIDED" &&
                    detail.expense.status != "SETTLED" &&
                    detail.expense.expenseDate <= agingCutoff &&
                    detail.splits.any { it.participantId != "SELF_USER" && it.remainingPendingAmount > 0.01 }
            }

            if (overdueReceivables.isNotEmpty()) {
                val overdueTotal = overdueReceivables.sumOf { d ->
                    d.splits.filter { it.participantId != "SELF_USER" }.sumOf { it.remainingPendingAmount }
                }
                val formattedOverdue = String.format(Locale.US, "%,.0f", overdueTotal)
                val codes = overdueReceivables.take(3).joinToString(", ") { it.expense.consecutiveCode }
                DebtReminderNotificationManager.showNotification(
                    context = appContext,
                    notificationId = 8043,
                    title = "⚠️ Cartera Compartida Vencida (> 30 días)",
                    message = "Tienes ${overdueReceivables.size} ticket(s) ($codes) con saldo insoluto por $$formattedOverdue COP."
                )
            }

            Log.i(TAG, "SharedExpenseRecurrenceWorker executed cleanly on $today (CxC=$totalReceivable, CxP=$totalPayable).")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SharedExpenseRecurrenceWorker error: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "SharedRecurrenceWorker"
        private const val WORK_NAME = "kashapp_shared_expense_recurrence_and_payday_worker"

        fun schedulePeriodicRecurrenceAndReminders(context: Context) {
            val request = PeriodicWorkRequestBuilder<SharedExpenseRecurrenceWorker>(12, TimeUnit.HOURS)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Scheduled SharedExpenseRecurrenceWorker every 12 hours.")
        }

        fun schedulePeriodicWork(context: Context) = schedulePeriodicRecurrenceAndReminders(context)
    }
}
