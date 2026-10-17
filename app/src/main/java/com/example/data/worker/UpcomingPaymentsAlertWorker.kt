package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.data.local.RoomLocalDb
import com.example.presentation.payments.notification.UpcomingPaymentsNotificationHelper
import com.example.presentation.payments.notification.UpcomingPaymentsNotificationPrefsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.temporal.ChronoUnit

/**
 * WorkManager CoroutineWorker that runs periodically in the background (or on-demand)
 * to inspect upcoming bills, credit card liabilities, and loans, and alerts the user
 * before their scheduled due dates.
 */
class UpcomingPaymentsAlertWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting UpcomingPaymentsAlertWorker background scan...")
        return@withContext try {
            val isTest = inputData.getBoolean(KEY_IS_TEST, false)
            val prefsManager = UpcomingPaymentsNotificationPrefsManager(appContext)
            val config = prefsManager.getConfig()

            if (isTest) {
                Log.i(TAG, "Executing test notification alert...")
                UpcomingPaymentsNotificationHelper.sendTestNotification(appContext)
                return@withContext Result.success(workDataOf(KEY_ALERTS_SENT to 1))
            }

            if (!config.enabled) {
                Log.d(TAG, "Notification alerts are disabled in user configuration. Skipping scan.")
                return@withContext Result.success(workDataOf(KEY_ALERTS_SENT to 0))
            }

            val database = RoomLocalDb.getDatabase(appContext)
            val financialDao = database.financialDao()
            val userId = inputData.getString(KEY_USER_ID) ?: "user_default"
            val today = LocalDate.now()
            val moneyFormatter = DecimalFormat("#,##0")
            var alertsSentCount = 0

            // 1. Process Scheduled Payments (Bills, Loans, Subscriptions, Rent, etc.)
            val pendingPayments = financialDao.getPendingScheduledPayments(userId)
            for (payment in pendingPayments) {
                // Check category preferences
                val isCreditCardCat = payment.category == "TARJETA_CREDITO"
                val isLoanCat = payment.category == "DEUDA_PRESTAMO"
                val isServiceCat = payment.category in listOf("SERVICIOS", "SUSCRIPCION", "ARRIENDO", "OTRO")

                if (isCreditCardCat && !config.notifyCreditCards) continue
                if (isLoanCat && !config.notifyLoansAndDebts) continue
                if (isServiceCat && !config.notifyServices) continue

                val dueDate = try {
                    LocalDate.parse(payment.dueDate.take(10))
                } catch (_: Exception) {
                    null
                } ?: continue

                val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
                val alertWindow = payment.reminderDaysBefore.coerceAtLeast(config.advanceNoticeDays).toLong()

                val shouldAlert = when {
                    daysUntil < 0 -> config.notifyDailyIfOverdue
                    daysUntil == 0L -> config.notifyOnDueDate
                    daysUntil in 1L..alertWindow -> true
                    else -> false
                }

                if (shouldAlert) {
                    val urgencyPrefix = when {
                        daysUntil < 0 -> "⚠️ VENCIDO (${-daysUntil}d)"
                        daysUntil == 0L -> "🚨 ¡VENCE HOY!"
                        daysUntil == 1L -> "⚡ ¡Vence MAÑANA!"
                        else -> "⏳ Vence en $daysUntil días"
                    }

                    val formattedAmount = "$ ${moneyFormatter.format(payment.amount)} ${payment.currency}"
                    val title = "$urgencyPrefix: ${payment.entityName}"
                    val message = "Recuerda abonar $formattedAmount antes del ${payment.dueDate} para evitar sobrecostos e intereses moratorios."
                    val notificationId = ("scheduled_${payment.id}".hashCode() and 0x7FFFFFFF)

                    UpcomingPaymentsNotificationHelper.showPaymentNotification(
                        context = appContext,
                        notificationId = notificationId,
                        title = title,
                        message = message,
                        isUrgent = daysUntil <= 1L
                    )
                    alertsSentCount++
                }
            }

            // 2. Process Credit Cards (Liabilities with cut-off & payment due dates)
            if (config.notifyCreditCards) {
                val accounts = financialDao.getAccounts(userId)
                val creditCards = accounts.filter { it.type == "CREDIT_CARD" || it.type == "CREDIT" }

                for (acc in creditCards) {
                    // Only alert if there is a debt balance on the card
                    if (acc.balance <= 0.0) continue

                    val ccEntity = financialDao.getAccountCreditCard(acc.id)
                    val dueDay = ccEntity?.paymentDueDay?.coerceIn(1, 28) ?: 15
                    val targetMonth = if (today.dayOfMonth > dueDay) today.monthValue + 1 else today.monthValue
                    val targetYear = if (targetMonth > 12) today.year + 1 else today.year
                    val safeMonth = if (targetMonth > 12) 1 else targetMonth

                    val dueDate = try {
                        LocalDate.of(targetYear, safeMonth, dueDay)
                    } catch (_: Exception) {
                        today.plusDays(7)
                    }

                    val daysUntil = ChronoUnit.DAYS.between(today, dueDate)
                    val shouldAlert = when {
                        daysUntil < 0 -> config.notifyDailyIfOverdue
                        daysUntil == 0L -> config.notifyOnDueDate
                        daysUntil in 1L..config.advanceNoticeDays.toLong() -> true
                        else -> false
                    }

                    if (shouldAlert) {
                        val minPayment = ccEntity?.minimumPaymentAmount ?: (acc.balance * 0.08).coerceAtLeast(100_000.0)
                        val urgencyPrefix = when {
                            daysUntil < 0 -> "⚠️ TARJETA EN MORA"
                            daysUntil == 0L -> "🚨 ¡LÍMITE DE PAGO HOY!"
                            daysUntil == 1L -> "⚡ ¡Límite de pago MAÑANA!"
                            else -> "💳 Límite de pago en $daysUntil días"
                        }

                        val formattedTotal = "$ ${moneyFormatter.format(acc.balance)} ${acc.currency}"
                        val formattedMin = "$ ${moneyFormatter.format(minPayment)} ${acc.currency}"
                        val title = "$urgencyPrefix: ${acc.name}"
                        val message = "Pago mínimo sugerido: $formattedMin (Saldo total: $formattedTotal). Evita cobro de intereses moratorios."
                        val notificationId = ("cc_debt_${acc.id}".hashCode() and 0x7FFFFFFF)

                        UpcomingPaymentsNotificationHelper.showPaymentNotification(
                            context = appContext,
                            notificationId = notificationId,
                            title = title,
                            message = message,
                            isUrgent = daysUntil <= 1L
                        )
                        alertsSentCount++
                    }
                }
            }

            prefsManager.updateLastRunTimestamp()
            Log.i(TAG, "UpcomingPaymentsAlertWorker finished successfully with $alertsSentCount notifications dispatched.")
            Result.success(workDataOf(KEY_ALERTS_SENT to alertsSentCount))
        } catch (t: Throwable) {
            Log.e(TAG, "UpcomingPaymentsAlertWorker error during execution: ${t.message}", t)
            if (runAttemptCount < 2) {
                Result.retry()
            } else {
                Result.failure(workDataOf("error" to (t.message ?: "Unknown error")))
            }
        }
    }

    companion object {
        const val TAG = "UpcomingAlertWorker"
        const val KEY_IS_TEST = "key_is_test_alert"
        const val KEY_USER_ID = "key_alert_user_id"
        const val KEY_ALERTS_SENT = "key_alerts_sent_count"
    }
}
