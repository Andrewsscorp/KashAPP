package com.example.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.domain.repository.ExchangeRateRepository
import com.example.domain.repository.FinancialRepository
import com.example.features.settings.domain.repository.SettingsRepository

/**
 * ViewModelProvider.Factory for creating DashboardViewModel with real dependency injection.
 */
class DashboardViewModelFactory(
    private val financialRepository: FinancialRepository? = null,
    private val exchangeRateRepository: ExchangeRateRepository? = null,
    private val settingsRepository: SettingsRepository? = null,
    private val fixedAssetRepository: com.example.domain.repository.FixedAssetRepository? = null,
    private val userSessionDao: com.example.data.local.dao.UserSessionDao? = null,
    private val userId: String = "user_default",
    private val initialUserName: String = "Andrés Felipe",
    private val initialUserEmail: String = "cdosuarez@gmail.com"
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(
                financialRepository = financialRepository,
                exchangeRateRepository = exchangeRateRepository,
                settingsRepository = settingsRepository,
                fixedAssetRepository = fixedAssetRepository,
                userSessionDao = userSessionDao,
                userId = userId,
                initialUserName = initialUserName,
                initialUserEmail = initialUserEmail
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
