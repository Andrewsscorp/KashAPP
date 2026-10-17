package com.example.presentation.payments.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

/**
 * Helper object responsible for creating notification channels and building/displaying
 * high-priority notifications for upcoming payments, credit cards, and loan obligations.
 */
object UpcomingPaymentsNotificationHelper {

    const val CHANNEL_ID = "kashapp_upcoming_payments_channel"
    private const val CHANNEL_NAME = "Alertas de Próximos Pagos y Deudas"
    private const val CHANNEL_DESC = "Alertas preventivas y notificaciones programadas de vencimientos de cuotas, tarjetas y servicios."
    private const val TAG = "UpcomingNotifHelper"

    /**
     * Creates the Android Notification Channel (API 26+).
     */
    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                enableLights(true)
                vibrationPattern = longArrayOf(0, 250, 150, 250)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    /**
     * Dispatches an individual payment or debt alert.
     */
    fun showPaymentNotification(
        context: Context,
        notificationId: Int,
        title: String,
        message: String,
        isUrgent: Boolean = false
    ) {
        createNotificationChannel(context)

        // Deep-link intent to launch MainActivity and open Upcoming Payments module
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("open_upcoming_payments", true)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(if (isUrgent) NotificationCompat.PRIORITY_MAX else NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val managerCompat = NotificationManagerCompat.from(context)
            if (managerCompat.areNotificationsEnabled()) {
                managerCompat.notify(notificationId, builder.build())
                Log.d(TAG, "Notification delivered successfully: $title")
            } else {
                Log.w(TAG, "Notifications are disabled at the system level for this application.")
            }
        } catch (e: SecurityException) {
            Log.e(TAG, "Permission POST_NOTIFICATIONS missing or denied: ${e.message}")
        } catch (t: Throwable) {
            Log.e(TAG, "Failed to dispatch payment notification: ${t.message}", t)
        }
    }

    /**
     * Sends an immediate test notification via WorkManager to confirm that background alerts work properly.
     */
    fun sendTestNotification(context: Context) {
        showPaymentNotification(
            context = context,
            notificationId = 88001,
            title = "🔔 KashApp: Alertas de Pago Activas",
            message = "¡Servicio WorkManager configurado con éxito! Recibirás avisos antes del vencimiento de tus tarjetas de crédito y préstamos.",
            isUrgent = false
        )
    }
}
