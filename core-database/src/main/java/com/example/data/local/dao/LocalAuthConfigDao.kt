package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.local.entity.LocalAuthConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface LocalAuthConfigDao {
    @Query("SELECT * FROM local_auth_config WHERE id = 1 LIMIT 1")
    fun getConfigFlow(): Flow<LocalAuthConfig?>

    @Query("SELECT * FROM local_auth_config WHERE id = 1 LIMIT 1")
    suspend fun getConfig(): LocalAuthConfig?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(config: LocalAuthConfig)

    @Query("UPDATE local_auth_config SET failed_attempts = :attempts, lockout_until_timestamp = :lockoutUntil WHERE id = 1")
    suspend fun updateAttempts(attempts: Int, lockoutUntil: Long)

    @Query("UPDATE local_auth_config SET failed_attempts = 0, lockout_until_timestamp = 0, last_login_timestamp = :loginTime WHERE id = 1")
    suspend fun resetLockoutAndRecordLogin(loginTime: Long)

    @Query("UPDATE local_auth_config SET lockout_until_timestamp = 0 WHERE id = 1")
    suspend fun clearLockoutExpiration()

    @Query("UPDATE local_auth_config SET is_biometric_enabled = :enabled WHERE id = 1")
    suspend fun updateBiometricEnabled(enabled: Boolean)
}
