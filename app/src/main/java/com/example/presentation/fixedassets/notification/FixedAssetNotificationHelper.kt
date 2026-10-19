package com.example.presentation.fixedassets.notification

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
import java.text.DecimalFormat

object FixedAssetNotificationHelper {

    const val CHANNEL_ID = "fixed_assets_channel"
    private const val CHANNEL_NAME = "Activos Fijos y Cierres NIIF"
    private const val CHANNEL_DESC = "Recordatorios de depreciación fin de mes, alertas de taller y eventos de auditoría contable."

    private var channelInitialized = false

    fun initChannel(context: Context) {
        if (channelInitialized || Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = CHANNEL_DESC
            enableVibration(true)
            enableLights(true)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
        channelInitialized = true
    }

    private fun getPendingIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TAB", "FIXED_ASSETS")
        }
        return PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    /**
     * Notificación de fin de mes para ejecutar el cierre de depreciación contable NIC 16.
     */
    fun sendDepreciationReminder(
        context: Context,
        pendingAssetsCount: Int,
        totalDepreciationAmount: Double
    ) {
        initChannel(context)
        if (!hasNotificationPermission(context)) return

        val moneyFmt = DecimalFormat("$ #,##0")
        val amountStr = moneyFmt.format(totalDepreciationAmount)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🔔 Cierre Mensual de Depreciación (NIC 16)")
            .setContentText("Tienes $pendingAssetsCount activos pendientes de cierre por $amountStr COP.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("El periodo contable actual tiene $pendingAssetsCount activos en uso listos para su cuota de depreciación por un total acumulado de $amountStr COP. Toca para procesar el lote automático.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2001, notification)
    }

    /**
     * Notificación de orden de mantenimiento en curso.
     */
    fun sendMaintenanceAlert(
        context: Context,
        assetName: String,
        orderTitle: String
    ) {
        initChannel(context)
        if (!hasNotificationPermission(context)) return

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🛠️ Orden en Taller: $assetName")
            .setContentText("Mantenimiento activo: $orderTitle")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("El activo '$assetName' ha ingresado a estado EN MANTENIMIENTO con la orden: '$orderTitle'. Las cuotas financieras se encuentran en supervisión.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2002, notification)
    }

    /**
     * Notificación de alta de nuevo activo patrimonial.
     */
    fun sendAssetCreatedNotification(
        context: Context,
        assetName: String,
        acquisitionValue: Double
    ) {
        initChannel(context)
        if (!hasNotificationPermission(context)) return

        val moneyFmt = DecimalFormat("$ #,##0")
        val amountStr = moneyFmt.format(acquisitionValue)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("📦 Alta Contable: $assetName")
            .setContentText("Nuevo activo registrado por $amountStr COP.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("Se ha incorporado el activo '$assetName' con base depreciable de $amountStr COP. Su movimiento contable de Adquisición y registro de Auditoría quedaron respaldados.")
            )
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(getPendingIntent(context))
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(context).notify(2003, notification)
    }
}
