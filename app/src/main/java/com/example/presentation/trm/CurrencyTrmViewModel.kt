package com.example.presentation.trm

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.ManualExchangeRateEntity
import com.example.data.local.entity.ManualRateHistoryLogEntity
import com.example.domain.model.CurrencyRateCardItem
import com.example.domain.model.EffectiveRate
import com.example.domain.model.HistoryRange
import com.example.domain.model.RateHistoryPoint
import com.example.domain.model.TestSuiteItemResult
import com.example.domain.repository.ExchangeRateRepository
import com.example.features.settings.domain.repository.SettingsRepository
import com.example.util.testsuite.ExchangeRateTestSuite
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class CurrencyTrmUiState(
    val baseCurrency: String = "COP",
    val availableCurrencies: List<String> = listOf("COP", "USD", "EUR", "MXN", "GBP", "CAD", "BRL", "CLP", "PEN", "ARS", "JPY", "CHF"),
    val rateCards: List<CurrencyRateCardItem> = emptyList(),
    val selectedHistoryPair: String = "USD / COP",
    val historyFromCurrency: String = "USD",
    val historyToCurrency: String = "COP",
    val selectedHistoryRange: HistoryRange = HistoryRange.DAYS_30,
    val historicalPoints: List<RateHistoryPoint> = emptyList(),
    val isLoadingHistory: Boolean = false,
    val isRefreshing: Boolean = false,
    val lastUpdateTimestamp: Long = System.currentTimeMillis(),
    val isConnectionOnline: Boolean = true,
    // Converter
    val converterAmountText: String = "2855",
    val converterFromCurrency: String = "USD",
    val converterToCurrency: String = "COP",
    val converterEffectiveRate: EffectiveRate? = null,
    val convertedAmount: Double = 0.0,
    // Manual rate
    val activeManualRate: ManualExchangeRateEntity? = null,
    val manualRates: List<ManualExchangeRateEntity> = emptyList(),
    val manualRateLogs: List<ManualRateHistoryLogEntity> = emptyList(),
    // Settings
    val isAutoUpdateEnabled: Boolean = true,
    val isOfficialTrmActive: Boolean = true,
    // Test Suite
    val testSuiteResults: List<TestSuiteItemResult> = emptyList(),
    val isRunningTestSuite: Boolean = false,
    val statusMessage: String? = null
)

