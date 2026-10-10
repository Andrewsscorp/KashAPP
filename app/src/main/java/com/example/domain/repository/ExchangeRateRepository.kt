package com.example.domain.repository

import com.example.data.local.entity.ManualExchangeRateEntity
import com.example.data.local.entity.ManualRateHistoryLogEntity
import com.example.domain.model.CurrencyRateCardItem
import com.example.domain.model.EffectiveRate
import com.example.domain.model.HistoryRange
import com.example.domain.model.RateHistoryPoint
import kotlinx.coroutines.flow.Flow

interface ExchangeRateRepository {
    fun observeEffectiveRate(from: String, to: String): Flow<EffectiveRate>
    fun observeRateCardsForBase(base: String, targetCurrencies: List<String>): Flow<List<CurrencyRateCardItem>>
    suspend fun getInstantEffectiveRate(from: String, to: String): EffectiveRate
    suspend fun refreshRates(baseCurrency: String, forceNetwork: Boolean = false): Result<Unit>
    suspend fun getHistoricalSeries(
        from: String,
        to: String,
        range: HistoryRange,
        customStart: String? = null,
        customEnd: String? = null
    ): Result<List<RateHistoryPoint>>
    suspend fun getAvailableCurrencies(): Result<Map<String, String>>
    suspend fun saveManualRate(
        from: String,
        to: String,
        rate: Double,
        isActive: Boolean,
        validFrom: String,
        validTo: String,
        note: String
    ): Result<Unit>
    suspend fun toggleManualRate(id: String, isActive: Boolean): Result<Unit>
    suspend fun deleteManualRate(id: String): Result<Unit>
    fun observeManualRates(): Flow<List<ManualExchangeRateEntity>>
    fun observeManualRateLogs(): Flow<List<ManualRateHistoryLogEntity>>
    suspend fun getLastUpdateTimestamp(): Long?
}
