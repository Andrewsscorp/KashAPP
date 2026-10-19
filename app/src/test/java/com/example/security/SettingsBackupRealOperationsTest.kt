package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.FinancialRepositoryImpl
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import com.example.features.settings.presentation.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
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
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SettingsBackupRealOperationsTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var fakeFirebaseDataSource: FakeFirebaseDataSource
    private lateinit var financialRepository: FinancialRepositoryImpl
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var viewModel: SettingsViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUserId = "user_settings_backup_test"

    private class FakeFirebaseDataSource(context: Context) : FirebaseDataSource(context) {
        var lastSavedPayload: Map<String, Any>? = null
        var mockCloudBackup: Map<String, Any>? = null
        var shouldThrowNetworkError = false

        override suspend fun syncOfflineFinancialBackup(userId: String, backupPayload: Map<String, Any>): Boolean {
            if (shouldThrowNetworkError) {
                throw IOException("Red no disponible / Modo avión activado")
            }
            lastSavedPayload = backupPayload
            mockCloudBackup = backupPayload
            return true
        }

        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? {
            if (shouldThrowNetworkError) {
                throw IOException("Error de conexión al obtener respaldo")
            }
            return mockCloudBackup
        }
    }

    private class FakeSettingsRepository : SettingsRepository {
        var preferences = UserPreferencesDomain()

        override fun observePreferences(userId: String): Flow<UserPreferencesDomain> = flowOf(preferences)
        override suspend fun getPreferences(userId: String): UserPreferencesDomain = preferences
        override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
            this.preferences = preferences
            return Result.success(preferences)
        }
        override suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit> {
            preferences = preferences.copy(privacyModeEnabled = enabled)
            return Result.success(Unit)
        }
        override suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit> {
            preferences = preferences.copy(baseCurrency = currency)
            return Result.success(Unit)
        }
        override suspend fun setLanguage(userId: String, language: String): Result<Unit> = Result.success(Unit)
        override suspend fun setTheme(userId: String, theme: String): Result<Unit> {
            preferences = preferences.copy(theme = theme)
            return Result.success(Unit)
        }
        override suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit> {
            preferences = preferences.copy(biometricsEnabled = enabled)
            return Result.success(Unit)
        }
        override suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit> {
            preferences = preferences.copy(budgetAlertsEnabled = enabled)
            return Result.success(Unit)
        }
        override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> {
            preferences = preferences.copy(paymentRemindersEnabled = enabled)
            return Result.success(Unit)
        }
        override suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setAccountExcludedFromDashboard(userId: String, accountId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun isAccountExcluded(userId: String, accountId: String): Boolean = false
        override suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectInventoryToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setAutoNoAffectDashboardForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setExcludeAllFixedAssetsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectDashboardToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setPrivacyPolicyConsent(userId: String, accepted: Boolean, version: String): Result<Unit> {
            preferences = preferences.copy(
                privacyPolicyAccepted = accepted,
                privacyPolicyVersion = version,
                privacyPolicyAcceptedTimestamp = if (accepted) System.currentTimeMillis() else 0L
            )
            return Result.success(Unit)
        }
        override suspend fun isPrivacyPolicyAccepted(userId: String): Boolean = preferences.privacyPolicyAccepted
        override suspend fun executeProtocolLogout(userId: String): Result<Unit> = Result.success(Unit)
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
        SecurityDatabase.installIntegrityGuards(database.openHelper.writableDatabase)
        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        fakeFirebaseDataSource = FakeFirebaseDataSource(context)

        financialRepository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            keystoreCryptoManager = cryptoManager,
            ioDispatcher = testDispatcher
        )

        fakeSettingsRepository = FakeSettingsRepository()

        viewModel = SettingsViewModel(
            repository = fakeSettingsRepository,
            financialRepository = financialRepository,
            userId = testUserId,
            ioDispatcher = testDispatcher
        )
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun settingsViewModel_reflectsRealLocalCountsAndCloudTimestamp() = runTest(testDispatcher) {
        // Seed 2 accounts
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_01", userId = testUserId, name = "Ahorros Real", balance = 1_500_000.0, currency = "COP", type = "SAVINGS")
        )
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_02", userId = testUserId, name = "Corriente Real", balance = 500_000.0, currency = "COP", type = "CHECKING")
        )

        // Seed 3 transactions
        for (i in 1..3) {
            database.financialDao().insertTransaction(
                TransactionEntity(
                    id = "tx_0$i",
                    userId = testUserId,
                    accountId = "acc_01",
                    accountName = "Ahorros Real",
                    categoryId = "cat_test",
                    categoryName = "Test",
                    amount = i * 20_000.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Gasto $i",
                    receiptUrl = null,
                    idempotencyKey = "tx_0$i"
                )
            )
        }

        viewModel.refreshLocalCounts()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Should show exactly 2 accounts", 2, state.localAccountsCount)
        assertEquals("Should show exactly 3 transactions", 3, state.localTransactionsCount)
    }

    @Test
    fun executeCloudBackup_whenNetworkSuccess_updatesRealStateAndMetadata() = runTest(testDispatcher) {
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_cloud", userId = testUserId, name = "Cuenta Nube", balance = 3_000_000.0, currency = "COP", type = "SAVINGS")
        )
        testScheduler.advanceUntilIdle()

        viewModel.executeCloudBackup()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBackingUp)
        assertNotNull(fakeFirebaseDataSource.lastSavedPayload)
        assertTrue(state.statusMessage?.contains("exitosamente") == true)
        assertTrue("Cloud backup timestamp must be updated with real time", state.lastCloudBackupTimestamp > 0L)
    }

    @Test
    fun executeCloudBackup_whenOfflineOrAirplaneMode_reportsRealFailureNeverSimulatedSuccess() = runTest(testDispatcher) {
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_cloud_fail", userId = testUserId, name = "Cuenta Offline", balance = 2_000_000.0, currency = "COP", type = "SAVINGS")
        )
        testScheduler.advanceUntilIdle()

        // Simulate airplane mode / network disconnection
        fakeFirebaseDataSource.shouldThrowNetworkError = true

        viewModel.executeCloudBackup()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isBackingUp)
        assertTrue("Must report error message", state.statusMessage?.contains("Error") == true)
        assertFalse("Must NEVER report simulated success during network failure", state.statusMessage?.contains("exitosamente") == true)
    }

    @Test
    fun executeCloudRestore_restoresRealEntitiesIntoRoom() = runTest(testDispatcher) {
        // Step 1: Create and backup 2 accounts and 5 transactions
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_res_1", userId = testUserId, name = "Restaurar 1", balance = 4_000_000.0, currency = "COP", type = "SAVINGS")
        )
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_res_2", userId = testUserId, name = "Restaurar 2", balance = 1_000_000.0, currency = "COP", type = "SAVINGS")
        )
        for (i in 1..5) {
            database.financialDao().insertTransaction(
                TransactionEntity(
                    id = "tx_res_$i",
                    userId = testUserId,
                    accountId = "acc_res_1",
                    accountName = "Restaurar 1",
                    categoryId = "cat_test",
                    categoryName = "Test",
                    amount = i * 10_000.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Gasto Restaurar $i",
                    receiptUrl = null,
                    idempotencyKey = "tx_res_$i"
                )
            )
        }
        testScheduler.advanceUntilIdle()

        viewModel.executeCloudBackup()
        testScheduler.advanceUntilIdle()

        // Step 2: Wipe local database (delete transactions then accounts)
        database.financialDao().deleteTransactionsByUserId(testUserId)
        database.financialDao().deleteAccountsByUserId(testUserId)
        testScheduler.advanceUntilIdle()

        assertEquals(0, database.financialDao().getAccounts(testUserId).size)
        assertEquals(0, database.financialDao().getTransactions(testUserId).size)

        // Step 3: Trigger real cloud restore
        viewModel.executeCloudRestore()
        testScheduler.advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isRestoring)
        assertTrue(state.statusMessage?.contains("exitosamente") == true)

        val restoredAccounts = database.financialDao().getAccounts(testUserId)
        val restoredTxs = database.financialDao().getTransactions(testUserId)
        assertEquals(2, restoredAccounts.size)
        assertEquals(5, restoredTxs.size)
    }

    @Test
    fun exportFinancialData_generatesRealFilesWithoutSimulatedDelay() = runTest(testDispatcher) {
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_exp_01", userId = testUserId, name = "Bancolombia Nómina", balance = 5_000_000.0, currency = "COP", type = "SAVINGS")
        )
        database.financialDao().insertTransaction(
            TransactionEntity(
                id = "tx_exp_01",
                userId = testUserId,
                accountId = "acc_exp_01",
                accountName = "Bancolombia Nómina",
                categoryId = "cat_servicios",
                categoryName = "Servicios",
                amount = 120_000.0,
                currency = "COP",
                type = "EXPENSE",
                date = "2026-10-18",
                description = "Pago Internet",
                receiptUrl = null,
                idempotencyKey = "tx_exp_01"
            )
        )
        testScheduler.advanceUntilIdle()

        // 1. Export JSON
        viewModel.exportFinancialData("JSON", "Este mes", context)
        testScheduler.advanceUntilIdle()

        val exportsDir = File(context.cacheDir, "shared_exports")
        val jsonFiles = exportsDir.listFiles { _, name -> name.startsWith("KashApp_Export_") && name.endsWith(".json") }
        assertNotNull(jsonFiles)
        assertTrue("Must generate real JSON export file in shared_exports", jsonFiles!!.isNotEmpty())
        val jsonContent = jsonFiles.first().readText()
        val jsonRoot = JSONObject(jsonContent)
        assertEquals("KashApp", jsonRoot.getString("appName"))
        assertEquals(1, jsonRoot.getInt("accountsCount"))
        assertEquals(1, jsonRoot.getInt("transactionsCount"))

        // 2. Export CSV
        viewModel.exportFinancialData("CSV", "Este mes", context)
        testScheduler.advanceUntilIdle()

        val csvFiles = exportsDir.listFiles { _, name -> name.startsWith("KashApp_Excel_") && name.endsWith(".csv") }
        assertNotNull(csvFiles)
        assertTrue("Must generate real CSV export file in shared_exports", csvFiles!!.isNotEmpty())
        val csvContent = csvFiles.first().readText()
        assertTrue(csvContent.contains("Bancolombia Nómina"))
    }

    @Test
    fun googleDriveManager_purgesLegacyUnencryptedBackupDirectory() {
        val driveManager = GoogleDriveManager(context)
        val legacyDir = File(context.filesDir, "drive_backup").apply { mkdirs() }
        File(legacyDir, "kashapp_encrypted_backup.json").writeText("legacy dummy content")
        assertTrue(legacyDir.exists())

        driveManager.purgeLocalDriveBackupDir()
        assertFalse("Legacy drive_backup directory must be purged completely", legacyDir.exists())
    }
}
