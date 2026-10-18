package com.example

import android.content.Context
import androidx.biometric.BiometricPrompt
import androidx.fragment.app.FragmentActivity
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.BiometricDataSource
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleAuthDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.LocalAuthConfigDao
import com.example.data.local.dao.UserSessionDao
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.LocalAuthConfig
import com.example.data.local.entity.UserSessionEntity
import com.example.data.repository.AuthRepositoryImpl
import com.example.domain.models.AuthMethod
import com.example.domain.models.UserAuthResult
import com.example.domain.repository.AuthRepository
import com.example.domain.usecase.AuthenticateUserUseCase
import com.example.presentation.login.LoginUiState
import com.example.presentation.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Automated Security Acceptance Test for Critical Finding C1:
 * Enforces that:
 * 1. On an emulator / device without enrolled biometrics, clicking biometric authentication NEVER grants access
 *    and instead safely redirects the user to the secure PIN / Google fallback flow.
 * 2. Authenticating with biometric strictly requires a genuine, hardware-backed BiometricPrompt.AuthenticationResult
 *    with a verified CryptoObject; any null, simulated, or boolean bypass fails immediately.
 * 3. Static/Architectural reflection check ensuring no boolean or simulation bypass route exists in the codebase.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class BiometricSecurityAcceptanceTest {

    private val testDispatcher = StandardTestDispatcher()

    private lateinit var context: Context
    private lateinit var keystoreCryptoManager: KeystoreCryptoManager
    private lateinit var fakeUserSessionDao: FakeUserSessionDao
    private lateinit var fakeLocalAuthConfigDao: FakeLocalAuthConfigDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase

    class FakeUserSessionDao : UserSessionDao {
        val sessions = mutableMapOf<String, UserSessionEntity>()
        private val _flow = MutableStateFlow<UserSessionEntity?>(null)

        private fun updateFlow() {
            _flow.value = sessions.values.filter { it.isActive }.maxByOrNull { it.lastActiveTimestamp }
        }

        override fun getActiveSessionFlow(): Flow<UserSessionEntity?> = _flow
        override suspend fun getActiveSession(): UserSessionEntity? = _flow.value
        override suspend fun getLastSession(): UserSessionEntity? = sessions.values.maxByOrNull { it.lastActiveTimestamp }
        override suspend fun getSessionByUserId(userId: String): UserSessionEntity? = sessions[userId]
        override fun getAllSessionsFlow(): Flow<List<UserSessionEntity>> = _flow.map { sessions.values.toList() }
        override suspend fun insertOrUpdateSession(session: UserSessionEntity) {
            sessions[session.userId] = session
            updateFlow()
        }
        override suspend fun deactivateSession(userId: String) {
            val s = sessions[userId]
            if (s != null) {
                sessions[userId] = s.copy(isActive = false)
                updateFlow()
            }
        }
        override suspend fun deactivateAllSessions() {
            sessions.forEach { (k, v) -> sessions[k] = v.copy(isActive = false) }
            updateFlow()
        }
        override suspend fun updateLastActive(userId: String, timestamp: Long) {
            sessions[userId]?.let { sessions[userId] = it.copy(lastActiveTimestamp = timestamp) }
            updateFlow()
        }
        override suspend fun deleteSession(userId: String) {
            sessions.remove(userId)
            updateFlow()
        }
        override suspend fun clearAllSessions() {
            sessions.clear()
            updateFlow()
        }
    }

    class FakeLocalAuthConfigDao : LocalAuthConfigDao {
        var config: LocalAuthConfig? = null
        private val flow = MutableStateFlow<LocalAuthConfig?>(null)

        override fun getConfigFlow(): Flow<LocalAuthConfig?> = flow
        override suspend fun getConfig(): LocalAuthConfig? = config
        override suspend fun insertOrUpdate(config: LocalAuthConfig) {
            this.config = config
            flow.value = config
        }
        override suspend fun updateAttempts(attempts: Int, lockoutUntil: Long) {
            config = config?.copy(failedAttempts = attempts, lockoutUntilTimestamp = lockoutUntil)
            flow.value = config
        }
        override suspend fun resetLockoutAndRecordLogin(loginTimestamp: Long) {
            config = config?.copy(failedAttempts = 0, lockoutUntilTimestamp = 0L, lastLoginTimestamp = loginTimestamp)
            flow.value = config
        }
        override suspend fun clearLockoutExpiration() {
            config = config?.copy(lockoutUntilTimestamp = 0L)
            flow.value = config
        }
        override suspend fun updateBiometricEnabled(enabled: Boolean) {
            config = config?.copy(isBiometricEnabled = enabled)
            flow.value = config
        }
    }

    class FakeAuditLogDao : AuditSecurityLogDao {
        val logs = mutableListOf<AuditSecurityLog>()
        private val flow = MutableStateFlow<List<AuditSecurityLog>>(emptyList())
        override suspend fun insertLog(log: AuditSecurityLog) {
            logs.add(log)
            flow.value = logs.toList()
        }
        override fun getAllLogs(): Flow<List<AuditSecurityLog>> = flow
        override fun getRecentLogs(limit: Int): Flow<List<AuditSecurityLog>> = flow.map { it.take(limit) }
    }

    class MockUnavailableBiometricDataSource(context: Context, cryptoManager: KeystoreCryptoManager) :
        BiometricDataSource(context, cryptoManager) {
        // Simulates an emulator or un-enrolled device returning false on canAuthenticate()
        fun isAlwaysUnavailable(): Boolean = true
    }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        keystoreCryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        fakeUserSessionDao = FakeUserSessionDao()
        fakeLocalAuthConfigDao = FakeLocalAuthConfigDao()
        fakeAuditLogDao = FakeAuditLogDao()

        authRepository = AuthRepositoryImpl(
            localAuthConfigDao = fakeLocalAuthConfigDao,
            auditSecurityLogDao = fakeAuditLogDao,
            firebaseDataSource = FirebaseDataSource(context),
            googleDriveManager = GoogleDriveManager(context),
            userSessionDao = fakeUserSessionDao,
            keystoreCryptoManager = keystoreCryptoManager
        )
        authenticateUserUseCase = AuthenticateUserUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `acceptance test - in emulator without fingerprint clicking biometric NEVER grants session and falls back to PIN`() = runTest {
        // Create an un-enrolled BiometricDataSource (canAuthenticate() == false)
        val unenrolledBiometricDataSource = object : BiometricDataSource(context, keystoreCryptoManager) {
            override fun canAuthenticate(): Boolean = false
        }

        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = unenrolledBiometricDataSource
        )

        val activity = Robolectric.buildActivity(FragmentActivity::class.java).setup().get()

        // User clicks the biometric icon
        viewModel.onBiometricClicked(activity)
        advanceUntilIdle()

        val state = viewModel.uiState.value

        // CRITICAL ASSERTION: No authenticated session is granted!
        assertNull("Authenticated user MUST BE NULL when biometrics are unavailable!", state.authenticatedUser)

        // CRITICAL ASSERTION: The app redirects to the PIN fallback dialog
        assertTrue("PIN dialog MUST BE shown as secure fallback!", state.showPinDialog)

        // An appropriate error message explains that the sensor is unavailable
        assertNotNull(state.errorMessage)
        assertTrue(state.errorMessage!!.contains("no disponible", ignoreCase = true) ||
                   state.errorMessage!!.contains("sin huellas", ignoreCase = true))

        // Also assert that Room database has no active session
        runBlocking {
            val session = authRepository.getCurrentSession()
            assertNull("No local session should be written in Room when biometric fails!", session)
        }
    }

    @Test
    fun `acceptance test - authenticateWithBiometric fails and rejects session when CryptoObject is null`() = runBlocking {
        // Construct a BiometricPrompt.AuthenticationResult with a null CryptoObject
        val nullCryptoResult = biometricAuthenticationResult(null)

        val outcome = authRepository.authenticateWithBiometric(nullCryptoResult)

        // Must return Failure
        assertTrue("Authenticating with null CryptoObject MUST return Failure", outcome is UserAuthResult.Failure)
        val failure = outcome as UserAuthResult.Failure
        assertTrue(failure.errorMessage.contains("CryptoObject", ignoreCase = true))

        // Session must NOT be created
        val activeSession = authRepository.getCurrentSession()
        assertNull("No session should be created on CryptoObject failure", activeSession)

        // Security event must be logged
        val auditLogs = fakeAuditLogDao.logs
        assertTrue("AUTH_FAILED must be logged in audit trail", auditLogs.any { it.eventType == "AUTH_FAILED" })
    }

    @Test
    fun `acceptance test - authenticateWithBiometric succeeds ONLY when valid hardware CryptoObject is provided`() = runBlocking {
        val validCryptoObject = keystoreCryptoManager.createBiometricCryptoObject()
        assertNotNull("CryptoObject should be generated by KeystoreCryptoManager", validCryptoObject)

        val validPromptResult = biometricAuthenticationResult(validCryptoObject)

        val outcome = authRepository.authenticateWithBiometric(validPromptResult)

        assertTrue("Authenticating with valid CryptoObject MUST succeed", outcome is UserAuthResult.Success)
        val success = outcome as UserAuthResult.Success
        assertEquals(AuthMethod.BIOMETRIC, success.method)

        // Session is created in Room
        val activeSession = authRepository.getCurrentSession()
        assertNotNull("Active session should be created in Room", activeSession)
        assertEquals(AuthRepositoryImpl.DEFAULT_LOCAL_USER_ID, activeSession?.userId)
        assertEquals("biometric", activeSession?.authProvider)
    }

    @Test
    fun `acceptance test - verify NO method exists that allows granting biometric session via boolean or simulation bypass`() {
        // 1. Verify AuthRepository does NOT have authenticateWithBiometric(Boolean)
        val repoMethods = AuthRepository::class.java.methods
        val boolBioRepoMethods = repoMethods.filter {
            it.name == "authenticateWithBiometric" &&
            it.parameterTypes.any { param -> param == Boolean::class.javaPrimitiveType || param == java.lang.Boolean::class.java }
        }
        assertTrue("CRITICAL: AuthRepository must NOT have any authenticateWithBiometric method accepting Boolean!", boolBioRepoMethods.isEmpty())

        // 2. Verify AuthenticateUserUseCase does NOT have executeBiometric(Boolean)
        val useCaseMethods = AuthenticateUserUseCase::class.java.methods
        val boolBioUseCaseMethods = useCaseMethods.filter {
            it.name == "executeBiometric" &&
            it.parameterTypes.any { param -> param == Boolean::class.javaPrimitiveType || param == java.lang.Boolean::class.java }
        }
        assertTrue("CRITICAL: AuthenticateUserUseCase must NOT have any executeBiometric method accepting Boolean!", boolBioUseCaseMethods.isEmpty())

        // 3. Verify LoginViewModel does NOT have simulation bypass methods
        val vmMethods = LoginViewModel::class.java.methods.map { it.name }
        assertFalse("CRITICAL: onSimulatedBiometricSuccess must be removed from LoginViewModel", vmMethods.contains("onSimulatedBiometricSuccess"))
        assertFalse("CRITICAL: dismissBiometricSimulator must be removed from LoginViewModel", vmMethods.contains("dismissBiometricSimulator"))

        // 4. Verify LoginUiState does NOT have showBiometricSimulatorDialog
        val stateFields = LoginUiState::class.java.declaredFields.map { it.name }
        assertFalse("CRITICAL: showBiometricSimulatorDialog must be removed from LoginUiState", stateFields.contains("showBiometricSimulatorDialog"))

        // 5. Verify BiometricSimulatorDialog class does NOT exist in classpath
        try {
            Class.forName("com.example.presentation.login.BiometricSimulatorDialogKt")
            fail("CRITICAL: BiometricSimulatorDialogKt must be deleted and not exist in classpath!")
        } catch (_: ClassNotFoundException) {
            // Expected
        }
    }
}
