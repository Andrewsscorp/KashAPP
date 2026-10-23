package com.example.feature.notificaciones.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.example.KashAppApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class NotificationActionBroadcastReceiver : BroadcastReceiver() {

    companion object {
        const val ACTION_MARK_READ = "com.example.kashapp.ACTION_NOTIFICATION_MARK_READ"
        const val ACTION_SNOOZE_2H = "com.example.kashapp.ACTION_NOTIFICATION_SNOOZE_2H"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val notificationId = intent.getStringExtra("extra_notification_id") ?: return
        val targetUserId = intent.getStringExtra("extra_user_id")?.takeIf { it.isNotBlank() } ?: return
        val appContainer = (context.applicationContext as? KashAppApplication)?.appContainer ?: return
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                // Verificar que la acción pertenezca al usuario con sesión activa
                val activeSessionUserId = runCatching {
                    appContainer.securityDatabase.userSessionDao().getActiveSession()?.userId
                }.getOrNull()
                if (!activeSessionUserId.isNullOrBlank() && activeSessionUserId != targetUserId) {
                    return@launch
                }

                val dao = appContainer.securityDatabase.notificationDao()
                when (intent.action) {
                    ACTION_MARK_READ -> {
                        dao.markAsRead(notificationId = notificationId, userId = targetUserId)
                    }
                    ACTION_SNOOZE_2H -> {
                        val twoHoursLater = System.currentTimeMillis() + (2 * 60 * 60 * 1000L)
                        dao.snoozeNotification(
                            notificationId = notificationId,
                            userId = targetUserId,
                            snoozeUntilEpochMs = twoHoursLater
                        )
                    }
                }
                val notificationKey = "$targetUserId:$notificationId".hashCode()
                NotificationManagerCompat.from(context).cancel(notificationKey)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
