package com.example.feature.capturaautomatica.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.data.local.entity.PendingAutoCaptureEntity
import java.text.NumberFormat
import java.util.Locale

object AutoCaptureNotificationHelper {

    const val CHANNEL_ID = "kashapp_auto_capture"
    const val CHANNEL_NAME = "Captura Automática Bancaria"
    const val CHANNEL_DESC = "Notificaciones detectadas de bancos colombianos para registro de movimientos en 1 toque"

    const val ACTION_CONFIRM_CAPTURE = "com.example.feature.capturaautomatica.ACTION_CONFIRM"
    const val ACTION_DISMISS_CAPTURE = "com.example.feature.capturaautomatica.ACTION_DISMISS"
    const val ACTION_OPEN_INBOX = "com.example.feature.capturaautomatica.ACTION_OPEN_INBOX"

    const val EXTRA_CAPTURE_ID = "extra_capture_id"
    const val EXTRA_USER_ID = "extra_user_id"
    const val EXTRA_NOTIFICATION_ID = "extra_notification_id"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                lockscreenVisibility = NotificationCompat.VISIBILITY_PRIVATE
                enableVibration(true)
                setShowBadge(true)
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.createNotificationChannel(channel)
        }
    }

    fun showCaptureNotification(
        context: Context,
        entity: PendingAutoCaptureEntity
    ) {
        createNotificationChannel(context)

        val notificationId = entity.id.hashCode()
        val formattedAmount = try {
            val localeCo = Locale.Builder().setLanguage("es").setRegion("CO").build()
            val format = NumberFormat.getCurrencyInstance(localeCo)
            format.maximumFractionDigits = 0
            format.format(entity.amount)
        } catch (e: Exception) {
            "$${entity.amount.toLong()}"
        }

        val typeText = if (entity.transactionType == "INCOME") "Ingreso" else "Gasto"
        val title = "$typeText detectado: ${entity.detectedBankName}"
        val content = "$formattedAmount en ${entity.merchantOrCounterparty}"

        // Intent de Confirmación en 1 toque
        val confirmIntent = Intent(context, AutoCaptureActionReceiver::class.java).apply {
            action = ACTION_CONFIRM_CAPTURE
            putExtra(EXTRA_CAPTURE_ID, entity.id)
            putExtra(EXTRA_USER_ID, entity.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val confirmPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 1,
            confirmIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Intent de Descarte
        val dismissIntent = Intent(context, AutoCaptureActionReceiver::class.java).apply {
            action = ACTION_DISMISS_CAPTURE
            putExtra(EXTRA_CAPTURE_ID, entity.id)
            putExtra(EXTRA_USER_ID, entity.userId)
            putExtra(EXTRA_NOTIFICATION_ID, notificationId)
        }
        val dismissPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId * 2 + 2,
            dismissIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Content Intent (Abre el Inbox en la app)
        val launchIntent = context.packageManager.getLaunchIntentForPackage(context.packageName)?.apply {
            action = ACTION_OPEN_INBOX
            putExtra(EXTRA_CAPTURE_ID, entity.id)
            putExtra(EXTRA_USER_ID, entity.userId)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val contentPendingIntent = launchIntent?.let {
            PendingIntent.getActivity(
                context,
                notificationId,
                it,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    "$content\nCuenta: ${entity.matchedAccountName ?: "Por asignar"}\nCategoría sugerida: ${entity.matchedCategoryName ?: "General"}"
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(
                android.R.drawable.ic_menu_save,
                "Confirmar",
                confirmPendingIntent
            )
            .addAction(
                android.R.drawable.ic_menu_close_clear_cancel,
                "Descartar",
                dismissPendingIntent
            )
            .build()

        try {
            NotificationManagerCompat.from(context).notify(notificationId, notification)
        } catch (e: SecurityException) {
            // Android 13+ missing POST_NOTIFICATIONS permission handled gracefully
        }
    }

    fun cancelNotification(context: Context, notificationId: Int) {
        NotificationManagerCompat.from(context).cancel(notificationId)
    }
}
