package com.example

import androidx.biometric.BiometricPrompt
import com.example.data.datasource.FirebaseDataSource
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
import com.example.domain.models.SessionState
import com.example.domain.models.UserAuthResult
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import androidx.test.core.app.ApplicationProvider
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AuthRepositorySessionTest {

    private lateinit var fakeUserSessionDao: FakeUserSessionDao
    private lateinit var fakeLocalAuthConfigDao: FakeLocalAuthConfigDao
    private lateinit var fakeAuditLogDao: FakeAuditSecurityLogDao
    private lateinit var authRepository: AuthRepositoryImpl

    class FakeUserSessionDao : UserSessionDao {
        val sessions = mutableMapOf<String, UserSessionEntity>()
        private val _flow = MutableStateFlow<UserSessionEntity?>(null)

        private fun updateFlow() {
            val active = sessions.values
                .filter { it.isActive }
                .maxByOrNull { it.lastActiveTimestamp }
            _flow.value = active
        }

        override fun getActiveSessionFlow(): Flow<UserSessionEntity?> = _flow

        override suspend fun getActiveSession(): UserSessionEntity? {
            return sessions.values
                .filter { it.isActive }
                .maxByOrNull { it.lastActiveTimestamp }
        }

        override suspend fun getLastSession(): UserSessionEntity? {
            return sessions.values.maxByOrNull { it.lastActiveTimestamp }
        }

        override suspend fun getSessionByUserId(userId: String): UserSessionEntity? = sessions[userId]

        override fun getAllSessionsFlow(): Flow<List<UserSessionEntity>> =
            _flow.map { sessions.values.toList() }

        override suspend fun insertOrUpdateSession(session: UserSessionEntity) {
            sessions[session.userId] = session
            updateFlow()
        }

        override suspend fun deactivateSession(userId: String) {
            sessions[userId]?.let {
                sessions[userId] = it.copy(isActive = false)
            }
            updateFlow()
        }

        override suspend fun deactivateAllSessions() {
            sessions.keys.forEach { k ->
                sessions[k] = sessions[k]!!.copy(isActive = false)
            }
            updateFlow()
        }

        override suspend fun updateLastActive(userId: String, timestamp: Long) {
            sessions[userId]?.let {
                sessions[userId] = it.copy(lastActiveTimestamp = timestamp)
            }
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
        var config: LocalAuthConfig? = LocalAuthConfig(id = 1)
        private val flow = MutableStateFlow<LocalAuthConfig?>(config)

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
            config = config?.copy(failedAttempts = 0, lockoutUntilTimestamp = 0, lastLoginTimestamp = loginTime)
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

    class FakeAuditSecurityLogDao : AuditSecurityLogDao {
        val logs = mutableListOf<AuditSecurityLog>()
        private val flow = MutableStateFlow<List<AuditSecurityLog>>(emptyList())

        override suspend fun insertLog(log: AuditSecurityLog) {
            logs.add(log)
            flow.value = logs.toList()
        }

        override fun getAllLogs(): Flow<List<AuditSecurityLog>> = flow

        override fun getRecentLogs(limit: Int): Flow<List<AuditSecurityLog>> =
            flow.map { list -> list.take(limit) }
    }

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        fakeUserSessionDao = FakeUserSessionDao()
        fakeLocalAuthConfigDao = FakeLocalAuthConfigDao()
        fakeAuditLogDao = FakeAuditSecurityLogDao()
        val realFirebaseDataSource = FirebaseDataSource(context)
        val realGoogleDriveManager = GoogleDriveManager(context)

        authRepository = AuthRepositoryImpl(
            localAuthConfigDao = fakeLocalAuthConfigDao,
            auditSecurityLogDao = fakeAuditLogDao,
            firebaseDataSource = realFirebaseDataSource,
            googleDriveManager = realGoogleDriveManager,
            userSessionDao = fakeUserSessionDao
        )
    }

    @Test
    fun authenticateWithPin_createsLocalRoomSession() = runBlocking {
        val setupSuccess = authRepository.setupPin("1234")
        assertTrue(setupSuccess)

        val result = authRepository.authenticateWithPin("1234")
        assertTrue(result is UserAuthResult.Success)

        val session = authRepository.getCurrentSession()
        assertNotNull(session)
        assertTrue(session?.isActive == true)
        assertEquals("kash_user_01", session?.userId)
        assertEquals("local_pin", session?.authProvider)

        val sessionState = authRepository.observeSessionState().first()
        assertTrue(sessionState is SessionState.Authenticated)
    }

    @Test
    fun authenticateWithBiometric_createsLocalRoomSession() = runBlocking {
        val cryptoManager = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val cryptoObject = cryptoManager.createBiometricCryptoObject()
        assertNotNull(cryptoObject)
        val authResult = biometricAuthenticationResult(cryptoObject)
        val result = authRepository.authenticateWithBiometric(authResult)
        assertTrue(result is UserAuthResult.Success)

        val session = authRepository.getCurrentSession()
        assertNotNull(session)
        assertTrue(session?.isActive == true)
        assertEquals(AuthRepositoryImpl.DEFAULT_LOCAL_USER_ID, session?.userId)
        assertEquals("biometric", session?.authProvider)
    }

    @Test
    fun authenticateWithBiometric_withNullCryptoObject_failsAndDoesNotCreateSession() = runBlocking {
        val authResult = biometricAuthenticationResult(null)
        val result = authRepository.authenticateWithBiometric(authResult)
        assertTrue(result is UserAuthResult.Failure)

        val session = authRepository.getCurrentSession()
        assertNull(session)
    }

    @Test
    fun logout_deactivatesLocalRoomSession() = runBlocking {
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_test_99",
                email = "tester@kashapp.com",
                displayName = "Tester User",
                isActive = true
            )
        )

        assertNotNull(authRepository.getCurrentSession())

        authRepository.logout()

        val afterLogout = authRepository.getCurrentSession()
        assertNull(afterLogout)

        val state = authRepository.observeSessionState().first()
        assertTrue(state is SessionState.Unauthenticated)

        val logoutLogs = fakeAuditLogDao.logs.filter { it.eventType == "LOGOUT" }
        assertEquals(1, logoutLogs.size)
    }

    @Test
    fun syncFirebaseSessionWithLocalDb_preservesExistingActiveSessionWhenOffline() = runBlocking {
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "offline_user_1",
                email = "offline@kashapp.com",
                displayName = "Offline Officer",
                authProvider = "firebase_google",
                isActive = true
            )
        )

        val syncResult = authRepository.syncFirebaseSessionWithLocalDb()
        assertTrue(syncResult.isSuccess)
        val session = syncResult.getOrNull()
        assertNotNull(session)
        assertEquals("offline_user_1", session?.userId)
        assertEquals("offline@kashapp.com", session?.email)
    }

    @Test
    fun recordActivity_updatesLastActiveTimestamp() = runBlocking {
        val now = System.currentTimeMillis()
        fakeUserSessionDao.insertOrUpdateSession(
            UserSessionEntity(
                userId = "user_act",
                email = "act@kashapp.com",
                displayName = "Activity User",
                isActive = true,
                lastActiveTimestamp = now - 50000
            )
        )

        authRepository.touchSession("user_act")

        val updated = fakeUserSessionDao.sessions["user_act"]
        assertNotNull(updated)
        assertTrue(updated!!.lastActiveTimestamp >= now)
    }

    @Test
    fun authenticateWithGoogle_whenFirebaseRejectsToken_failsWithoutCreatingSession() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val rejectingFirebase = object : FirebaseDataSource(context) {
            override suspend fun signInWithGoogleTokenToUser(idToken: String) =
                Result.failure<com.google.firebase.auth.FirebaseUser>(IllegalArgumentException("invalid token"))
        }
        val repository = AuthRepositoryImpl(
            localAuthConfigDao = fakeLocalAuthConfigDao,
            auditSecurityLogDao = fakeAuditLogDao,
            firebaseDataSource = rejectingFirebase,
            googleDriveManager = GoogleDriveManager(context),
            userSessionDao = fakeUserSessionDao
        )

        val result = repository.authenticateWithGoogle("invalid-token", "attacker@example.com", "Attacker")

        assertTrue(result is UserAuthResult.Failure)
        assertNull(fakeUserSessionDao.getActiveSession())
    }
}
