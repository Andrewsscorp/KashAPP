package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.ExchangeRateCacheEntity
import com.example.data.local.entity.ManualExchangeRateEntity
import com.example.data.local.entity.ManualRateHistoryLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExchangeRateDao {

    // ==========================================
    // CACHED RATES (Frankfurter / Firebase)
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRates(rates: List<ExchangeRateCacheEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCachedRate(rate: ExchangeRateCacheEntity)

    @Query("SELECT * FROM exchange_rate_cache WHERE baseCurrency = :base AND targetCurrency = :target LIMIT 1")
    suspend fun getCachedRate(base: String, target: String): ExchangeRateCacheEntity?

    @Query("SELECT * FROM exchange_rate_cache WHERE baseCurrency = :base")
    fun observeCachedRatesByBase(base: String): Flow<List<ExchangeRateCacheEntity>>

    @Query("SELECT * FROM exchange_rate_cache")
    fun observeAllCachedRates(): Flow<List<ExchangeRateCacheEntity>>

    @Query("SELECT * FROM exchange_rate_cache")
    suspend fun getAllCachedRates(): List<ExchangeRateCacheEntity>

    @Query("SELECT MAX(lastUpdatedMillis) FROM exchange_rate_cache")
    suspend fun getLatestRateTimestamp(): Long?

    // ==========================================
    // MANUAL USER RATES
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualRate(manualRate: ManualExchangeRateEntity)

    @Query("SELECT * FROM manual_exchange_rates WHERE fromCurrency = :from AND toCurrency = :to LIMIT 1")
    suspend fun getManualRate(from: String, to: String): ManualExchangeRateEntity?

    @Query("SELECT * FROM manual_exchange_rates WHERE id = :id LIMIT 1")
    suspend fun getManualRateById(id: String): ManualExchangeRateEntity?

    @Query("SELECT * FROM manual_exchange_rates ORDER BY updatedAt DESC")
    fun observeAllManualRates(): Flow<List<ManualExchangeRateEntity>>

    @Query("SELECT * FROM manual_exchange_rates WHERE (userId = :userId OR userId = '') ORDER BY updatedAt DESC")
    fun observeAllManualRatesByUser(userId: String): Flow<List<ManualExchangeRateEntity>>

    @Query("SELECT * FROM manual_exchange_rates ORDER BY updatedAt DESC")
    suspend fun getAllManualRates(): List<ManualExchangeRateEntity>

    @Query("SELECT * FROM manual_exchange_rates WHERE (userId = :userId OR userId = '') ORDER BY updatedAt DESC")
    suspend fun getAllManualRatesByUser(userId: String): List<ManualExchangeRateEntity>

    @Query("UPDATE manual_exchange_rates SET isActive = :isActive, updatedAt = :updatedAt WHERE id = :id")
    suspend fun updateManualRateStatus(id: String, isActive: Boolean, updatedAt: Long = System.currentTimeMillis())

    @Query("DELETE FROM manual_exchange_rates WHERE id = :id")
    suspend fun deleteManualRate(id: String)

    // ==========================================
    // MANUAL RATE AUDIT LOGS
    // ==========================================

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManualRateLog(log: ManualRateHistoryLogEntity)

    @Query("SELECT * FROM manual_rate_history_logs ORDER BY timestamp DESC")
    fun observeManualRateLogs(): Flow<List<ManualRateHistoryLogEntity>>

    @Query("SELECT * FROM manual_rate_history_logs ORDER BY timestamp DESC LIMIT 100")
    suspend fun getRecentManualRateLogs(): List<ManualRateHistoryLogEntity>
}
