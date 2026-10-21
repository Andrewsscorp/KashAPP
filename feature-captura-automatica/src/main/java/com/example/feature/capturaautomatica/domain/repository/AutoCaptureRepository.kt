package com.example.feature.capturaautomatica.domain.repository

import com.example.data.local.entity.PendingAutoCaptureEntity
import kotlinx.coroutines.flow.Flow

interface AutoCaptureRepository {
    fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>>
    fun observePendingCount(userId: String): Flow<Int>
    suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity?

    suspend fun processNotification(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long = System.currentTimeMillis(),
        userId: String
    ): Result<PendingAutoCaptureEntity?>

    suspend fun confirmCapture(
        captureId: String,
        userId: String,
        customAccountId: String? = null,
        customAccountName: String? = null,
        customCategoryId: String? = null,
        customCategoryName: String? = null,
        customTags: List<String> = emptyList()
    ): Result<String>

    suspend fun updateDraftSelection(
        captureId: String,
        userId: String,
        accountId: String?,
        accountName: String?,
        categoryId: String?,
        categoryName: String?
    ): Result<Unit>

    suspend fun dismissCapture(captureId: String, userId: String): Result<Unit>

    suspend fun simulateTestCapture(
        userId: String,
        bankName: String = "Bancolombia"
    ): Result<PendingAutoCaptureEntity>
}
