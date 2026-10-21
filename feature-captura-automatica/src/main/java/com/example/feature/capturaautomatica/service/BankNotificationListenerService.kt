package com.example.feature.capturaautomatica.service

import android.app.Notification
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
 * 100% on-device notification listener service for Colombian financial entities.
 * Strictly filters out OTP/2FA codes and respects multi-tenant active sessions.
 */
class BankNotificationListenerService : NotificationListenerService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null || sbn.notification == null) return

        val packageName = sbn.packageName ?: return

        val extras = sbn.notification.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()
        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()
        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()
        val subText = extras.getCharSequence(Notification.EXTRA_SUB_TEXT)?.toString()

        val fullTextForAnalysis = listOfNotNull(title, text, bigText, subText).joinToString(" ")

        // 1. Filtrado instantáneo por paquete bancario soportado o SMS transaccional
        val bank = SupportedColombianBank.findByPackage(packageName, title, fullTextForAnalysis)
            ?: if (packageName.contains("bank", ignoreCase = true) ||
                packageName.contains("banco", ignoreCase = true) ||
                packageName.contains("wallet", ignoreCase = true) ||
                packageName.contains("finance", ignoreCase = true)
            ) {
                SupportedColombianBank.ENTIDAD_FINANCIERA
            } else {
                return
            }

        // 2. Filtro estricto Anti-OTP y Privacidad (Ley 1581)
        if (OtpSafetyFilter.isSensitiveAuthMessage(fullTextForAnalysis)) {
            Log.i(TAG, "Notificación descartada por filtro de seguridad Anti-OTP/2FA de ${bank.displayName}")
            return
        }

        val effectiveText = if (!bigText.isNullOrBlank()) bigText else text
        val postTime = sbn.postTime

        serviceScope.launch {
            try {
                // 3. Verificación de sesión de usuario activa (Aislamiento Multi-inquilino)
                val activeUserId = AutoCaptureEntryPoint.getActiveUserId(applicationContext)
                if (activeUserId.isNullOrBlank()) {
                    Log.w(TAG, "Notificación bancaria recibida sin sesión de usuario activa en KashApp. Omitiendo.")
                    return@launch
                }

                val repository = AutoCaptureEntryPoint.getRepository(applicationContext)
                if (repository == null) {
                    Log.w(TAG, "AutoCaptureRepository no disponible en AutoCaptureEntryPoint.")
                    return@launch
                }

                // 4. Procesamiento y registro en Room v38
                val result = repository.processNotification(
                    packageName = packageName,
                    title = title,
                    text = effectiveText,
                    postTime = postTime,
                    userId = activeUserId
                )

                val pendingCapture = result.getOrNull()
                if (pendingCapture != null) {
                    Log.i(TAG, "Captura bancaria exitosa: $${pendingCapture.amount} en ${pendingCapture.merchantOrCounterparty}")
                    // 5. Desplegar notificación interactiva al usuario con acciones rápidas de 1 toque
                    AutoCaptureNotificationHelper.showCaptureNotification(applicationContext, pendingCapture)
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Error procesando notificación bancaria: ${e.message}", e)
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        private const val TAG = "BankNotificationListener"
    }
}
