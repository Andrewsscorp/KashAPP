package com.example.feature.reconciliation

import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.FinancialDao
import com.example.feature.reconciliation.data.repository.ReconciliationRepositoryImpl
import com.example.feature.reconciliation.model.ReconciliationParams
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
 * Isolated Unit Test: Audit Trail Immutability and Metadata Completeness.
 * Verifies that all required banking-grade audit fields are preserved.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ReconciliationAuditTrailImmutabilityTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    @Test
    fun executeReconciliation_capturesCompleteAuditTrailMetadata() = runTest {
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
            userId = "usr_sox_compliance_42",
            accountId = "acc_safe_deposit",
            accountName = "Caja Fuerte Principal",
            previousBalance = 15_000_000.0,
            reconciledRealBalance = 14_950_000.0,
            reconciliationDate = "2026-10-26",
            notes = "Arqueo físico semanal realizado por tesorería",
            reconciledByUserId = "usr_auditor_07",
            reconciledByUserName = "Carlos Mendoza (Auditor)",
            deviceModel = "Lenovo Tab P12 Pro"
        )

        val result = repository.executeReconciliation(params)
        assertTrue(result.success)

        val log = fakeReconciliationDao.getReconciliationById(result.reconciliationLogId)
        assertNotNull(log)
        assertEquals("usr_sox_compliance_42", log?.userId)
        assertEquals("acc_safe_deposit", log?.accountId)
        assertEquals("Caja Fuerte Principal", log?.accountName)
        assertEquals(15_000_000.0, log?.previousSystemBalance ?: 0.0, 0.0001)
        assertEquals(14_950_000.0, log?.reconciledRealBalance ?: 0.0, 0.0001)
        assertEquals(-50_000.0, log?.discrepancyAmount ?: 0.0, 0.0001)
        assertEquals("2026-10-26", log?.reconciliationDate)
        assertNotNull(log?.reconciliationTime)
        assertEquals("usr_auditor_07", log?.reconciledByUserId)
        assertEquals("Carlos Mendoza (Auditor)", log?.reconciledByUserName)
        assertEquals("Lenovo Tab P12 Pro", log?.deviceModel)
        assertEquals("Arqueo físico semanal realizado por tesorería", log?.notes)
        assertEquals("ADJUSTED_DEFICIT", log?.status)
        assertTrue((log?.reconciliationTimestamp ?: 0L) > 0L)
    }
}
