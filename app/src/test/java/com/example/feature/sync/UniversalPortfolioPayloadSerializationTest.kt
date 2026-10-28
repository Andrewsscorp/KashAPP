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
import com.example.data.local.entity.AssetMovementEntity
import com.example.data.local.entity.BudgetEntity
import com.example.data.local.entity.BudgetItemEntity
import com.example.data.local.entity.BudgetWithItems
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.FinancialGoalEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.ScheduledPaymentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.lang.reflect.Proxy
import java.util.zip.GZIPInputStream

/**
 * Isolated Enterprise Unit Test: Universal Portfolio 10-Module E2EE Serialization.
 *
 * Verifies that all 10 financial modules are serialized into the E2EE cloud backup
 * and can be cleanly decrypted and validated with the email-derived deterministic key.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class UniversalPortfolioPayloadSerializationTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    class CapturingFirebaseDataSource(private val email: String) : FirebaseDataSource() {
        var lastSyncedUserId: String? = null
        var lastEnvelope: Map<String, Any>? = null

        override fun getCurrentUserEmail(): String? = email
        override fun getCurrentUserId(): String? = "fb_temp_uid_999"

        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? = null

        override suspend fun syncOfflineFinancialBackup(userId: String, envelope: Map<String, Any>): Boolean {
            lastSyncedUserId = userId
            lastEnvelope = envelope
            return true
        }
    }

    @Test
    fun backupFullPortfolio_serializesAll10ModulesWithDeterministicEmailKey() = runTest {
        val email = "andres.portfolio@kashapp.co"
        val expectedEmailUid = EmailDeterministicUidGenerator.fromEmail(email)
        val firebaseDataSource = CapturingFirebaseDataSource(email)
        val cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)

        val account = AccountEntity(id = "acc_01", userId = expectedEmailUid, name = "Ahorros Principal", balance = 5000000.0, availableBalance = 5000000.0, type = "SAVINGS", currency = "COP")
        val transaction = TransactionEntity(
            id = "tx_01",
            userId = expectedEmailUid,
            accountId = "acc_01",
            accountName = "Ahorros Principal",
            categoryId = "cat_01",
            categoryName = "Alimentación",
            amount = 120000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-27",
            description = "Mercado Semanal",
            receiptUrl = null,
            idempotencyKey = "tx_01"
        )
        val category = CategoryEntity(id = "cat_01", name = "Alimentación", icon = "restaurant", colorHex = "#FF5722", type = "EXPENSE")
        val creditCard = AccountCreditCardEntity(id = "cc_01", accountId = "acc_01", franchise = "VISA", category = "PLATINUM", quotaTotal = 10000000.0, quotaAvailable = 8000000.0, quotaUsed = 2000000.0, cutOffDay = 15, paymentDueDay = 5)
        val fixedAsset = FixedAssetEntity(id = "asset_01", userId = expectedEmailUid, name = "MacBook Pro M3", type = "TANGIBLE", category = "Tecnología", acquisitionDate = "2026-01-15", acquisitionValue = 12000000.0, currency = "COP")
        val assetMovement = AssetMovementEntity(id = "mov_01", assetId = "asset_01", userId = expectedEmailUid, type = "MAINTENANCE", title = "Mantenimiento Preventivo", date = "2026-06-20", amount = 350000.0)
        val goal = FinancialGoalEntity(id = "goal_01", userId = expectedEmailUid, name = "Fondo Emergencia", targetAmount = 20000000.0, savedAmount = 5000000.0, targetAccountId = "acc_01", targetAccountName = "Ahorros Principal")
        val budgetEntity = BudgetEntity(id = "bgt_01", userId = expectedEmailUid, name = "Octubre 2026", startDate = "2026-10-01", endDate = "2026-10-31", totalAmount = 3000000.0)
        val budgetItem = BudgetItemEntity(id = "itm_01", budgetId = "bgt_01", categoryId = "cat_01", categoryName = "Alimentación", allocatedAmount = 1500000.0, spentAmount = 120000.0)
        val budgetWithItems = BudgetWithItems(budget = budgetEntity, items = listOf(budgetItem))
        val scheduledPayment = ScheduledPaymentEntity(id = "sp_01", userId = expectedEmailUid, entityName = "Internet Fibra", amount = 150000.0, dueDate = "2026-10-30")
        val cdt = AccountCdtEntity(id = "cdt_01", accountId = "acc_01", certificateNumber = "CDT-8877", issueDate = "2026-05-01", maturityDate = "2026-11-01", initialAmount = 10000000.0)
        val reconciliation = AccountReconciliationLogEntity(
            id = "rec_01", userId = expectedEmailUid, accountId = "acc_01", accountName = "Ahorros Principal",
            previousSystemBalance = 5000000.0, reconciledRealBalance = 5000000.0, discrepancyAmount = 0.0,
            reconciliationDate = "2026-10-27", reconciliationTime = "12:00:00", reconciledByUserId = expectedEmailUid,
            reconciledByUserName = "Andres", status = "BALANCED", deviceModel = "Pixel 8"
        )

        val financialDaoProxy = Proxy.newProxyInstance(
            FinancialDao::class.java.classLoader,
            arrayOf(FinancialDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAccounts" -> listOf(account)
                "getTransactions" -> listOf(transaction)
                "getCategories" -> listOf(category)
                "getAccountCreditCardsByUser" -> listOf(creditCard)
                "getGoals" -> listOf(goal)
                "getAllBudgetsWithItems" -> listOf(budgetWithItems)
                "getScheduledPayments" -> listOf(scheduledPayment)
                "getAccountCdtsByUser" -> listOf(cdt)
                else -> null
            }
        } as FinancialDao

        val fixedAssetDaoProxy = Proxy.newProxyInstance(
            FixedAssetDao::class.java.classLoader,
            arrayOf(FixedAssetDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getAssets" -> listOf(fixedAsset)
                "getAllMovements" -> listOf(assetMovement)
                else -> null
            }
        } as FixedAssetDao

        val reconciliationDaoProxy = Proxy.newProxyInstance(
            ReconciliationDao::class.java.classLoader,
            arrayOf(ReconciliationDao::class.java)
        ) { _, method, _ ->
            when (method.name) {
                "getReconciliations" -> listOf(reconciliation)
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
            fixedAssetDao = fixedAssetDaoProxy,
            reconciliationDao = reconciliationDaoProxy,
            keystoreCryptoManager = cryptoManager,
            ioDispatcher = testDispatcher
        )

        val result = repository.backupFullPortfolioToCloud(expectedEmailUid)
        assertTrue("El respaldo de los 10 módulos debe ser exitoso", result.isSuccess)

        val envelope = firebaseDataSource.lastEnvelope
        assertNotNull("El sobre de respaldo debe haber sido transmitido", envelope)
        assertEquals(expectedEmailUid, firebaseDataSource.lastSyncedUserId)

        // 10 modules: 1 account + 1 tx + 1 card + 1 asset + 1 movement + 1 goal + 1 budget + 1 payment + 1 cdt + 1 rec = 10 records
        val recordCount = envelope?.get("recordCount") as? Int
        assertEquals(10, recordCount)

        // Descifrar con la clave de correo y validar contenido JSON interno
        val encryptedPayload = envelope?.get("encryptedPayload") as String
        val decryptedCompressed = cryptoManager.decryptCloudPayload(encryptedPayload, expectedEmailUid)
        assertNotNull("El payload cifrado debe descifrarse perfectamente con el UID derivado del correo", decryptedCompressed)

        val decompressedJson = GZIPInputStream(ByteArrayInputStream(android.util.Base64.decode(decryptedCompressed, android.util.Base64.NO_WRAP)))
            .bufferedReader(Charsets.UTF_8).use { it.readText() }

        val rootObj = JSONObject(decompressedJson)
        assertEquals("9.3.5", rootObj.optString("version"))
        assertEquals(1, rootObj.getJSONArray("accounts").length())
        assertEquals(1, rootObj.getJSONArray("transactions").length())
        assertEquals(1, rootObj.getJSONArray("fixedAssets").length())
        assertEquals(1, rootObj.getJSONArray("assetMovements").length())
        assertEquals(1, rootObj.getJSONArray("creditCards").length())
        assertEquals(1, rootObj.getJSONArray("goals").length())
        assertEquals(1, rootObj.getJSONArray("budgets").length())
        assertEquals(1, rootObj.getJSONArray("scheduledPayments").length())
        assertEquals(1, rootObj.getJSONArray("cdts").length())
        assertEquals(1, rootObj.getJSONArray("reconciliations").length())

        assertEquals("Fondo Emergencia", rootObj.getJSONArray("goals").getJSONObject(0).getString("name"))
        assertEquals("CDT-8877", rootObj.getJSONArray("cdts").getJSONObject(0).getString("certificateNumber"))
        assertEquals("Internet Fibra", rootObj.getJSONArray("scheduledPayments").getJSONObject(0).getString("entityName"))
        assertEquals("Ahorros Principal", rootObj.getJSONArray("reconciliations").getJSONObject(0).getString("accountName"))
    }
}
