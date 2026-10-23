package com.example.feature.notificaciones.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.NotificationEntity
import com.example.feature.notificaciones.data.NotificationRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

@OptIn(ExperimentalCoroutinesApi::class)
class NotificationCenterViewModel(
    private val repository: NotificationRepositoryImpl,
    initialUserId: String
) : ViewModel() {

    private val activeUserIdFlow = MutableStateFlow(initialUserId.trim())

    private val _statusMessage = MutableStateFlow<String?>(null)
    val statusMessage: StateFlow<String?> = _statusMessage.asStateFlow()

    val notifications: StateFlow<List<NotificationEntity>> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(emptyList())
            else repository.observeActiveNotifications(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    val unreadCount: StateFlow<Int> = activeUserIdFlow
        .flatMapLatest { uid ->
            if (uid.isBlank()) flowOf(0)
            else repository.observeUnreadCount(uid)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        val uid = activeUserIdFlow.value
        if (uid.isNotBlank()) {
            repository.initializeCloudSyncAndProactiveScan(uid)
        }
    }

    fun setActiveUserId(newUserId: String) {
        val clean = newUserId.trim()
        if (activeUserIdFlow.value != clean) {
            activeUserIdFlow.value = clean
            _statusMessage.value = null
            if (clean.isNotBlank()) {
                repository.initializeCloudSyncAndProactiveScan(clean)
            } else {
                repository.onUserLogout()
            }
        }
    }

    fun runProactiveScan() {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            val inserted = repository.runProactiveEvaluation(uid)
            _statusMessage.value = if (inserted > 0) {
                "Escaneo completado: $inserted nueva(s) alerta(s) detectada(s)."
            } else {
                "Escaneo al día: todos los módulos están sincronizados."
            }
        }
    }

    fun markAsRead(notificationId: String) {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.markAsRead(notificationId, uid)
        }
    }

    fun markAllAsRead() {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.markAllAsRead(uid)
            _statusMessage.value = "Todas las notificaciones fueron marcadas como leídas."
        }
    }

    fun snoozeTwoHours(notificationId: String) {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.snoozeHours(notificationId, uid, hours = 2)
            _statusMessage.value = "Notificación pospuesta por 2 horas."
        }
    }

    fun archive(notificationId: String) {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            repository.archive(notificationId, uid)
        }
    }

    fun sendFirebasePush(
        title: String,
        body: String,
        category: String,
        severity: String,
        route: String,
        isBroadcast: Boolean
    ) {
        val uid = activeUserIdFlow.value
        if (uid.isBlank()) return
        viewModelScope.launch {
            val res = repository.sendFirebaseCloudNotification(
                userId = uid,
                title = title,
                body = body,
                category = category,
                severity = severity,
                deepLinkRoute = route,
                actionLabel = "Ver ahora",
                isGlobalBroadcast = isBroadcast,
                targetJurisdiction = "ALL"
            )
            _statusMessage.value = if (res.isSuccess) {
                "Push enviado y sincronizado con Firebase Cloud (${res.getOrNull()})."
            } else {
                "Alerta disparada localmente (modo offline / sin conexión a Firestore)."
            }
        }
    }

    class Factory(
        private val repository: NotificationRepositoryImpl,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return NotificationCenterViewModel(repository, userId) as T
        }
    }
}
