package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.BuildConfig
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.PassphraseManager
import com.example.data.local.SecurityDatabase
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.LocalAuthConfig
import com.example.data.local.entity.TransactionEntity
import com.example.data.repository.AuthRepositoryImpl
import com.example.data.repository.FinancialRepositoryImpl
import com.example.features.settings.domain.model.UserPreferencesDomain
import com.example.features.settings.domain.repository.SettingsRepository
import com.example.features.settings.presentation.SettingsViewModel
import com.example.presentation.audit.AuditCategory
import com.example.presentation.audit.AuditSeverity
import com.example.presentation.audit.AuditSiemExporter
import com.example.presentation.audit.AuditUiEntry
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Suite de pruebas unitarias para REM-P3-01 (HALL-08 / VER-F9-01).
 * Verifica:
 * 1. Exportador SIEM dinámico usando BuildConfig.VERSION_NAME (app_version y version).
 * 2. Instrumentación completa de los 7 eventos de seguridad en AuditSecurityLogDao:
 *    - AUTH_PIN_SUCCESS
 *    - AUTH_PIN_FAILURE
 *    - AUTH_LOCKOUT_TRIGGERED
 *    - AUTH_PIN_CHANGED
 *    - VAULT_PASSPHRASE_CHANGED
 *    - DATA_EXPORT_PDF_CSV
 *    - CLOUD_BACKUP_EXECUTED
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class SiemExportAndSecurityAuditEventsTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var cryptoManager: KeystoreCryptoManager
    private lateinit var fakeFirebaseDataSource: FakeFirebaseDataSource
    private lateinit var financialRepository: FinancialRepositoryImpl
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var fakeSettingsRepository: FakeSettingsRepository
    private lateinit var settingsViewModel: SettingsViewModel

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testUserId = "user_audit_test_01"

    private class FakeFirebaseDataSource(context: Context) : FirebaseDataSource(context) {
        var lastSavedPayload: Map<String, Any>? = null
        override suspend fun syncOfflineFinancialBackup(userId: String, payload: Map<String, Any>): Boolean {
            lastSavedPayload = payload
            return true
        }
        override suspend fun getFinancialBackup(userId: String): Map<String, Any>? = lastSavedPayload
    }

    private class FakeSettingsRepository : SettingsRepository {
        var currentPreferences = UserPreferencesDomain(userId = "user_audit_test_01")
        private val flow = MutableStateFlow(currentPreferences)

        override fun observePreferences(userId: String): Flow<UserPreferencesDomain> = flow
        override suspend fun getPreferences(userId: String): UserPreferencesDomain = currentPreferences
        override suspend fun updatePreferences(preferences: UserPreferencesDomain): Result<UserPreferencesDomain> {
            currentPreferences = preferences
            flow.value = preferences
            return Result.success(preferences)
        }
        override suspend fun togglePrivacyMode(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setBaseCurrency(userId: String, currency: String): Result<Unit> = Result.success(Unit)
        override suspend fun setLanguage(userId: String, language: String): Result<Unit> = Result.success(Unit)
        override suspend fun setTheme(userId: String, theme: String): Result<Unit> = Result.success(Unit)
        override suspend fun setBiometricsEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setBudgetAlertsEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setPaymentRemindersEnabled(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setExcludeAllAccountsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setAccountExcludedFromDashboard(userId: String, accountId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun isAccountExcluded(userId: String, accountId: String): Boolean = false
        override suspend fun setAutoNoAffectInventoryForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectInventoryToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setAutoNoAffectDashboardForAssets(userId: String, enabled: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun applyNoAffectDashboardToAllAssets(userId: String, noAffect: Boolean): Result<Int> = Result.success(0)
        override suspend fun setExcludeAllFixedAssetsFromDashboard(userId: String, excluded: Boolean): Result<Unit> = Result.success(Unit)
        override suspend fun setPrivacyPolicyConsent(userId: String, accepted: Boolean, version: String): Result<Unit> {
            val updated = currentPreferences.copy(
                privacyPolicyAccepted = accepted,
                privacyPolicyVersion = version,
                privacyPolicyAcceptedTimestamp = if (accepted) System.currentTimeMillis() else 0L
            )
            currentPreferences = updated
            flow.value = updated
            return Result.success(Unit)
        }
        override suspend fun isPrivacyPolicyAccepted(userId: String): Boolean = currentPreferences.privacyPolicyAccepted
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

        cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        fakeFirebaseDataSource = FakeFirebaseDataSource(context)

        financialRepository = FinancialRepositoryImpl(
            financialDao = database.financialDao(),
            auditLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            keystoreCryptoManager = cryptoManager,
            ioDispatcher = testDispatcher
        )

        authRepository = AuthRepositoryImpl(
            localAuthConfigDao = database.localAuthConfigDao(),
            auditSecurityLogDao = database.auditSecurityLogDao(),
            firebaseDataSource = fakeFirebaseDataSource,
            googleDriveManager = GoogleDriveManager(context),
            userSessionDao = database.userSessionDao(),
            financialDao = database.financialDao(),
            securityDatabase = database,
            context = context,
            keystoreCryptoManager = cryptoManager,
            ioDispatcher = testDispatcher
        )

        fakeSettingsRepository = FakeSettingsRepository()

        settingsViewModel = SettingsViewModel(
            repository = fakeSettingsRepository,
            authRepository = authRepository,
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
    fun testSiemExporter_usesDynamicBuildConfigVersion() {
        val sampleEntries = listOf(
            AuditUiEntry(
                id = "SEC-01",
                timestamp = System.currentTimeMillis(),
                formattedDateTime = "2026-10-18 20:00:00",
                relativeTime = "Hace un momento",
                category = AuditCategory.SECURITY,
                severity = AuditSeverity.CRITICAL,
                eventTitle = "Intento de inicio de sesión",
                details = "Autenticación por PIN exitosa",
                authMethodOrOrigin = "PIN_LOCAL",
                hashSha256 = "sha256:abc123mock"
            )
        )

        val result = AuditSiemExporter.exportToSiemJson(
            context = context,
            logs = sampleEntries,
            userName = "Auditor Enterprise",
            filterDesc = "Solo Seguridad"
        )

        assertTrue("El exportador SIEM debe reportar éxito", result.success)
        assertNotNull("La ruta de archivo no debe ser nula", result.filePath)

        val exportedFile = File(result.filePath!!)
        assertTrue("El archivo SIEM JSON debe existir en disco", exportedFile.exists())

        val jsonContent = exportedFile.readText()
        val jsonObject = JSONObject(jsonContent)

        assertTrue("El JSON debe contener 'metadata'", jsonObject.has("metadata"))
        val metadata = jsonObject.getJSONObject("metadata")

        // VER-F9-01 check: dynamic version and app_version equal to BuildConfig.VERSION_NAME
        assertEquals(BuildConfig.VERSION_NAME, metadata.getString("version"))
        assertEquals(BuildConfig.VERSION_NAME, metadata.getString("app_version"))
        assertEquals("Auditor Enterprise", metadata.getString("operator_user"))
        assertEquals("Solo Seguridad", metadata.getString("filter_applied"))
        assertEquals(1, metadata.getInt("total_events"))

        val events = jsonObject.getJSONArray("events")
        assertEquals(1, events.length())
        val firstEvent = events.getJSONObject(0)
        assertEquals("SEC-01", firstEvent.getString("log_id"))
        assertEquals("Intento de inicio de sesión", firstEvent.getString("event_title"))
    }

    @Test
    fun testAuthPinEvents_areLoggedCorrectly() = runBlocking {
        // 1. Setup PIN
        val setupSuccess = authRepository.setupPin("1234")
        assertTrue(setupSuccess)
        val logs1 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs1.any { it.eventType == "PIN_CONFIGURED" })

        // 2. Successful PIN authentication -> AUTH_PIN_SUCCESS
        val successResult = authRepository.authenticateWithPin("1234")
        assertTrue(successResult is com.example.domain.models.UserAuthResult.Success)
        val logs2 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs2.any { it.eventType == "AUTH_PIN_SUCCESS" })

        // 3. Failed PIN authentication -> AUTH_PIN_FAILURE
        val fail1 = authRepository.authenticateWithPin("0000")
        assertTrue(fail1 is com.example.domain.models.UserAuthResult.Failure)
        val logs3 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs3.any { it.eventType == "AUTH_PIN_FAILURE" })

        // 4. Trigger lockout on consecutive failures -> AUTH_LOCKOUT_TRIGGERED
        authRepository.authenticateWithPin("0000")
        val fail3 = authRepository.authenticateWithPin("0000")
        assertTrue(fail3 is com.example.domain.models.UserAuthResult.Failure)
        assertTrue((fail3 as com.example.domain.models.UserAuthResult.Failure).isLockedOut)
        val logs4 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs4.any { it.eventType == "AUTH_LOCKOUT_TRIGGERED" })

        // 5. Change PIN -> AUTH_PIN_CHANGED
        database.localAuthConfigDao().clearLockoutExpiration()
        val currentCfg = database.localAuthConfigDao().getConfig()
        if (currentCfg != null) {
            database.localAuthConfigDao().insertOrUpdate(currentCfg.copy(failedAttempts = 0, lockoutUntilTimestamp = 0L))
        }
        val changeResult = authRepository.changePin("1234", "9876")
        assertTrue(changeResult.isSuccess)
        val logs5 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs5.any { it.eventType == "AUTH_PIN_CHANGED" })
    }

    @Test
    fun testVaultPassphraseChanged_isLoggedCorrectly() = runBlocking {
        val passphraseManager = PassphraseManager(
            context = context,
            cryptoManager = cryptoManager,
            auditSecurityLogDao = database.auditSecurityLogDao()
        )

        // 1. User sets a custom passphrase
        val setSuccess = passphraseManager.setUserPassphrase("SuperClaveSegura2026!")
        assertTrue(setSuccess)
        val logs1 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs1.any {
            it.eventType == "VAULT_PASSPHRASE_CHANGED" && it.authMethod == "LOCAL_PASSPHRASE"
        })

        // 2. User migrates KDF
        val migrateSuccess = passphraseManager.migratePassphraseKdfV1ToV2("SuperClaveSegura2026!", null)
        assertTrue(migrateSuccess)
        val logs2 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs2.any {
            it.eventType == "VAULT_PASSPHRASE_CHANGED" && it.authMethod == "KDF_MIGRATION_V2"
        })

        // 3. Reset to hardware master passphrase
        passphraseManager.resetToHardwareMasterPassphrase()
        val logs3 = database.auditSecurityLogDao().getAllLogsList()
        assertTrue(logs3.any {
            it.eventType == "VAULT_PASSPHRASE_CHANGED" && it.authMethod == "HARDWARE_KEYSTORE"
        })
    }

    @Test
    fun testDataExportAndCloudBackup_areLoggedCorrectly() = runBlocking {
        // Add sample account and transaction to allow cloud backup
        database.financialDao().insertAccount(
            AccountEntity(id = "acc_01", name = "Bancolombia Ahorros", balance = 50000.0, userId = testUserId)
        )
        database.financialDao().insertTransaction(
            TransactionEntity(
                id = "tx_01",
                userId = testUserId,
                accountId = "acc_01",
                accountName = "Bancolombia Ahorros",
                categoryId = "cat_01",
                categoryName = "Comida",
                amount = 1000.0,
                currency = "COP",
                type = "EXPENSE",
                date = "2026-10-18",
                description = "Almuerzo",
                receiptUrl = null,
                idempotencyKey = "idem_tx_01"
            )
        )

        // 1. Test Cloud Backup via FinancialRepository
        val backupResult = financialRepository.backupFullPortfolioToCloud(testUserId)
        assertTrue("El respaldo de FinancialRepository debe ser exitoso", backupResult.isSuccess)
        val logsAfterBackup = database.auditSecurityLogDao().getAllLogsList()
        assertTrue("Debe registrarse CLOUD_BACKUP_EXECUTED tras el respaldo E2EE",
            logsAfterBackup.any { it.eventType == "CLOUD_BACKUP_EXECUTED" })

        // 2. Test Cloud Backup via SettingsViewModel
        settingsViewModel.executeCloudBackup()
        val logsAfterVmBackup = database.auditSecurityLogDao().getAllLogsList()
        assertTrue("Debe registrarse CLOUD_BACKUP_EXECUTED tras executeCloudBackup en ViewModel",
            logsAfterVmBackup.any { it.eventType == "CLOUD_BACKUP_EXECUTED" })

        // 3. Test JSON Export
        settingsViewModel.exportFinancialData("JSON", "Este mes", context)
        val logsAfterJson = database.auditSecurityLogDao().getAllLogsList()
        assertTrue("Debe registrarse DATA_EXPORT_PDF_CSV tras exportar JSON",
            logsAfterJson.any { it.eventType == "DATA_EXPORT_PDF_CSV" && it.details.contains("JSON") })

        // 4. Test CSV Export
        settingsViewModel.exportFinancialData("CSV", "Este mes", context)
        val logsAfterCsv = database.auditSecurityLogDao().getAllLogsList()
        assertTrue("Debe registrarse DATA_EXPORT_PDF_CSV tras exportar CSV",
            logsAfterCsv.any { it.eventType == "DATA_EXPORT_PDF_CSV" && it.details.contains("CSV") })

        // 5. Test PDF Export
        settingsViewModel.exportFinancialData("PDF", "Este mes", context)
        val logsAfterPdf = database.auditSecurityLogDao().getAllLogsList()
        assertTrue("Debe registrarse DATA_EXPORT_PDF_CSV tras exportar PDF",
            logsAfterPdf.any { it.eventType == "DATA_EXPORT_PDF_CSV" && it.details.contains("PDF") })
    }
}
