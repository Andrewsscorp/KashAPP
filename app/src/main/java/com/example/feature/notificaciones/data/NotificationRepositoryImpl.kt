package com.example.feature.notificaciones.data

import android.content.Context
import com.example.data.local.dao.AutoCaptureDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.NotificationDao
import com.example.data.local.entity.NotificationEntity
import com.example.feature.impuestosdian.data.repository.DianTaxRepositoryImpl
import com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher
import com.example.feature.notificaciones.domain.engine.ProactiveFinancialNotificationEngine
import com.example.feature.notificaciones.domain.engine.QuietHoursPolicy
import com.example.feature.notificaciones.service.FcmTopicSubscriptionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.util.UUID

class NotificationRepositoryImpl(
    private val context: Context,
    private val notificationDao: NotificationDao,
    private val financialDao: FinancialDao,
    private val autoCaptureDao: AutoCaptureDao,
    private val dianTaxRepository: DianTaxRepositoryImpl,
    private val proactiveEngine: ProactiveFinancialNotificationEngine = ProactiveFinancialNotificationEngine()
) {
    private val repoScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val systemDispatcher = RichSystemNotificationDispatcher(context)
    private val firestoreBridge = FirestoreNotificationBridge(
        context = context,
        notificationDao = notificationDao,
        externalScope = repoScope
    )

    fun observeActiveNotifications(userId: String): Flow<List<NotificationEntity>> {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return flowOf(emptyList())
        return notificationDao.observeActiveNotifications(safeUser)
    }

    fun observeUnreadCount(userId: String): Flow<Int> {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return flowOf(0)
        return notificationDao.observeUnreadCount(safeUser)
    }

    fun initializeCloudSyncAndProactiveScan(userId: String) {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) {
            onUserLogout()
            return
        }
        repoScope.launch {
            val taxProfile = dianTaxRepository.observeTaxConfiguration(safeUser).value
            FcmTopicSubscriptionManager.syncTopicsForJurisdiction(taxProfile.jurisdiction)
            firestoreBridge.startRealtimeListening(safeUser, taxProfile.jurisdiction.code)
            runProactiveEvaluation(safeUser)
        }
    }

    suspend fun runProactiveEvaluation(
        userId: String,
        quietHoursPolicy: QuietHoursPolicy = QuietHoursPolicy()
    ): Int {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return 0

        val taxReport = runCatching { dianTaxRepository.observeTaxRadarReport(safeUser).first() }.getOrNull()
        val accounts = runCatching { financialDao.getAccounts(safeUser) }.getOrDefault(emptyList())
        val creditCards = runCatching { financialDao.getAccountCreditCardsByUser(safeUser) }.getOrElse {
            accounts
                .filter { it.type.equals("CREDIT_CARD", ignoreCase = true) }
                .mapNotNull { acc -> runCatching { financialDao.getAccountCreditCard(acc.id) }.getOrNull() }
        }
        val scheduled = runCatching { financialDao.getScheduledPaymentsFlow(safeUser).first() }.getOrDefault(emptyList())
        val pendingCaptures = runCatching { autoCaptureDao.observePendingCaptures(safeUser).first() }.getOrDefault(emptyList())

        val candidates = proactiveEngine.evaluateAllModules(
            userId = safeUser,
            taxReport = taxReport,
            accounts = accounts,
            creditCards = creditCards,
            scheduledPayments = scheduled,
            pendingAutoCaptures = pendingCaptures
        )

        var newlyInserted = 0
        val nowTime = LocalTime.now()
        candidates.forEach { entity ->
            if (entity.userId == safeUser) {
                val rowId = notificationDao.insertNotificationIgnoreDuplicate(entity)
                if (rowId != -1L) {
                    newlyInserted++
                    if (!quietHoursPolicy.shouldSuppressSystemPush(nowTime, entity.severity)) {
                        systemDispatcher.dispatchSystemNotification(entity)
                    }
                }
            }
        }
        return newlyInserted
    }

    suspend fun sendFirebaseCloudNotification(
        userId: String,
        title: String,
        body: String,
        category: String,
        severity: String,
        deepLinkRoute: String,
        actionLabel: String?,
        isGlobalBroadcast: Boolean,
        targetJurisdiction: String
    ): Result<String> {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) {
            return Result.failure(IllegalStateException("No hay sesión de usuario activa para enviar notificaciones."))
        }
        val uniqueSuffix = UUID.randomUUID().toString()
        val localFallback = NotificationEntity(
            id = "${safeUser}_notif_studio_$uniqueSuffix",
            userId = safeUser,
            dedupKey = "${safeUser}_STUDIO_$uniqueSuffix",
            title = title,
            body = body,
            category = category,
            severity = severity,
            sourceOrigin = if (isGlobalBroadcast) "FIRESTORE_BROADCAST" else "FCM_PUSH",
            deepLinkRoute = deepLinkRoute,
            actionLabel = actionLabel ?: "Abrir módulo",
            isRead = false,
            syncedToFirestore = true
        )
        notificationDao.insertNotificationIgnoreDuplicate(localFallback)
        systemDispatcher.dispatchSystemNotification(localFallback)

        return firestoreBridge.publishCloudNotification(
            targetUserId = safeUser,
            title = title,
            body = body,
            category = category,
            severity = severity,
            deepLinkRoute = deepLinkRoute,
            actionLabel = actionLabel,
            isGlobalBroadcast = isGlobalBroadcast,
            targetJurisdiction = targetJurisdiction
        )
    }

    suspend fun markAsRead(notificationId: String, userId: String) {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return
        notificationDao.markAsRead(notificationId, safeUser)
    }

    suspend fun markAllAsRead(userId: String) {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return
        notificationDao.markAllAsRead(safeUser)
    }

    suspend fun archive(notificationId: String, userId: String) {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return
        notificationDao.archiveNotification(notificationId, safeUser)
    }

    suspend fun snoozeHours(notificationId: String, userId: String, hours: Int = 2) {
        val safeUser = userId.trim()
        if (safeUser.isBlank()) return
        val until = System.currentTimeMillis() + (hours * 3_600_000L)
        notificationDao.snoozeNotification(notificationId, safeUser, until)
    }

    /**
     * Limpieza inmediata al cerrar sesión o cambiar de usuario:
     * detiene listeners en tiempo real de Firestore, desuscribe tópicos FCM y
     * elimina cualquier notificación visible en la barra de estado del sistema Android.
     */
    fun onUserLogout() {
        firestoreBridge.stopListening()
        FcmTopicSubscriptionManager.unsubscribeAllOnLogout()
        systemDispatcher.cancelAllSystemNotifications()
    }
}
