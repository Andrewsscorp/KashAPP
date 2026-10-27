package com.example.feature.capturaautomatica

import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.AutoCaptureDao
import com.example.data.local.dao.MerchantDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.capturaautomatica.data.repository.AutoCaptureRepositoryImpl
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.UUID

class AutoCaptureRepositoryTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var fakeAutoCaptureDao: FakeAutoCaptureDao
    private lateinit var fakeMerchantDao: FakeMerchantDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao

    private val fakeAccounts = mutableListOf<AccountEntity>()
    private val fakeCategories = mutableListOf<CategoryEntity>()
    private val fakeTransactions = mutableListOf<TransactionEntity>()

    private lateinit var repository: AutoCaptureRepositoryImpl

    private val testUserId = "user_test_123"

    @Before
    fun setup() {
        fakeAutoCaptureDao = FakeAutoCaptureDao()
        fakeMerchantDao = FakeMerchantDao()
        fakeAuditLogDao = FakeAuditLogDao()

        fakeAccounts.clear()
        fakeCategories.clear()
        fakeTransactions.clear()

        // Seed account
        fakeAccounts.add(
            AccountEntity(
                id = "acc_bancolombia_1",
                userId = testUserId,
                name = "Ahorros Bancolombia",
                balance = 500000.0,
                currency = "COP",
                bankName = "Bancolombia",
                accountNumberMasked = "••••4821",
                favoriteForExpenses = true
            )
        )

        // Seed category
        fakeCategories.add(
            CategoryEntity(
                id = "cat_mercado",
                name = "Mercado",
                type = "EXPENSE"
            )
        )

        repository = AutoCaptureRepositoryImpl(
            autoCaptureDao = fakeAutoCaptureDao,
            financialDao = null,
            merchantDao = fakeMerchantDao,
            auditSecurityLogDao = fakeAuditLogDao,
            accountProvider = { userId -> fakeAccounts.filter { it.userId == userId } },
            categoryProvider = { fakeCategories },
            recordExpense = { userId, accountId, accountName, categoryId, categoryName, amount, dateStr, desc, idempotencyKey, tags ->
                val txId = "tx_${UUID.randomUUID().toString().take(8)}"
                fakeTransactions.add(
                    TransactionEntity(
                        id = txId,
                        userId = userId,
                        accountId = accountId,
                        accountName = accountName,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        amount = amount,
                        currency = "COP",
                        type = "EXPENSE",
                        date = dateStr,
                        description = desc,
                        receiptUrl = null,
                        idempotencyKey = idempotencyKey,
                        tags = tags.joinToString(",")
                    )
                )
                val accIdx = fakeAccounts.indexOfFirst { it.id == accountId }
                if (accIdx != -1) {
                    val acc = fakeAccounts[accIdx]
                    fakeAccounts[accIdx] = acc.copy(balance = acc.balance - amount)
                }
                Result.success(txId)
            },
            recordIncome = { userId, accountId, accountName, categoryId, categoryName, amount, dateStr, desc, idempotencyKey, tags ->
                val txId = "tx_${UUID.randomUUID().toString().take(8)}"
                fakeTransactions.add(
                    TransactionEntity(
                        id = txId,
                        userId = userId,
                        accountId = accountId,
                        accountName = accountName,
                        categoryId = categoryId,
                        categoryName = categoryName,
                        amount = amount,
                        currency = "COP",
                        type = "INCOME",
                        date = dateStr,
                        description = desc,
                        receiptUrl = null,
                        idempotencyKey = idempotencyKey,
                        tags = tags.joinToString(",")
                    )
                )
                val accIdx = fakeAccounts.indexOfFirst { it.id == accountId }
                if (accIdx != -1) {
                    val acc = fakeAccounts[accIdx]
                    fakeAccounts[accIdx] = acc.copy(balance = acc.balance + amount)
                }
                Result.success(txId)
            },
            ioDispatcher = testDispatcher
        )
    }

    @Test
    fun testProcessNotification_success_createsPendingCapture() = runTest(testDispatcher) {
        val packageName = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821"

        val result = repository.processNotification(
            packageName = packageName,
            title = title,
            text = text,
            postTime = 1776600000000L,
            userId = testUserId
        )

        assertTrue(result.isSuccess)
        val capture = result.getOrNull()
        assertNotNull(capture)
        assertEquals(48500.0, capture!!.amount, 0.01)
        assertEquals("D1 TUNJA CENTRO", capture.merchantOrCounterparty)
        assertEquals("acc_bancolombia_1", capture.matchedAccountId)
        assertEquals("cat_mercado", capture.matchedCategoryId)
        assertEquals("PENDING", capture.status)
        assertEquals(1, fakeAutoCaptureDao.captures.size)
    }

    @Test
    fun testProcessNotification_idempotency_preventsDuplicate() = runTest(testDispatcher) {
        val packageName = "com.todo1.mobile"
        val title = "Bancolombia"
        val text = "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821"

        val first = repository.processNotification(packageName, title, text, 1776600000000L, testUserId)
        assertTrue(first.isSuccess)
        assertNotNull(first.getOrNull())

        // Same notification within bucket -> should return null
        val second = repository.processNotification(packageName, title, text, 1776600010000L, testUserId)
        assertTrue(second.isSuccess)
        assertNull(second.getOrNull())
        assertEquals(1, fakeAutoCaptureDao.captures.size)
    }

    @Test
    fun testConfirmCapture_success_createsTransactionAndUpdatesBalance() = runTest(testDispatcher) {
        val processRes = repository.processNotification(
            "com.todo1.mobile",
            "Bancolombia",
            "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821",
            1776600000000L,
            testUserId
        )
        val capture = processRes.getOrNull()!!

        val confirmRes = repository.confirmCapture(capture.id, testUserId)
        assertTrue(confirmRes.isSuccess)

        // Check capture updated to CONFIRMED
        val updated = fakeAutoCaptureDao.captures.find { it.id == capture.id }
        assertNotNull(updated)
        assertEquals("CONFIRMED", updated!!.status)
        assertNotNull(updated.linkedTransactionId)

        // Check transaction recorded
        assertEquals(1, fakeTransactions.size)
        val tx = fakeTransactions.first()
        assertEquals(48500.0, tx.amount, 0.01)
        assertEquals("EXPENSE", tx.type)

        // Check account balance deducted: 500,000 - 48,500 = 451,500
        val acc = fakeAccounts.first()
        assertEquals(451500.0, acc.balance, 0.01)

        // Check audit log
        assertTrue(fakeAuditLogDao.logs.any { it.eventType == "AUTO_CAPTURE_CONFIRMED" })
    }

    @Test
    fun testDismissCapture_success_marksStatusDismissed() = runTest(testDispatcher) {
        val processRes = repository.processNotification(
            "com.todo1.mobile",
            "Bancolombia",
            "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821",
            1776600000000L,
            testUserId
        )
        val capture = processRes.getOrNull()!!

        val dismissRes = repository.dismissCapture(capture.id, testUserId)
        assertTrue(dismissRes.isSuccess)

        val updated = fakeAutoCaptureDao.captures.find { it.id == capture.id }
        assertNotNull(updated)
        assertEquals("DISMISSED", updated!!.status)
        assertEquals(0, fakeTransactions.size)
        assertTrue(fakeAuditLogDao.logs.any { it.eventType == "AUTO_CAPTURE_DISMISSED" })
    }

    @Test
    fun testUpdateDraftSelection_persistsDraftChanges() = runTest(testDispatcher) {
        val processRes = repository.processNotification(
            "com.todo1.mobile",
            "Bancolombia",
            "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821",
            1776600000000L,
            testUserId
        )
        val capture = processRes.getOrNull()!!

        val updateRes = repository.updateDraftSelection(
            captureId = capture.id,
            userId = testUserId,
            accountId = "acc_custom_draft",
            accountName = "Cuenta Borrador",
            categoryId = "cat_custom_draft",
            categoryName = "Mercado Draft"
        )
        assertTrue(updateRes.isSuccess)

        val updated = fakeAutoCaptureDao.captures.find { it.id == capture.id }
        assertNotNull(updated)
        assertEquals("acc_custom_draft", updated!!.matchedAccountId)
        assertEquals("Cuenta Borrador", updated.matchedAccountName)
        assertEquals("cat_custom_draft", updated.matchedCategoryId)
        assertEquals("Mercado Draft", updated.matchedCategoryName)
    }

    @Test
    fun testConfirmCapture_withCustomTags_forwardsTagsToTransaction() = runTest(testDispatcher) {
        val processRes = repository.processNotification(
            "com.todo1.mobile",
            "Bancolombia",
            "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 19/10/2026 T.Deb *4821",
            1776600000000L,
            testUserId
        )
        val capture = processRes.getOrNull()!!

        val confirmRes = repository.confirmCapture(
            captureId = capture.id,
            userId = testUserId,
            customTags = listOf("Supermercado", "Quincena")
        )
        assertTrue(confirmRes.isSuccess)

        val createdTx = fakeTransactions.firstOrNull()
        assertNotNull(createdTx)
        assertEquals("Supermercado,Quincena", createdTx!!.tags)
    }
}

