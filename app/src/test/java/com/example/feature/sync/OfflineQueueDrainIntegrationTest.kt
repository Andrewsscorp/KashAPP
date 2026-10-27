package com.example.feature.sync

import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.entity.OutboxOperationEntity
import com.example.feature.sync.engine.OutboxSyncEngine
import com.example.feature.sync.model.SyncOperationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Isolated Enterprise Integration Test Suite for Offline Outbox Queue Draining.
 *
 * Requirements:
 * 1. Offline mutations are enqueued with PENDING status.
 * 2. On network connection, pending operations are drained and sent to remote cloud storage.
 * 3. Successful sync transitions status to DELIVERED.
 * 4. Failed sync increments retryCount and schedules exponential backoff.
 * 5. Draining triggers 7-day retention purge of old delivered operations.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class OfflineQueueDrainIntegrationTest {

    private lateinit var outboxDao: FakeOutboxDao
    private lateinit var syncEngine: OutboxSyncEngine

    private val testDispatcher = UnconfinedTestDispatcher()
    private val now = 1730000000000L

    class StubFirebaseDataSource : FirebaseDataSource() {
        var failPredicate: (OutboxOperationEntity) -> Boolean = { false }
        val syncedOps = mutableListOf<OutboxOperationEntity>()

        override suspend fun syncOutboxOperation(operation: OutboxOperationEntity): Result<Unit> {
            return if (failPredicate(operation)) {
                Result.failure(RuntimeException("Simulated network outage to Firestore"))
            } else {
                syncedOps.add(operation)
                Result.success(Unit)
            }
        }
    }

    private lateinit var stubFirebaseDataSource: StubFirebaseDataSource

    @Before
    fun setup() {
        outboxDao = FakeOutboxDao()
        stubFirebaseDataSource = StubFirebaseDataSource()

        // Create sync engine with in-memory test fakes
        syncEngine = OutboxSyncEngine(
            outboxDao = outboxDao,
            financialDao = null as? FinancialDao ?: createEmptyFinancialDaoProxy(),
            fixedAssetDao = null,
            firebaseDataSource = stubFirebaseDataSource,
            cryptoManager = null,
            ioDispatcher = testDispatcher
        )
    }

    private fun createEmptyFinancialDaoProxy(): FinancialDao {
        return java.lang.reflect.Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, _, _ -> null } as FinancialDao
    }

    @Test
    fun enqueueOperation_insertsPendingOutboxEntity() = runTest {
        val result = syncEngine.enqueueOperation(
            userId = "user_test_01",
            entityType = "TRANSACTION",
            entityId = "tx_999",
            operation = SyncOperationType.CREATE,
            payloadJson = "{\"amount\": 50000}"
        )

        assertTrue(result.isSuccess)
        val entity = result.getOrThrow()
        assertEquals("PENDING", entity.status)
        assertEquals("TRANSACTION", entity.entityType)
        assertEquals("tx_999", entity.entityId)
        assertEquals(0, entity.retryCount)

        assertEquals(1, outboxDao.operations.size)
        assertEquals("tx_999", outboxDao.operations.first().entityId)
    }

    @Test
    fun drainOutboxQueue_processesSuccessAndFailureCorrectly() = runTest {
        val opSuccess = OutboxOperationEntity(
            id = "op_success",
            userId = "user_test_01",
            entityType = "TRANSACTION",
            entityId = "tx_101",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_101",
            status = "PENDING",
            retryCount = 0,
            nextRetryAt = now - 1000L,
            createdAt = now
        )

        val opFail = OutboxOperationEntity(
            id = "op_fail",
            userId = "user_test_01",
            entityType = "TRANSACTION",
            entityId = "tx_102",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_102",
            status = "PENDING",
            retryCount = 1,
            nextRetryAt = now - 1000L,
            createdAt = now
        )

        // Pre-seed 1 old delivered operation (> 7 days) that should be purged
        val oldDelivered = OutboxOperationEntity(
            id = "op_old_delivered",
            userId = "user_test_01",
            entityType = "TRANSACTION",
            entityId = "tx_old",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_old",
            status = "DELIVERED",
            createdAt = now - (8L * 24L * 60L * 60L * 1000L) // 8 days old
        )

        outboxDao.insertOperations(listOf(opSuccess, opFail, oldDelivered))

        // Fail op_fail specifically
        stubFirebaseDataSource.failPredicate = { it.id == "op_fail" }

        val drainResult = syncEngine.drainOutboxQueue(currentTimeMillis = now)

        assertTrue(drainResult.isSuccess)
        val stats = drainResult.getOrThrow()
        assertEquals(1, stats.pushedCount)
        assertEquals(1, stats.failedCount)
        assertEquals(1, stats.purgedDeliveredCount)

        // Verify opSuccess was marked DELIVERED
        val updatedSuccess = outboxDao.operations.find { it.id == "op_success" }
        assertNotNull(updatedSuccess)
        assertEquals("DELIVERED", updatedSuccess!!.status)

        // Verify opFail remained PENDING with incremented retryCount and future nextRetryAt
        val updatedFail = outboxDao.operations.find { it.id == "op_fail" }
        assertNotNull(updatedFail)
        assertEquals("PENDING", updatedFail!!.status)
        assertEquals(2, updatedFail.retryCount)
        assertTrue(updatedFail.nextRetryAt > now)

        // Verify old delivered operation was purged by 7-day retention rule
        val purgedCheck = outboxDao.operations.find { it.id == "op_old_delivered" }
        assertEquals("La operación entregada con más de 7 días debió ser purgada", null, purgedCheck)
    }
}
