package com.example.feature.reconciliation.presentation

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountReconciliationLogEntity
import com.example.feature.reconciliation.model.ReconciliationDiscrepancy

/**
 * State container for the Reconciliation Wizard and History screens.
 */
data class ReconciliationUiState(
    val selectedAccount: AccountEntity? = null,
    val realBalanceInput: String = "",
    val reconciliationDate: String = "",
    val notesInput: String = "",
    val calculatedDiscrepancy: ReconciliationDiscrepancy? = null,
    val isExecuting: Boolean = false,
    val executionSuccessMessage: String? = null,
    val errorMessage: String? = null,
    val historyLogs: List<AccountReconciliationLogEntity> = emptyList(),
    val isLoadingHistory: Boolean = false
)
