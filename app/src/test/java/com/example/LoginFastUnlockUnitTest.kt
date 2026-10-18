package com.example

import android.content.Context
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
import com.example.domain.usecase.AuthenticateUserUseCase
import com.example.presentation.login.LoginViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
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
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class LoginFastUnlockUnitTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var context: Context
    private lateinit var fakeUserSessionDao: FakeUserSessionDao
    private lateinit var fakeLocalAuthConfigDao: FakeLocalAuthConfigDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao
    private lateinit var authRepository: AuthRepositoryImpl
    private lateinit var authenticateUserUseCase: AuthenticateUserUseCase

    private class FakeUserSessionDao : UserSessionDao {
        val sessions = mutableMapOf<String, UserSessionEntity>()
        private val _flow = MutableStateFlow<UserSessionEntity?>(null)

        override fun getActiveSessionFlow(): Flow<UserSessionEntity?> = _flow
        override suspend fun getActiveSession(): UserSessionEntity? = _flow.value
        override suspend fun getLastSession(): UserSessionEntity? =
            sessions.values.maxByOrNull { it.lastActiveTimestamp }
        override suspend fun getSessionByUserId(userId: String): UserSessionEntity? = sessions[userId]
        override fun getAllSessionsFlow(): Flow<List<UserSessionEntity>> = flowOf(sessions.values.toList())
        override suspend fun insertOrUpdateSession(session: UserSessionEntity) {
            sessions[session.userId] = session
            if (session.isActive) _flow.value = session
        }
        override suspend fun deactivateSession(userId: String) {
            sessions[userId]?.let { sessions[userId] = it.copy(isActive = false) }
            if (_flow.value?.userId == userId) _flow.value = null
        }
        override suspend fun deactivateAllSessions() {
            sessions.keys.forEach { id -> sessions[id] = sessions[id]!!.copy(isActive = false) }
            _flow.value = null
        }
        override suspend fun updateLastActive(userId: String, timestamp: Long) {
            sessions[userId]?.let { sessions[userId] = it.copy(lastActiveTimestamp = timestamp) }
        }
        override suspend fun deleteSession(userId: String) {
            sessions.remove(userId)
            if (_flow.value?.userId == userId) _flow.value = null
        }
        override suspend fun clearAllSessions() {
            sessions.clear()
            _flow.value = null
        }
    }

    private class FakeLocalAuthConfigDao : LocalAuthConfigDao {
        var config: LocalAuthConfig? = null
        private val _flow = MutableStateFlow<LocalAuthConfig?>(null)

        override fun getConfigFlow(): Flow<LocalAuthConfig?> = _flow
        override suspend fun getConfig(): LocalAuthConfig? = config
        override suspend fun insertOrUpdate(c: LocalAuthConfig) {
            config = c
            _flow.value = c
        }
        override suspend fun updateAttempts(failedAttempts: Int, lockoutUntil: Long) {
            config = config?.copy(failedAttempts = failedAttempts, lockoutUntilTimestamp = lockoutUntil)
            _flow.value = config
        }
        override suspend fun resetLockoutAndRecordLogin(loginTime: Long) {
            config = config?.copy(failedAttempts = 0, lockoutUntilTimestamp = 0L, lastLoginTimestamp = loginTime)
            _flow.value = config
        }
        override suspend fun clearLockoutExpiration() {
            config = config?.copy(lockoutUntilTimestamp = 0L)
            _flow.value = config
        }
        override suspend fun updateBiometricEnabled(enabled: Boolean) {
            config = config?.copy(isBiometricEnabled = enabled)
            _flow.value = config
        }
    }

    private class FakeAuditLogDao : AuditSecurityLogDao {
        val logs = mutableListOf<AuditSecurityLog>()
        override suspend fun insertLog(log: AuditSecurityLog) { logs.add(log) }
        override fun getAllLogs(): Flow<List<AuditSecurityLog>> = flowOf(logs)
        override fun getRecentLogs(limit: Int): Flow<List<AuditSecurityLog>> = flowOf(logs.take(limit))
    }

    private class TestableBiometricDataSource(
        context: Context,
        cryptoManager: KeystoreCryptoManager,
        var canAuth: Boolean
    ) : BiometricDataSource(context, cryptoManager) {
        override fun canAuthenticate(): Boolean = canAuth
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext()
        val cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        fakeUserSessionDao = FakeUserSessionDao()
        fakeLocalAuthConfigDao = FakeLocalAuthConfigDao()
        fakeAuditLogDao = FakeAuditLogDao()

        authRepository = AuthRepositoryImpl(
            userSessionDao = fakeUserSessionDao,
            localAuthConfigDao = fakeLocalAuthConfigDao,
            auditSecurityLogDao = fakeAuditLogDao,
            firebaseDataSource = FirebaseDataSource(context),
            googleDriveManager = GoogleDriveManager(context),
            keystoreCryptoManager = cryptoManager
        )
        authenticateUserUseCase = AuthenticateUserUseCase(authRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `startup with remembered user and biometric enabled triggers autoPromptBiometric`() = runTest(testDispatcher) {
        // Setup existing user session and enabled biometrics
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_david_99",
                email = "david@kashapp.com",
                displayName = "David Suarez",
                photoUrl = null,
                authProvider = "firebase_google",
                sessionCreatedAt = System.currentTimeMillis(),
                lastLoginTimestamp = System.currentTimeMillis(),
                lastActiveTimestamp = System.currentTimeMillis(),
                isActive = true
            )
        )
        fakeLocalAuthConfigDao.insertOrUpdate(
            LocalAuthConfig(id = 1, pinHash = "pin_hash_123", pinSalt = "salt_123", isBiometricEnabled = true)
        )

        val biometricDataSource = TestableBiometricDataSource(context, KeystoreCryptoManager(true), canAuth = true)
        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = biometricDataSource
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Remembered user should be resolved", state.rememberedUser)
        assertEquals("user_david_99", state.rememberedUser?.userId)
        assertEquals("David Suarez", state.rememberedUser?.displayName)
        assertTrue("autoPromptBiometricTrigger must be armed (> 0)", state.autoPromptBiometricTrigger > 0L)
        assertFalse("showPinDialog should be false when biometric is triggered", state.showPinDialog)

        // Consuming the trigger clears it to 0
        viewModel.consumeAutoPromptBiometric()
        assertEquals(0L, viewModel.uiState.value.autoPromptBiometricTrigger)
    }

    @Test
    fun `startup with remembered user but biometrics unavailable triggers PIN dialog directly`() = runTest(testDispatcher) {
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_pin_only",
                email = "user@kashapp.com",
                displayName = "Usuario PIN",
                authProvider = "local_pin",
                sessionCreatedAt = System.currentTimeMillis(),
                lastLoginTimestamp = System.currentTimeMillis(),
                lastActiveTimestamp = System.currentTimeMillis(),
                isActive = true
            )
        )
        fakeLocalAuthConfigDao.insertOrUpdate(
            LocalAuthConfig(id = 1, pinHash = "hash_456", pinSalt = "salt_456", isBiometricEnabled = true)
        )

        // Biometrics cannot authenticate on hardware
        val biometricDataSource = TestableBiometricDataSource(context, KeystoreCryptoManager(true), canAuth = false)
        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = biometricDataSource
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull("Remembered user should be loaded", state.rememberedUser)
        assertEquals("user_pin_only", state.rememberedUser?.userId)
        assertEquals(0L, state.autoPromptBiometricTrigger)
        assertTrue("showPinDialog must be true directly when biometrics is unavailable", state.showPinDialog)
    }

    @Test
    fun `startup with fresh install and no session does NOT trigger auto unlock`() = runTest(testDispatcher) {
        val biometricDataSource = TestableBiometricDataSource(context, KeystoreCryptoManager(true), canAuth = true)
        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = biometricDataSource
        )

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Remembered user should be null on clean install", state.rememberedUser)
        assertEquals(0L, state.autoPromptBiometricTrigger)
        assertFalse("showPinDialog should be false on clean install", state.showPinDialog)
    }

    @Test
    fun `lockSession locks dashboard and rearms biometric prompt while preserving rememberedUser`() = runTest(testDispatcher) {
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_lock_test",
                email = "lock@kashapp.com",
                displayName = "Lock User",
                authProvider = "firebase_google",
                sessionCreatedAt = System.currentTimeMillis(),
                lastLoginTimestamp = System.currentTimeMillis(),
                lastActiveTimestamp = System.currentTimeMillis(),
                isActive = true
            )
        )
        fakeLocalAuthConfigDao.insertOrUpdate(
            LocalAuthConfig(id = 1, pinHash = "hash", pinSalt = "salt", isBiometricEnabled = true)
        )

        val biometricDataSource = TestableBiometricDataSource(context, KeystoreCryptoManager(true), canAuth = true)
        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = biometricDataSource
        )

        advanceUntilIdle()

        // Lock session (e.g. user presses back or minimizes)
        viewModel.lockSession()
        advanceUntilIdle()

        val lockedState = viewModel.uiState.value
        assertNull("authenticatedUser must be null when locked", lockedState.authenticatedUser)
        assertNotNull("rememberedUser must be retained", lockedState.rememberedUser)
        assertEquals("user_lock_test", lockedState.rememberedUser?.userId)
        assertTrue("Biometric trigger must be rearmed", lockedState.autoPromptBiometricTrigger > 0L)
    }

    @Test
    fun `onSwitchAccount logs out and clears remembered user to allow new login`() = runTest(testDispatcher) {
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_switch",
                email = "switch@kashapp.com",
                displayName = "Switch User",
                authProvider = "firebase_google",
                sessionCreatedAt = System.currentTimeMillis(),
                lastLoginTimestamp = System.currentTimeMillis(),
                lastActiveTimestamp = System.currentTimeMillis(),
                isActive = true
            )
        )
        fakeLocalAuthConfigDao.insertOrUpdate(
            LocalAuthConfig(id = 1, pinHash = "hash", pinSalt = "salt", isBiometricEnabled = true)
        )

        val biometricDataSource = TestableBiometricDataSource(context, KeystoreCryptoManager(true), canAuth = true)
        val viewModel = LoginViewModel(
            authenticateUserUseCase = authenticateUserUseCase,
            authRepository = authRepository,
            googleAuthDataSource = GoogleAuthDataSource(context),
            biometricDataSource = biometricDataSource
        )

        advanceUntilIdle()
        assertNotNull(viewModel.uiState.value.rememberedUser)

        viewModel.onSwitchAccount()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("rememberedUser must be cleared after onSwitchAccount", state.rememberedUser)
        assertNull("authenticatedUser must be null", state.authenticatedUser)
    }
}
