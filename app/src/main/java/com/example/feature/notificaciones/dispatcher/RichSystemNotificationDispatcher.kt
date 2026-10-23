package com.example.feature.notificaciones.dispatcher

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.local.entity.NotificationEntity
import com.example.feature.notificaciones.receiver.NotificationActionBroadcastReceiver

class RichSystemNotificationDispatcher(private val context: Context) {

    companion object {
        const val CHANNEL_CRITICAL = "kash_channel_critical_finance"
        const val CHANNEL_TAX_DIAN = "kash_channel_tax_dian"
        const val CHANNEL_COLLAB_BANK = "kash_channel_collab_bank"
        const val CHANNEL_AI_DIGEST = "kash_channel_ai_digest"
    }

    init {
        ensureChannelsCreated()
    }

    private fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_CRITICAL,
                    "Alertas Críticas, Sobregiros y Vencimientos",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas urgentes de topes superados, pago de tarjetas y liquidez"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_TAX_DIAN,
                    "Radar Tributario DIAN & Control 4x1000",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avisos de topes en UVT, 350/65 UVT exentas y Factura Electrónica"
                },
                NotificationChannel(
                    CHANNEL_COLLAB_BANK,
                    "Gastos Compartidos & Captura Bancaria",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Movimientos detectados en bancos y liquidaciones de grupos"
                },
                NotificationChannel(
                    CHANNEL_AI_DIGEST,
                    "Resúmenes Inteligentes & Novedades Cloud",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Briefing semanal con IA, TRM y campañas informativas"
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    fun dispatchSystemNotification(entity: NotificationEntity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = when {
            entity.severity == "CRITICAL" -> CHANNEL_CRITICAL
            entity.category == "TAX_DIAN" || entity.category == "BANK_GMF" -> CHANNEL_TAX_DIAN
            entity.category == "SHARED_EXPENSE" || entity.category == "AUTO_CAPTURE" -> CHANNEL_COLLAB_BANK
            else -> CHANNEL_AI_DIGEST
        }

        val notificationKey = "${entity.userId}:${entity.id}".hashCode()

        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("extra_notification_id", entity.id)
            putExtra("extra_notification_user_id", entity.userId)
            putExtra("extra_deep_link_route", entity.deepLinkRoute)
        }
        val contentPendingIntent = PendingIntent.getActivity(
            context,
            notificationKey,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción 1: Marcar como leída sin abrir la app (vinculada estrictamente al userId propietario)
        val markReadIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
            action = NotificationActionBroadcastReceiver.ACTION_MARK_READ
            putExtra("extra_notification_id", entity.id)
            putExtra("extra_user_id", entity.userId)
        }
        val markReadPending = PendingIntent.getBroadcast(
            context,
            ("${entity.userId}_${entity.id}_read").hashCode(),
            markReadIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Acción 2: Posponer 2 horas (Snooze)
        val snoozeIntent = Intent(context, NotificationActionBroadcastReceiver::class.java).apply {
            action = NotificationActionBroadcastReceiver.ACTION_SNOOZE_2H
            putExtra("extra_notification_id", entity.id)
            putExtra("extra_user_id", entity.userId)
        }
        val snoozePending = PendingIntent.getBroadcast(
            context,
            ("${entity.userId}_${entity.id}_snooze").hashCode(),
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_shield_check)
            .setContentTitle(entity.title)
            .setContentText(entity.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(entity.body))
            .setPriority(
                if (entity.severity == "CRITICAL") NotificationCompat.PRIORITY_HIGH
                else NotificationCompat.PRIORITY_DEFAULT
            )
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Marcar leída", markReadPending)
            .addAction(0, "Posponer 2h", snoozePending)

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationKey, builder.build())
        }
    }

    fun cancelAllSystemNotifications() {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
