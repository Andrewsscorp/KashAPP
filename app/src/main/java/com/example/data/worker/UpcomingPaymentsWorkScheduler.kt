package com.example.data.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.presentation.payments.notification.UpcomingPaymentsNotificationPrefsManager
import java.util.concurrent.TimeUnit

/**
 * Utility scheduler that orchestrates [UpcomingPaymentsAlertWorker] using Android Jetpack WorkManager.
 */
object UpcomingPaymentsWorkScheduler {

    const val TAG = "UpcomingWorkScheduler"
    const val PERIODIC_WORK_NAME = "kashapp_upcoming_payments_periodic_alert"
    const val IMMEDIATE_WORK_NAME = "kashapp_upcoming_payments_immediate_alert"

    /**
     * Enqueues recurring background execution of [UpcomingPaymentsAlertWorker]
     * according to user preferences.
     */
    fun schedulePeriodicAlerts(
        context: Context,
        userId: String = "user_default",
        forceUpdate: Boolean = false
    ) {
        val prefs = UpcomingPaymentsNotificationPrefsManager(context).getConfig()
        val workManager = WorkManager.getInstance(context.applicationContext)

        if (!prefs.enabled) {
            Log.i(TAG, "Notification alerts are disabled in preferences. Canceling periodic worker.")
            workManager.cancelUniqueWork(PERIODIC_WORK_NAME)
            return
        }

        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<UpcomingPaymentsAlertWorker>(
            repeatInterval = 12,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = 2,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .setInputData(
                workDataOf(
                    UpcomingPaymentsAlertWorker.KEY_USER_ID to userId,
                    UpcomingPaymentsAlertWorker.KEY_IS_TEST to false
                )
            )
            .addTag(TAG)
            .build()

        workManager.enqueueUniquePeriodicWork(
            PERIODIC_WORK_NAME,
            if (forceUpdate) ExistingPeriodicWorkPolicy.UPDATE else ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )

        Log.i(TAG, "Scheduled periodic UpcomingPaymentsAlertWorker (every 12 hrs, update=$forceUpdate).")
    }

    /**
     * Runs an immediate one-time execution of [UpcomingPaymentsAlertWorker].
     * Useful for testing notifications from the UI or performing a check right after configuration updates.
     */
    fun triggerImmediateCheck(
        context: Context,
        userId: String = "user_default",
        isTestNotification: Boolean = false
    ) {
        val workRequest = OneTimeWorkRequestBuilder<UpcomingPaymentsAlertWorker>()
            .setInputData(
                workDataOf(
                    UpcomingPaymentsAlertWorker.KEY_USER_ID to userId,
                    UpcomingPaymentsAlertWorker.KEY_IS_TEST to isTestNotification
                )
            )
            .addTag(TAG)
            .build()

        WorkManager.getInstance(context.applicationContext).enqueueUniqueWork(
            "${IMMEDIATE_WORK_NAME}_${if (isTestNotification) "test" else "scan"}",
            ExistingWorkPolicy.REPLACE,
            workRequest
        )

        Log.i(TAG, "Enqueued immediate UpcomingPaymentsAlertWorker (test=$isTestNotification).")
    }

    /**
     * Cancels scheduled periodic alerts.
     */
    fun cancelAlerts(context: Context) {
        WorkManager.getInstance(context.applicationContext).cancelUniqueWork(PERIODIC_WORK_NAME)
        Log.i(TAG, "Canceled periodic UpcomingPaymentsAlertWorker.")
    }
}
