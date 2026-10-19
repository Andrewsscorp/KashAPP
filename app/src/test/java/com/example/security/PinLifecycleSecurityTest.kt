package com.example.security

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.LocalAuthConfigDao
import com.example.data.local.dao.UserSessionDao
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.LocalAuthConfig
import com.example.data.local.entity.UserSessionEntity
import com.example.data.repository.AuthRepositoryImpl
import com.example.domain.models.UserAuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PinLifecycleSecurityTest {

    private lateinit var fakeLocalAuthConfigDao: FakeLocalAuthConfigDao
    private lateinit var fakeUserSessionDao: FakeUserSessionDao
    private lateinit var fakeAuditLogDao: FakeAuditLogDao
    private lateinit var authRepository: AuthRepositoryImpl

    private class FakeLocalAuthConfigDao : LocalAuthConfigDao {
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

        override suspend fun resetLockoutAndRecordLogin(loginTime: Long) {
            config = config?.copy(failedAttempts = 0, lockoutUntilTimestamp = 0L, lastLoginTimestamp = loginTime)
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

    private class FakeUserSessionDao : UserSessionDao {
        var activeSession: UserSessionEntity? = null
        override fun getActiveSessionFlow(): Flow<UserSessionEntity?> = flowOf(activeSession)
        override suspend fun getActiveSession(): UserSessionEntity? = activeSession
        override suspend fun getLastSession(): UserSessionEntity? = activeSession
        override suspend fun getSessionByUserId(userId: String): UserSessionEntity? = activeSession
        override fun getAllSessionsFlow(): Flow<List<UserSessionEntity>> = flowOf(listOfNotNull(activeSession))
        override suspend fun insertOrUpdateSession(session: UserSessionEntity) { activeSession = session }
        override suspend fun deactivateSession(userId: String) { activeSession = activeSession?.copy(isActive = false) }
        override suspend fun deactivateAllSessions() { activeSession = activeSession?.copy(isActive = false) }
        override suspend fun updateLastActive(userId: String, timestamp: Long) { activeSession = activeSession?.copy(lastActiveTimestamp = timestamp) }
        override suspend fun deleteSession(userId: String) { activeSession = null }
        override suspend fun clearAllSessions() { activeSession = null }
    }

    private class FakeAuditLogDao : AuditSecurityLogDao {
        val logs = mutableListOf<AuditSecurityLog>()
        override suspend fun insertLog(log: AuditSecurityLog) { logs.add(log) }
        override fun getAllLogs(): Flow<List<AuditSecurityLog>> = flowOf(logs)
        override suspend fun getAllLogsList(): List<AuditSecurityLog> = logs.toList()
        override fun getRecentLogs(limit: Int): Flow<List<AuditSecurityLog>> = flowOf(logs.take(limit))
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        fakeLocalAuthConfigDao = FakeLocalAuthConfigDao()
        fakeUserSessionDao = FakeUserSessionDao()
        fakeAuditLogDao = FakeAuditLogDao()

        authRepository = AuthRepositoryImpl(
            localAuthConfigDao = fakeLocalAuthConfigDao,
            auditSecurityLogDao = fakeAuditLogDao,
            firebaseDataSource = FirebaseDataSource(context),
            googleDriveManager = GoogleDriveManager(context),
            userSessionDao = fakeUserSessionDao
        )
    }

    @Test
    fun authenticateWithPin_whenNoPinConfigured_failsAndDoesNotAutoConfigure() = runBlocking {
        // Initially no PIN is stored
        assertFalse(authRepository.isPinConfigured())

        // Authentication attempt must be rejected and must NEVER auto-configure
        val result = authRepository.authenticateWithPin("1234")
        assertTrue("Expected Failure when unconfigured", result is UserAuthResult.Failure)
        val failure = result as UserAuthResult.Failure
        assertTrue(failure.errorMessage.contains("PIN no configurado"))

        // Confirm still unconfigured
        assertFalse("PIN should remain unconfigured after failed auth", authRepository.isPinConfigured())
    }

    @Test
    fun setupPin_withValidPin_configuresPinSuccessfully() = runBlocking {
        val success = authRepository.setupPin("5829")
        assertTrue(success)
        assertTrue(authRepository.isPinConfigured())

        val authResult = authRepository.authenticateWithPin("5829")
        assertTrue(authResult is UserAuthResult.Success)
    }

    @Test
    fun setupPin_withInvalidLengthOrNonDigits_returnsFalse() = runBlocking {
        assertFalse("Too short (< 4)", authRepository.setupPin("123"))
        assertFalse("Too long (> 8)", authRepository.setupPin("123456789"))
        assertFalse("Non numeric characters", authRepository.setupPin("12ab"))
        assertFalse(authRepository.isPinConfigured())
    }

    @Test
    fun changePin_whenNoPinConfigured_returnsFailure() = runBlocking {
        val result = authRepository.changePin("1234", "5829")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalStateException)
    }

    @Test
    fun changePin_withWrongCurrentPin_failsAndIncrementsFailedAttempts() = runBlocking {
        authRepository.setupPin("5829")

        val result = authRepository.changePin("9999", "8472")
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull() is IllegalArgumentException)
        assertTrue(result.exceptionOrNull()!!.message!!.contains("PIN actual incorrecto"))

        val config = fakeLocalAuthConfigDao.getConfig()
        assertEquals(1, config?.failedAttempts)
    }

    @Test
    fun changePin_withTrivialOrIdenticalNewPin_fails() = runBlocking {
        authRepository.setupPin("5829")

        val trivialPins = listOf("0000", "1111", "1234", "4321", "111111", "123456", "999999")
        for (trivial in trivialPins) {
            val result = authRepository.changePin("5829", trivial)
            assertTrue("Expected failure for trivial PIN: $trivial", result.isFailure)
            assertTrue(result.exceptionOrNull()!!.message!!.contains("predecible"))
        }

        val samePinResult = authRepository.changePin("5829", "5829")
        assertTrue(samePinResult.isFailure)
        assertTrue(samePinResult.exceptionOrNull()!!.message!!.contains("diferente"))
    }

    @Test
    fun changePin_withValidCurrentPinAndStrongNewPin_succeedsAndUpdatesCredentials() = runBlocking {
        authRepository.setupPin("5829")
        val oldHash = fakeLocalAuthConfigDao.getConfig()?.pinHash
        val oldSalt = fakeLocalAuthConfigDao.getConfig()?.pinSalt

        val changeResult = authRepository.changePin("5829", "9381")
        assertTrue(changeResult.isSuccess)

        val newHash = fakeLocalAuthConfigDao.getConfig()?.pinHash
        val newSalt = fakeLocalAuthConfigDao.getConfig()?.pinSalt

        assertNotEquals(oldHash, newHash)
        assertNotEquals(oldSalt, newSalt)
        assertEquals(0, fakeLocalAuthConfigDao.getConfig()?.failedAttempts)

        // Old PIN must no longer work
        val oldAuth = authRepository.authenticateWithPin("5829")
        assertTrue(oldAuth is UserAuthResult.Failure)

        // New PIN works
        val newAuth = authRepository.authenticateWithPin("9381")
        assertTrue(newAuth is UserAuthResult.Success)
    }

    @Test
    fun antiBruteForce_exponentialBackoffProgression() {
        assertEquals(0L, AuthRepositoryImpl.calculateLockoutDurationMs(0))
        assertEquals(0L, AuthRepositoryImpl.calculateLockoutDurationMs(1))
        assertEquals(0L, AuthRepositoryImpl.calculateLockoutDurationMs(2))
        assertEquals(30_000L, AuthRepositoryImpl.calculateLockoutDurationMs(3))
        assertEquals(60_000L, AuthRepositoryImpl.calculateLockoutDurationMs(4))
        assertEquals(300_000L, AuthRepositoryImpl.calculateLockoutDurationMs(5))
        assertEquals(900_000L, AuthRepositoryImpl.calculateLockoutDurationMs(6))
        assertEquals(900_000L, AuthRepositoryImpl.calculateLockoutDurationMs(7))
        assertEquals(1_800_000L, AuthRepositoryImpl.calculateLockoutDurationMs(8))
        assertEquals(1_800_000L, AuthRepositoryImpl.calculateLockoutDurationMs(10))
    }

    @Test
    fun resetLockout_clearsLockoutExpiration_butPreservesFailedAttempts() = runBlocking {
        authRepository.setupPin("5829")

        // 3 failed attempts to trigger lockout
        authRepository.authenticateWithPin("0001")
        authRepository.authenticateWithPin("0002")
        val lockedResult = authRepository.authenticateWithPin("0003")

        assertTrue(lockedResult is UserAuthResult.Failure)
        assertTrue((lockedResult as UserAuthResult.Failure).isLockedOut)
        assertEquals(3, fakeLocalAuthConfigDao.getConfig()?.failedAttempts)
        assertTrue(fakeLocalAuthConfigDao.getConfig()?.lockoutUntilTimestamp ?: 0L > 0L)

        // Countdown expires -> UI calls resetLockout()
        authRepository.resetLockout()

        // Lockout expiration cleared to allow attempt, BUT failed attempts preserved!
        assertEquals(0L, fakeLocalAuthConfigDao.getConfig()?.lockoutUntilTimestamp)
        assertEquals(3, fakeLocalAuthConfigDao.getConfig()?.failedAttempts)

        // 4th failure triggers 60s lockout (exponential tier 4), proving ladder was preserved
        val fourthResult = authRepository.authenticateWithPin("0004")
        assertTrue(fourthResult is UserAuthResult.Failure)
        val fourthFailure = fourthResult as UserAuthResult.Failure
        assertTrue(fourthFailure.isLockedOut)
        assertEquals(60L, fourthFailure.lockoutSecondsRemaining)
        assertEquals(4, fakeLocalAuthConfigDao.getConfig()?.failedAttempts)
    }
}
