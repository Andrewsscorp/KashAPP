package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.example.data.local.entity.AuditSecurityLog
import kotlinx.coroutines.flow.Flow

@Dao
interface AuditSecurityLogDao {
    @Insert
    suspend fun insertLog(log: AuditSecurityLog)

    @Query("SELECT * FROM audit_security_logs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<AuditSecurityLog>>

    @Query("SELECT * FROM audit_security_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<AuditSecurityLog>

    @Query("SELECT * FROM audit_security_logs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int = 20): Flow<List<AuditSecurityLog>>

    @Query("SELECT * FROM audit_security_logs WHERE (userId = :userId OR userId = '') ORDER BY timestamp DESC")
    fun getAllLogsByUser(userId: String): Flow<List<AuditSecurityLog>>

    @Query("SELECT * FROM audit_security_logs WHERE (userId = :userId OR userId = '') ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogsByUser(userId: String, limit: Int = 20): Flow<List<AuditSecurityLog>>
}
