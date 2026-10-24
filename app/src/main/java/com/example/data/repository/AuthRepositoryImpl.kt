package com.example.data.repository

import android.content.Context
import android.util.Log
import androidx.biometric.BiometricPrompt
import androidx.room.withTransaction
import com.example.data.datasource.FirebaseDataSource
import com.example.data.datasource.GoogleDriveManager
import com.example.data.local.Pbkdf2PinHasher
import com.example.data.local.SecurityDatabase
import com.example.data.local.dao.AssetAuditLogDao
import com.example.data.local.dao.AssetCategoryDao
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.dao.LocalAuthConfigDao
import com.example.data.local.dao.MerchantDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.dao.SharedExpenseDao
import com.example.data.local.dao.ThirdPartyCategoryDao
import com.example.data.local.dao.ThirdPartyDao
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
import java.io.File

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
    private val financialDao: FinancialDao? = null,
    private val fixedAssetDao: FixedAssetDao? = null,
    private val sharedExpenseDao: SharedExpenseDao? = null,
    private val thirdPartyDao: ThirdPartyDao? = null,
    private val thirdPartyCategoryDao: ThirdPartyCategoryDao? = null,
    private val merchantDao: MerchantDao? = null,
    private val outboxDao: OutboxDao? = null,
    private val assetAuditLogDao: AssetAuditLogDao? = null,
    private val assetCategoryDao: AssetCategoryDao? = null,
    private val autoCaptureDao: com.example.data.local.dao.AutoCaptureDao? = null,
    private val securityDatabase: SecurityDatabase? = null,
    private val context: Context? = null,
    private val keystoreCryptoManager: com.example.data.local.KeystoreCryptoManager = com.example.data.local.KeystoreCryptoManager(),
    private val ioDispatcher: kotlinx.coroutines.CoroutineDispatcher = kotlinx.coroutines.Dispatchers.IO,
    private val onLogoutCleanup: (suspend (String) -> Unit)? = null
) : AuthRepository {

    companion object {
        const val BASE_MAX_ATTEMPTS = 3
        const val MAX_FAILED_ATTEMPTS = 3
        const val LOCKOUT_DURATION_MS = 30_000L // 30 seconds initial anti-brute force lockout
        const val DEFAULT_LOCAL_USER_ID = "kash_user_01"

        fun calculateLockoutDurationMs(attempts: Int): Long {
            return when {
                attempts < 3 -> 0L
                attempts == 3 -> 30_000L       // 30 segundos
                attempts == 4 -> 60_000L       // 1 minuto
                attempts == 5 -> 300_000L      // 5 minutos
                attempts in 6..7 -> 900_000L   // 15 minutos
                else -> 1_800_000L             // 30 minutos
            }
        }
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
                    lockoutRemainingSeconds = 0L,
                    boundUserId = null
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
                    lockoutRemainingSeconds = remainingSeconds,
                    boundUserId = entity.boundUserId
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

    override suspend fun isPinConfigured(): Boolean {
        val config = localAuthConfigDao.getConfig()
        return config != null && config.pinHash.isNotBlank()
    }

    override suspend fun authenticateWithPin(pin: String): UserAuthResult {
        val now = System.currentTimeMillis()
        val currentConfig = localAuthConfigDao.getConfig()

        // Unconfigured PIN: strictly reject and require setting up a PIN
        if (currentConfig == null || currentConfig.pinHash.isBlank()) {
            return UserAuthResult.Failure(
                errorMessage = "PIN no configurado en el dispositivo. Debe configurar un PIN primero.",
                remainingAttempts = 0,
                isLockedOut = false,
                lockoutSecondsRemaining = 0L
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
            val boundUserId = currentConfig.boundUserId
            val targetSession = if (!boundUserId.isNullOrBlank()) {
                userSessionDao?.getActiveSessionByUserId(boundUserId)
                    ?: userSessionDao?.getSessionByUserId(boundUserId)
            } else {
                userSessionDao?.getActiveSession() ?: userSessionDao?.getLastActiveSession()
            }
            val currentFbUser = firebaseDataSource.getCurrentUser()
            val userId = targetSession?.userId ?: boundUserId ?: currentFbUser?.uid ?: DEFAULT_LOCAL_USER_ID
            val displayName = targetSession?.displayName ?: currentFbUser?.displayName ?: "Usuario KashApp"
            val email = targetSession?.email ?: currentFbUser?.email
            ensureLocalSession(userId, displayName, email, "local_pin")
            logSecurityEvent(
                eventType = "AUTH_PIN_SUCCESS",
                method = AuthMethod.PIN,
                details = "Autenticación exitosa por PIN para usuario: $userId"
            )
            UserAuthResult.Success(
                userId = userId,
                displayName = displayName,
                email = email,
                method = AuthMethod.PIN
            )
        } else {
            val newAttempts = currentConfig.failedAttempts + 1
            val lockoutDurationMs = calculateLockoutDurationMs(newAttempts)
            if (lockoutDurationMs > 0L) {
                val lockoutUntil = now + lockoutDurationMs
                localAuthConfigDao.updateAttempts(newAttempts, lockoutUntil)
                val lockoutSec = lockoutDurationMs / 1000L
                logSecurityEvent(
                    eventType = "AUTH_LOCKOUT_TRIGGERED",
                    method = AuthMethod.PIN,
                    details = "Bloqueo por $lockoutSec s tras $newAttempts intentos fallidos"
                )
                UserAuthResult.Failure(
                    errorMessage = "PIN incorrecto. $newAttempts intentos fallidos. Bloqueo de ${lockoutSec}s activado.",
                    remainingAttempts = 0,
                    isLockedOut = true,
                    lockoutSecondsRemaining = lockoutSec
                )
            } else {
                localAuthConfigDao.updateAttempts(newAttempts, 0L)
                val remaining = (BASE_MAX_ATTEMPTS - newAttempts).coerceAtLeast(0)
                logSecurityEvent(
                    eventType = "AUTH_PIN_FAILURE",
                    method = AuthMethod.PIN,
                    details = "Intento fallido de PIN: $newAttempts"
                )
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
        val cleanPin = pin.trim()
        if (cleanPin.length !in 4..8 || !cleanPin.all { it.isDigit() }) {
            return false
        }
        val hashResult = Pbkdf2PinHasher.createNewPinHash(cleanPin)
        val activeSession = userSessionDao?.getActiveSession() ?: userSessionDao?.getLastActiveSession()
        val currentFbUser = firebaseDataSource.getCurrentUser()
        val boundUserId = activeSession?.userId ?: currentFbUser?.uid
        val config = LocalAuthConfig(
            id = 1,
            pinHash = hashResult.hashFormatted,
            pinSalt = hashResult.saltBase64,
            isBiometricEnabled = true,
            failedAttempts = 0,
            lockoutUntilTimestamp = 0L,
            lastLoginTimestamp = System.currentTimeMillis(),
            boundUserId = boundUserId
        )
        localAuthConfigDao.insertOrUpdate(config)
        logSecurityEvent(
            eventType = "PIN_CONFIGURED",
            method = AuthMethod.PIN,
            details = "PIN configurado con hash y salt PBKDF2 vinculado a ${boundUserId ?: "dispositivo"}"
        )
        return true
    }

    override suspend fun changePin(currentPin: String, newPin: String): Result<Unit> {
        val currentConfig = localAuthConfigDao.getConfig()
        if (currentConfig == null || currentConfig.pinHash.isBlank()) {
            return Result.failure(IllegalStateException("No hay un PIN configurado actualmente en este dispositivo."))
        }

        val now = System.currentTimeMillis()
        if (currentConfig.lockoutUntilTimestamp > now) {
            val remainingSec = ((currentConfig.lockoutUntilTimestamp - now) / 1000L).coerceAtLeast(1L)
            return Result.failure(IllegalStateException("Dispositivo bloqueado temporalmente. Espera $remainingSec s."))
        }

        // 1. Verify current PIN
        val isCurrentValid = Pbkdf2PinHasher.verifyPin(currentPin, currentConfig.pinHash, currentConfig.pinSalt)
        if (!isCurrentValid) {
            val newAttempts = currentConfig.failedAttempts + 1
            val lockoutMs = calculateLockoutDurationMs(newAttempts)
            val lockoutUntil = if (lockoutMs > 0L) now + lockoutMs else 0L
            localAuthConfigDao.updateAttempts(newAttempts, lockoutUntil)
            logSecurityEvent(
                eventType = "AUTH_PIN_CHANGE_FAILED",
                method = AuthMethod.PIN,
                details = "Intento fallido de cambio de PIN. Intento $newAttempts"
            )
            val remaining = (BASE_MAX_ATTEMPTS - newAttempts).coerceAtLeast(0)
            val msg = if (lockoutMs > 0L) {
                "PIN actual incorrecto. Dispositivo bloqueado por ${lockoutMs / 1000L}s."
            } else {
                "PIN actual incorrecto. Te quedan $remaining intento(s)."
            }
            return Result.failure(IllegalArgumentException(msg))
        }

        // 2. Validate new PIN format and strength
        val cleanNew = newPin.trim()
        if (cleanNew.length !in 4..8 || !cleanNew.all { it.isDigit() }) {
            return Result.failure(IllegalArgumentException("El nuevo PIN debe tener entre 4 y 8 dígitos numéricos."))
        }

        val trivialPins = setOf(
            "0000", "1111", "2222", "3333", "4444", "5555", "6666", "7777", "8888", "9999",
            "1234", "4321", "000000", "111111", "123456", "654321"
        )
        if (cleanNew in trivialPins || cleanNew.all { it == cleanNew[0] }) {
            return Result.failure(IllegalArgumentException("El nuevo PIN es demasiado predecible. Elige uno más seguro."))
        }

        if (cleanNew == currentPin) {
            return Result.failure(IllegalArgumentException("El nuevo PIN debe ser diferente al PIN actual."))
        }

        // 3. Hash with new cryptographic salt
        val hashResult = Pbkdf2PinHasher.createNewPinHash(cleanNew)
        val updatedConfig = currentConfig.copy(
            pinHash = hashResult.hashFormatted,
            pinSalt = hashResult.saltBase64,
            failedAttempts = 0,
            lockoutUntilTimestamp = 0L,
            lastLoginTimestamp = now,
            boundUserId = currentConfig.boundUserId ?: userSessionDao?.getActiveSession()?.userId
        )
        localAuthConfigDao.insertOrUpdate(updatedConfig)

        logSecurityEvent(
            eventType = "AUTH_PIN_CHANGED",
            method = AuthMethod.PIN,
            details = "PIN de seguridad actualizado exitosamente con PBKDF2 y nuevo salt"
        )

        return Result.success(Unit)
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
        val currentConfig = localAuthConfigDao.getConfig()
        val boundUserId = currentConfig?.boundUserId
        val targetSession = if (!boundUserId.isNullOrBlank()) {
            userSessionDao?.getActiveSessionByUserId(boundUserId)
                ?: userSessionDao?.getSessionByUserId(boundUserId)
        } else {
            userSessionDao?.getActiveSession() ?: userSessionDao?.getLastActiveSession()
        }
        val currentFbUser = firebaseDataSource.getCurrentUser()
        val userId = targetSession?.userId ?: boundUserId ?: currentFbUser?.uid ?: DEFAULT_LOCAL_USER_ID
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
    ) = kotlinx.coroutines.withContext(ioDispatcher) {
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
        localAuthConfigDao.clearLockoutExpiration()
    }

    override suspend fun logout() {
        val activeUser = userSessionDao?.getActiveSession()
        val uid = activeUser?.userId.orEmpty()
        firebaseDataSource.signOut()
        userSessionDao?.deactivateAllSessions()
        try {
            onLogoutCleanup?.invoke(uid)
        } catch (e: Exception) {
            Log.w("AuthRepositoryImpl", "Notice during logout cleanup: ${e.message}")
        }
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
        if (userId.isBlank()) {
            return Result.failure(IllegalArgumentException("Cannot delete account: userId cannot be blank"))
        }
        return try {
            // 1. Delete Firestore cloud backups and Firebase Authentication account
            try {
                firebaseDataSource.deleteCloudAccountAndData(userId)
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Cloud account deletion notice: ${e.message}")
            }

            // 2. Delete all local financial records in strict topological order to respect SQLite guard triggers
            val performDeletions = suspend {
                // Step A: Dependent transactions and financial goals referencing accounts
                financialDao?.deleteTransactionsByUserId(userId)
                financialDao?.deleteGoalsByUserId(userId)
                financialDao?.deleteInvestmentsByUserId(userId)
                financialDao?.deleteScheduledPaymentsByUserId(userId)
                financialDao?.deleteBudgetItemsByUserId(userId)
                financialDao?.deleteBudgetsByUserId(userId)
                financialDao?.deleteIncomeSourcesByUserId(userId)

                // Step B: Fixed Asset movements & audit logs (prevents fixed_assets_delete_history_guard abort)
                assetAuditLogDao?.deleteAuditLogsForUserAssets(userId)
                assetAuditLogDao?.deleteAuditLogsByUserId(userId)
                fixedAssetDao?.deleteMovementsByUserId(userId)
                fixedAssetDao?.deleteAssetsByUserId(userId)
                assetCategoryDao?.deleteCategoriesByUserId(userId)

                // Step C: Shared expenses, settlement payments, participants, and groups
                sharedExpenseDao?.deleteSettlementPaymentsByUserId(userId)
                sharedExpenseDao?.deleteSharedExpenseLineItemsByUserId(userId)
                sharedExpenseDao?.deleteSharedExpenseSplitsByUserId(userId)
                sharedExpenseDao?.deleteSharedExpensePayersByUserId(userId)
                sharedExpenseDao?.deleteSharedExpensesByUserId(userId)
                sharedExpenseDao?.deleteGroupMembersByUserId(userId)
                sharedExpenseDao?.deleteGroupsByUserId(userId)
                sharedExpenseDao?.deleteParticipantsByUserId(userId)
                sharedExpenseDao?.deleteSequenceConfigsByUserId(userId)

                // Step D: Third parties and custom categories
                thirdPartyDao?.deleteThirdPartiesByUserId(userId)
                thirdPartyCategoryDao?.deleteCategoriesByUserId(userId)

                // Step E: Merchants, Outbox operations, and Pending auto-captures
                merchantDao?.deleteMerchantsByUserId(userId)
                outboxDao?.deleteOperationsByUserId(userId)
                (autoCaptureDao ?: securityDatabase?.autoCaptureDao())?.deleteAllByUserId(userId)

                // Step F: Accounts (now completely safe to delete since no transactions/goals/investments/payments reference them)
                financialDao?.deleteAccountsByUserId(userId)

                // Step G: User sessions
                userSessionDao?.deleteSession(userId)
                userSessionDao?.deactivateAllSessions()
            }

            if (securityDatabase != null) {
                securityDatabase.withTransaction {
                    performDeletions()
                }
            } else {
                performDeletions()
            }

            // 3. Purge local encrypted user files, vouchers, attachments, and exports
            context?.let { ctx ->
                try {
                    val hiddenDocsDir = File(ctx.filesDir, "hidden_docs")
                    if (hiddenDocsDir.exists()) {
                        hiddenDocsDir.listFiles()?.forEach { it.delete() }
                    }
                    val exportsDir = File(ctx.cacheDir, "shared_exports")
                    if (exportsDir.exists()) {
                        exportsDir.deleteRecursively()
                    }
                } catch (e: Exception) {
                    Log.w("AuthRepositoryImpl", "Notice during local user file purge: ${e.message}")
                }
            }

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

        // Rescate seguro de datos locales legados: solo si el usuario activo no posee aún cuentas personalizadas
        if (userId.isNotBlank() && userId != "user_default") {
            try {
                val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                val currentAccounts = financialDao?.getAccounts(userId).orEmpty()
                val hasUserAccounts = currentAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
                if (!hasUserAccounts) {
                    val legacyDefault = financialDao?.getAccounts("user_default").orEmpty()
                    if (legacyDefault.any { !dummyIds.contains(it.id) || it.balance > 0.0 }) {
                        financialDao?.migrateAccountsUserDefault(userId)
                        financialDao?.migrateTransactionsUserDefault(userId)
                        financialDao?.migrateGoalsUserDefault(userId)
                        financialDao?.migrateBudgetsUserDefault(userId)
                        financialDao?.migrateScheduledPaymentsUserDefault(userId)
                        financialDao?.migrateIncomeSourcesUserDefault(userId)
                        fixedAssetDao?.migrateAssetsUserDefault(userId)
                        fixedAssetDao?.migrateMovementsUserDefault(userId)
                        sharedExpenseDao?.migrateExpensesUserDefault(userId)
                        sharedExpenseDao?.migrateParticipantsUserDefault(userId)
                        sharedExpenseDao?.migrateGroupsUserDefault(userId)
                        thirdPartyDao?.migrateThirdPartiesUserDefault(userId)
                        thirdPartyCategoryDao?.migrateCategoriesUserDefault(userId)
                        Log.i("AuthRepositoryImpl", "✓ Se recuperaron datos legados de 'user_default' para $userId")
                    } else if (userId != "kash_user_01") {
                        val legacyKash01 = financialDao?.getAccounts("kash_user_01").orEmpty()
                        if (legacyKash01.any { !dummyIds.contains(it.id) || it.balance > 0.0 }) {
                            financialDao?.migrateAccountsFromUserId("kash_user_01", userId)
                            financialDao?.migrateTransactionsFromUserId("kash_user_01", userId)
                            financialDao?.migrateGoalsFromUserId("kash_user_01", userId)
                            financialDao?.migrateBudgetsFromUserId("kash_user_01", userId)
                            financialDao?.migrateScheduledPaymentsFromUserId("kash_user_01", userId)
                            financialDao?.migrateIncomeSourcesFromUserId("kash_user_01", userId)
                            Log.i("AuthRepositoryImpl", "✓ Se recuperaron datos legados de 'kash_user_01' para $userId")
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w("AuthRepositoryImpl", "Aviso en migración segura multi-tenant: ${e.message}")
            }
        }
    }
}