// =========================================================================
// Fake Implementations for isolated repository testing
// =========================================================================

private class FakeAutoCaptureDao : AutoCaptureDao {
    val captures = mutableListOf<PendingAutoCaptureEntity>()

    override fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>> {
        return flowOf(captures.filter { it.userId == userId && it.status == "PENDING" })
    }

    override fun observePendingCount(userId: String): Flow<Int> {
        return flowOf(captures.count { it.userId == userId && it.status == "PENDING" })
    }

    override suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity? {
        return captures.find { it.id == id && it.userId == userId }
    }

    override suspend fun existsByIdempotencyKey(idempotencyKey: String): Boolean {
        return captures.any { it.idempotencyKey == idempotencyKey }
    }

    override suspend fun insertPendingCapture(entity: PendingAutoCaptureEntity): Long {
        if (captures.any { it.idempotencyKey == entity.idempotencyKey }) {
            return -1L
        }
        captures.add(entity)
        return captures.size.toLong()
    }

    override suspend fun updateCaptureStatus(id: String, userId: String, status: String, linkedTransactionId: String?) {
        val index = captures.indexOfFirst { it.id == id && it.userId == userId }
        if (index != -1) {
            val old = captures[index]
            captures[index] = old.copy(status = status, linkedTransactionId = linkedTransactionId)
        }
    }

