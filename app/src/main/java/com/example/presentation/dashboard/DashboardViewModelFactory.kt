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
    private val userId: String = "user_default"
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(DashboardViewModel::class.java)) {
            return DashboardViewModel(
                financialRepository = financialRepository,
                exchangeRateRepository = exchangeRateRepository,
                settingsRepository = settingsRepository,
                fixedAssetRepository = fixedAssetRepository,
                userId = userId
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
