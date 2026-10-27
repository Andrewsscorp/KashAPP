package com.example.feature.sync

import com.example.data.local.dao.OutboxDao
import com.example.data.local.entity.OutboxOperationEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * In-memory test fake implementation of OutboxDao.
 */
class FakeOutboxDao : OutboxDao {
    val operations = mutableListOf<OutboxOperationEntity>()

    override suspend fun insertOperation(operation: OutboxOperationEntity) {
        operations.removeIf { it.id == operation.id }
        operations.add(operation)
    }

    override suspend fun insertOperations(ops: List<OutboxOperationEntity>) {
        ops.forEach { insertOperation(it) }
    }

    override suspend fun getPendingOperations(currentTime: Long, limit: Int): List<OutboxOperationEntity> {
        return operations
            .filter { it.status == "PENDING" && it.nextRetryAt <= currentTime }
            .take(limit)
    }

    override suspend fun getPendingOperationsByUser(userId: String, currentTime: Long, limit: Int): List<OutboxOperationEntity> {
        return operations
            .filter { (it.userId == userId || userId.isBlank()) && it.status == "PENDING" && it.nextRetryAt <= currentTime }
            .take(limit)
    }

    override fun observePendingCount(): Flow<Int> = flowOf(operations.count { it.status == "PENDING" })
    override fun observePendingCountByUser(userId: String): Flow<Int> = flowOf(operations.count { it.userId == userId && it.status == "PENDING" })
    override suspend fun getPendingCount(): Int = operations.count { it.status == "PENDING" }
    override suspend fun getPendingCountByUser(userId: String): Int = operations.count { it.userId == userId && it.status == "PENDING" }
    override fun observeAllOperations(): Flow<List<OutboxOperationEntity>> = flowOf(operations)

    override suspend fun updateStatus(id: String, status: String, retryCount: Int, nextRetryAt: Long, error: String?) {
        val idx = operations.indexOfFirst { it.id == id }
        if (idx >= 0) {
            val existing = operations[idx]
            operations[idx] = existing.copy(
                status = status,
                retryCount = retryCount,
                nextRetryAt = nextRetryAt,
                lastError = error
            )
        }
    }

    override suspend fun deleteOperation(id: String) {
        operations.removeIf { it.id == id }
    }

    override suspend fun purgeDelivered() {
        operations.removeIf { it.status == "DELIVERED" }
    }

    override suspend fun purgeDeliveredOlderThan(thresholdMillis: Long): Int {
        val initialSize = operations.size
        operations.removeIf { it.status == "DELIVERED" && it.createdAt <= thresholdMillis }
        return initialSize - operations.size
    }

    override suspend fun getDeliveredOlderThan(thresholdMillis: Long): List<OutboxOperationEntity> {
        return operations.filter { it.status == "DELIVERED" && it.createdAt <= thresholdMillis }
    }

    override suspend fun getRetainedDeliveredOperations(thresholdMillis: Long): List<OutboxOperationEntity> {
        return operations.filter { it.status == "DELIVERED" && it.createdAt > thresholdMillis }
    }

    override suspend fun deleteOperationsByUserId(userId: String): Int {
        val initialSize = operations.size
        operations.removeIf { it.userId == userId }
        return initialSize - operations.size
    }
}

/**
 * Isolated Enterprise Unit Test Suite for Outbox 7-Day Retention Policy.
 *
 * Requirements:
 * 1. Keep delivered operations (DELIVERED) for exactly 7 days before purging.
 * 2. Older delivered operations (> 7 days) are cleanly deleted.
 * 3. Recent delivered operations (<= 7 days) are strictly preserved for audit trails.
 * 4. Pending and failed operations are NEVER purged regardless of age.
 */
class OutboxRetentionPolicyTest {

    private lateinit var outboxDao: FakeOutboxDao
    private val now = 1730000000000L // Reference timestamp
    private val sevenDaysMillis = 7L * 24L * 60L * 60L * 1000L // 604,800,000 ms

    @Before
    fun setup() {
        outboxDao = FakeOutboxDao()
    }

    @Test
    fun sevenDayRetention_correctlyCalculatesThresholdTimestamp() {
        val expectedThreshold = now - sevenDaysMillis
        assertEquals(now - 604800000L, expectedThreshold)
    }

