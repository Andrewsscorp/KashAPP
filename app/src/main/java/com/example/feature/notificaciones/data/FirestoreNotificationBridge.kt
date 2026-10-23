package com.example.feature.notificaciones.data

import android.content.Context
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.NotificationEntity
import com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher
import com.google.firebase.firestore.DocumentChange
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.Query
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import java.util.UUID

class FirestoreNotificationBridge(
    private val context: Context,
    private val notificationDao: NotificationDao,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance(),
    private val externalScope: CoroutineScope
) {
    @Volatile
    private var activeListeningUserId: String? = null
    private var userListener: ListenerRegistration? = null
    private var broadcastListener: ListenerRegistration? = null
    private val systemDispatcher = RichSystemNotificationDispatcher(context)

    fun startRealtimeListening(userId: String, activeJurisdictionCode: String) {
        val cleanUser = userId.trim()
        // Aislamiento multi-usuario: no abrir listeners de nube para usuarios vacíos o genéricos no autenticados
        if (cleanUser.isBlank() || cleanUser == "anonymous") {
            stopListening()
            return
        }

        // Si ya había un listener de otro usuario previo, cerrarlo inmediatamente antes de enlazar el nuevo
        stopListening()
        activeListeningUserId = cleanUser

        // 1. Listener en tiempo real para notificaciones personales exclusivas de este userId
        userListener = firestore.collection("users")
            .document(cleanUser)
            .collection("notifications")
            .orderBy("createdAtEpochMs", Query.Direction.DESCENDING)
            .limit(40)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                // Verificar que el usuario activo no haya cambiado ni cerrado sesión mientras llegaba el callback
                if (activeListeningUserId != cleanUser) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        val docTargetUser = doc.getString("targetUserId") ?: doc.getString("userId") ?: cleanUser
                        if (docTargetUser != cleanUser) continue

                        val rawDedup = doc.getString("dedupKey") ?: "FS_USER_${doc.id}"
                        val entity = NotificationEntity(
                            id = "${cleanUser}_${doc.id}",
                            userId = cleanUser,
                            dedupKey = "${cleanUser}_$rawDedup",
                            title = doc.getString("title") ?: "Notificación KashApp",
                            body = doc.getString("body") ?: "",
                            category = doc.getString("category") ?: "FIREBASE_CLOUD",
                            severity = doc.getString("severity") ?: "INFO",
                            sourceOrigin = doc.getString("sourceOrigin") ?: "FIRESTORE_TRIGGER",
                            deepLinkRoute = doc.getString("deepLinkRoute") ?: "DASHBOARD",
                            actionLabel = doc.getString("actionLabel"),
                            actionPayloadJson = doc.getString("actionPayloadJson"),
                            isRead = doc.getBoolean("isRead") ?: false,
                            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
                            syncedToFirestore = true
                        )
                        externalScope.launch(Dispatchers.IO) {
                            if (activeListeningUserId != cleanUser) return@launch
                            val inserted = notificationDao.insertNotificationIgnoreDuplicate(entity)
                            if (inserted != -1L && !entity.isRead) {
                                systemDispatcher.dispatchSystemNotification(entity)
                            }
                        }
                    }
                }
            }

        // 2. Listener para campañas globales o por país desde la colección "broadcast_notifications"
        broadcastListener = firestore.collection("broadcast_notifications")
            .whereIn("targetJurisdiction", listOf("ALL", activeJurisdictionCode))
            .limit(20)
            .addSnapshotListener { snapshot, error ->
                if (error != null || snapshot == null) return@addSnapshotListener
                if (activeListeningUserId != cleanUser) return@addSnapshotListener

                for (change in snapshot.documentChanges) {
                    if (change.type == DocumentChange.Type.ADDED) {
                        val doc = change.document
                        // Si el broadcast fue restringido a un usuario específico distinto, ignorarlo
                        val restrictedUser = doc.getString("targetUserId")
                        if (!restrictedUser.isNullOrBlank() && restrictedUser != "ALL" && restrictedUser != cleanUser) {
                            continue
                        }

                        val entity = NotificationEntity(
                            id = "bcast_${doc.id}_$cleanUser",
                            userId = cleanUser,
                            dedupKey = "${cleanUser}_BCAST_${doc.id}",
                            title = doc.getString("title") ?: "Anuncio Oficial KashApp",
                            body = doc.getString("body") ?: "",
                            category = doc.getString("category") ?: "FIREBASE_CLOUD",
                            severity = doc.getString("severity") ?: "INFO",
                            sourceOrigin = "FIRESTORE_BROADCAST",
                            deepLinkRoute = doc.getString("deepLinkRoute") ?: "DASHBOARD",
                            actionLabel = doc.getString("actionLabel") ?: "Abrir",
                            createdAtEpochMs = doc.getLong("createdAtEpochMs") ?: System.currentTimeMillis(),
                            syncedToFirestore = true
                        )
                        externalScope.launch(Dispatchers.IO) {
                            if (activeListeningUserId != cleanUser) return@launch
                            val inserted = notificationDao.insertNotificationIgnoreDuplicate(entity)
                            if (inserted != -1L) {
                                systemDispatcher.dispatchSystemNotification(entity)
                            }
                        }
                    }
                }
            }
    }

    /**
     * Publica una notificación en Firestore garantizando que las notificaciones personales queden
     * estrictamente aisladas bajo `users/{targetUserId}/notifications`.
     */
    suspend fun publishCloudNotification(
        targetUserId: String,
        title: String,
        body: String,
        category: String,
        severity: String,
        deepLinkRoute: String,
        actionLabel: String?,
        isGlobalBroadcast: Boolean = false,
        targetJurisdiction: String = "ALL"
    ): Result<String> = runCatching {
        val safeTargetUser = targetUserId.trim().ifBlank { "user_default" }
        val docId = "cloud_${UUID.randomUUID()}"
        val payload = mapOf(
            "id" to docId,
            "userId" to safeTargetUser,
            "targetUserId" to if (isGlobalBroadcast) "ALL" else safeTargetUser,
            "dedupKey" to "CLOUD_$docId",
            "title" to title,
            "body" to body,
            "category" to category,
            "severity" to severity,
            "sourceOrigin" to if (isGlobalBroadcast) "FIRESTORE_BROADCAST" else "FIRESTORE_TRIGGER",
            "deepLinkRoute" to deepLinkRoute,
            "actionLabel" to (actionLabel ?: "Ver detalle"),
            "targetJurisdiction" to targetJurisdiction,
            "isRead" to false,
            "createdAtEpochMs" to System.currentTimeMillis()
        )

        if (isGlobalBroadcast) {
            firestore.collection("broadcast_notifications").document(docId).set(payload).await()
        } else {
            firestore.collection("users")
                .document(safeTargetUser)
                .collection("notifications")
                .document(docId)
                .set(payload)
                .await()
        }
        docId
    }

    fun stopListening() {
        activeListeningUserId = null
        userListener?.remove()
        broadcastListener?.remove()
        userListener = null
        broadcastListener = null
    }
}
