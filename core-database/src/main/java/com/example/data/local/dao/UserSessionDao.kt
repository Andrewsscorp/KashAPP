package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.UserSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * Room Data Access Object for local encrypted user session persistence.
 */
@Dao
interface UserSessionDao {

    @Query("SELECT * FROM user_sessions WHERE is_active = 1 ORDER BY last_active_timestamp DESC LIMIT 1")
    fun getActiveSessionFlow(): Flow<UserSessionEntity?>

    @Query("SELECT * FROM user_sessions WHERE is_active = 1 ORDER BY last_active_timestamp DESC LIMIT 1")
    suspend fun getActiveSession(): UserSessionEntity?

    @Query("SELECT * FROM user_sessions ORDER BY last_active_timestamp DESC LIMIT 1")
    suspend fun getLastSession(): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE is_active = 1 ORDER BY last_active_timestamp DESC LIMIT 1")
    suspend fun getLastActiveSession(): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE user_id = :userId LIMIT 1")
    suspend fun getSessionByUserId(userId: String): UserSessionEntity?

    @Query("SELECT * FROM user_sessions WHERE user_id = :userId AND is_active = 1 LIMIT 1")
    suspend fun getActiveSessionByUserId(userId: String): UserSessionEntity?

    @Query("SELECT * FROM user_sessions ORDER BY last_active_timestamp DESC")
    fun getAllSessionsFlow(): Flow<List<UserSessionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateSession(session: UserSessionEntity)

    @Query("UPDATE user_sessions SET is_active = 0 WHERE user_id = :userId")
    suspend fun deactivateSession(userId: String)

    @Query("UPDATE user_sessions SET is_active = 0")
    suspend fun deactivateAllSessions()

    @Query("UPDATE user_sessions SET last_active_timestamp = :timestamp WHERE user_id = :userId")
    suspend fun updateLastActive(userId: String, timestamp: Long)

    @Query("DELETE FROM user_sessions WHERE user_id = :userId")
    suspend fun deleteSession(userId: String)

    @Query("DELETE FROM user_sessions")
    suspend fun clearAllSessions()
}
