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
}
