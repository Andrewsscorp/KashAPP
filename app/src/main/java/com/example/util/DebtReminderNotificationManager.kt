package com.example.util

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R
import java.util.Calendar

class DebtReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val debtName = intent?.getStringExtra("debt_name") ?: "Obligación financiera"
        val amount = intent?.getDoubleExtra("amount", 0.0) ?: 0.0
        val dueDay = intent?.getIntExtra("due_day", 1) ?: 1
        val message = intent?.getStringExtra("message")
            ?: "Tu deuda '$debtName' tiene fecha de pago programada (Día $dueDay). Recuerda abonar a tiempo para evitar cobro de intereses moratorios."

        DebtReminderNotificationManager.showNotification(
            context = context,
            notificationId = (debtName.hashCode() and 0x7FFFFFFF),
            title = "🔔 Recordatorio de Pago KashApp",
            message = message
        )
    }
}

object DebtReminderNotificationManager {

    const val CHANNEL_ID = "kash_reminders_channel"
    private const val CHANNEL_NAME = "Recordatorios de Pago y Deudas"
    private const val CHANNEL_DESC = "Alertas preventivas de fechas de corte, cuotas y límites de pago de deudas y tarjetas."

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
                vibrationPattern = longArrayOf(0, 300, 150, 300)
            }
            val manager = context.getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(
        context: Context,
        notificationId: Int = 1001,
        title: String,
        message: String
    ) {
        createNotificationChannel(context)

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
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
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(NotificationCompat.DEFAULT_ALL)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)

        try {
            val managerCompat = NotificationManagerCompat.from(context)
            managerCompat.notify(notificationId, builder.build())
            Log.d("DebtReminderManager", "Notification delivered successfully: $title")
        } catch (e: SecurityException) {
            Log.e("DebtReminderManager", "Permission POST_NOTIFICATIONS not granted: ${e.message}")
        } catch (t: Throwable) {
            Log.e("DebtReminderManager", "Failed to dispatch notification: ${t.message}", t)
        }
    }

    fun scheduleReminder(
        context: Context,
        debtId: String,
        debtName: String,
        amount: Double,
        dueDay: Int,
        daysAhead: Int = 3,
        hourOfDay: Int = 9,
        minute: Int = 0
    ) {
        createNotificationChannel(context)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        val intent = Intent(context, DebtReminderReceiver::class.java).apply {
            putExtra("debt_name", debtName)
            putExtra("amount", amount)
            putExtra("due_day", dueDay)
            putExtra("message", "Tu deuda '$debtName' vence en $daysAhead días (Día $dueDay). Valor de la cuota: $${String.format(java.util.Locale.US, "%,.0f", amount)}.")
        }

        val requestCode = (debtId.hashCode() and 0x7FFFFFFF)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) PendingIntent.FLAG_IMMUTABLE else 0)
        )

        // Calculate next target time
        val calendar = Calendar.getInstance().apply {
            val currentDay = get(Calendar.DAY_OF_MONTH)
            val targetDay = (dueDay - daysAhead).coerceAtLeast(1)
            set(Calendar.DAY_OF_MONTH, targetDay)
            set(Calendar.HOUR_OF_DAY, hourOfDay)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) {
                add(Calendar.MONTH, 1)
            }
        }

        try {
            alarmManager.setAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
            Log.d("DebtReminderManager", "Scheduled reminder for $debtName at ${calendar.time}")
        } catch (e: SecurityException) {
            Log.w("DebtReminderManager", "Cannot schedule exact alarm: ${e.message}")
        } catch (t: Throwable) {
            Log.e("DebtReminderManager", "Error scheduling alarm: ${t.message}")
        }
    }
}
