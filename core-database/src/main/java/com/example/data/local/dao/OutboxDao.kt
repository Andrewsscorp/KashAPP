package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.OutboxOperationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface OutboxDao {

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOperation(operation: OutboxOperationEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertOperations(operations: List<OutboxOperationEntity>)

    @Query("SELECT * FROM outbox_operations WHERE status = 'PENDING' AND nextRetryAt <= :currentTime ORDER BY createdAt ASC LIMIT :limit")
    suspend fun getPendingOperations(currentTime: Long = System.currentTimeMillis(), limit: Int = 50): List<OutboxOperationEntity>

    @Query("SELECT COUNT(*) FROM outbox_operations WHERE status = 'PENDING'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM outbox_operations WHERE status = 'PENDING'")
    suspend fun getPendingCount(): Int

    @Query("SELECT * FROM outbox_operations ORDER BY createdAt DESC LIMIT 100")
    fun observeAllOperations(): Flow<List<OutboxOperationEntity>>

    @Query("UPDATE outbox_operations SET status = :status, retryCount = :retryCount, nextRetryAt = :nextRetryAt, lastError = :error WHERE id = :id")
    suspend fun updateStatus(id: String, status: String, retryCount: Int, nextRetryAt: Long, error: String?)

    @Query("DELETE FROM outbox_operations WHERE id = :id")
    suspend fun deleteOperation(id: String)

    @Query("DELETE FROM outbox_operations WHERE status = 'DELIVERED'")
    suspend fun purgeDelivered()

    @Query("DELETE FROM outbox_operations WHERE userId = :userId AND :userId != ''")
    suspend fun deleteOperationsByUserId(userId: String): Int
}
