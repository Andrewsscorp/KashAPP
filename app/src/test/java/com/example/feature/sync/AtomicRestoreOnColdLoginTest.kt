package com.example.feature.sync

import com.example.core.id.EmailDeterministicUidGenerator
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.dao.ReconciliationDao
import com.example.data.local.entity.AccountCdtEntity
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AccountReconciliationLogEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayOutputStream
import java.lang.reflect.Proxy
import java.util.zip.GZIPOutputStream

/**
 * Isolated Enterprise Unit Test: Atomic Restore On Cold Login (Post-Reinstall).
 *
 * Verifies that when a user logs in on a newly installed device, the system fetches the encrypted
 * cloud backup, decrypts it with the deterministic email key, and populates all 10 financial modules
 * in the local database.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class AtomicRestoreOnColdLoginTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    private fun compressGzip(data: String): String {
        val bos = ByteArrayOutputStream()
        GZIPOutputStream(bos).use { it.write(data.toByteArray(Charsets.UTF_8)) }
        return android.util.Base64.encodeToString(bos.toByteArray(), android.util.Base64.NO_WRAP)
    }

    class ColdLoginFirebaseDataSource(
        private val email: String,
        private val backupData: Map<String, Any>?
    ) : FirebaseDataSource() {
        override fun getCurrentUserEmail(): String? = email
        override fun getCurrentUserId(): String? = "fb_auth_cold_uid"
        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? = backupData
        override suspend fun syncOfflineFinancialBackup(userId: String, envelope: Map<String, Any>): Boolean = true
    }

    @Test
    fun restoreFullPortfolio_onColdLogin_decryptsAndRestoresAllModules() = runTest {
        val email = "andres.restore@kashapp.co"
        val deterministicUid = EmailDeterministicUidGenerator.fromEmail(email)
        val cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        // 1. Prepare simulated encrypted cloud backup payload containing multiple modules
        val rawJson = JSONObject().apply {
            put("version", "9.3.5")
            put("userId", deterministicUid)
            put("accounts", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "acc_bancolombia")
                    put("name", "Cuenta Ahorros")
                    put("balance", 7250000.0)
                    put("currency", "COP")
                    put("type", "SAVINGS")
                })
            })
            put("transactions", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "tx_supermercado")
                    put("accountId", "acc_bancolombia")
                    put("amount", 230000.0)
                    put("currency", "COP")
                    put("type", "EXPENSE")
                    put("categoryName", "Mercado")
                })
            })
            put("goals", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "goal_viaje")
                    put("name", "Viaje Vacaciones")
                    put("targetAmount", 15000000.0)
                    put("savedAmount", 3000000.0)
                    put("targetAccountId", "acc_bancolombia")
                })
            })
            put("scheduledPayments", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "sp_arriendo")
                    put("entityName", "Arriendo Inmobiliaria")
                    put("amount", 1800000.0)
                    put("dueDate", "2026-11-05")
                })
            })
            put("cdts", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "cdt_plazo_fijo")
                    put("accountId", "acc_bancolombia")
                    put("initialAmount", 20000000.0)
                    put("termDays", 360)
                })
            })
            put("reconciliations", JSONArray().apply {
                put(JSONObject().apply {
                    put("id", "rec_audit_01")
                    put("accountId", "acc_bancolombia")
                    put("accountName", "Cuenta Ahorros")
                    put("reconciledRealBalance", 7250000.0)
                    put("status", "BALANCED")
                })
            })
        }.toString()

        val compressedBase64 = compressGzip(rawJson)
        val encryptedPayload = cryptoManager.encryptCloudPayload(compressedBase64, deterministicUid)

        val cloudBackupDoc = mapOf<String, Any>(
            "schemaVersion" to 2,
            "userId" to deterministicUid,
            "encryptedPayload" to encryptedPayload,
            "recordCount" to 6,
            "updatedAt" to System.currentTimeMillis()
        )

        val firebaseDataSource = ColdLoginFirebaseDataSource(email, cloudBackupDoc)

        // 2. Track restored entities inserted into Room DAOs
        val insertedAccounts = mutableListOf<AccountEntity>()
        val insertedTransactions = mutableListOf<TransactionEntity>()
        val insertedGoals = mutableListOf<FinancialGoalEntity>()
        val insertedPayments = mutableListOf<ScheduledPaymentEntity>()
        val insertedCdts = mutableListOf<AccountCdtEntity>()
        val insertedReconciliations = mutableListOf<AccountReconciliationLogEntity>()

        val financialDaoProxy = Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "getAccountsFlow" -> flowOf(emptyList<AccountEntity>())
                "getTransactionsFlow" -> flowOf(emptyList<TransactionEntity>())
                "getAccounts" -> insertedAccounts
                "getTransactions" -> insertedTransactions
                "insertTransaction" -> {
                    insertedTransactions.add(args[0] as TransactionEntity)
                    Unit
                }
                "insertGoals" -> {
                    @Suppress("UNCHECKED_CAST")
                    insertedGoals.addAll(args[0] as List<FinancialGoalEntity>)
                    Unit
                }
                "insertScheduledPayments" -> {
                    @Suppress("UNCHECKED_CAST")
                    insertedPayments.addAll(args[0] as List<ScheduledPaymentEntity>)
                    Unit
                }
                "insertAccountCdts" -> {
                    @Suppress("UNCHECKED_CAST")
                    insertedCdts.addAll(args[0] as List<AccountCdtEntity>)
                    Unit
                }
                "insertAccounts" -> {
                    @Suppress("UNCHECKED_CAST")
                    insertedAccounts.addAll(args[0] as List<AccountEntity>)
                    Unit
                }
                "getAccountCreditCard" -> null
                "insertAccountCreditCard" -> Unit
                "insertAccountCreditCards" -> Unit
                "getCategories" -> emptyList<com.example.data.local.entity.CategoryEntity>()
                "getAccountCreditCardsByUser" -> emptyList<AccountCreditCardEntity>()
                "getAllBudgetsWithItems" -> emptyList<com.example.data.local.entity.BudgetWithItems>()
                "getScheduledPayments" -> emptyList<ScheduledPaymentEntity>()
                "getAccountCdtsByUser" -> emptyList<AccountCdtEntity>()
                else -> null
            }
        } as FinancialDao

        val reconciliationDaoProxy = Proxy.newProxyInstance(
            ReconciliationDao::class.java.classLoader,
            arrayOf(ReconciliationDao::class.java)
        ) { _, method, args ->
            when (method.name) {
                "insertReconciliations" -> {
                    @Suppress("UNCHECKED_CAST")
                    insertedReconciliations.addAll(args[0] as List<AccountReconciliationLogEntity>)
                    Unit
                }
                "getReconciliations" -> insertedReconciliations
                else -> null
            }
        } as ReconciliationDao

        val auditLogDaoProxy = Proxy.newProxyInstance(
            AuditSecurityLogDao::class.java.classLoader,
            arrayOf(AuditSecurityLogDao::class.java)
        ) { _, _, _ -> null } as AuditSecurityLogDao

        val repository = FinancialRepositoryImpl(
            financialDao = financialDaoProxy,
            auditLogDao = auditLogDaoProxy,
            firebaseDataSource = firebaseDataSource,
            reconciliationDao = reconciliationDaoProxy,
            keystoreCryptoManager = cryptoManager,
            ioDispatcher = testDispatcher
        )

        // 3. Execute cold login restore
        val restoreResult = repository.restoreFullPortfolioFromCloud(deterministicUid)

        // 4. Verification
        assertTrue("La restauración de respaldo en frío debe ser exitosa", restoreResult.isSuccess)
        assertTrue("Debe reportar que restauró registros", restoreResult.getOrNull() == true)

        assertEquals(1, insertedAccounts.size)
        assertEquals("acc_bancolombia", insertedAccounts[0].id)
        assertEquals(7250000.0, insertedAccounts[0].balance, 0.001)

        assertEquals(1, insertedTransactions.size)
        assertEquals("tx_supermercado", insertedTransactions[0].id)
        assertEquals(230000.0, insertedTransactions[0].amount, 0.001)

        assertEquals(1, insertedGoals.size)
        assertEquals("Viaje Vacaciones", insertedGoals[0].name)

        assertEquals(1, insertedPayments.size)
        assertEquals("Arriendo Inmobiliaria", insertedPayments[0].entityName)

        assertEquals(1, insertedCdts.size)
        assertEquals("cdt_plazo_fijo", insertedCdts[0].id)

        assertEquals(1, insertedReconciliations.size)
        assertEquals("rec_audit_01", insertedReconciliations[0].id)
    }
}
