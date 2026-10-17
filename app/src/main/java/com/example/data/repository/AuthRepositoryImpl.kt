package com.example.data.repository

import androidx.biometric.BiometricPrompt
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.Pbkdf2PinHasher
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.LocalAuthConfigDao
import com.example.data.local.dao.UserSessionDao
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.LocalAuthConfig
import com.example.data.local.entity.UserSessionEntity
import com.example.domain.models.AuditLogEntry
import com.example.domain.models.AuthMethod
import com.example.domain.models.SecurityConfig
import com.example.domain.models.SessionState
import com.example.domain.models.UserAuthResult
import com.example.domain.models.UserSession
import com.example.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import android.util.Log

/**
 * Concrete implementation of AuthRepository.
 * Bridges Firebase Authentication with the local encrypted Room database (SQLCipher)
 * for seamless, offline-first user session management, PBKDF2 PIN verification,
 * biometric authentication, anti-brute force lockout, and immutable security audit logs.
 */
class AuthRepositoryImpl(
    private val localAuthConfigDao: LocalAuthConfigDao,
    private val auditSecurityLogDao: AuditSecurityLogDao,
    private val firebaseDataSource: FirebaseDataSource,
    private val googleDriveManager: GoogleDriveManager,
    private val userSessionDao: UserSessionDao? = null,
    private val financialDao: com.example.data.local.dao.FinancialDao? = null,
    private val keystoreCryptoManager: com.example.data.local.KeystoreCryptoManager = com.example.data.local.KeystoreCryptoManager()
) : AuthRepository {

    companion object {
        const val MAX_FAILED_ATTEMPTS = 3
        const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds anti-brute force lockout
        const val DEFAULT_LOCAL_USER_ID = "kash_user_01"
    }

    override fun getSecurityConfig(): Flow<SecurityConfig> {
        return localAuthConfigDao.getConfigFlow().map { entity ->
            val now = System.currentTimeMillis()
            if (entity == null || entity.pinHash.isBlank()) {
                SecurityConfig(
                    hasPinSet = false,
                    isBiometricEnabled = true,
                    failedAttempts = 0,
                    maxAllowedAttempts = MAX_FAILED_ATTEMPTS,
                    isLockedOut = false,
                    lockoutRemainingSeconds = 0L
                )
            } else {
                val isLocked = entity.lockoutUntilTimestamp > now
                val remainingSeconds = if (isLocked) {
                    ((entity.lockoutUntilTimestamp - now) / 1000L).coerceAtLeast(1L)
                } else 0L

                SecurityConfig(
                    hasPinSet = true,
                    isBiometricEnabled = entity.isBiometricEnabled,
                    failedAttempts = entity.failedAttempts,
                    maxAllowedAttempts = MAX_FAILED_ATTEMPTS,
                    isLockedOut = isLocked,
                    lockoutRemainingSeconds = remainingSeconds
                )
            }
        }.catch { e ->
            Log.e("AuthRepositoryImpl", "Error reading security config from database: ${e.message}", e)
            emit(
                SecurityConfig(
                    hasPinSet = false,
                    isBiometricEnabled = true,
                    failedAttempts = 0,
                    maxAllowedAttempts = MAX_FAILED_ATTEMPTS,
                    isLockedOut = false,
                    lockoutRemainingSeconds = 0L
                )
            )
        }
    }

    override suspend fun authenticateWithPin(pin: String): UserAuthResult {
        val now = System.currentTimeMillis()
        val currentConfig = localAuthConfigDao.getConfig()

        // Default or unconfigured PIN fallback: automatically set up if first time
        if (currentConfig == null || currentConfig.pinHash.isBlank()) {
            val hashResult = Pbkdf2PinHasher.createNewPinHash(pin)
            val initialConfig = LocalAuthConfig(
                id = 1,
                pinHash = hashResult.hashFormatted,
                pinSalt = hashResult.saltBase64,
                isBiometricEnabled = true,
                failedAttempts = 0,
                lockoutUntilTimestamp = 0L,
                lastLoginTimestamp = now
            )
            localAuthConfigDao.insertOrUpdate(initialConfig)
            ensureLocalSession(DEFAULT_LOCAL_USER_ID, "Usuario KashApp", null, "local_pin")
            return UserAuthResult.Success(
                userId = DEFAULT_LOCAL_USER_ID,
                displayName = "Usuario KashApp",
                email = null,
                method = AuthMethod.PIN
            )
        }

        // Check if currently locked out by Anti-Brute Force mechanism
        if (currentConfig.lockoutUntilTimestamp > now) {
            val remainingSec = ((currentConfig.lockoutUntilTimestamp - now) / 1000L).coerceAtLeast(1L)
            return UserAuthResult.Failure(
                errorMessage = "Dispositivo bloqueado temporalmente por intentos fallidos. Espera $remainingSec s.",
                isLockedOut = true,
                lockoutSecondsRemaining = remainingSec
            )
        }

        // Verify the PBKDF2 hash using its stored salt
        val isValid = Pbkdf2PinHasher.verifyPin(pin, currentConfig.pinHash, currentConfig.pinSalt)

        return if (isValid) {
            localAuthConfigDao.resetLockoutAndRecordLogin(now)
            val targetSession = userSessionDao?.getActiveSession() ?: userSessionDao?.getLastSession()
            val currentFbUser = firebaseDataSource.getCurrentUser()
            val userId = targetSession?.userId ?: currentFbUser?.uid ?: DEFAULT_LOCAL_USER_ID
            val displayName = targetSession?.displayName ?: currentFbUser?.displayName ?: "Usuario KashApp"
            val email = targetSession?.email ?: currentFbUser?.email
            ensureLocalSession(userId, displayName, email, "local_pin")
            UserAuthResult.Success(
                userId = userId,
                displayName = displayName,
                email = email,
                method = AuthMethod.PIN
            )
        } else {
            val newAttempts = currentConfig.failedAttempts + 1
            if (newAttempts >= MAX_FAILED_ATTEMPTS) {
                val lockoutUntil = now + LOCKOUT_DURATION_MS
                localAuthConfigDao.updateAttempts(newAttempts, lockoutUntil)
                UserAuthResult.Failure(
                    errorMessage = "PIN incorrecto. 3 intentos fallidos consecutivos. Bloqueo de 30s activado.",
                    remainingAttempts = 0,
                    isLockedOut = true,
                    lockoutSecondsRemaining = LOCKOUT_DURATION_MS / 1000L
                )
            } else {
                localAuthConfigDao.updateAttempts(newAttempts, 0L)
                val remaining = MAX_FAILED_ATTEMPTS - newAttempts
                UserAuthResult.Failure(
                    errorMessage = "PIN incorrecto. Te quedan $remaining intento(s).",
                    remainingAttempts = remaining,
                    isLockedOut = false,
                    lockoutSecondsRemaining = 0L
                )
            }
        }
    }

    override suspend fun setupPin(pin: String): Boolean {
        val hashResult = Pbkdf2PinHasher.createNewPinHash(pin)
        val config = LocalAuthConfig(
            id = 1,
            pinHash = hashResult.hashFormatted,
            pinSalt = hashResult.saltBase64,
            isBiometricEnabled = true,
            failedAttempts = 0,
            lockoutUntilTimestamp = 0L,
            lastLoginTimestamp = System.currentTimeMillis()
        )
        localAuthConfigDao.insertOrUpdate(config)
        return true
    }

    override suspend fun authenticateWithBiometric(result: BiometricPrompt.AuthenticationResult): UserAuthResult {
        val cryptoObject = result.cryptoObject
        if (cryptoObject == null || !keystoreCryptoManager.verifyBiometricCryptoObject(cryptoObject)) {
            logSecurityEvent(
                eventType = "AUTH_FAILED",
                method = AuthMethod.BIOMETRIC,
                details = "Fallo en verificación criptográfica de CryptoObject en autenticación biométrica"
            )
            return UserAuthResult.Failure(
                errorMessage = "Autenticación biométrica fallida: CryptoObject no válido o no verificado por hardware.",
                isLockedOut = false
            )
        }

        val now = System.currentTimeMillis()
        localAuthConfigDao.resetLockoutAndRecordLogin(now)
        val targetSession = userSessionDao?.getActiveSession() ?: userSessionDao?.getLastSession()
        val currentFbUser = firebaseDataSource.getCurrentUser()
        val userId = targetSession?.userId ?: currentFbUser?.uid ?: DEFAULT_LOCAL_USER_ID
        val displayName = targetSession?.displayName ?: currentFbUser?.displayName ?: "Usuario KashApp"
        val email = targetSession?.email ?: currentFbUser?.email
        ensureLocalSession(userId, displayName, email, "biometric")
        logSecurityEvent(
            eventType = "AUTH_SUCCESS",
            method = AuthMethod.BIOMETRIC,
            details = "Usuario autenticado mediante huella dactilar para sesión: $userId"
        )
        return UserAuthResult.Success(
            userId = userId,
            displayName = displayName,
            email = email,
            method = AuthMethod.BIOMETRIC
        )
    }

    override suspend fun authenticateWithGoogle(
        idToken: String?,
        email: String?,
        displayName: String?
    ): UserAuthResult {
        if (idToken.isNullOrBlank()) {
            return UserAuthResult.Failure(
                errorMessage = "No se recibió un token de Google verificable.",
                isLockedOut = false
            )
        }

        val now = System.currentTimeMillis()
        var finalUserId: String
        var finalEmail: String? = email
        var finalDisplayName: String = displayName ?: "Usuario Google"
        var photoUrl: String? = null
        var isEmailVerified = false

        // Bridge with Firebase Authentication
        val userResult = firebaseDataSource.signInWithGoogleTokenToUser(idToken)
        if (userResult.isFailure) {
            return UserAuthResult.Failure(
                errorMessage = "No fue posible validar la cuenta de Google con Firebase.",
                isLockedOut = false
            )
        }
        val fbUser = userResult.getOrThrow()
        finalUserId = fbUser.uid
        finalEmail = fbUser.email ?: finalEmail
        finalDisplayName = fbUser.displayName ?: finalDisplayName
        photoUrl = fbUser.photoUrl?.toString()
        isEmailVerified = fbUser.isEmailVerified

        // Persist active session to local encrypted Room database
        val sessionEntity = UserSessionEntity(
            userId = finalUserId,
            email = finalEmail,
            displayName = finalDisplayName,
            photoUrl = photoUrl,
            authProvider = "firebase_google",
            isEmailVerified = isEmailVerified,
            idToken = idToken,
            sessionCreatedAt = now,
            lastLoginTimestamp = now,
            lastActiveTimestamp = now,
            isActive = true,
            isAnonymous = false
        )
        userSessionDao?.deactivateAllSessions()
        userSessionDao?.insertOrUpdateSession(sessionEntity)

        // Update local auth config login time
        localAuthConfigDao.resetLockoutAndRecordLogin(now)

        return UserAuthResult.Success(
            userId = finalUserId,
            displayName = finalDisplayName,
            email = finalEmail,
            method = AuthMethod.GOOGLE,
            token = idToken,
            driveSyncEnabled = false
        )
    }

    private suspend fun buildEncryptedBackupPayload(userId: String): String {
        return try {
            val accountsList = financialDao?.getAccounts(userId).orEmpty()
            val movementsList = financialDao?.getTransactions(userId).orEmpty()
            val budgetsList = financialDao?.getAllBudgetsWithItems(userId).orEmpty()

            val backupJson = org.json.JSONObject().apply {
                put("backupVersion", 2)
                put("timestamp", System.currentTimeMillis())
                put("userId", userId)

                val accountsArr = org.json.JSONArray()
                for (acc in accountsList) {
                    accountsArr.put(org.json.JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("type", acc.type)
                        put("balance", acc.balance)
                        put("currency", acc.currency)
                    })
                }
                put("accounts", accountsArr)

                val movementsArr = org.json.JSONArray()
                for (tx in movementsList) {
                    movementsArr.put(org.json.JSONObject().apply {
                        put("id", tx.id)
                        put("amount", tx.amount)
                        put("type", tx.type)
                        put("date", tx.date)
                        put("description", tx.description)
                        put("category", tx.categoryName)
                    })
                }
                put("movements", movementsArr)

                val budgetsArr = org.json.JSONArray()
                for (b in budgetsList) {
                    budgetsArr.put(org.json.JSONObject().apply {
                        put("id", b.budget.id)
                        put("name", b.budget.name)
                        put("totalLimit", b.budget.totalAmount)
                        put("spentAmount", b.items.sumOf { it.spentAmount })
                    })
                }
                put("budgets", budgetsArr)
            }.toString()

            keystoreCryptoManager.encryptData(backupJson)
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error al generar respaldo cifrado para Google Drive: ${e.message}", e)
            keystoreCryptoManager.encryptData("{\"backupVersion\":2,\"timestamp\":${System.currentTimeMillis()},\"userId\":\"$userId\",\"status\":\"error_generating\"}")
        }
    }

    override suspend fun logSecurityEvent(
        eventType: String,
        method: AuthMethod,
        details: String
    ) {
        val entry = AuditSecurityLog(
            timestamp = System.currentTimeMillis(),
            eventType = eventType,
            authMethod = method.displayName,
            details = details
        )
        auditSecurityLogDao.insertLog(entry)
    }

    override fun getAuditLogs(): Flow<List<AuditLogEntry>> {
        return auditSecurityLogDao.getAllLogs().map { list ->
            list.map { entity ->
                AuditLogEntry(
                    id = entity.id,
                    timestamp = entity.timestamp,
                    eventType = entity.eventType,
                    authMethod = entity.authMethod,
                    details = entity.details
                )
            }
        }.catch { e ->
            Log.e("AuthRepositoryImpl", "Error reading audit logs from database: ${e.message}", e)
            emit(emptyList())
        }
    }

    override suspend fun setBiometricEnabled(enabled: Boolean) {
        localAuthConfigDao.updateBiometricEnabled(enabled)
    }

    override suspend fun resetLockout() {
        localAuthConfigDao.resetLockoutAndRecordLogin(System.currentTimeMillis())
    }

    override suspend fun logout() {
        val activeUser = userSessionDao?.getActiveSession()
        firebaseDataSource.signOut()
        userSessionDao?.deactivateAllSessions()
        logSecurityEvent(
            eventType = "LOGOUT",
            method = AuthMethod.OTHER,
            details = "Sesión cerrada de Firebase y base cifrada local para: ${activeUser?.email ?: activeUser?.userId ?: "desconocido"}"
        )
    }

    override fun getActiveSession(): Flow<UserSession?> {
        return userSessionDao?.getActiveSessionFlow()?.map { it?.toDomain() }?.catch { e ->
            Log.e("AuthRepositoryImpl", "Error reading active session: ${e.message}", e)
            emit(null)
        } ?: flowOf(null)
    }

    override suspend fun getCurrentSession(): UserSession? {
        return userSessionDao?.getActiveSession()?.toDomain()
    }

    override suspend fun getLastSession(): UserSession? {
        return userSessionDao?.getLastSession()?.toDomain()
    }

    override fun observeSessionState(): Flow<SessionState> {
        return getActiveSession().map { session ->
            if (session != null && session.isActive) {
                SessionState.Authenticated(session)
            } else {
                SessionState.Unauthenticated
            }
        }
    }

    override suspend fun syncFirebaseSessionWithLocalDb(): Result<UserSession?> {
        return try {
            val fbUser = firebaseDataSource.getCurrentUser()
            if (fbUser != null) {
                val now = System.currentTimeMillis()
                val token = firebaseDataSource.getIdToken(false).getOrNull()
                val sessionEntity = UserSessionEntity(
                    userId = fbUser.uid,
                    email = fbUser.email,
                    displayName = fbUser.displayName ?: "Usuario Firebase",
                    photoUrl = fbUser.photoUrl?.toString(),
                    authProvider = if (fbUser.isAnonymous) "firebase_anonymous" else "firebase",
                    isEmailVerified = fbUser.isEmailVerified,
                    idToken = token,
                    sessionCreatedAt = fbUser.metadata?.creationTimestamp ?: now,
                    lastLoginTimestamp = now,
                    lastActiveTimestamp = now,
                    isActive = true,
                    isAnonymous = fbUser.isAnonymous
                )
                userSessionDao?.deactivateAllSessions()
                userSessionDao?.insertOrUpdateSession(sessionEntity)
                localAuthConfigDao.resetLockoutAndRecordLogin(now)
                Result.success(sessionEntity.toDomain())
            } else {
                // If Firebase has no user currently online, check local encrypted session (Offline First)
                val localSession = userSessionDao?.getActiveSession()
                if (localSession != null && localSession.isActive) {
                    Result.success(localSession.toDomain())
                } else {
                    Result.success(null)
                }
            }
        } catch (e: Exception) {
            // Offline fallback: rely on local encrypted database session
            val localSession = userSessionDao?.getActiveSession()
            if (localSession != null) {
                Result.success(localSession.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun signInWithEmailAndPassword(
        email: String,
        password: String
    ): Result<UserSession> {
        return try {
            val fbResult = firebaseDataSource.signInWithEmailAndPassword(email, password)
            if (fbResult.isFailure) {
                val err = fbResult.exceptionOrNull() ?: Exception("Error al autenticar con Firebase")
                logSecurityEvent("AUTH_FAILURE", AuthMethod.OTHER, "Error login email $email: ${err.message}")
                return Result.failure(err)
            }
            val fbUser = fbResult.getOrThrow()
            val now = System.currentTimeMillis()
            val token = firebaseDataSource.getIdToken(false).getOrNull()
            val session = UserSessionEntity(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = fbUser.displayName ?: email.substringBefore("@"),
                photoUrl = fbUser.photoUrl?.toString(),
                authProvider = "firebase_password",
                isEmailVerified = fbUser.isEmailVerified,
                idToken = token,
                sessionCreatedAt = fbUser.metadata?.creationTimestamp ?: now,
                lastLoginTimestamp = now,
                lastActiveTimestamp = now,
                isActive = true,
                isAnonymous = false
            )
            userSessionDao?.deactivateAllSessions()
            userSessionDao?.insertOrUpdateSession(session)
            localAuthConfigDao.resetLockoutAndRecordLogin(now)
            logSecurityEvent("AUTH_SUCCESS", AuthMethod.OTHER, "Login exitoso con correo y sincronizado en Room cifrado")
            Result.success(session.toDomain())
        } catch (e: Exception) {
            logSecurityEvent("AUTH_ERROR", AuthMethod.OTHER, "Excepción login email: ${e.message}")
            Result.failure(e)
        }
    }

    override suspend fun signUpWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String
    ): Result<UserSession> {
        return try {
            val fbResult = firebaseDataSource.createUserWithEmailAndPassword(email, password, displayName)
            if (fbResult.isFailure) {
                val err = fbResult.exceptionOrNull() ?: Exception("Error al crear cuenta en Firebase")
                logSecurityEvent("REGISTER_FAILURE", AuthMethod.OTHER, "Fallo registro email $email: ${err.message}")
                return Result.failure(err)
            }
            val fbUser = fbResult.getOrThrow()
            val now = System.currentTimeMillis()
            val token = firebaseDataSource.getIdToken(false).getOrNull()
            val session = UserSessionEntity(
                userId = fbUser.uid,
                email = fbUser.email ?: email,
                displayName = displayName.ifBlank { fbUser.displayName ?: email.substringBefore("@") },
                photoUrl = fbUser.photoUrl?.toString(),
                authProvider = "firebase_password",
                isEmailVerified = fbUser.isEmailVerified,
                idToken = token,
                sessionCreatedAt = now,
                lastLoginTimestamp = now,
                lastActiveTimestamp = now,
                isActive = true,
                isAnonymous = false
            )
            userSessionDao?.deactivateAllSessions()
            userSessionDao?.insertOrUpdateSession(session)
            localAuthConfigDao.resetLockoutAndRecordLogin(now)
            logSecurityEvent("ACCOUNT_REGISTERED", AuthMethod.OTHER, "Cuenta creada y sincronizada en Room cifrado para $email")
            Result.success(session.toDomain())
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun signInAnonymously(): Result<UserSession> {
        return try {
            val fbResult = firebaseDataSource.signInAnonymously()
            val (userId, authProvider) = if (fbResult.isSuccess) {
                val fbUser = fbResult.getOrThrow()
                Pair(fbUser.uid, "firebase_anonymous")
            } else {
                Pair("guest_user_${System.currentTimeMillis()}", "local_guest")
            }
            val now = System.currentTimeMillis()
            val session = UserSessionEntity(
                userId = userId,
                email = null,
                displayName = "Invitado",
                photoUrl = null,
                authProvider = authProvider,
                isEmailVerified = false,
                idToken = null,
                sessionCreatedAt = now,
                lastLoginTimestamp = now,
                lastActiveTimestamp = now,
                isActive = true,
                isAnonymous = true
            )
            userSessionDao?.deactivateAllSessions()
            userSessionDao?.insertOrUpdateSession(session)
            localAuthConfigDao.resetLockoutAndRecordLogin(now)
            Result.success(session.toDomain())
        } catch (e: Exception) {
            val now = System.currentTimeMillis()
            val fallbackSession = UserSessionEntity(
                userId = "guest_user_$now",
                email = null,
                displayName = "Invitado",
                photoUrl = null,
                authProvider = "local_guest",
                isEmailVerified = false,
                idToken = null,
                sessionCreatedAt = now,
                lastLoginTimestamp = now,
                lastActiveTimestamp = now,
                isActive = true,
                isAnonymous = true
            )
            userSessionDao?.deactivateAllSessions()
            userSessionDao?.insertOrUpdateSession(fallbackSession)
            Result.success(fallbackSession.toDomain())
        }
    }

    override suspend fun touchSession(userId: String) {
        userSessionDao?.updateLastActive(userId, System.currentTimeMillis())
    }

    override suspend fun clearSession() {
        userSessionDao?.clearAllSessions()
    }

    override suspend fun deleteAccountPermanently(userId: String): Result<Unit> {
        return try {
            // 1. Delete Firestore cloud backups and Firebase Authentication account
            firebaseDataSource.deleteCloudAccountAndData(userId)

            // 2. Delete all local financial records belonging to this user
            financialDao?.deleteAccountsByUserId(userId)
            financialDao?.deleteTransactionsByUserId(userId)
            financialDao?.deleteGoalsByUserId(userId)
            financialDao?.deleteBudgetsByUserId(userId)
            financialDao?.deleteInvestmentsByUserId(userId)
            financialDao?.deleteScheduledPaymentsByUserId(userId)

            // 3. Purge user session and deactivate all local sessions
            userSessionDao?.deleteSession(userId)
            userSessionDao?.deactivateAllSessions()

            logSecurityEvent(
                eventType = "ACCOUNT_DELETED",
                method = AuthMethod.OTHER,
                details = "Cuenta y datos eliminados de acuerdo a políticas de Google para $userId"
            )
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("AuthRepositoryImpl", "Error al eliminar cuenta permanentemente: ${e.message}", e)
            Result.failure(e)
        }
    }

    private suspend fun ensureLocalSession(
        userId: String,
        displayName: String,
        email: String?,
        provider: String
    ) {
        val now = System.currentTimeMillis()
        val existing = userSessionDao?.getSessionByUserId(userId)
        val entity = existing?.copy(
            lastActiveTimestamp = now,
            isActive = true
        ) ?: UserSessionEntity(
            userId = userId,
            displayName = displayName,
            email = email,
            authProvider = provider,
            sessionCreatedAt = now,
            lastLoginTimestamp = now,
            lastActiveTimestamp = now,
            isActive = true
        )
        userSessionDao?.deactivateAllSessions()
        userSessionDao?.insertOrUpdateSession(entity)
    }
}