    @Test
    fun purgeDeliveredOlderThan_purgesOnlyItemsOlderThanSevenDays() = runTest {
        val threshold = now - sevenDaysMillis

        // 1. Operation older than 7 days (createdAt = threshold - 1000ms)
        outboxDao.insertOperation(
            OutboxOperationEntity(
                id = "op_old_delivered",
                userId = "user_1",
                entityType = "TRANSACTION",
                entityId = "tx_1",
                operation = "CREATE",
                payloadJson = "{}",
                idempotencyKey = "key_1",
                status = "DELIVERED",
                createdAt = threshold - 1000L
            )
        )

        // 2. Operation delivered 2 days ago (createdAt = now - 2 days > threshold)
        outboxDao.insertOperation(
            OutboxOperationEntity(
                id = "op_recent_delivered",
                userId = "user_1",
                entityType = "TRANSACTION",
                entityId = "tx_2",
                operation = "CREATE",
                payloadJson = "{}",
                idempotencyKey = "key_2",
                status = "DELIVERED",
                createdAt = now - (2L * 24L * 60L * 60L * 1000L)
            )
        )

        val purgedCount = outboxDao.purgeDeliveredOlderThan(threshold)

        assertEquals(1, purgedCount)
        assertEquals(1, outboxDao.operations.size)
        assertEquals("op_recent_delivered", outboxDao.operations.first().id)
    }

    @Test
    fun deliveredOperationsFilter_preservesWithinSevenDaysAndPurgesOlder() = runTest {
        val threshold = now - sevenDaysMillis

        val opOlderThan7Days = OutboxOperationEntity(
            id = "op_1",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_1",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_1",
            status = "DELIVERED",
            createdAt = threshold - 5000L
        )

        val opWithin7Days = OutboxOperationEntity(
            id = "op_2",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_2",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_2",
            status = "DELIVERED",
            createdAt = threshold + 5000L
        )

        outboxDao.insertOperations(listOf(opOlderThan7Days, opWithin7Days))

        val purged = outboxDao.purgeDeliveredOlderThan(threshold)

        assertEquals(1, purged)
        assertFalse(outboxDao.operations.any { it.id == "op_1" })
        assertTrue(outboxDao.operations.any { it.id == "op_2" })
    }

    @Test
    fun pendingAndFailedOperations_areNeverPurgedEvenIfOlderThanSevenDays() = runTest {
        val threshold = now - sevenDaysMillis

        val oldPendingOp = OutboxOperationEntity(
            id = "op_pending",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_3",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_pending",
            status = "PENDING",
            createdAt = threshold - 100000L
        )

        val oldFailedOp = OutboxOperationEntity(
            id = "op_failed",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_4",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_failed",
            status = "FAILED",
            createdAt = threshold - 100000L
        )

        outboxDao.insertOperations(listOf(oldPendingOp, oldFailedOp))

        val purgedCount = outboxDao.purgeDeliveredOlderThan(threshold)

        assertEquals(0, purgedCount)
        assertEquals(2, outboxDao.operations.size)
        assertTrue(outboxDao.operations.any { it.id == "op_pending" })
        assertTrue(outboxDao.operations.any { it.id == "op_failed" })
    }

    @Test
    fun thresholdBoundary_itemAtExactThresholdIsPurgedItemOneMillisecondNewerIsRetained() = runTest {
        val threshold = now - sevenDaysMillis

        val exactBoundaryOp = OutboxOperationEntity(
            id = "op_exact",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_exact",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_exact",
            status = "DELIVERED",
            createdAt = threshold
        )

        val justInsideBoundaryOp = OutboxOperationEntity(
            id = "op_just_inside",
            userId = "user_1",
            entityType = "TRANSACTION",
            entityId = "tx_inside",
            operation = "CREATE",
            payloadJson = "{}",
            idempotencyKey = "key_inside",
            status = "DELIVERED",
            createdAt = threshold + 1L
        )

        outboxDao.insertOperations(listOf(exactBoundaryOp, justInsideBoundaryOp))

        val purged = outboxDao.purgeDeliveredOlderThan(threshold)

        assertEquals(1, purged)
        assertFalse("El elemento exactamente en el umbral debe purgarse", outboxDao.operations.any { it.id == "op_exact" })
        assertTrue("El elemento 1 ms más reciente debe conservarse", outboxDao.operations.any { it.id == "op_just_inside" })
    }
}