class CurrencyTrmViewModel(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository,
    private val testSuite: ExchangeRateTestSuite
) : ViewModel() {

    private val _uiState = MutableStateFlow(CurrencyTrmUiState())
    val uiState: StateFlow<CurrencyTrmUiState> = _uiState.asStateFlow()

    private val timeFormat = SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale("es", "CO"))

    init {
        loadInitialData()
        observeRateUpdates()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            // 1. Observe Base Currency from Settings
            settingsRepository.observePreferences("user_default").collect { prefs ->
                val base = prefs.baseCurrency.ifBlank { "COP" }
                _uiState.update { it.copy(baseCurrency = base, historyToCurrency = base, converterToCurrency = base) }
                refreshRates(forceNetwork = false)
                loadHistoricalSeries()
                recalculateConversion()
            }
        }
    }

    private fun observeRateUpdates() {
        viewModelScope.launch {
            combine(
                exchangeRateRepository.observeManualRates(),
                exchangeRateRepository.observeManualRateLogs()
            ) { manualRates, logs ->
                Pair(manualRates, logs)
            }.collect { (manualRates, logs) ->
                val active = manualRates.firstOrNull { it.isActive }
                _uiState.update {
                    it.copy(
                        manualRates = manualRates,
                        manualRateLogs = logs,
                        activeManualRate = active
                    )
                }
                recalculateConversion()
            }
        }

        viewModelScope.launch {
            _uiState.collect { state ->
                val base = state.baseCurrency
                val targets = listOf("USD", "EUR", "GBP", "MXN", "CAD", "BRL").filter { !it.equals(base, ignoreCase = true) }
                exchangeRateRepository.observeRateCardsForBase(base, targets)
                    .catch { e -> _uiState.update { it.copy(isConnectionOnline = false) } }
                    .collect { cards ->
                        _uiState.update { it.copy(rateCards = cards, isConnectionOnline = true) }
                    }
            }
        }
    }

    fun setBaseCurrency(newBase: String) {
        viewModelScope.launch {
            val upper = newBase.trim().uppercase()
            _uiState.update {
                it.copy(
                    baseCurrency = upper,
                    historyToCurrency = upper,
                    converterToCurrency = upper,
                    selectedHistoryPair = "${it.historyFromCurrency} / $upper"
                )
            }
            settingsRepository.setBaseCurrency("user_default", upper)
            refreshRates(forceNetwork = true)
            loadHistoricalSeries()
            recalculateConversion()
        }
    }

    fun refreshRates(forceNetwork: Boolean = true) {
        viewModelScope.launch {
            _uiState.update { it.copy(isRefreshing = true) }
            val res = exchangeRateRepository.refreshRates(_uiState.value.baseCurrency, forceNetwork = forceNetwork)
            val isSuccess = res.isSuccess
            val timestamp = System.currentTimeMillis()
            _uiState.update {
                it.copy(
                    isRefreshing = false,
                    isConnectionOnline = isSuccess,
                    lastUpdateTimestamp = timestamp,
                    statusMessage = if (isSuccess) "Tasas de cambio actualizadas correctamente" else "Sin conexión: Usando última tasa almacenada"
                )
            }
            loadHistoricalSeries()
            recalculateConversion()
        }
    }

    fun setHistoryRange(range: HistoryRange, customStart: String? = null, customEnd: String? = null) {
        _uiState.update { it.copy(selectedHistoryRange = range) }
        loadHistoricalSeries(customStart, customEnd)
    }

    fun setHistoryPair(pair: String) {
        val parts = pair.split("/").map { it.trim() }
        val from = parts.getOrNull(0) ?: "USD"
        val to = parts.getOrNull(1) ?: _uiState.value.baseCurrency
        _uiState.update {
            it.copy(
                selectedHistoryPair = "$from / $to",
                historyFromCurrency = from,
                historyToCurrency = to
            )
        }
        loadHistoricalSeries()
    }

    private fun loadHistoricalSeries(customStart: String? = null, customEnd: String? = null) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingHistory = true) }
            val state = _uiState.value
            val res = exchangeRateRepository.getHistoricalSeries(
                from = state.historyFromCurrency,
                to = state.historyToCurrency,
                range = state.selectedHistoryRange,
                customStart = customStart,
                customEnd = customEnd
            )
            if (res.isSuccess) {
                _uiState.update {
                    it.copy(
                        historicalPoints = res.getOrThrow(),
                        isLoadingHistory = false
                    )
                }
            } else {
                _uiState.update { it.copy(isLoadingHistory = false) }
            }
        }
    }

    // ==========================================
    // CONVERTER ACTIONS
    // ==========================================

    fun onConverterAmountChanged(newText: String) {
        val filtered = newText.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
        if (filtered.count { it == '.' } <= 1) {
            _uiState.update { it.copy(converterAmountText = filtered) }
            recalculateConversion()
        }
    }

    fun setConverterFromCurrency(currency: String) {
        _uiState.update { it.copy(converterFromCurrency = currency.uppercase()) }
        recalculateConversion()
    }

    fun setConverterToCurrency(currency: String) {
        _uiState.update { it.copy(converterToCurrency = currency.uppercase()) }
        recalculateConversion()
    }

    fun swapConverterCurrencies() {
        val currentFrom = _uiState.value.converterFromCurrency
        val currentTo = _uiState.value.converterToCurrency
        _uiState.update {
            it.copy(
                converterFromCurrency = currentTo,
                converterToCurrency = currentFrom
            )
        }
        recalculateConversion()
    }

    private fun recalculateConversion() {
        viewModelScope.launch {
            val state = _uiState.value
            val amount = state.converterAmountText.toDoubleOrNull() ?: 0.0
            val effective = exchangeRateRepository.getInstantEffectiveRate(state.converterFromCurrency, state.converterToCurrency)
            val converted = amount * effective.rate
            _uiState.update {
                it.copy(
                    converterEffectiveRate = effective,
                    convertedAmount = converted
                )
            }
        }
    }

    // ==========================================
    // MANUAL RATE ACTIONS
    // ==========================================

    fun saveManualRate(
        from: String,
        to: String,
        rate: Double,
        isActive: Boolean,
        validFrom: String,
        validTo: String,
        note: String
    ) {
        viewModelScope.launch {
            val res = exchangeRateRepository.saveManualRate(
                from = from.uppercase(),
                to = to.uppercase(),
                rate = rate,
                isActive = isActive,
                validFrom = validFrom,
                validTo = validTo,
                note = note
            )
            if (res.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Tasa manual guardada y activada con éxito") }
                recalculateConversion()
                loadHistoricalSeries()
            } else {
                _uiState.update { it.copy(statusMessage = "Error al guardar tasa manual: ${res.exceptionOrNull()?.message}") }
            }
        }
    }

    fun toggleManualRateActive(id: String, isActive: Boolean) {
        viewModelScope.launch {
            val res = exchangeRateRepository.toggleManualRate(id, isActive)
            if (res.isSuccess) {
                _uiState.update { it.copy(statusMessage = if (isActive) "Tasa manual activada" else "Tasa manual desactivada (usando tasa automática)") }
                recalculateConversion()
            }
        }
    }

    fun resetToAutomaticRate(id: String) {
        viewModelScope.launch {
            val res = exchangeRateRepository.deleteManualRate(id)
            if (res.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Restablecido a tasa automática de Frankfurter") }
                recalculateConversion()
            }
        }
    }

    fun toggleOfficialTrm(enabled: Boolean) {
        _uiState.update { it.copy(isOfficialTrmActive = enabled) }
    }

    // ==========================================
    // TEST SUITE ACTIONS
    // ==========================================

    fun runTestSuite() {
        viewModelScope.launch {
            _uiState.update { it.copy(isRunningTestSuite = true, testSuiteResults = emptyList()) }
            val results = testSuite.runAllTests()
            _uiState.update {
                it.copy(
                    testSuiteResults = results,
                    isRunningTestSuite = false,
                    statusMessage = "Módulo de pruebas completado: ${results.count { it.status == TestSuiteItemResult.TestStatus.OK }}/${results.size} OK"
                )
            }
        }
    }

    fun clearStatusMessage() {
        _uiState.update { it.copy(statusMessage = null) }
    }
}

class CurrencyTrmViewModelFactory(
    private val exchangeRateRepository: ExchangeRateRepository,
    private val settingsRepository: SettingsRepository,
    private val testSuite: ExchangeRateTestSuite
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return CurrencyTrmViewModel(exchangeRateRepository, settingsRepository, testSuite) as T
    }
}
