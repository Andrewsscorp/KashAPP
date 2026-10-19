package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.AssetAuditLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AssetAuditLogDao {
    @Query("SELECT * FROM asset_audit_logs WHERE userId = :userId AND :userId != '' ORDER BY timestamp DESC")
    fun observeAuditLogs(userId: String): Flow<List<AssetAuditLogEntity>>

    @Query("SELECT * FROM asset_audit_logs ORDER BY timestamp DESC")
    fun getAllAssetLogs(): Flow<List<AssetAuditLogEntity>>

    @Query("SELECT * FROM asset_audit_logs WHERE assetId = :assetId ORDER BY timestamp DESC")
    fun observeAuditLogsForAsset(assetId: String): Flow<List<AssetAuditLogEntity>>

    @Query("SELECT * FROM asset_audit_logs WHERE userId = :userId AND :userId != '' ORDER BY timestamp DESC")
    suspend fun getAllAuditLogs(userId: String): List<AssetAuditLogEntity>

    @Query("SELECT * FROM asset_audit_logs ORDER BY timestamp DESC")
    suspend fun getAllLogsList(): List<AssetAuditLogEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AssetAuditLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLogs(logs: List<AssetAuditLogEntity>)

    @Query("DELETE FROM asset_audit_logs WHERE userId = :userId AND :userId != ''")
    suspend fun deleteAuditLogsByUserId(userId: String): Int

    @Query("DELETE FROM asset_audit_logs WHERE assetId IN (SELECT id FROM fixed_assets WHERE userId = :userId AND :userId != '')")
    suspend fun deleteAuditLogsForUserAssets(userId: String): Int
}
