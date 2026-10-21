package com.example.feature.capturaautomatica.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface AutoCaptureUiEvent {
    data class ShowMessage(val message: String) : AutoCaptureUiEvent
    data class ShowError(val error: String) : AutoCaptureUiEvent
}

class AutoCaptureViewModel(
    private val repository: AutoCaptureRepository,
    private val userId: String
) : ViewModel() {

    val pendingCaptures: StateFlow<List<PendingAutoCaptureEntity>> =
        repository.observePendingCaptures(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = emptyList()
            )

    val pendingCount: StateFlow<Int> =
        repository.observePendingCount(userId)
            .stateIn(
                scope = viewModelScope,
                started = SharingStarted.WhileSubscribed(5000),
                initialValue = 0
            )

    private val _isProcessing = MutableStateFlow(false)
    val isProcessing: StateFlow<Boolean> = _isProcessing.asStateFlow()

    private val _events = MutableSharedFlow<AutoCaptureUiEvent>()
    val events: SharedFlow<AutoCaptureUiEvent> = _events.asSharedFlow()

    fun confirmCapture(
        captureId: String,
        customAccountId: String? = null,
        customAccountName: String? = null,
        customCategoryId: String? = null,
        customCategoryName: String? = null,
        customTags: List<String> = emptyList()
    ) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.confirmCapture(
                captureId = captureId,
                userId = userId,
                customAccountId = customAccountId,
                customAccountName = customAccountName,
                customCategoryId = customCategoryId,
                customCategoryName = customCategoryName,
                customTags = customTags
            )
            _isProcessing.value = false
            if (result.isSuccess) {
                _events.emit(AutoCaptureUiEvent.ShowMessage("¡Movimiento registrado y confirmado exitosamente!"))
            } else {
                _events.emit(AutoCaptureUiEvent.ShowError(result.exceptionOrNull()?.message ?: "Error al confirmar"))
            }
        }
    }

    fun updateDraftSelection(
        captureId: String,
        accountId: String?,
        accountName: String?,
        categoryId: String?,
        categoryName: String?
    ) {
        viewModelScope.launch {
            repository.updateDraftSelection(
                captureId = captureId,
                userId = userId,
                accountId = accountId,
                accountName = accountName,
                categoryId = categoryId,
                categoryName = categoryName
            )
        }
    }

    fun dismissCapture(captureId: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.dismissCapture(captureId, userId)
            _isProcessing.value = false
            if (result.isSuccess) {
                _events.emit(AutoCaptureUiEvent.ShowMessage("Notificación descartada"))
            } else {
                _events.emit(AutoCaptureUiEvent.ShowError(result.exceptionOrNull()?.message ?: "Error al descartar"))
            }
        }
    }

    fun simulateCapture(bankName: String) {
        viewModelScope.launch {
            _isProcessing.value = true
            val result = repository.simulateTestCapture(userId, bankName)
            _isProcessing.value = false
            if (result.isSuccess) {
                _events.emit(AutoCaptureUiEvent.ShowMessage("Simulación de $bankName creada con éxito"))
            } else {
                _events.emit(AutoCaptureUiEvent.ShowError(result.exceptionOrNull()?.message ?: "Error en simulación"))
            }
        }
    }

    class Factory(
        private val repository: AutoCaptureRepository,
        private val userId: String
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return AutoCaptureViewModel(repository, userId) as T
        }
    }
}
