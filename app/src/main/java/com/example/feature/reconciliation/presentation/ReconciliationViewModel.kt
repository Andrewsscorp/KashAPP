package com.example.feature.reconciliation.presentation

import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.AccountEntity
import com.example.feature.reconciliation.domain.CalculateDiscrepancyUseCase
import com.example.feature.reconciliation.domain.ExecuteReconciliationUseCase
import com.example.feature.reconciliation.domain.GetReconciliationHistoryUseCase
import com.example.feature.reconciliation.model.ReconciliationParams
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReconciliationViewModel(
    private val calculateDiscrepancyUseCase: CalculateDiscrepancyUseCase,
    private val executeReconciliationUseCase: ExecuteReconciliationUseCase,
    private val getReconciliationHistoryUseCase: GetReconciliationHistoryUseCase,
    private val userId: String,
    private val userName: String
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        ReconciliationUiState(
            reconciliationDate = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
        )
    )
    val uiState: StateFlow<ReconciliationUiState> = _uiState.asStateFlow()

    fun selectAccount(account: AccountEntity) {
        _uiState.update { current ->
            val prevBalance = account.balance
            val real = current.realBalanceInput.toDoubleOrNull() ?: prevBalance
            val disc = calculateDiscrepancyUseCase(prevBalance, real)
            current.copy(
                selectedAccount = account,
                realBalanceInput = current.realBalanceInput.ifBlank { prevBalance.toString() },
                calculatedDiscrepancy = disc,
                errorMessage = null,
                executionSuccessMessage = null
            )
        }
        loadHistoryForAccount(account.id)
    }

    fun onRealBalanceChanged(rawInput: String) {
        val sanitized = rawInput.filter { it.isDigit() || it == '.' || it == '-' }
        val prevBalance = _uiState.value.selectedAccount?.balance ?: 0.0
        val realValue = sanitized.toDoubleOrNull() ?: prevBalance
        val disc = calculateDiscrepancyUseCase(prevBalance, realValue)

        _uiState.update {
            it.copy(
                realBalanceInput = sanitized,
                calculatedDiscrepancy = disc,
                errorMessage = null
            )
        }
    }

    fun onDateChanged(date: String) {
        _uiState.update { it.copy(reconciliationDate = date) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notesInput = notes) }
    }

    fun loadHistoryForAccount(accountId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            getReconciliationHistoryUseCase.observeHistory(userId, accountId).collect { logs ->
                _uiState.update { it.copy(historyLogs = logs, isLoadingHistory = false) }
            }
        }
    }

    fun submitReconciliation(onSuccess: () -> Unit) {
        val account = _uiState.value.selectedAccount ?: run {
            _uiState.update { it.copy(errorMessage = "Selecciona una cuenta válida para conciliar.") }
            return
        }

        val realBalance = _uiState.value.realBalanceInput.toDoubleOrNull() ?: run {
            _uiState.update { it.copy(errorMessage = "Ingresa un saldo real numérico válido.") }
            return
        }

        val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim()

        viewModelScope.launch {
            _uiState.update { it.copy(isExecuting = true, errorMessage = null) }

            val params = ReconciliationParams(
                userId = userId,
                accountId = account.id,
                accountName = account.name,
                previousBalance = account.balance,
                reconciledRealBalance = realBalance,
                reconciliationDate = _uiState.value.reconciliationDate.ifBlank {
                    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date())
                },
                notes = _uiState.value.notesInput.takeIf { it.isNotBlank() },
                reconciledByUserId = userId,
                reconciledByUserName = userName,
                deviceModel = deviceModel
            )

            val result = executeReconciliationUseCase(params)

            if (result.success) {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        executionSuccessMessage = "Cuenta conciliada exitosamente (${result.status.label})."
                    )
                }
                onSuccess()
            } else {
                _uiState.update {
                    it.copy(
                        isExecuting = false,
                        errorMessage = result.errorMessage ?: "Error desconocido al procesar la conciliación."
                    )
                }
            }
        }
    }

    fun clearFeedback() {
        _uiState.update { it.copy(errorMessage = null, executionSuccessMessage = null) }
    }
}
