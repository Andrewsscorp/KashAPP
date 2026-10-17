package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.presentation.dashboard.DashboardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Enterprise Integration Test suite verifying:
 * 1. Cloud portfolio restore from Firebase Firestore (List, Map, and Single Account formats).
 * 2. Automatic cleanup of 0-balance dummy accounts upon restoring genuine funded accounts.
 * 3. App re-install simulation: verifying DashboardViewModel computes positive availableCash and netWorth (NOT $0).
 * 4. Safety guard: preventing startup wipe where zero-balance default dummy accounts overwrite cloud backups.
 * 5. Full portfolio backup serialization to Firestore.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class CloudSyncAndRestoreIntegrationTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var database: SecurityDatabase
    private lateinit var fakeFirebaseDataSource: FakeTestFirebaseDataSource
    private lateinit var repository: FinancialRepositoryImpl

    private val testUserId = "test_user_firebase_uid_123"

    class FakeTestFirebaseDataSource(context: Context) : FirebaseDataSource(context) {
        val userBackups = mutableMapOf<String, Map<String, Any>>()
        var fakeBackupPayload: Map<String, Any>?
            get() = userBackups["test_user_firebase_uid_123"]
            set(value) {
                if (value != null) userBackups["test_user_firebase_uid_123"] = value
                else userBackups.remove("test_user_firebase_uid_123")
            }
        var lastSavedPayload: Map<String, Any>? = null
        var syncOfflineFinancialBackupCallCount = 0

        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? {
            return userBackups[userId]
        }

        override suspend fun syncOfflineFinancialBackup(userId: String, backupPayload: Map<String, Any>): Boolean {
            syncOfflineFinancialBackupCallCount++
            lastSavedPayload = backupPayload
            userBackups[userId] = backupPayload
            return true
        }
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        val directExecutor = java.util.concurrent.Executor { command -> command.run() }
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .setQueryExecutor(directExecutor)
            .setTransactionExecutor(directExecutor)
            .allowMainThreadQueries()
            .build()
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)

        fakeFirebaseDataSource = FakeTestFirebaseDataSource(context)
        repository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        database.close()
    }

    @Test
    fun `restoreFromCloud when payload has 1 million account cleans dummy accounts and reflects funded balance`() = runTest(testDispatcher) {
        // 1. Simulate initial dummy accounts seeded locally (as happens on a clean install)
        val dummyAccounts = listOf(
            AccountEntity("acc_ahorros", testUserId, "Cuenta de ahorros", 0.0, "COP", "SAVINGS"),
            AccountEntity("acc_corriente", testUserId, "Cuenta corriente", 0.0, "COP", "SAVINGS"),
            AccountEntity("acc_billetera", testUserId, "Billetera digital", 0.0, "COP", "WALLET"),
            AccountEntity("acc_efectivo", testUserId, "Efectivo", 0.0, "COP", "CASH")
        )
        database.financialDao().insertAccounts(dummyAccounts)
        assertEquals(4, database.financialDao().getAccounts(testUserId).size)

        // 2. Setup Firebase Cloud Backup containing 1 account with $1,000,000 COP
        val cloudAccountMap = mapOf(
            "id" to "acc_bancolombia_real_999",
            "name" to "Bancolombia Principal",
            "balance" to 1000000.0,
            "currency" to "COP",
            "type" to "SAVINGS",
            "bankName" to "Bancolombia",
            "isFavorite" to true
        )
        fakeFirebaseDataSource.fakeBackupPayload = mapOf(
            "userId" to testUserId,
            "version" to "9.0.0",
            "table" to "portfolio_backup",
            "accounts" to listOf(cloudAccountMap),
            "transactions" to emptyList<Map<String, Any>>()
        )

        // 3. Trigger cloud restore
        val restoreResult = repository.restoreFinancialBackupFromCloudIfNeeded(testUserId)
        assertTrue("Restore from cloud should succeed", restoreResult.isSuccess)
        assertTrue("RestoredAny flag should be true", restoreResult.getOrDefault(false))

        // 4. Verify local database accounts
        val currentAccounts = database.financialDao().getAccounts(testUserId)
        // Dummy 0-balance accounts must have been cleaned up!
        assertEquals("Only the genuine funded account should remain", 1, currentAccounts.size)
        val restoredAccount = currentAccounts.first()
        assertEquals("acc_bancolombia_real_999", restoredAccount.id)
        assertEquals("Bancolombia Principal", restoredAccount.name)
        assertEquals(1000000.0, restoredAccount.balance, 0.001)

        // 5. Verify initial transaction was created so KPIs and CashFlow reflect the 1 million
        val transactions = database.financialDao().getTransactions(testUserId)
        assertEquals(1, transactions.size)
        assertEquals(1000000.0, transactions.first().amount, 0.001)
        assertEquals("INCOME", transactions.first().type)
    }

    @Test
    fun `restoreFromCloud when payload has multiple accounts and transactions restores all accurately`() = runTest(testDispatcher) {
        val cloudAccounts = listOf(
            mapOf("id" to "acc_1", "name" to "Cuenta Ahorros", "balance" to 800000.0, "currency" to "COP", "type" to "SAVINGS", "bankName" to "Bancolombia", "isFavorite" to true),
            mapOf("id" to "acc_2", "name" to "Nequi", "balance" to 200000.0, "currency" to "COP", "type" to "WALLET", "bankName" to "Nequi", "isFavorite" to false)
        )
        val cloudTransactions = listOf(
            mapOf("id" to "tx_1", "accountId" to "acc_1", "accountName" to "Cuenta Ahorros", "amount" to 850000.0, "type" to "INCOME", "currency" to "COP", "date" to "2026-10-01", "categoryId" to "cat_salario", "categoryName" to "Salario"),
            mapOf("id" to "tx_2", "accountId" to "acc_1", "accountName" to "Cuenta Ahorros", "amount" to 50000.0, "type" to "EXPENSE", "currency" to "COP", "date" to "2026-10-05", "categoryId" to "cat_alimentacion", "categoryName" to "Mercado")
        )

        fakeFirebaseDataSource.fakeBackupPayload = mapOf(
            "userId" to testUserId,
            "version" to "9.0.0",
            "table" to "portfolio_backup",
            "accounts" to cloudAccounts,
            "transactions" to cloudTransactions
        )

        val restoreResult = repository.restoreFinancialBackupFromCloudIfNeeded(testUserId)
        assertTrue(restoreResult.isSuccess)
        assertTrue(restoreResult.getOrDefault(false))

        val accounts = database.financialDao().getAccounts(testUserId)
        assertEquals(2, accounts.size)
        val totalBalance = accounts.sumOf { it.balance }
        assertEquals(1000000.0, totalBalance, 0.001)

        val txs = database.financialDao().getTransactions(testUserId)
        assertEquals(2, txs.size)
        assertTrue(txs.any { it.id == "tx_1" && it.amount == 850000.0 && it.type == "INCOME" })
        assertTrue(txs.any { it.id == "tx_2" && it.amount == 50000.0 && it.type == "EXPENSE" })
    }

    @Test
    fun `reinstallSimulation DashboardViewModel initializes and displays 1 million net worth instead of 0`() = runTest(testDispatcher) {
        // Simulates clean app reinstall: local DB is empty, cloud contains 1,000,000 COP backup
        val cloudAccountMap = mapOf(
            "id" to "acc_reinstall_test",
            "name" to "Cuenta Principal",
            "balance" to 1000000.0,
            "currency" to "COP",
            "type" to "SAVINGS",
            "bankName" to "Bancolombia",
            "isFavorite" to true
        )
        fakeFirebaseDataSource.fakeBackupPayload = mapOf(
            "userId" to testUserId,
            "version" to "9.0.0",
            "table" to "portfolio_backup",
            "accounts" to listOf(cloudAccountMap),
            "transactions" to emptyList<Map<String, Any>>()
        )

        // Instantiate DashboardViewModel
        val viewModel = DashboardViewModel(
            financialRepository = repository,
            userId = testUserId
        )

        // Advance all coroutines (init cloud restore + Room flow observations)
        advanceUntilIdle()

        val uiState = viewModel.uiState.value
        assertFalse("Loading state should complete", uiState.isLoading)

        // Net Worth and Available Cash must be $1,000,000 COP (NOT 0!)
        val expectedAmount = BigDecimal("1000000.00")
        assertEquals(expectedAmount, uiState.netWorth.availableCash)
        assertEquals(expectedAmount, uiState.netWorth.totalNetWorth)

        // Accounts list in UI state must contain the restored account
        assertEquals(1, uiState.accounts.size)
        assertEquals("Cuenta Principal", uiState.accounts.first().name)
        assertEquals(BigDecimal.valueOf(1000000.0).setScale(2, RoundingMode.HALF_EVEN), uiState.accounts.first().balance)
    }

    @Test
    fun `backupFullPortfolioToCloud safety guard prevents overwriting cloud when local accounts are only 0-balance defaults`() = runTest(testDispatcher) {
        // Initial setup: cloud has existing data
        fakeFirebaseDataSource.fakeBackupPayload = mapOf(
            "userId" to testUserId,
            "balance" to 1000000.0,
            "name" to "Cuenta Protegida"
        )

        // Local DB only has the 4 empty default dummy accounts with $0 balance
        val defaultDummies = listOf(
            AccountEntity("acc_ahorros", testUserId, "Cuenta de ahorros", 0.0, "COP", "SAVINGS"),
            AccountEntity("acc_corriente", testUserId, "Cuenta corriente", 0.0, "COP", "SAVINGS"),
            AccountEntity("acc_billetera", testUserId, "Billetera digital", 0.0, "COP", "WALLET"),
            AccountEntity("acc_efectivo", testUserId, "Efectivo", 0.0, "COP", "CASH")
        )
        database.financialDao().insertAccounts(defaultDummies)

        val beforeCallCount = fakeFirebaseDataSource.syncOfflineFinancialBackupCallCount

        // Attempting to back up empty defaults
        val result = repository.backupFullPortfolioToCloud(testUserId)
        assertTrue(result.isSuccess)

        // The safety guard must SKIP uploading to Firestore
        assertEquals("Firestore sync should NOT be called for empty $0 defaults", beforeCallCount, fakeFirebaseDataSource.syncOfflineFinancialBackupCallCount)
        assertEquals(1000000.0, fakeFirebaseDataSource.fakeBackupPayload?.get("balance"))
    }

    @Test
    fun `backupFullPortfolioToCloud uploads real accounts and transactions to cloud`() = runTest(testDispatcher) {
        // Create genuine account with 1,000,000 COP
        val createdAccount = repository.createAccount(
            name = "Bancolombia Nómina",
            type = "SAVINGS",
            balance = 1000000.0,
            currency = "COP",
            userId = testUserId,
            bankName = "Bancolombia",
            isFavorite = true
        )
        assertNotNull(createdAccount)

        // Record genuine expense
        val expenseResult = repository.recordExpense(
            userId = testUserId,
            accountId = createdAccount.id,
            accountName = createdAccount.name,
            categoryId = "cat_servicios",
            categoryName = "Servicios",
            amount = 75000.0,
            currency = "COP",
            dateFormatted = "2026-10-14",
            description = "Pago de luz",
            receiptUrl = null,
            isRecurring = false,
            saveAsTemplate = false,
            tags = emptyList(),
            idempotencyKey = "tx_test_75k",
            affectsBudget = true
        )
        assertTrue(expenseResult.isSuccess)

        // Call full portfolio backup
        val backupResult = repository.backupFullPortfolioToCloud(testUserId)
        assertTrue(backupResult.isSuccess)

        // Verify the payload sent to Firestore
        val payload = fakeFirebaseDataSource.lastSavedPayload
        assertNotNull(payload)
        assertEquals("portfolio_backup", payload?.get("table"))
        assertEquals(testUserId, payload?.get("userId"))

        @Suppress("UNCHECKED_CAST")
        val accountsList = payload?.get("accounts") as? List<Map<String, Any>>
        assertNotNull(accountsList)
        assertTrue(accountsList!!.any { it["name"] == "Bancolombia Nómina" && (it["balance"] as Number).toDouble() == 925000.0 })

        @Suppress("UNCHECKED_CAST")
        val transactionsList = payload?.get("transactions") as? List<Map<String, Any>>
        assertNotNull(transactionsList)
        assertTrue(transactionsList!!.any { it["categoryName"] == "Servicios" && (it["amount"] as Number).toDouble() == 75000.0 })
    }

    @Test
    fun `multiAccountIsolation when userB logs in they do NOT receive userA data`() = runTest(testDispatcher) {
        val userAId = "user_A_uid"
        val userBId = "user_B_uid"

        // 1. User A has a real account with 1,000,000 COP both in cloud and locally
        fakeFirebaseDataSource.userBackups[userAId] = mapOf(
            "userId" to userAId,
            "version" to "9.0.0",
            "table" to "portfolio_backup",
            "accounts" to listOf(
                mapOf(
                    "id" to "acc_user_a_million",
                    "name" to "Cuenta Millonaria de A",
                    "balance" to 1000000.0,
                    "currency" to "COP",
                    "type" to "SAVINGS",
                    "bankName" to "Bancolombia"
                )
            )
        )
        database.financialDao().insertAccounts(
            listOf(
                AccountEntity("acc_user_a_million", userAId, "Cuenta Millonaria de A", 1000000.0, "COP", "SAVINGS")
            )
        )

        // 2. User B logs in on the same device. User B has no cloud backup.
        val viewModelB = DashboardViewModel(
            financialRepository = repository,
            userId = userBId
        )

        advanceUntilIdle()

        val uiStateB = viewModelB.uiState.value
        assertFalse("Loading state should complete for user B", uiStateB.isLoading)

        // 3. User B must NOT receive User A's 1 million pesos!
        assertEquals("User B must start with 0 available cash", BigDecimal("0.00"), uiStateB.netWorth.availableCash)
        assertEquals("User B must start with 0 total net worth", BigDecimal("0.00"), uiStateB.netWorth.totalNetWorth)
        assertFalse("User B must NOT see User A's accounts", uiStateB.accounts.any { it.name.contains("Cuenta Millonaria") })

        // 4. Verify User A's data in the database remains completely intact for User A
        val userAAccounts = database.financialDao().getAccounts(userAId)
        assertEquals(1, userAAccounts.size)
        assertEquals(1000000.0, userAAccounts.first().balance, 0.001)
    }

    @Test
    fun `biometricAndPinAuth resolution links to the primary user session rather than creating isolated phantom user`() = runTest(testDispatcher) {
        val primaryGoogleUid = "google_user_primary_uid_777"

        // 1. User logged in with Google, leaving an active/last session in Room
        val session = com.example.data.local.entity.UserSessionEntity(
            userId = primaryGoogleUid,
            displayName = "Juan Pérez",
            email = "juan.perez@gmail.com",
            authProvider = "firebase_google",
            isActive = true,
            lastLoginTimestamp = System.currentTimeMillis(),
            lastActiveTimestamp = System.currentTimeMillis()
        )
        database.userSessionDao().insertOrUpdateSession(session)

        // 2. Instantiate AuthRepositoryImpl with the test database
        val keystoreCryptoManager = com.example.data.local.KeystoreCryptoManager()
        val authRepo = com.example.data.repository.AuthRepositoryImpl(
            localAuthConfigDao = database.localAuthConfigDao(),
            auditSecurityLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            googleDriveManager = com.example.data.datasource.GoogleDriveManager(ApplicationProvider.getApplicationContext()),
            userSessionDao = database.userSessionDao(),
            financialDao = database.financialDao(),
            keystoreCryptoManager = keystoreCryptoManager
        )

        // 3. Setup a PIN for testing
        authRepo.setupPin("1234")

        // 4. Authenticate using PIN
        val pinOutcome = authRepo.authenticateWithPin("1234")
        assertTrue("PIN auth should succeed", pinOutcome is com.example.domain.models.UserAuthResult.Success)

        // 5. Verify the resolved userId is Juan's primary Google UID, NOT "user_default" or "kash_user_bio"!
        val successUser = pinOutcome as com.example.domain.models.UserAuthResult.Success
        assertEquals("Resolved userId must match primary Google session", primaryGoogleUid, successUser.userId)
        assertEquals("Resolved email must match primary Google session", "juan.perez@gmail.com", successUser.email)
        assertEquals("Resolved displayName must match primary Google session", "Juan Pérez", successUser.displayName)
    }
}
