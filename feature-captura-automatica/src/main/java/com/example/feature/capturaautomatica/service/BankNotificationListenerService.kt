package com.example.feature.capturaautomatica.service

import android.app.Notification
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
import com.example.feature.capturaautomatica.di.AutoCaptureEntryPoint
import com.example.feature.capturaautomatica.domain.model.SupportedColombianBank
import com.example.feature.capturaautomatica.domain.parser.OtpSafetyFilter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/**
 * Servicio de escucha de notificaciones 100% en dispositivo para entidades bancarias colombianas.
 * Diseñado con persistencia continua para operar incluso si la aplicación se encuentra CERRADA
 * o fue removida de la lista de aplicaciones recientes.
 */
class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Garantizar que Android reviva el servicio automáticamente si el proceso es terminado por memoria
        return START_STICKY
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        Log.i(TAG, "NotificationListenerService CONECTADO exitosamente al sistema Android.")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "NotificationListenerService DESCONECTADO del sistema Android. Solicitando re-enlace inmediato...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, BankNotificationListenerService::class.java))
            } catch (e: Throwable) {
                Log.w(TAG, "requestRebind notice: ${e.message}")
            }
        }
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.i(TAG, "KashApp cerrada por el usuario. Solicitando re-enlace para mantener escucha activa en segundo plano...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, BankNotificationListenerService::class.java))
            } catch (e: Throwable) {
                Log.w(TAG, "requestRebind en onTaskRemoved notice: ${e.message}")
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.notification == null) return

        val packageName = sbn.packageName ?: return

        // 0. Ignorar notificaciones de la propia KashApp para evitar bucles de retroalimentación
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val fullTextForAnalysis = listOfNotNull(title, text, bigText, subText).joinToString(" ")

        // 1. Filtrado instantáneo por paquete bancario soportado, SMS transaccional o palabras clave financieras
        val bank = SupportedColombianBank.findByPackage(packageName, title, fullTextForAnalysis)
            ?: if (packageName.contains("bank", ignoreCase = true) ||
                packageName.contains("banco", ignoreCase = true) ||
                packageName.contains("wallet", ignoreCase = true) ||
                packageName.contains("finance", ignoreCase = true) ||
                hasFinancialMovementKeywords(fullTextForAnalysis)
            ) {
                SupportedColombianBank.ENTIDAD_FINANCIERA
            } else {
                return
            }

        Log.i(TAG, "Notificación detectada para ${bank.displayName} ($packageName) con app ${if (isAppForeground()) "abierta" else "cerrada"}")

        // 2. Filtro estricto Anti-OTP y Privacidad (Ley 1581)
        if (OtpSafetyFilter.isSensitiveAuthMessage(fullTextForAnalysis)) {
            Log.i(TAG, "Notificación descartada por filtro de seguridad Anti-OTP/2FA de ${bank.displayName}")
            return
        }

        val effectiveText = if (!bigText.isNullOrBlank()) bigText else text
        val postTime = sbn.postTime

        serviceScope.launch {
            try {
                // 3. Verificación de sesión de usuario (con fallback garantizado a 'user_default' si la app está cerrada)
                val activeUserId = AutoCaptureEntryPoint.getActiveUserId(applicationContext)

                // 4. Obtención del repositorio (con fallback directo a RoomLocalDb si el contenedor UI está frío)
                val repository = AutoCaptureEntryPoint.getRepository(applicationContext)
                if (repository == null) {
                    Log.e(TAG, "AutoCaptureRepository no disponible en AutoCaptureEntryPoint.")
                    return@launch
                }

                // 5. Procesamiento y registro persistente en Room SQLite v38
                val result = repository.processNotification(
                    packageName = packageName,
                    title = title,
                    text = effectiveText,
                    postTime = postTime,
                    userId = activeUserId
                )

                val pendingCapture = result.getOrNull()
                if (pendingCapture != null) {
                    Log.i(TAG, "Captura bancaria exitosa: $${pendingCapture.amount} en ${pendingCapture.merchantOrCounterparty} (ID: ${pendingCapture.id})")
                    // 6. Desplegar notificación interactiva Heads-Up en pantalla con botones [Confirmar] y [Descartar]
                    AutoCaptureNotificationHelper.showCaptureNotification(applicationContext, pendingCapture)
                } else {
                    Log.d(TAG, "Notificación de ${bank.displayName} no contiene monto monetario válido: '$effectiveText'")
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error procesando notificación bancaria con app cerrada: ${e.message}", e)
            }
        }
    }

    private fun hasFinancialMovementKeywords(text: String): Boolean {
        val lower = text.lowercase()
        val hasAction = lower.contains("compra") ||
            lower.contains("pago") ||
            lower.contains("transferencia") ||
            lower.contains("enviaste") ||
            lower.contains("recibiste") ||
            lower.contains("consignacion") ||
            lower.contains("consignación") ||
            lower.contains("retiro") ||
            lower.contains("debito") ||
            lower.contains("débito") ||
            lower.contains("credito") ||
            lower.contains("crédito") ||
            lower.contains("transfiya") ||
            lower.contains("bre-b") ||
            lower.contains("pse")
        val hasAmount = lower.contains("$") || lower.contains("cop") || lower.contains("por valor") || lower.contains("por $")
        return hasAction && hasAmount
    }

    private fun isAppForeground(): Boolean {
        return try {
            val appProcessInfo = android.app.ActivityManager.RunningAppProcessInfo()
            android.app.ActivityManager.getMyMemoryState(appProcessInfo)
            appProcessInfo.importance == android.app.ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
        } catch (e: Exception) {
            false
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "BankNotificationListener"

        /**
         * Fuerza el re-enlace del servicio con el NotificationManager de Android.
         * Esencial para evitar que sistemas agresivos de ahorro de batería o cierre de tareas
         * dejen el servicio inactivo cuando el usuario cierra KashApp.
         */
        fun ensureServiceRebind(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                try {
                    val componentName = ComponentName(context, BankNotificationListenerService::class.java)
                    requestRebind(componentName)
                    Log.i(TAG, "requestRebind invocado exitosamente para mantener activo el servicio en segundo plano")
                } catch (e: Throwable) {
                    Log.w(TAG, "requestRebind aviso: ${e.message}")
                }
            }
            try {
                val componentName = ComponentName(context, BankNotificationListenerService::class.java)
                val pm = context.packageManager
                pm.setComponentEnabledSetting(
                    componentName,
                    PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                    PackageManager.DONT_KILL_APP
                )
            } catch (e: Throwable) {
                Log.w(TAG, "setComponentEnabledSetting aviso: ${e.message}")
            }
        }
    }
}
