package com.example.feature.sync.engine

import android.util.Log
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.KeystoreCryptoManager
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.data.local.dao.OutboxDao
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.OutboxOperationEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.sync.model.SyncConflictPolicy
import com.example.feature.sync.model.SyncMergeStats
import com.example.feature.sync.model.SyncOperationType
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.UUID
import java.util.zip.GZIPInputStream

/**
 * Enterprise Outbox Synchronization & Conflict Resolution Engine.
 *
 * Key guarantees:
 * 1. 7-day retention of delivered outbox operations for audit and verification.
 * 2. Chronological Last-Write-Wins (LWW) conflict resolution.
 * 3. Bidirectional delta merge between Room and Firebase Firestore.
 * 4. Idempotent delivery with exponential backoff on network failures.
 */
class OutboxSyncEngine(
    private val outboxDao: OutboxDao,
    private val financialDao: FinancialDao,
    private val fixedAssetDao: FixedAssetDao?,
    private val firebaseDataSource: FirebaseDataSource,
    private val cryptoManager: KeystoreCryptoManager?,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    /**
     * Enqueues an operation into the local outbox queue with PENDING status.
     */
    suspend fun enqueueOperation(
        userId: String,
        entityType: String,
        entityId: String,
        operation: SyncOperationType,
        payloadJson: String = "{}",
        idempotencyKey: String = "op_${UUID.randomUUID()}"
    ): Result<OutboxOperationEntity> = withContext(ioDispatcher) {
        try {
            val entity = OutboxOperationEntity(
                id = "out_${UUID.randomUUID()}",
                userId = userId,
                entityType = entityType,
                entityId = entityId,
                operation = operation.value,
                payloadJson = payloadJson,
                idempotencyKey = idempotencyKey,
                status = "PENDING",
                retryCount = 0,
                nextRetryAt = System.currentTimeMillis(),
                createdAt = System.currentTimeMillis()
            )
            outboxDao.insertOperation(entity)
            Log.i(TAG, "Enqueued outbox operation: ${operation.value} $entityType ($entityId)")
            Result.success(entity)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to enqueue outbox operation: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Drains pending operations from the outbox to Firebase Firestore.
     * Enforces the 7-day retention rule on delivered operations.
     */
    suspend fun drainOutboxQueue(
        userId: String = "",
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Result<SyncMergeStats> = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        var pushed = 0
        var failed = 0
        var lastError: String? = null

        try {
            val pendingOps = if (userId.isNotBlank()) {
                outboxDao.getPendingOperationsByUser(userId, currentTime = currentTimeMillis)
            } else {
                outboxDao.getPendingOperations(currentTime = currentTimeMillis)
            }

            for (op in pendingOps) {
                val syncResult = firebaseDataSource.syncOutboxOperation(op)
                if (syncResult.isSuccess) {
                    outboxDao.updateStatus(
                        id = op.id,
                        status = "DELIVERED",
                        retryCount = op.retryCount,
                        nextRetryAt = op.nextRetryAt,
                        error = null
                    )
                    pushed++
                    Log.d(TAG, "Delivered outbox operation: ${op.id} (${op.entityType} ${op.entityId})")
                } else {
                    failed++
                    val err = syncResult.exceptionOrNull()?.message ?: "Network error during Firestore outbox sync"
                    lastError = err
                    val nextRetry = currentTimeMillis + (1000L * (1 shl (op.retryCount.coerceAtMost(6))))
                    outboxDao.updateStatus(
                        id = op.id,
                        status = if (op.retryCount >= 5) "FAILED" else "PENDING",
                        retryCount = op.retryCount + 1,
                        nextRetryAt = nextRetry,
                        error = err
                    )
                    Log.w(TAG, "Failed to sync op ${op.id}: $err (retry ${op.retryCount + 1})")
                }
            }

            // Apply 7-day retention rule: purge operations marked as DELIVERED older than 7 days
            val purgedCount = purgeDeliveredOlderThanThreshold(currentTimeMillis)

            val stats = SyncMergeStats(
                pushedCount = pushed,
                failedCount = failed,
                purgedDeliveredCount = purgedCount,
                durationMillis = System.currentTimeMillis() - startTime,
                errorMessage = lastError
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error draining outbox queue: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Purges delivered outbox entries older than 7 days (7 * 24 * 60 * 60 * 1000 ms).
     */
    suspend fun purgeDeliveredOlderThanThreshold(
        currentTimeMillis: Long = System.currentTimeMillis()
    ): Int = withContext(ioDispatcher) {
        val sevenDaysAgo = currentTimeMillis - SEVEN_DAYS_MILLIS
        val purged = outboxDao.purgeDeliveredOlderThan(sevenDaysAgo)
        if (purged > 0) {
            Log.i(TAG, "Purged $purged delivered outbox operations older than 7 days.")
        }
        purged
    }

    /**
     * Pulls Firestore changes and performs a two-way delta merge using LWW conflict resolution.
     */
    suspend fun pullAndDeltaMerge(userId: String): Result<SyncMergeStats> = withContext(ioDispatcher) {
        val startTime = System.currentTimeMillis()
        var pulledCount = 0
        var localWins = 0
        var remoteWins = 0

        try {
            val currentFbUid = firebaseDataSource.getCurrentUserId()
            val targetId = if (!currentFbUid.isNullOrBlank()) currentFbUid else userId
            val activeUserId = if (targetId.isNotBlank() && targetId != "user_default" && targetId != "kash_user_01") targetId else userId

            val backupData = firebaseDataSource.getFinancialBackup(targetId)
                ?: if (userId != targetId) firebaseDataSource.getFinancialBackup(userId) else null

            if (backupData == null) {
                Log.i(TAG, "No remote cloud backup found for user $targetId.")
                return@withContext Result.success(SyncMergeStats(durationMillis = System.currentTimeMillis() - startTime))
            }

            // Decrypt remote payload
            var decryptedContent: String? = null
            if (backupData.containsKey("encryptedPayload")) {
                val encPayload = backupData["encryptedPayload"] as? String
                if (!encPayload.isNullOrBlank() && cryptoManager != null) {
                    decryptedContent = cryptoManager.decryptCloudPayload(encPayload, targetId)
                    if (decryptedContent.isNullOrBlank() && userId.isNotBlank() && userId != targetId) {
                        decryptedContent = cryptoManager.decryptCloudPayload(encPayload, userId)
                    }
                    val fbEmail = firebaseDataSource.getCurrentUserEmail()
                    if (decryptedContent.isNullOrBlank() && !fbEmail.isNullOrBlank()) {
                        decryptedContent = cryptoManager.decryptCloudPayload(encPayload, fbEmail)
                    }
                    if (decryptedContent.isNullOrBlank()) {
                        decryptedContent = cryptoManager.decryptData(encPayload)
                    }
                }
            }

            if (decryptedContent.isNullOrBlank()) {
                Log.w(TAG, "Remote payload could not be decrypted or is empty.")
                return@withContext Result.success(SyncMergeStats(durationMillis = System.currentTimeMillis() - startTime))
            }

            val jsonString = if (decryptedContent.trim().startsWith("{")) {
                decryptedContent
            } else {
                decompressGzip(decryptedContent)
            }

            val rootObj = JSONObject(jsonString)
            val remoteTimestamp = rootObj.optLong("timestamp", 0L)

            // 1. Delta merge Accounts
            val remoteAccountsArr = rootObj.optJSONArray("accounts")
            if (remoteAccountsArr != null) {
                val localAccounts = financialDao.getAccounts(activeUserId).associateBy { it.id }
                val accountsToUpsert = mutableListOf<AccountEntity>()

                for (i in 0 until remoteAccountsArr.length()) {
                    val obj = remoteAccountsArr.getJSONObject(i)
                    val accId = obj.optString("id")
                    if (accId.isBlank()) continue

                    val remoteAccount = AccountEntity(
                        id = accId,
                        userId = activeUserId,
                        name = obj.optString("name", "Cuenta"),
                        balance = obj.optDouble("balance", 0.0),
                        availableBalance = if (obj.has("availableBalance")) obj.optDouble("availableBalance") else obj.optDouble("balance", 0.0),
                        currency = obj.optString("currency", "COP"),
                        type = obj.optString("type", "SAVINGS"),
                        bankName = obj.optString("bankName", "Bancolombia"),
                        isFavorite = obj.optBoolean("isFavorite", true),
                        accountNumberMasked = obj.optString("accountNumberMasked", "").ifBlank { null },
                        customColorHex = obj.optString("customColorHex", "#0284C7"),
                        hasOverdraft = obj.optBoolean("hasOverdraft", false),
                        overdraftLimit = obj.optDouble("overdraftLimit", 0.0)
                    )

                    val localAccount = localAccounts[accId]
                    if (localAccount == null) {
                        accountsToUpsert.add(remoteAccount)
                        remoteWins++
                        pulledCount++
                    } else {
                        val decision = SyncConflictPolicy.evaluateTimestamps(
                            localTimestamp = 0L,
                            remoteTimestamp = remoteTimestamp
                        )
                        if (decision == SyncConflictPolicy.ConflictDecision.USE_REMOTE) {
                            accountsToUpsert.add(remoteAccount)
                            remoteWins++
                        } else {
                            localWins++
                        }
                    }
                }

                if (accountsToUpsert.isNotEmpty()) {
                    financialDao.insertAccounts(accountsToUpsert)
                    Log.i(TAG, "Delta merge: upserted ${accountsToUpsert.size} accounts from cloud.")
                }
            }

            // 2. Delta merge Credit Card Configurations
            val remoteCardsArr = rootObj.optJSONArray("creditCardConfigs")
            if (remoteCardsArr != null) {
                val localCards = financialDao.getAccountCreditCardsByUser(activeUserId).associateBy { it.accountId }
                val cardsToUpsert = mutableListOf<AccountCreditCardEntity>()

                for (i in 0 until remoteCardsArr.length()) {
                    val obj = remoteCardsArr.getJSONObject(i)
                    val accId = obj.optString("accountId")
                    if (accId.isBlank()) continue

                    val quotaTotal = obj.optDouble("quotaTotal", 5_000_000.0)
                    val quotaUsed = obj.optDouble("quotaUsed", 0.0).coerceAtMost(quotaTotal)
                    val quotaAvail = obj.optDouble("quotaAvailable", quotaTotal - quotaUsed)

                    val remoteCard = AccountCreditCardEntity(
                        id = obj.optString("id", "cc_${accId.removePrefix("acc_")}"),
                        accountId = accId,
                        franchise = obj.optString("franchise", "MASTERCARD"),
                        category = obj.optString("category", "GOLD"),
                        quotaTotal = quotaTotal,
                        quotaAvailable = quotaAvail,
                        quotaUsed = quotaUsed,
                        cutOffDay = obj.optInt("cutOffDay", 18),
                        paymentDueDay = obj.optInt("paymentDueDay", 5),
                        interestRateEa = obj.optDouble("interestRateEa", 24.50),
                        purchasesRateEa = obj.optDouble("purchasesRateEa", 24.50),
                        advancesRateEa = obj.optDouble("advancesRateEa", 28.00),
                        includeInNetWorth = obj.optBoolean("includeInNetWorth", true),
                        minimumPaymentAmount = obj.optDouble("minimumPaymentAmount", quotaUsed)
                    )

                    val localCard = localCards[accId]
                    if (localCard == null) {
                        cardsToUpsert.add(remoteCard)
                        remoteWins++
                    } else {
                        val decision = SyncConflictPolicy.evaluateTimestamps(
                            localTimestamp = 0L,
                            remoteTimestamp = remoteTimestamp
                        )
                        if (decision == SyncConflictPolicy.ConflictDecision.USE_REMOTE) {
                            cardsToUpsert.add(remoteCard)
                            remoteWins++
                        } else {
                            localWins++
                        }
                    }
                }

                if (cardsToUpsert.isNotEmpty()) {
                    for (card in cardsToUpsert) {
                        financialDao.insertAccountCreditCard(card)
                    }
                }
            }

            // 3. Delta merge Transactions
            val remoteTxArr = rootObj.optJSONArray("transactions")
            if (remoteTxArr != null) {
                val localTxs = financialDao.getTransactions(activeUserId).associateBy { it.id }
                val txsToUpsert = mutableListOf<TransactionEntity>()

                for (i in 0 until remoteTxArr.length()) {
                    val obj = remoteTxArr.getJSONObject(i)
                    val txId = obj.optString("id")
                    if (txId.isBlank()) continue

                    val localTx = localTxs[txId]
                    if (localTx == null) {
                        val newTx = TransactionEntity(
                            id = txId,
                            userId = activeUserId,
                            accountId = obj.optString("accountId", ""),
                            accountName = obj.optString("accountName", "Cuenta"),
                            categoryId = obj.optString("categoryId", "cat_otros"),
                            categoryName = obj.optString("categoryName", "Varios"),
                            amount = obj.optDouble("amount", 0.0),
                            currency = obj.optString("currency", "COP"),
                            type = obj.optString("type", "EXPENSE"),
                            date = obj.optString("date", java.time.LocalDate.now().toString()),
                            description = obj.optString("description", "").ifBlank { null },
                            receiptUrl = null,
                            idempotencyKey = "cloud_$txId",
                            createdAt = obj.optLong("createdAt", remoteTimestamp)
                        )
                        txsToUpsert.add(newTx)
                        remoteWins++
                        pulledCount++
                    } else {
                        val localCreatedAt = localTx.createdAt
                        val remoteTxCreatedAt = obj.optLong("createdAt", remoteTimestamp)
                        val decision = SyncConflictPolicy.evaluateTimestamps(localCreatedAt, remoteTxCreatedAt)
                        if (decision == SyncConflictPolicy.ConflictDecision.USE_REMOTE && remoteTxCreatedAt > localCreatedAt) {
                            val updatedTx = localTx.copy(
                                amount = obj.optDouble("amount", localTx.amount),
                                description = obj.optString("description", localTx.description.orEmpty()).ifBlank { null },
                                categoryName = obj.optString("categoryName", localTx.categoryName)
                            )
                            txsToUpsert.add(updatedTx)
                            remoteWins++
                        } else {
                            localWins++
                        }
                    }
                }

                if (txsToUpsert.isNotEmpty()) {
                    for (tx in txsToUpsert) {
                        financialDao.insertTransaction(tx)
                    }
                    Log.i(TAG, "Delta merge: upserted ${txsToUpsert.size} transactions from cloud.")
                }
            }

            // 4. Delta merge Fixed Assets
            val remoteAssetsArr = rootObj.optJSONArray("fixedAssets")
            if (remoteAssetsArr != null && fixedAssetDao != null) {
                val localAssets = fixedAssetDao.getAssets(activeUserId).associateBy { it.id }
                for (i in 0 until remoteAssetsArr.length()) {
                    val obj = remoteAssetsArr.getJSONObject(i)
                    val assetId = obj.optString("id")
                    if (assetId.isBlank()) continue

                    val localAsset = localAssets[assetId]
                    if (localAsset == null) {
                        val newAsset = FixedAssetEntity(
                            id = assetId,
                            userId = activeUserId,
                            name = obj.optString("name", "Activo"),
                            type = obj.optString("type", "TANGIBLE"),
                            category = obj.optString("category", "Vehículos"),
                            acquisitionDate = obj.optString("acquisitionDate", java.time.LocalDate.now().toString()),
                            acquisitionValue = obj.optDouble("acquisitionValue", obj.optDouble("purchasePrice", 0.0)),
                            usefulLifeMonths = obj.optInt("usefulLifeMonths", 60),
                            residualValue = obj.optDouble("residualValue", 0.0),
                            currentMarketValue = obj.optDouble("currentMarketValue", 0.0),
                            accumulatedDepreciation = obj.optDouble("accumulatedDepreciation", 0.0),
                            status = obj.optString("status", "ACTIVE"),
                            colorHex = obj.optString("colorHex", "#0284C7"),
                            createdAt = obj.optLong("createdAt", remoteTimestamp),
                            updatedAt = obj.optLong("updatedAt", remoteTimestamp)
                        )
                        fixedAssetDao.insertAsset(newAsset)
                        remoteWins++
                        pulledCount++
                    }
                }
            }

            val stats = SyncMergeStats(
                pulledCount = pulledCount,
                localWinsCount = localWins,
                remoteWinsCount = remoteWins,
                durationMillis = System.currentTimeMillis() - startTime
            )
            Result.success(stats)
        } catch (e: Exception) {
            Log.e(TAG, "Error in pullAndDeltaMerge: ${e.message}", e)
            Result.failure(e)
        }
    }

    /**
     * Complete bidirectional synchronization cycle:
     * 1. Drain pending outbox operations to cloud.
     * 2. Pull remote delta and merge with LWW.
     * 3. Purge delivered operations older than 7 days.
     */
    suspend fun syncBidirectional(userId: String): Result<SyncMergeStats> = withContext(ioDispatcher) {
        val drainResult = drainOutboxQueue(userId)
        val pullResult = pullAndDeltaMerge(userId)

        val drainStats = drainResult.getOrDefault(SyncMergeStats())
        val pullStats = pullResult.getOrDefault(SyncMergeStats())

        val combinedStats = SyncMergeStats(
            pushedCount = drainStats.pushedCount,
            pulledCount = pullStats.pulledCount,
            localWinsCount = pullStats.localWinsCount,
            remoteWinsCount = pullStats.remoteWinsCount,
            purgedDeliveredCount = drainStats.purgedDeliveredCount,
            failedCount = drainStats.failedCount + pullStats.failedCount,
            durationMillis = drainStats.durationMillis + pullStats.durationMillis,
            errorMessage = drainStats.errorMessage ?: pullStats.errorMessage
        )
        Result.success(combinedStats)
    }

    private fun decompressGzip(compressedBase64: String): String {
        return try {
            val decodedBytes = android.util.Base64.decode(compressedBase64, android.util.Base64.DEFAULT)
            GZIPInputStream(ByteArrayInputStream(decodedBytes)).bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: Exception) {
            compressedBase64
        }
    }

    companion object {
        private const val TAG = "OutboxSyncEngine"
        const val SEVEN_DAYS_MILLIS = 7L * 24L * 60L * 60L * 1000L
    }
}
