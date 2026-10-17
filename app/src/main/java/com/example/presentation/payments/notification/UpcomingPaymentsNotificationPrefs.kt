package com.example.presentation.payments.notification

import android.content.Context
import android.content.SharedPreferences

/**
 * Model representing user notification and alert preferences for upcoming payments,
 * credit cards, and loan obligations.
 */
data class PaymentNotificationConfig(
    val enabled: Boolean = true,
    val advanceNoticeDays: Int = 3, // 1, 2, 3, 5, 7 days before due date
    val notifyOnDueDate: Boolean = true,
    val notifyDailyIfOverdue: Boolean = true,
    val notifyCreditCards: Boolean = true,
    val notifyLoansAndDebts: Boolean = true,
    val notifyServices: Boolean = true,
    val preferredHour: Int = 9, // 09:00 AM
    val preferredMinute: Int = 0,
    val soundAndVibrate: Boolean = true,
    val lastRunTimestamp: Long = 0L
)

/**
 * SharedPreferences manager to persist and load [PaymentNotificationConfig]
 * synchronously and safely from both UI and WorkManager background tasks.
 */
class UpcomingPaymentsNotificationPrefsManager(context: Context) {

    private val prefs: SharedPreferences = context.applicationContext.getSharedPreferences(
        PREFS_NAME,
        Context.MODE_PRIVATE
    )

    fun getConfig(): PaymentNotificationConfig {
        return PaymentNotificationConfig(
            enabled = prefs.getBoolean(KEY_ENABLED, true),
            advanceNoticeDays = prefs.getInt(KEY_ADVANCE_DAYS, 3),
            notifyOnDueDate = prefs.getBoolean(KEY_ON_DUE_DATE, true),
            notifyDailyIfOverdue = prefs.getBoolean(KEY_DAILY_IF_OVERDUE, true),
            notifyCreditCards = prefs.getBoolean(KEY_NOTIFY_CREDIT_CARDS, true),
            notifyLoansAndDebts = prefs.getBoolean(KEY_NOTIFY_LOANS_DEBTS, true),
            notifyServices = prefs.getBoolean(KEY_NOTIFY_SERVICES, true),
            preferredHour = prefs.getInt(KEY_PREFERRED_HOUR, 9),
            preferredMinute = prefs.getInt(KEY_PREFERRED_MINUTE, 0),
            soundAndVibrate = prefs.getBoolean(KEY_SOUND_VIBRATE, true),
            lastRunTimestamp = prefs.getLong(KEY_LAST_RUN, 0L)
        )
    }

    fun saveConfig(config: PaymentNotificationConfig) {
        prefs.edit()
            .putBoolean(KEY_ENABLED, config.enabled)
            .putInt(KEY_ADVANCE_DAYS, config.advanceNoticeDays)
            .putBoolean(KEY_ON_DUE_DATE, config.notifyOnDueDate)
            .putBoolean(KEY_DAILY_IF_OVERDUE, config.notifyDailyIfOverdue)
            .putBoolean(KEY_NOTIFY_CREDIT_CARDS, config.notifyCreditCards)
            .putBoolean(KEY_NOTIFY_LOANS_DEBTS, config.notifyLoansAndDebts)
            .putBoolean(KEY_NOTIFY_SERVICES, config.notifyServices)
            .putInt(KEY_PREFERRED_HOUR, config.preferredHour)
            .putInt(KEY_PREFERRED_MINUTE, config.preferredMinute)
            .putBoolean(KEY_SOUND_VIBRATE, config.soundAndVibrate)
            .apply()
    }

    fun updateLastRunTimestamp(timestamp: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_LAST_RUN, timestamp).apply()
    }

    companion object {
        private const val PREFS_NAME = "kashapp_upcoming_payments_notification_prefs"
        private const val KEY_ENABLED = "key_notif_enabled"
        private const val KEY_ADVANCE_DAYS = "key_advance_notice_days"
        private const val KEY_ON_DUE_DATE = "key_on_due_date"
        private const val KEY_DAILY_IF_OVERDUE = "key_daily_if_overdue"
        private const val KEY_NOTIFY_CREDIT_CARDS = "key_notify_credit_cards"
        private const val KEY_NOTIFY_LOANS_DEBTS = "key_notify_loans_debts"
        private const val KEY_NOTIFY_SERVICES = "key_notify_services"
        private const val KEY_PREFERRED_HOUR = "key_preferred_hour"
        private const val KEY_PREFERRED_MINUTE = "key_preferred_minute"
        private const val KEY_SOUND_VIBRATE = "key_sound_vibrate"
        private const val KEY_LAST_RUN = "key_last_run_timestamp"
    }
}
