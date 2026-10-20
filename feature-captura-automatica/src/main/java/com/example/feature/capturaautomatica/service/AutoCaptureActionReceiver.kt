package com.example.feature.capturaautomatica.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.feature.capturaautomatica.di.AutoCaptureEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * BroadcastReceiver for handling 1-tap notification quick actions:
 * - ACTION_CONFIRM: Commits movement to Room database with atomic consistency.
 * - ACTION_DISMISS: Ignores/dismisses the captured draft and cancels system alert.
 */
class AutoCaptureActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent == null) return

        val action = intent.action ?: return
        val captureId = intent.getStringExtra(AutoCaptureNotificationHelper.EXTRA_CAPTURE_ID) ?: return
        val userId = intent.getStringExtra(AutoCaptureNotificationHelper.EXTRA_USER_ID) ?: return
        val notificationId = intent.getIntExtra(AutoCaptureNotificationHelper.EXTRA_NOTIFICATION_ID, -1)

        // Cancelar inmediatamente la notificación de la barra de estado
        if (notificationId != -1) {
            AutoCaptureNotificationHelper.cancelNotification(context, notificationId)
        }

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val repository = AutoCaptureEntryPoint.getRepository(context)
                if (repository == null) {
                    Log.e(TAG, "No se pudo obtener AutoCaptureRepository en AutoCaptureActionReceiver")
                    return@launch
                }

                when (action) {
                    AutoCaptureNotificationHelper.ACTION_CONFIRM_CAPTURE -> {
                        val result = repository.confirmCapture(captureId, userId)
                        if (result.isSuccess) {
                            Log.i(TAG, "Captura $captureId confirmada con éxito (Tx: ${result.getOrNull()})")
                        } else {
                            Log.e(TAG, "Error confirmando captura $captureId: ${result.exceptionOrNull()?.message}")
                        }
                    }
                    AutoCaptureNotificationHelper.ACTION_DISMISS_CAPTURE -> {
                        val result = repository.dismissCapture(captureId, userId)
                        if (result.isSuccess) {
                            Log.i(TAG, "Captura $captureId descartada")
                        } else {
                            Log.e(TAG, "Error descartando captura $captureId: ${result.exceptionOrNull()?.message}")
                        }
                    }
                }
            } catch (e: Throwable) {
                Log.e(TAG, "Excepción en AutoCaptureActionReceiver: ${e.message}", e)
            } finally {
                pendingResult.finish()
            }
        }
    }

    companion object {
        private const val TAG = "AutoCaptureActionReceiver"
    }
}
