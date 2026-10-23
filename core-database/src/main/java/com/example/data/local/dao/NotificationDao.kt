package com.example.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.local.entity.NotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface NotificationDao {

    @Query(
        """
        SELECT * FROM notifications
        WHERE userId = :userId
          AND isArchived = 0
          AND (snoozedUntilEpochMs IS NULL OR snoozedUntilEpochMs <= :nowEpochMs)
        ORDER BY
          CASE severity
            WHEN 'CRITICAL' THEN 1
            WHEN 'WARNING' THEN 2
            WHEN 'INFO' THEN 3
            ELSE 4
          END ASC,
          createdAtEpochMs DESC
        """
    )
    fun observeActiveNotifications(
        userId: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Flow<List<NotificationEntity>>

    @Query(
        """
        SELECT COUNT(*) FROM notifications
        WHERE userId = :userId
          AND isRead = 0
          AND isArchived = 0
          AND (snoozedUntilEpochMs IS NULL OR snoozedUntilEpochMs <= :nowEpochMs)
        """
    )
    fun observeUnreadCount(
        userId: String,
        nowEpochMs: Long = System.currentTimeMillis()
    ): Flow<Int>

    @Query("SELECT EXISTS(SELECT 1 FROM notifications WHERE userId = :userId AND dedupKey = :dedupKey)")
    suspend fun existsByDedupKey(userId: String, dedupKey: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertNotificationIgnoreDuplicate(notification: NotificationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertNotifications(notifications: List<NotificationEntity>)

    @Query("UPDATE notifications SET isRead = 1, readAtEpochMs = :readAt WHERE id = :notificationId AND userId = :userId")
    suspend fun markAsRead(notificationId: String, userId: String, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isRead = 1, readAtEpochMs = :readAt WHERE userId = :userId AND isRead = 0")
    suspend fun markAllAsRead(userId: String, readAt: Long = System.currentTimeMillis())

    @Query("UPDATE notifications SET isArchived = 1 WHERE id = :notificationId AND userId = :userId")
    suspend fun archiveNotification(notificationId: String, userId: String)

    @Query("UPDATE notifications SET snoozedUntilEpochMs = :snoozeUntilEpochMs, isRead = 1 WHERE id = :notificationId AND userId = :userId")
    suspend fun snoozeNotification(notificationId: String, userId: String, snoozeUntilEpochMs: Long)

    @Query("DELETE FROM notifications WHERE userId = :userId AND isArchived = 1")
    suspend fun clearArchived(userId: String)
}
