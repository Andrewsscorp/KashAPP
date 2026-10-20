package com.example.feature.capturaautomatica

import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository
import com.example.feature.capturaautomatica.ui.AutoCaptureUiEvent
import com.example.feature.capturaautomatica.ui.AutoCaptureViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AutoCaptureViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val testUserId = "user_test_viewmodel"

    private lateinit var fakeRepository: FakeViewModelRepository
    private lateinit var viewModel: AutoCaptureViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeViewModelRepository()
        viewModel = AutoCaptureViewModel(fakeRepository, testUserId)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun testPendingCaptures_reflectsRepositoryFlow() = runTest(testDispatcher) {
        val collectJob = launch { viewModel.pendingCaptures.collect {} }
        advanceUntilIdle()

        assertEquals(0, viewModel.pendingCaptures.value.size)

        fakeRepository.emitCaptures(
            listOf(
                PendingAutoCaptureEntity(
                    id = "cap_1",
                    userId = testUserId,
                    sourcePackage = "com.todo1.mobile",
                    detectedBankName = "Bancolombia",
                    transactionType = "EXPENSE",
                    amount = 50000.0,
                    merchantOrCounterparty = "EXITO",
                    idempotencyKey = "key_1",
                    notificationTimestamp = System.currentTimeMillis()
                )
            )
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.pendingCaptures.value.size)
        assertEquals(50000.0, viewModel.pendingCaptures.value.first().amount, 0.01)

        collectJob.cancel()
    }

    @Test
    fun testConfirmCapture_emitsSuccessEvent() = runTest(testDispatcher) {
        val events = mutableListOf<AutoCaptureUiEvent>()
        val eventJob = launch { viewModel.events.collect { events.add(it) } }

        viewModel.confirmCapture("cap_1")
        advanceUntilIdle()

        assertTrue(events.any { it is AutoCaptureUiEvent.ShowMessage })
        val msg = events.filterIsInstance<AutoCaptureUiEvent.ShowMessage>().first()
        assertTrue(msg.message.contains("confirmado"))

        eventJob.cancel()
    }

    @Test
    fun testDismissCapture_emitsDismissEvent() = runTest(testDispatcher) {
        val events = mutableListOf<AutoCaptureUiEvent>()
        val eventJob = launch { viewModel.events.collect { events.add(it) } }

        viewModel.dismissCapture("cap_1")
        advanceUntilIdle()

        assertTrue(events.any { it is AutoCaptureUiEvent.ShowMessage })
        val msg = events.filterIsInstance<AutoCaptureUiEvent.ShowMessage>().first()
        assertTrue(msg.message.contains("descartada"))

        eventJob.cancel()
    }

    @Test
    fun testSimulateCapture_emitsSuccessEvent() = runTest(testDispatcher) {
        val events = mutableListOf<AutoCaptureUiEvent>()
        val eventJob = launch { viewModel.events.collect { events.add(it) } }

        viewModel.simulateCapture("Bancolombia")
        advanceUntilIdle()

        assertTrue(events.any { it is AutoCaptureUiEvent.ShowMessage })
        val msg = events.filterIsInstance<AutoCaptureUiEvent.ShowMessage>().first()
        assertTrue(msg.message.contains("Bancolombia"))

        eventJob.cancel()
    }
}

private class FakeViewModelRepository : AutoCaptureRepository {
    private val capturesFlow = MutableStateFlow<List<PendingAutoCaptureEntity>>(emptyList())
    private val countFlow = MutableStateFlow(0)

    fun emitCaptures(list: List<PendingAutoCaptureEntity>) {
        capturesFlow.value = list
        countFlow.value = list.size
    }

    override fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>> = capturesFlow
    override fun observePendingCount(userId: String): Flow<Int> = countFlow

    override suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity? =
        capturesFlow.value.find { it.id == id && it.userId == userId }

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
    ): Result<String> = Result.success("tx_confirmed_99")

    override suspend fun dismissCapture(captureId: String, userId: String): Result<Unit> = Result.success(Unit)

    override suspend fun simulateTestCapture(userId: String, bankName: String): Result<PendingAutoCaptureEntity> {
        val entity = PendingAutoCaptureEntity(
            id = "sim_1",
            userId = userId,
            sourcePackage = "com.todo1.mobile",
            detectedBankName = bankName,
            transactionType = "EXPENSE",
            amount = 30000.0,
            merchantOrCounterparty = "COMERCIO TEST",
            idempotencyKey = "sim_key_1",
            notificationTimestamp = System.currentTimeMillis()
        )
        return Result.success(entity)
    }
}
