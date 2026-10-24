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
        const val CHANNEL_SECURITY_CRITICAL = "kash_security_critical"
        const val CHANNEL_PAYMENTS_DUE = "kash_payments_due"
        const val CHANNEL_BUDGETS_GOALS = "kash_budgets_goals"
        const val CHANNEL_TAX_AND_ASSETS = "kash_tax_and_assets"
        const val CHANNEL_WEEKLY_DIGEST = "kash_weekly_digest"

        // Backward compatibility channel aliases
        const val CHANNEL_CRITICAL = CHANNEL_SECURITY_CRITICAL
        const val CHANNEL_TAX_DIAN = CHANNEL_TAX_AND_ASSETS
        const val CHANNEL_COLLAB_BANK = "kash_channel_collab_bank"
        const val CHANNEL_AI_DIGEST = CHANNEL_WEEKLY_DIGEST

        /**
         * Sanitiza montos y dígitos sensibles para la pantalla de bloqueo (OWASP Shoulder Surfing protection).
         */
        fun sanitizeForLockScreen(text: String): String {
            val currencyOrNumberRegex = Regex("""(\$\s?[\d.,]+(\s?COP)?)|\b\d{4,}\b""")
            return text.replace(currencyOrNumberRegex) { "$ ••••••" }
        }
    }

    init {
        ensureChannelsCreated()
    }

    private fun ensureChannelsCreated() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            val channels = listOf(
                NotificationChannel(
                    CHANNEL_SECURITY_CRITICAL,
                    "Alertas Críticas, Sobregiros y Blindaje",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Alertas urgentes de sobregiros, seguridad y anomalías detectadas por IA"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_PAYMENTS_DUE,
                    "Fechas de Corte y Pagos Programados",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos de vencimiento de tarjetas de crédito, CDTs y deudas"
                    enableVibration(true)
                },
                NotificationChannel(
                    CHANNEL_BUDGETS_GOALS,
                    "Presupuestos por Sobres y Metas",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Superación de umbrales en presupuestos y avance de metas de ahorro"
                },
                NotificationChannel(
                    CHANNEL_TAX_AND_ASSETS,
                    "Radar Tributario DIAN & Patrimonio",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Avisos de topes en UVT, calendario tributario DIAN, 4x1000 y activos fijos"
                },
                NotificationChannel(
                    CHANNEL_WEEKLY_DIGEST,
                    "Resúmenes Semanales & Novedades Cloud",
                    NotificationManager.IMPORTANCE_LOW
                ).apply {
                    description = "Briefing semanal consolidado con análisis financiero sin interrupciones sonoras"
                },
                NotificationChannel(
                    CHANNEL_COLLAB_BANK,
                    "Gastos Compartidos & Captura Bancaria",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Movimientos detectados en bancos y liquidaciones de grupos"
                }
            )
            channels.forEach { manager.createNotificationChannel(it) }
        }
    }

    /**
     * Sanitiza montos y dígitos sensibles para la pantalla de bloqueo (OWASP Shoulder Surfing protection).
     */
    fun sanitizeForLockScreen(text: String): String {
        val currencyOrNumberRegex = Regex("""(\$\s?[\d.,]+(\s?COP)?)|\b\d{4,}\b""")
        return text.replace(currencyOrNumberRegex, "$ ••••••")
    }

    fun dispatchSystemNotification(entity: NotificationEntity, maskOnLockScreen: Boolean = true) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) return
        }

        val channelId = when {
            entity.severity == "CRITICAL" -> CHANNEL_SECURITY_CRITICAL
            entity.category in listOf("PAYMENT_DUE", "CREDIT_CARD", "DEBT") -> CHANNEL_PAYMENTS_DUE
            entity.category in listOf("BUDGET", "GOAL", "ENVELOPE") -> CHANNEL_BUDGETS_GOALS
            entity.category in listOf("TAX_DIAN", "BANK_GMF", "FIXED_ASSET") -> CHANNEL_TAX_AND_ASSETS
            entity.category in listOf("SHARED_EXPENSE", "AUTO_CAPTURE") -> CHANNEL_COLLAB_BANK
            else -> CHANNEL_WEEKLY_DIGEST
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
            .setAutoCancel(true)
            .setContentIntent(contentPendingIntent)
            .addAction(0, "Marcar leída", markReadPending)
            .addAction(0, "Posponer 2h", snoozePending)

        if (maskOnLockScreen) {
            builder.setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            val publicTitle = sanitizeForLockScreen(entity.title)
            val publicBody = sanitizeForLockScreen(entity.body)
            val publicNotification = NotificationCompat.Builder(context, channelId)
                .setSmallIcon(R.drawable.ic_shield_check)
                .setContentTitle(publicTitle)
                .setContentText(publicBody)
                .build()
            builder.setPublicVersion(publicNotification)
        } else {
            builder.setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
        }

        runCatching {
            NotificationManagerCompat.from(context).notify(notificationKey, builder.build())
        }
    }

    /**
     * Despacha una notificación de prueba real para que el usuario verifique la apariencia
     * y el enmascaramiento en la pantalla de bloqueo de su dispositivo físico.
     */
    fun dispatchTestNotification(userId: String = "user_default", maskOnLockScreen: Boolean = true) {
        val testEntity = NotificationEntity(
            id = "${userId}_test_${System.currentTimeMillis()}",
            userId = userId,
            dedupKey = "test_notif_${System.currentTimeMillis()}",
            title = "Alerta de Prueba: Compra de $ 1.450.000 COP",
            body = "Tarjeta Visa *4512 registró un movimiento de $ 1.450.000 COP. Saldo disponible al día.",
            category = "PAYMENT_DUE",
            severity = "HIGH",
            sourceOrigin = "TEST_RUNNER",
            deepLinkRoute = "SETTINGS_NOTIFICATIONS"
        )
        dispatchSystemNotification(testEntity, maskOnLockScreen)
    }

    fun cancelAllSystemNotifications() {
        runCatching {
            NotificationManagerCompat.from(context).cancelAll()
        }
    }
}
