package com.example.feature.reconciliation

import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.TransactionEntity
import com.example.feature.reconciliation.data.repository.ReconciliationRepositoryImpl
import com.example.feature.reconciliation.model.ReconciliationParams
import com.example.feature.reconciliation.model.ReconciliationStatus
import com.example.feature.sync.FakeOutboxDao
import com.example.feature.sync.engine.OutboxSyncEngine
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

/**
 * Isolated Unit Test: Reconciliation with Zero Discrepancy.
 * Verifies that no adjustment movement is created, but the audit certificate is issued.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReconciliationZeroDifferenceTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun executeReconciliation_whenZeroDifference_createsAuditLogWithoutMovement() = runTest {
        val fakeReconciliationDao = FakeReconciliationDao()
        val fakeOutboxDao = FakeOutboxDao()
        var insertedTx: TransactionEntity? = null
        var updatedBalance: Double? = null

        val financialDaoProxy = Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "insertTransaction" -> {
                    insertedTx = args[0] as TransactionEntity
                    Unit
                }
                "setAccountBalanceDirectly" -> {
                    updatedBalance = args[1] as Double
                    1
                }
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
            accountId = "acc_bancolombia_01",
            accountName = "Bancolombia Ahorros",
            previousBalance = 3_500_000.0,
            reconciledRealBalance = 3_500_000.0,
            reconciliationDate = "2026-10-26",
            notes = "Extracto de octubre revisado - Saldo exacto",
            reconciledByUserId = "user_kash_01",
            reconciledByUserName = "Andrés Felipe",
            deviceModel = "Samsung Tablet S9"
        )

        val result = repository.executeReconciliation(params)

        assertTrue(result.success)
        assertEquals(ReconciliationStatus.BALANCED, result.status)
        assertEquals(0.0, result.discrepancyAmount, 0.0001)
        assertNull(result.adjustmentTransactionId)
        assertNull(insertedTx)
        assertEquals(3_500_000.0, updatedBalance ?: 0.0, 0.0001)

        val auditLogs = fakeReconciliationDao.getAllLogs()
        assertEquals(1, auditLogs.size)
        val log = auditLogs.first()
        assertEquals("BALANCED", log.status)
        assertEquals(3_500_000.0, log.previousSystemBalance, 0.0001)
        assertEquals(3_500_000.0, log.reconciledRealBalance, 0.0001)
        assertEquals(0.0, log.discrepancyAmount, 0.0001)
        assertNull(log.adjustmentTransactionId)
    }
}
