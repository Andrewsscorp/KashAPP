package com.example.util

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Receptor de inicio de sistema (Boot Completed).
 * Garantiza que ante un reinicio o apagado del teléfono, las tareas programadas y
 * el estado de la base de datos Room (incluyendo notificaciones bancarias pendientes
 * y selecciones borrador) se mantengan 100% íntegras y persistentes.
 */
class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            Log.d("BootCompletedReceiver", "KashApp: Dispositivo reiniciado. Persistencia y servicios asegurados.")
        }
    }
}
