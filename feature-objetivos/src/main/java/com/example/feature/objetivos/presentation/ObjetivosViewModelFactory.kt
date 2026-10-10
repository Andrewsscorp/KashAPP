package com.example.feature.objetivos.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.data.local.dao.FinancialDao
import com.example.feature.objetivos.sync.DataSyncManager

class ObjetivosViewModelFactory(
    private val financialDao: FinancialDao,
    private val userId: String = "user_default",
    private val dataSyncManager: DataSyncManager? = null
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ObjetivosViewModel::class.java)) {
            val syncManager = dataSyncManager ?: DataSyncManager(financialDao, userId)
            return ObjetivosViewModel(
                financialDao = financialDao,
                userId = userId,
                dataSyncManager = syncManager
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
