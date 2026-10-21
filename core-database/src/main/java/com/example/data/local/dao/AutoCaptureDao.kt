package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.PendingAutoCaptureEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AutoCaptureDao {

    @Query("""
        SELECT * FROM pending_auto_captures
        WHERE userId = :userId AND status = 'PENDING'
        ORDER BY notificationTimestamp DESC
    """)
    fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>>

    @Query("SELECT COUNT(*) FROM pending_auto_captures WHERE userId = :userId AND status = 'PENDING'")
    fun observePendingCount(userId: String): Flow<Int>

    @Query("SELECT * FROM pending_auto_captures WHERE id = :id AND userId = :userId LIMIT 1")
    suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity?

    @Query("SELECT EXISTS(SELECT 1 FROM pending_auto_captures WHERE idempotencyKey = :idempotencyKey)")
    suspend fun existsByIdempotencyKey(idempotencyKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPendingCapture(entity: PendingAutoCaptureEntity): Long

    @Query("""
        UPDATE pending_auto_captures
        SET status = :status, linkedTransactionId = :linkedTransactionId
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateCaptureStatus(id: String, userId: String, status: String, linkedTransactionId: String? = null)

    @Query("DELETE FROM pending_auto_captures WHERE userId = :userId")
    suspend fun deleteAllByUserId(userId: String)

    @Query("SELECT * FROM pending_auto_captures WHERE userId = :userId ORDER BY notificationTimestamp DESC")
    suspend fun getAllCapturesByUserId(userId: String): List<PendingAutoCaptureEntity>

    @Query("""
        UPDATE pending_auto_captures
        SET matchedAccountId = :accountId,
            matchedAccountName = :accountName,
            matchedCategoryId = :categoryId,
            matchedCategoryName = :categoryName
        WHERE id = :id AND userId = :userId
    """)
    suspend fun updateDraftSelection(
        id: String,
        userId: String,
        accountId: String?,
        accountName: String?,
        categoryId: String?,
        categoryName: String?
    )

    @Query("DELETE FROM pending_auto_captures WHERE id = :id AND userId = :userId")
    suspend fun deleteCaptureById(id: String, userId: String)
}
