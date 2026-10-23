package com.example.feature.notificaciones.service

import com.example.KashAppApplication
import com.example.data.local.entity.NotificationEntity
import com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.util.UUID

class KashFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        val appContainer = (applicationContext as? KashAppApplication)?.appContainer

        serviceScope.launch {
            val activeSessionUserId = runCatching {
                appContainer?.securityDatabase?.userSessionDao()?.getActiveSession()?.userId
            }.getOrNull()
            val firebaseUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val resolvedUserId = activeSessionUserId?.takeIf { it.isNotBlank() } ?: firebaseUid

            getSharedPreferences("kash_fcm_prefs", MODE_PRIVATE)
                .edit()
                .putString("fcm_token", token)
                .putString("fcm_bound_user_id", resolvedUserId ?: "")
                .apply()

            if (!resolvedUserId.isNullOrBlank() && resolvedUserId != "user_default" && resolvedUserId != "anonymous") {
                runCatching {
                    FirebaseFirestore.getInstance()
                        .collection("users")
                        .document(resolvedUserId)
                        .collection("fcm_tokens")
                        .document(token)
                        .set(
                            mapOf(
                                "token" to token,
                                "userId" to resolvedUserId,
                                "platform" to "ANDROID",
                                "updatedAt" to FieldValue.serverTimestamp()
                            )
                        )
                }
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        val appContainer = (applicationContext as? KashAppApplication)?.appContainer ?: return

        serviceScope.launch {
            // Aislamiento estricto multi-usuario: resolver únicamente la sesión autenticada activa en este dispositivo
            val activeSessionUserId = runCatching {
                appContainer.securityDatabase.userSessionDao().getActiveSession()?.userId
            }.getOrNull()
            val firebaseUid = runCatching { FirebaseAuth.getInstance().currentUser?.uid }.getOrNull()
            val activeUserId = activeSessionUserId?.takeIf { it.isNotBlank() }
                ?: firebaseUid?.takeIf { it.isNotBlank() }
                ?: return@launch // Si no hay sesión activa autenticada, descartar para no contaminar sesiones

            val data = remoteMessage.data
            val targetUserId = data["targetUserId"] ?: data["userId"]

            // Si el push viene dirigido a un usuario específico distinto al usuario activo actual, se bloquea estrictamente
            if (!targetUserId.isNullOrBlank() && targetUserId != "ALL" && targetUserId != activeUserId) {
                return@launch
            }

            val title = data["title"] ?: remoteMessage.notification?.title ?: "Aviso Financiero KashApp"
            val body = data["body"] ?: remoteMessage.notification?.body ?: "Tienes una nueva actualización en tu radar financiero."
            val category = data["category"] ?: "FIREBASE_CLOUD"
            val severity = data["severity"] ?: "INFO"
            val deepLinkRoute = data["deepLinkRoute"] ?: "DASHBOARD"
            val actionLabel = data["actionLabel"] ?: "Ver en KashApp"
            val payloadJson = data["actionPayloadJson"]
            val rawDedupKey = data["dedupKey"] ?: remoteMessage.messageId ?: "FCM_${UUID.randomUUID()}"
            // Clave de deduplicación prefijada con el userId para evitar colisiones entre usuarios distintos
            val scopedDedupKey = "${activeUserId}_$rawDedupKey"

            val entity = NotificationEntity(
                id = "notif_fcm_${activeUserId}_${UUID.randomUUID()}",
                userId = activeUserId,
                dedupKey = scopedDedupKey,
                title = title,
                body = body,
                category = category,
                severity = severity,
                sourceOrigin = "FCM_PUSH",
                deepLinkRoute = deepLinkRoute,
                actionLabel = actionLabel,
                actionPayloadJson = payloadJson,
                isRead = false,
                syncedToFirestore = true
            )

            val rowId = appContainer.securityDatabase.notificationDao().insertNotificationIgnoreDuplicate(entity)
            if (rowId != -1L) {
                RichSystemNotificationDispatcher(applicationContext).dispatchSystemNotification(entity)
            }
        }
    }
}
