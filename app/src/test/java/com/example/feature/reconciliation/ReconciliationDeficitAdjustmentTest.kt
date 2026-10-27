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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.lang.reflect.Proxy

/**
 * Isolated Unit Test: Reconciliation with Negative Discrepancy (Deficit).
 * Verifies that an EXPENSE adjustment movement is created and linked to the audit log.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReconciliationDeficitAdjustmentTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun executeReconciliation_whenDeficit_createsExpenseMovementAndAuditedLog() = runTest {
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
            accountId = "acc_efectivo_01",
            accountName = "Efectivo Billetera",
            previousBalance = 500_000.0,
            reconciledRealBalance = 420_000.0,
            reconciliationDate = "2026-10-26",
            notes = "Gastos menores en efectivo no registrados",
            reconciledByUserId = "user_kash_01",
            reconciledByUserName = "Andrés Felipe",
            deviceModel = "Pixel 8 Pro"
        )

        val result = repository.executeReconciliation(params)

        assertTrue(result.success)
        assertEquals(ReconciliationStatus.ADJUSTED_DEFICIT, result.status)
        assertEquals(-80_000.0, result.discrepancyAmount, 0.0001)
        assertNotNull(result.adjustmentTransactionId)

        // Verify movement: Expense of 80,000
        assertNotNull(insertedTx)
        assertEquals("EXPENSE", insertedTx?.type)
        assertEquals(80_000.0, insertedTx?.amount ?: 0.0, 0.0001)
        assertEquals("cat_conciliacion", insertedTx?.categoryId)
        assertEquals("CONCILIACION,AUDITORIA", insertedTx?.tags)
        assertEquals(result.adjustmentTransactionId, insertedTx?.id)

        // Verify balance updated to 420,000
        assertEquals(420_000.0, updatedBalance ?: 0.0, 0.0001)

        // Verify audit log
        val auditLogs = fakeReconciliationDao.getAllLogs()
        assertEquals(1, auditLogs.size)
        val log = auditLogs.first()
        assertEquals("ADJUSTED_DEFICIT", log.status)
        assertEquals(500_000.0, log.previousSystemBalance, 0.0001)
        assertEquals(420_000.0, log.reconciledRealBalance, 0.0001)
        assertEquals(-80_000.0, log.discrepancyAmount, 0.0001)
        assertEquals(result.adjustmentTransactionId, log.adjustmentTransactionId)
    }
}
