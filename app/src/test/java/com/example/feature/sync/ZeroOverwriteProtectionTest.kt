package com.example.feature.sync

import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountEntity
import com.example.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.lang.reflect.Proxy

/**
 * Isolated Enterprise Unit Test: Zero-Overwrite Cloud Safety Guard.
 *
 * Verifies that an empty or reinstalled local database with $0 default dummy accounts
 * CANNOT overwrite a populated user backup on Firebase, completely eliminating data loss.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class ZeroOverwriteProtectionTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    class StubCloudDataSource(
        val existingCloudBackup: Map<String, Any>?
    ) : FirebaseDataSource() {
        var syncOfflineCalled = false
        var overwrittenEnvelope: Map<String, Any>? = null

        override fun getCurrentUserEmail(): String? = "andres@gmail.com"
        override fun getCurrentUserId(): String? = "usr_andres_firebase"

        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? {
            return existingCloudBackup
        }

        override suspend fun syncOfflineFinancialBackup(userId: String, envelope: Map<String, Any>): Boolean {
            syncOfflineCalled = true
            overwrittenEnvelope = envelope
            return true
        }
    }

    @Test
    fun backupFullPortfolio_whenLocalDatabaseEmptyAndRemoteHasData_abortsAndRefusesToOverwrite() = runTest {
        val userId = "usr_test_andres"

        // 1. Existing remote backup in Firebase has 18 records (accounts, transactions, assets)
        val populatedRemoteBackup = mapOf<String, Any>(
            "schemaVersion" to 2,
            "recordCount" to 18,
            "encryptedPayload" to "dummy_encrypted_payload_with_real_user_data",
            "updatedAt" to System.currentTimeMillis() - 100000
        )
        val stubFirebase = StubCloudDataSource(existingCloudBackup = populatedRemoteBackup)

        // 2. Local device only has default dummy empty accounts ($0 balance) after fresh reinstall
        val localDummyAccounts = listOf(
            AccountEntity(id = "acc_ahorros", userId = userId, name = "Ahorros", balance = 0.0, availableBalance = 0.0, type = "SAVINGS", currency = "COP"),
            AccountEntity(id = "acc_corriente", userId = userId, name = "Corriente", balance = 0.0, availableBalance = 0.0, type = "CHECKING", currency = "COP")
        )

        val financialDaoProxy = Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAccounts" -> localDummyAccounts
                "getTransactions" -> emptyList<com.example.data.local.entity.TransactionEntity>()
                "getAccountCreditCardsByUser" -> emptyList<com.example.data.local.entity.AccountCreditCardEntity>()
                "getCategories" -> emptyList<com.example.data.local.entity.CategoryEntity>()
                "getGoals" -> emptyList<com.example.data.local.entity.FinancialGoalEntity>()
                "getAllBudgetsWithItems" -> emptyList<com.example.data.local.entity.BudgetWithItems>()
                "getScheduledPayments" -> emptyList<com.example.data.local.entity.ScheduledPaymentEntity>()
                "getAccountCdtsByUser" -> emptyList<com.example.data.local.entity.AccountCdtEntity>()
                else -> null
            }
        } as FinancialDao

        val auditLogDaoProxy = Proxy.newProxyInstance(
            AuditSecurityLogDao::class.java.classLoader,
            arrayOf(AuditSecurityLogDao::class.java)
        ) { _, _, _ -> null } as AuditSecurityLogDao

        val repository = FinancialRepositoryImpl(
            financialDao = financialDaoProxy,
            auditLogDao = auditLogDaoProxy,
            firebaseDataSource = stubFirebase,
            ioDispatcher = testDispatcher
        )

        // 3. Trigger cloud backup from clean install
        val result = repository.backupFullPortfolioToCloud(userId)

        // 4. Verification:
        // - Result must be a failure (or aborted)
        // - Firestore remote backup must NOT be overwritten
        assertTrue("El respaldo debió fallar por protección anti-sobrescritura", result.isFailure)
        assertFalse("syncOfflineFinancialBackup jamás debe ser llamado cuando el local está vacío y remoto poblado", stubFirebase.syncOfflineCalled)
        val exception = result.exceptionOrNull()
        assertNotNull(exception)
        assertTrue(
            "El mensaje de error debe indicar la protección anti-sobrescritura",
            exception?.message?.contains("Zero-overwrite guard") == true
        )
    }
}