    override suspend fun updateDraftSelection(
        id: String,
        userId: String,
        accountId: String?,
        accountName: String?,
        categoryId: String?,
        categoryName: String?
    ) {
        val index = captures.indexOfFirst { it.id == id && it.userId == userId }
        if (index != -1) {
            val old = captures[index]
            captures[index] = old.copy(
                matchedAccountId = accountId,
                matchedAccountName = accountName,
                matchedCategoryId = categoryId,
                matchedCategoryName = categoryName
            )
        }
    }

    override suspend fun deleteAllByUserId(userId: String) {
        captures.removeAll { it.userId == userId }
    }

    override suspend fun getAllCapturesByUserId(userId: String): List<PendingAutoCaptureEntity> {
        return captures.filter { it.userId == userId }
    }

    override suspend fun deleteCaptureById(id: String, userId: String) {
        captures.removeAll { it.id == id && it.userId == userId }
    }
}

private class FakeMerchantDao : MerchantDao {
    val merchants = mutableListOf<MerchantEntity>()

    override suspend fun findMerchantByNameOrAlias(name: String): MerchantEntity? {
        return merchants.find { it.canonicalName.equals(name, ignoreCase = true) || it.alias.equals(name, ignoreCase = true) }
    }

    override suspend fun insertMerchant(merchant: MerchantEntity) {
        merchants.add(merchant)
    }

    override suspend fun insertMerchants(merchants: List<MerchantEntity>) {
        this.merchants.addAll(merchants)
    }

    override suspend fun updateMerchant(merchant: MerchantEntity) {}
    override suspend fun setMerchantActive(id: String, isActive: Boolean, updatedAt: Long) {}
    override suspend fun getCount(): Int = merchants.size
    override suspend fun deleteMerchantsByUserId(userId: String): Int {
        val before = merchants.size
        merchants.removeAll { it.userId == userId }
        return before - merchants.size
    }

    override fun getActiveMerchantsFlow(userId: String): Flow<List<MerchantEntity>> = flowOf(merchants.filter { it.isActive })
    override fun getAllMerchantsFlow(userId: String): Flow<List<MerchantEntity>> = flowOf(merchants)
    override suspend fun getMerchantById(id: String): MerchantEntity? = merchants.find { it.id == id }
}

private class FakeAuditLogDao : AuditSecurityLogDao {
    val logs = mutableListOf<AuditSecurityLog>()

    override suspend fun insertLog(log: AuditSecurityLog) {
        logs.add(log)
    }

    override fun getAllLogs(): Flow<List<AuditSecurityLog>> = flowOf(logs)
    override suspend fun getAllLogsList(): List<AuditSecurityLog> = logs
    override fun getRecentLogs(limit: Int): Flow<List<AuditSecurityLog>> = flowOf(logs.takeLast(limit))
    override fun getAllLogsByUser(userId: String): Flow<List<AuditSecurityLog>> = flowOf(logs.filter { it.userId == userId })
    override fun getRecentLogsByUser(userId: String, limit: Int): Flow<List<AuditSecurityLog>> = flowOf(logs.filter { it.userId == userId }.takeLast(limit))
}
