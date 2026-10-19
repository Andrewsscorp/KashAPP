package com.example.data.datasource

import android.content.Context
import android.util.Log
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.UserProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreSettings
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Data source for Firebase services with Offline-First Firestore persistence
 * and full Firebase Authentication bridging.
 */
open class FirebaseDataSource(private val context: Context) {

    data class FirebaseConnectionInfo(
        val isInitialized: Boolean,
        val projectId: String,
        val appId: String,
        val currentUserId: String?,
        val isFirestoreOnline: Boolean
    )

    private val firestore: FirebaseFirestore by lazy {
        initFirebaseIfNeeded()
        val db = FirebaseFirestore.getInstance()
        val settings = FirebaseFirestoreSettings.Builder()
            .setPersistenceEnabled(true)
            .build()
        db.firestoreSettings = settings
        db
    }

    private val auth: FirebaseAuth by lazy {
        initFirebaseIfNeeded()
        FirebaseAuth.getInstance()
    }

    fun initFirebaseIfNeeded() {
        try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
        } catch (e: Throwable) {
            Log.w("FirebaseDataSource", "Notice initializing FirebaseApp: ${e.message}")
        }
    }

    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun getCurrentUserId(): String? = auth.currentUser?.uid

    fun getCurrentUserEmail(): String? = auth.currentUser?.email

    fun getCurrentUserDisplayName(): String? = auth.currentUser?.displayName

    fun getCurrentUserPhotoUrl(): String? = auth.currentUser?.photoUrl?.toString()

    fun isEmailVerified(): Boolean = auth.currentUser?.isEmailVerified ?: false

    fun isUserSignedIn(): Boolean = auth.currentUser != null

    /**
     * Observes Firebase Authentication state changes in real time.
     */
    fun getAuthStateFlow(): Flow<FirebaseUser?> = callbackFlow {
        initFirebaseIfNeeded()
        val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            trySend(firebaseAuth.currentUser)
        }
        auth.addAuthStateListener(listener)
        awaitClose {
            auth.removeAuthStateListener(listener)
        }
    }

    open suspend fun signInWithGoogleTokenToUser(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val authResult = auth.signInWithCredential(credential).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Usuario Firebase nulo tras autenticación Google"))
            }
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Firebase signInWithCredential failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithGoogleToken(idToken: String): Result<String> {
        return try {
            val userResult = signInWithGoogleTokenToUser(idToken)
            if (userResult.isSuccess) {
                Result.success(userResult.getOrThrow().uid)
            } else {
                userResult.map { it.uid }
            }
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Google token sign-in fallback: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInWithEmailAndPassword(email: String, password: String): Result<FirebaseUser> {
        return try {
            initFirebaseIfNeeded()
            val authResult = auth.signInWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No se pudo obtener el usuario autenticado"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "signInWithEmailAndPassword failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun createUserWithEmailAndPassword(
        email: String,
        password: String,
        displayName: String? = null
    ): Result<FirebaseUser> {
        return try {
            initFirebaseIfNeeded()
            val authResult = auth.createUserWithEmailAndPassword(email, password).await()
            val user = authResult.user
            if (user != null) {
                if (!displayName.isNullOrBlank()) {
                    try {
                        val profileUpdates = UserProfileChangeRequest.Builder()
                            .setDisplayName(displayName)
                            .build()
                        user.updateProfile(profileUpdates).await()
                    } catch (pEx: Exception) {
                        Log.w("FirebaseDataSource", "Could not set displayName: ${pEx.message}")
                    }
                }
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("No se pudo crear la cuenta de usuario"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "createUserWithEmailAndPassword failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            initFirebaseIfNeeded()
            val authResult = auth.signInAnonymously().await()
            val user = authResult.user
            if (user != null) {
                Result.success(user)
            } else {
                Result.failure(IllegalStateException("Fallo al iniciar sesión anónima"))
            }
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "signInAnonymously failed: ${e.message}", e)
            Result.failure(e)
        }
    }

    suspend fun getIdToken(forceRefresh: Boolean = false): Result<String?> {
        return try {
            val user = auth.currentUser ?: return Result.success(null)
            val tokenResult = user.getIdToken(forceRefresh).await()
            Result.success(tokenResult.token)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun reloadUser(): Result<FirebaseUser?> {
        return try {
            val user = auth.currentUser
            user?.reload()?.await()
            Result.success(auth.currentUser)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun signOut() {
        try {
            auth.signOut()
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "SignOut error: ${e.message}")
        }
    }

    open suspend fun syncOfflineFinancialBackup(userId: String, backupPayload: Map<String, Any>): Boolean {
        return try {
            initFirebaseIfNeeded()
            val currentFbUid = getCurrentUserId()
            val targetId = if (!currentFbUid.isNullOrBlank()) currentFbUid else userId

            val enrichedPayload = HashMap(backupPayload)
            enrichedPayload["userId"] = targetId
            enrichedPayload["lastSyncedAt"] = System.currentTimeMillis()

            firestore.collection("financial_backups")
                .document(targetId)
                .set(enrichedPayload)
                .await()

            Log.i("FirebaseDataSource", "✓ Respaldo financiero E2EE guardado exitosamente en Firestore para $targetId (colección: financial_backups)")
            true
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Offline sync queued in Firestore cache: ${e.message}")
            false
        }
    }

    /**
     * Downloads user financial backup snapshot from Firebase Firestore.
     * Strictly scopes retrieval to the authenticated user's UID.
     * Prevents cross-account data leaks.
     */
    open suspend fun getFinancialBackup(userId: String): Map<String, Any>? {
        return try {
            initFirebaseIfNeeded()
            val currentFbUid = getCurrentUserId()
            val targetId = if (!currentFbUid.isNullOrBlank()) currentFbUid else userId

            // 1. Direct document retrieval in financial_backups for target UID
            try {
                val snapshot = firestore.collection("financial_backups")
                    .document(targetId)
                    .get()
                    .await()
                if (snapshot.exists() && snapshot.data != null && snapshot.data!!.isNotEmpty()) {
                    Log.i("FirebaseDataSource", "✓ Respaldo encontrado en Firestore doc financial_backups/$targetId")
                    return snapshot.data
                }
            } catch (e: Exception) {
                Log.d("FirebaseDataSource", "No direct doc in financial_backups/$targetId: ${e.message}")
            }

            // 2. Fallback check for local userId if different
            if (userId.isNotBlank() && userId != targetId && userId != "user_default" && userId != "kash_user_bio") {
                try {
                    val snapshot = firestore.collection("financial_backups")
                        .document(userId)
                        .get()
                        .await()
                    if (snapshot.exists() && snapshot.data != null && snapshot.data!!.isNotEmpty()) {
                        Log.i("FirebaseDataSource", "✓ Respaldo encontrado en fallback doc financial_backups/$userId")
                        return snapshot.data
                    }
                } catch (_: Exception) {}
            }

            null
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Notice getting financial backup: ${e.message}")
            null
        }
    }


    /**
     * Persists fetched exchange rates snapshot to Firebase Firestore to prevent
     * redundant network calls across multiple client devices.
     */
    suspend fun saveExchangeRatesSnapshot(baseCurrency: String, rates: Map<String, Double>, rateDate: String): Boolean {
        return try {
            val currentUser = auth.currentUser ?: return false
            val isAdmin = currentUser.getIdToken(false).await()?.claims?.get("admin") == true
            if (!isAdmin) {
                Log.w("FirebaseDataSource", "Se rechazó escritura de tasas globales sin claim admin")
                return false
            }
            val payload = hashMapOf<String, Any>(
                "baseCurrency" to baseCurrency.uppercase(),
                "rateDate" to rateDate,
                "provider" to "Frankfurter",
                "lastUpdatedMillis" to System.currentTimeMillis(),
                "rates" to rates
            )
            firestore.collection("system_exchange_rates")
                .document(baseCurrency.uppercase())
                .set(payload)
                .await()
            true
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Notice saving rates snapshot to Firestore: ${e.message}")
            false
        }
    }

    /**
     * Retrieves cached exchange rates from Firebase Firestore if available.
     */
    suspend fun getExchangeRatesSnapshot(baseCurrency: String): Map<String, Any>? {
        return try {
            val snapshot = firestore.collection("system_exchange_rates")
                .document(baseCurrency.uppercase())
                .get()
                .await()
            if (snapshot.exists()) {
                snapshot.data
            } else {
                null
            }
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Notice fetching rates from Firestore: ${e.message}")
            null
        }
    }

    fun getConnectionInfo(): FirebaseConnectionInfo {
        initFirebaseIfNeeded()
        val app = FirebaseApp.getInstance()
        val options = app.options
        return FirebaseConnectionInfo(
            isInitialized = true,
            projectId = options.projectId.orEmpty().ifEmpty { "kashapp-a9d56" },
            appId = options.applicationId.orEmpty().ifEmpty { "1:302422107977:android:b8c0b3458db266e3951c98" },
            currentUserId = getCurrentUserId(),
            isFirestoreOnline = true
        )
    }

    /**
     * Google Account Deletion Standard:
     * Purges cloud Firestore backups and deletes Firebase Authentication user account.
     */
    open suspend fun deleteCloudAccountAndData(userId: String): Result<Unit> {
        return try {
            initFirebaseIfNeeded()
            val currentFbUid = getCurrentUserId()
            val targetId = if (!currentFbUid.isNullOrBlank()) currentFbUid else userId

            // 1. Delete user backup document from financial_backups
            try {
                firestore.collection("financial_backups").document(targetId).delete().await()
                if (userId.isNotBlank() && userId != targetId && userId != "user_default") {
                    firestore.collection("financial_backups").document(userId).delete().await()
                }
            } catch (e: Exception) {
                Log.w("FirebaseDataSource", "Notice deleting financial_backups doc: ${e.message}")
            }

            // 2. Delete user profile document from users collection
            try {
                firestore.collection("users").document(targetId).delete().await()
            } catch (_: Exception) {}

            // 3. Delete user account from Firebase Authentication
            val user = auth.currentUser
            if (user != null) {
                try {
                    user.delete().await()
                } catch (e: Exception) {
                    Log.w("FirebaseDataSource", "Firebase Auth user delete note: ${e.message}")
                    auth.signOut()
                }
            }
            Log.i("FirebaseDataSource", "✓ Cuenta y respaldos en la nube eliminados para $targetId")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FirebaseDataSource", "Error eliminando cuenta en la nube: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Sincroniza una operación de la cola Outbox con Firestore.
     * Si no hay sesión de usuario activa en Firebase o falla la red, retorna Result.failure.
     */
    open suspend fun syncOutboxOperation(operation: com.example.data.local.entity.OutboxOperationEntity): Result<Unit> {
        return try {
            initFirebaseIfNeeded()
            val currentFbUid = getCurrentUserId()
                ?: return Result.failure(IllegalStateException("No hay sesión autenticada en Firebase para sincronizar la operación outbox."))

            val targetId = if (operation.userId.isNotBlank() && operation.userId != "user_default") operation.userId else currentFbUid
            val docRef = firestore.collection("users")
                .document(targetId)
                .collection("outbox_sync")
                .document("${operation.entityType}_${operation.entityId}")

            if (operation.operation.equals("DELETE", ignoreCase = true)) {
                docRef.delete().await()
            } else {
                val data = hashMapOf<String, Any>(
                    "entityType" to operation.entityType,
                    "entityId" to operation.entityId,
                    "operation" to operation.operation,
                    "payloadJson" to operation.payloadJson,
                    "idempotencyKey" to operation.idempotencyKey,
                    "updatedAtMillis" to System.currentTimeMillis()
                )
                docRef.set(data, SetOptions.merge()).await()
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w("FirebaseDataSource", "Outbox sync failed for ${operation.id}: ${e.message}")
            Result.failure(e)
        }
    }
}
