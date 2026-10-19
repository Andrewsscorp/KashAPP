package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.OutboxOperationEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.domain.security.FirebaseApiProtectionGuard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class PlayIntegrityNetworkAndOutboxSecurityTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUserId = "user_net_sec_99"

    private class ControlledFirebaseDataSource(
        context: Context,
        var shouldSucceedSync: Boolean = true
    ) : FirebaseDataSource(context) {
        var syncedOperations = mutableListOf<OutboxOperationEntity>()

        override suspend fun syncOutboxOperation(operation: OutboxOperationEntity): Result<Unit> {
            return if (shouldSucceedSync) {
                syncedOperations.add(operation)
                Result.success(Unit)
            } else {
                Result.failure(IllegalStateException("Simulated network timeout connecting to Firestore"))
            }
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()

        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .setQueryExecutor { it.run() }
            .setTransactionExecutor { it.run() }
            .build()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun testNetworkSecurityConfig_existsAndForbidsCleartextTraffic() {
        val configFile = listOf(
            File("app/src/main/res/xml/network_security_config.xml"),
            File("src/main/res/xml/network_security_config.xml"),
            File("../app/src/main/res/xml/network_security_config.xml")
        ).firstOrNull { it.exists() }

        assertNotNull("network_security_config.xml must exist in res/xml", configFile)
        val content = configFile!!.readText(Charsets.UTF_8)
        assertTrue("Cleartext traffic must be strictly forbidden", content.contains("cleartextTrafficPermitted=\"false\""))

        val manifestFile = listOf(
            File("app/src/main/AndroidManifest.xml"),
            File("src/main/AndroidManifest.xml"),
            File("../app/src/main/AndroidManifest.xml")
        ).firstOrNull { it.exists() }

        assertNotNull("AndroidManifest.xml must exist", manifestFile)
        val manifestContent = manifestFile!!.readText(Charsets.UTF_8)
        assertTrue("AndroidManifest must declare networkSecurityConfig", manifestContent.contains("android:networkSecurityConfig=\"@xml/network_security_config\""))
    }

    @Test
    fun testFirebaseAiProtection_blocksUnauthenticatedCallsWhenRequired() {
        // When requireAuthenticatedUser = true and no session is active, calls to cloud AI must be blocked
        val verdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = "Analiza este gasto de almuerzo",
            participantRealNames = emptyList(),
            requireAuthenticatedUser = true
        )

        assertTrue("Unauthenticated call must be blocked", verdict is FirebaseApiProtectionGuard.GuardVerdict.Blocked)
        val blocked = verdict as FirebaseApiProtectionGuard.GuardVerdict.Blocked
        assertEquals("UNAUTHENTICATED_SESSION", blocked.reasonCode)
    }

    @Test
    fun testExchangeRateValidation_rejectsNegativeNaNAndExtremeRates() = runTest {
        val fakeFirebase = ControlledFirebaseDataSource(context)
        val repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebase
        )

        // 1. Negative rate
        val resNegative = repository.createCurrency(testUserId, "XAU", "Oro", "$", -150.0)
        assertTrue("Negative rate must be rejected", resNegative.isFailure)

        // 2. Zero rate
        val resZero = repository.createCurrency(testUserId, "XAU", "Oro", "$", 0.0)
        assertTrue("Zero rate must be rejected", resZero.isFailure)

        // 3. NaN rate
        val resNan = repository.createCurrency(testUserId, "XAU", "Oro", "$", Double.NaN)
        assertTrue("NaN rate must be rejected", resNan.isFailure)

        // 4. Infinite rate
        val resInf = repository.createCurrency(testUserId, "XAU", "Oro", "$", Double.POSITIVE_INFINITY)
        assertTrue("Infinite rate must be rejected", resInf.isFailure)

        // 5. Extreme unrealistic rate (>= 1,000,000,000)
        val resExtreme = repository.createCurrency(testUserId, "XAU", "Oro", "$", 1_000_000_000.0)
        assertTrue("Extreme rate >= 1B must be rejected", resExtreme.isFailure)

        // 6. Valid rate succeeds
        val resValid = repository.createCurrency(testUserId, "XAU", "Oro", "$", 4150.0)
        assertTrue("Valid positive finite rate must succeed", resValid.isSuccess)
    }

    @Test
    fun testProcessOutboxQueue_performsRealNetworkSyncAndExponentialBackoff() = runTest {
        val outboxDao = database.outboxDao()
        val fakeFirebase = ControlledFirebaseDataSource(context, shouldSucceedSync = true)

        val repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebase,
            outboxDao = outboxDao
        )

        val op1 = OutboxOperationEntity(
            id = "op_sync_01",
            userId = testUserId,
            entityType = "TRANSACTION",
            entityId = "tx_101",
            operation = "CREATE",
            payloadJson = "{\"amount\":50000}",
            idempotencyKey = "idem_sync_01"
        )
        outboxDao.insertOperation(op1)

        // 1. Successful sync
        val processedCount = repository.processOutboxQueue()
        assertTrue(processedCount.isSuccess)
        assertEquals(1, processedCount.getOrNull())
        assertEquals(1, fakeFirebase.syncedOperations.size)

        // Check delivered status
        val remainingPending = outboxDao.getPendingOperations()
        assertTrue("Delivered operation must no longer be pending", remainingPending.isEmpty())

        // 2. Failure & Backoff test
        val fakeFirebaseFailing = ControlledFirebaseDataSource(context, shouldSucceedSync = false)
        val failingRepo = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseFailing,
            outboxDao = outboxDao
        )

        val op2 = OutboxOperationEntity(
            id = "op_sync_02",
            userId = testUserId,
            entityType = "ACCOUNT",
            entityId = "acc_202",
            operation = "UPDATE",
            payloadJson = "{\"balance\":100000}",
            idempotencyKey = "idem_sync_02",
            retryCount = 0
        )
        outboxDao.insertOperation(op2)

        val nowBefore = System.currentTimeMillis()
        val failResult = failingRepo.processOutboxQueue()
        assertTrue(failResult.isSuccess) // Queue processor itself handled error per operation
        assertEquals(0, failResult.getOrNull())

        // Inspect operation state in DB
        val pendingAfterFail = outboxDao.getPendingOperations(currentTime = Long.MAX_VALUE)
        assertEquals(1, pendingAfterFail.size)
        val failedOp = pendingAfterFail.first()
        assertEquals(1, failedOp.retryCount)
        assertTrue("nextRetryAt must be in the future with exponential backoff", failedOp.nextRetryAt >= nowBefore + 1000L)
        assertNotNull(failedOp.lastError)
        assertTrue(failedOp.lastError!!.contains("Simulated network timeout"))
    }
}
