package com.example.feature.capturaautomatica

import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.feature.capturaautomatica.di.AutoCaptureEntryPoint
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository
import com.example.feature.capturaautomatica.service.AutoCaptureNotificationHelper
import com.example.feature.capturaautomatica.service.QuickCashTileService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class AutoCaptureServiceTest {

    @Test
    fun testAutoCaptureEntryPoint_setAndGetRepository() {
        val fakeRepo = object : AutoCaptureRepository {
            override fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>> = emptyFlow()
            override fun observePendingCount(userId: String): Flow<Int> = emptyFlow()
            override suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity? = null
            override suspend fun processNotification(
                packageName: String,
                title: String?,
                text: String?,
                postTime: Long,
                userId: String
            ): Result<PendingAutoCaptureEntity?> = Result.success(null)
            override suspend fun confirmCapture(
                captureId: String,
                userId: String,
                customAccountId: String?,
                customAccountName: String?,
                customCategoryId: String?,
                customCategoryName: String?
            ): Result<String> = Result.success("tx_123")
            override suspend fun dismissCapture(captureId: String, userId: String): Result<Unit> = Result.success(Unit)
            override suspend fun simulateTestCapture(userId: String, bankName: String): Result<PendingAutoCaptureEntity> {
                throw UnsupportedOperationException()
            }
        }

        AutoCaptureEntryPoint.setRepository(fakeRepo)
        val retrieved = AutoCaptureEntryPoint.getRepository()
        assertNotNull(retrieved)
    }

    @Test
    fun testAutoCaptureEntryPoint_activeUserIdProvider() = runTest {
        AutoCaptureEntryPoint.setActiveUserIdProvider { "user_hawk_99" }
        val userId = AutoCaptureEntryPoint.getActiveUserId()
        assertEquals("user_hawk_99", userId)
    }

    @Test
    fun testNotificationHelperConstants() {
        assertEquals("kashapp_auto_capture", AutoCaptureNotificationHelper.CHANNEL_ID)
        assertEquals("com.example.feature.capturaautomatica.ACTION_CONFIRM", AutoCaptureNotificationHelper.ACTION_CONFIRM_CAPTURE)
        assertEquals("com.example.feature.capturaautomatica.ACTION_DISMISS", AutoCaptureNotificationHelper.ACTION_DISMISS_CAPTURE)
        assertEquals("com.example.feature.capturaautomatica.ACTION_OPEN_INBOX", AutoCaptureNotificationHelper.ACTION_OPEN_INBOX)
    }

    @Test
    fun testQuickCashTileConstants() {
        assertEquals("extra_open_quick_cash_dialog", QuickCashTileService.EXTRA_OPEN_QUICK_CASH)
    }
}
