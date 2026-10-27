package com.example.feature.reconciliation

import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.TransactionEntity
import com.example.feature.reconciliation.data.repository.ReconciliationRepositoryImpl
import com.example.feature.reconciliation.model.ReconciliationParams
import com.example.feature.sync.FakeOutboxDao
import com.example.feature.sync.engine.OutboxSyncEngine
import com.example.feature.sync.model.SyncEntityType
import com.example.feature.sync.model.SyncOperationType
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

/**
 * Isolated Unit Test: Transactional Outbox Enqueuing for Reconciliations.
 * Verifies that movements, account balance updates, and audit logs are queued for cloud synchronization.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReconciliationOutboxEnqueueTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun executeReconciliation_enqueuesTransactionAccountAndLogIntoOutbox() = runTest {
        val fakeReconciliationDao = FakeReconciliationDao()
        val fakeOutboxDao = FakeOutboxDao()

        val financialDaoProxy = Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "insertTransaction" -> Unit
                "setAccountBalanceDirectly" -> 1
                else -> null
            }
        } as FinancialDao

        val syncEngine = OutboxSyncEngine(
            outboxDao = fakeOutboxDao,
            financialDao = financialDaoProxy,
            fixedAssetDao = null,
            firebaseDataSource = object : FirebaseDataSource() {},
            cryptoManager = null,
            ioDispatcher = testDispatcher
        )

        val repository = ReconciliationRepositoryImpl(
            reconciliationDao = fakeReconciliationDao,
            financialDao = financialDaoProxy,
            outboxSyncEngine = syncEngine
        )

        val params = ReconciliationParams(
            userId = "user_kash_01",
            accountId = "acc_corriente_01",
            accountName = "Corriente Davivienda",
            previousBalance = 2_000_000.0,
            reconciledRealBalance = 2_150_000.0,
            reconciliationDate = "2026-10-26",
            notes = "Ajuste de corte mensual",
            reconciledByUserId = "user_kash_01",
            reconciledByUserName = "Andrés Felipe",
            deviceModel = "Samsung Tablet S9"
        )

        val result = repository.executeReconciliation(params)
        assertTrue(result.success)

        val pendingOps = fakeOutboxDao.getPendingOperations(System.currentTimeMillis() + 10_000L)

        // Should have 3 enqueued operations:
        // 1. Transaction creation (adjustment)
        // 2. Account balance update
        // 3. Reconciliation Log creation
        assertEquals(3, pendingOps.size)

        val txOp = pendingOps.find { it.entityType == SyncEntityType.TRANSACTION.value }
        assertNotNull(txOp)
        assertEquals(SyncOperationType.CREATE.value, txOp?.operation)
        assertTrue(txOp?.payloadJson?.contains("cat_conciliacion") == true)

        val accOp = pendingOps.find { it.entityType == SyncEntityType.ACCOUNT.value }
        assertNotNull(accOp)
        assertEquals(SyncOperationType.UPDATE.value, accOp?.operation)
        assertTrue(accOp?.payloadJson?.contains("2150000") == true)

        val logOp = pendingOps.find { it.entityType == SyncEntityType.RECONCILIATION_LOG.value }
        assertNotNull(logOp)
        assertEquals(SyncOperationType.CREATE.value, logOp?.operation)
        assertTrue(logOp?.payloadJson?.contains("ADJUSTED_SURPLUS") == true)
    }
}
