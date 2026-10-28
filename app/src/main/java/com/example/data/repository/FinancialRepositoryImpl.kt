package com.example.data.repository

import android.util.Log
import com.example.data.datasource.FirebaseDataSource
import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.entity.AccountCdtEntity
import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.CityEntity
import com.example.data.local.entity.CountryEntity
import com.example.data.local.entity.CurrencyEntity
import com.example.data.local.entity.PayrollConceptEntity
import com.example.data.local.entity.TransactionEntity
import com.example.data.local.entity.TransferGroupEntity
import com.example.domain.repository.FinancialRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

/**
 * Production implementation of FinancialRepository enforcing ACID transactions,
 * Room local storage, audit logs, and background sync to Firebase and Drive.
 */
class FinancialRepositoryImpl(
    private val financialDao: FinancialDao,
    private val auditLogDao: AuditSecurityLogDao,
    private val firebaseDataSource: FirebaseDataSource,
    private val merchantDao: com.example.data.local.dao.MerchantDao? = null,
    private val outboxDao: com.example.data.local.dao.OutboxDao? = null,
    private val keystoreCryptoManager: com.example.data.local.KeystoreCryptoManager? = null,
    private val fixedAssetDao: com.example.data.local.dao.FixedAssetDao? = null,
    private val reconciliationDao: com.example.data.local.dao.ReconciliationDao? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : FinancialRepository {

    private val cryptoManager: com.example.data.local.KeystoreCryptoManager by lazy {
        keystoreCryptoManager ?: com.example.data.local.KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
    }

    private val syncScope = CoroutineScope(ioDispatcher)
    private val deletedAccountIds = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    companion object {
        fun compressGzip(input: String): String {
            val bos = java.io.ByteArrayOutputStream()
            java.util.zip.GZIPOutputStream(bos).use { gzip ->
                gzip.write(input.toByteArray(Charsets.UTF_8))
            }
            val compressedBytes = bos.toByteArray()
            return try {
                java.util.Base64.getEncoder().encodeToString(compressedBytes)
            } catch (_: Throwable) {
                android.util.Base64.encodeToString(compressedBytes, android.util.Base64.NO_WRAP)
            }
        }

        fun decompressGzip(compressedBase64: String): String {
            val bytes = try {
                java.util.Base64.getDecoder().decode(compressedBase64)
            } catch (_: Throwable) {
                android.util.Base64.decode(compressedBase64, android.util.Base64.NO_WRAP)
            }
            return java.util.zip.GZIPInputStream(java.io.ByteArrayInputStream(bytes))
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        }
    }

    override fun getAccounts(userId: String): Flow<List<AccountEntity>> {
        return financialDao.getAccountsFlow(userId)
    }

    override suspend fun getAccountsSnapshot(userId: String): List<AccountEntity> = withContext(ioDispatcher) {
        financialDao.getAccounts(userId)
    }

    override fun getCategories(): Flow<List<CategoryEntity>> {
        return financialDao.getCategoriesFlow()
    }

    override fun getCategories(userId: String): Flow<List<CategoryEntity>> {
        return financialDao.getCategoriesFlow(userId)
    }

    override fun getTransactions(userId: String): Flow<List<TransactionEntity>> {
        return financialDao.getTransactionsFlow(userId)
    }

    override suspend fun getTransactionsSnapshot(userId: String): List<TransactionEntity> = withContext(ioDispatcher) {
        financialDao.getTransactions(userId)
    }

    override suspend fun getAccountById(accountId: String): AccountEntity? {
        return financialDao.getAccountById(accountId)
    }

    override suspend fun recordExpense(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        description: String?,
        receiptUrl: String?,
        isRecurring: Boolean,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String,
        affectsBudget: Boolean
    ): Result<TransactionEntity> {
        return try {
            // 1. Validation & Anti-Tampering Checks
            if (amount <= 0.0) {
                return Result.failure(IllegalArgumentException("El monto debe ser superior a cero."))
            }

            // 2. Check for duplicate idempotency key (Mobile network retry protection)
            val existingTx = financialDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existingTx != null) {
                Log.i("FinancialRepository", "Idempotency key duplicate detected. Returning existing record.")
                return Result.success(existingTx)
            }

            // 3. Construct sanitized Transaction entity
            val sanitizedDescription = description?.trim()?.take(500)
            val transactionId = "tx_${UUID.randomUUID().toString().take(12)}"
            val transaction = TransactionEntity(
                id = transactionId,
                userId = userId,
                accountId = accountId,
                accountName = accountName,
                categoryId = categoryId,
                categoryName = categoryName,
                amount = amount,
                currency = currency,
                type = "EXPENSE",
                date = dateFormatted,
                description = sanitizedDescription,
                receiptUrl = receiptUrl,
                idempotencyKey = idempotencyKey,
                isRecurring = isRecurring,
                saveAsTemplate = saveAsTemplate,
                tags = tags.joinToString(","),
                syncStatus = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            // 4. Atomic ACID execution in Room: Inserts TX and deducts Account balance
            financialDao.executeExpenseTransaction(
                transaction = transaction,
                userId = userId,
                affectsBudget = affectsBudget
            )

            // 5. Financial Audit Log
            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "CREATE_EXPENSE_TRANSACTION",
                        authMethod = "SESSION",
                        details = "Gasto registrado: $currency $amount en $categoryName (Cuenta: $accountName)"
                    )
                )
            } catch (e: Exception) {
                Log.w("FinancialRepository", "Audit log insert non-critical failure: ${e.message}")
            }

            // 6. Transactional Outbox Enqueue (Offline-first durability)
            try {
                outboxDao?.insertOperation(
                    com.example.data.local.entity.OutboxOperationEntity(
                        id = "out_${UUID.randomUUID()}",
                        userId = userId,
                        entityType = "TRANSACTION",
                        entityId = transaction.id,
                        operation = "CREATE",
                        payloadJson = org.json.JSONObject().apply {
                            put("id", transaction.id)
                            put("amount", transaction.amount)
                            put("type", transaction.type)
                            put("accountId", transaction.accountId)
                            put("categoryId", transaction.categoryId)
                            put("categoryName", transaction.categoryName)
                            put("date", transaction.date)
                            put("description", transaction.description.orEmpty())
                        }.toString(),
                        idempotencyKey = idempotencyKey,
                        status = "PENDING"
                    )
                )
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Outbox enqueue notice: ${ex.message}")
            }

            // 7. Background Asynchronous Sync to Firebase Firestore (Offline-first)
            syncScope.launch {
                try {
                    backupFullPortfolioToCloud(userId)
                } catch (ex: Exception) {
                    Log.w("FinancialRepository", "Background portfolio sync notice: ${ex.message}")
                }
            }

            Result.success(transaction)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error recording expense: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getIncomeSources(userId: String): Flow<List<com.example.data.local.entity.IncomeSourceEntity>> {
        return financialDao.getIncomeSourcesFlow(userId)
    }

    override suspend fun recordIncome(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        sourceId: String?,
        sourceName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        description: String?,
        receiptUrl: String?,
        isRecurring: Boolean,
        recurrenceFrequency: String,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String
    ): Result<TransactionEntity> {
        return try {
            // 1. Validation & Anti-Tampering Checks
            if (amount <= 0.0) {
                return Result.failure(IllegalArgumentException("El monto debe ser superior a cero."))
            }
            if (accountId.isBlank()) {
                return Result.failure(IllegalArgumentException("Debes seleccionar una cuenta receptora."))
            }

            // 2. Check for duplicate idempotency key (Mobile network retry protection)
            val existingTx = financialDao.getTransactionByIdempotencyKey(idempotencyKey)
            if (existingTx != null) {
                Log.i("FinancialRepository", "Idempotency key duplicate detected for income. Returning existing record.")
                return Result.success(existingTx)
            }

            // 3. Construct sanitized Transaction entity
            val sanitizedDescription = description?.trim()?.take(255)
            val transactionId = "tx_${UUID.randomUUID().toString().take(12)}"
            val transaction = TransactionEntity(
                id = transactionId,
                userId = userId,
                accountId = accountId,
                accountName = accountName,
                categoryId = categoryId,
                categoryName = categoryName,
                sourceId = sourceId,
                sourceName = sourceName,
                amount = amount,
                currency = currency,
                type = "INCOME",
                date = dateFormatted,
                description = sanitizedDescription,
                receiptUrl = receiptUrl,
                idempotencyKey = idempotencyKey,
                isRecurring = isRecurring,
                recurrenceFrequency = recurrenceFrequency,
                saveAsTemplate = saveAsTemplate,
                tags = tags.joinToString(","),
                syncStatus = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            // 4. Atomic ACID execution in Room: Inserts TX and INCREMENTS Account balance (SUM)
            financialDao.executeIncomeTransaction(
                transaction = transaction,
                userId = userId
            )

            // 5. Financial Audit Log
            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "CREATE_INCOME_TRANSACTION",
                        authMethod = "SESSION",
                        details = "Ingreso registrado: $currency $amount de $sourceName a cuenta $accountName ($categoryName)"
                    )
                )
            } catch (e: Exception) {
                Log.w("FinancialRepository", "Audit log insert non-critical failure: ${e.message}")
            }

            // 6. Transactional Outbox Enqueue (Offline-first durability)
            try {
                outboxDao?.insertOperation(
                    com.example.data.local.entity.OutboxOperationEntity(
                        id = "out_${UUID.randomUUID()}",
                        userId = userId,
                        entityType = "TRANSACTION",
                        entityId = transaction.id,
                        operation = "CREATE",
                        payloadJson = org.json.JSONObject().apply {
                            put("id", transaction.id)
                            put("amount", transaction.amount)
                            put("type", transaction.type)
                            put("accountId", transaction.accountId)
                            put("categoryId", transaction.categoryId)
                            put("categoryName", transaction.categoryName)
                            put("date", transaction.date)
                            put("description", transaction.description.orEmpty())
                        }.toString(),
                        idempotencyKey = idempotencyKey,
                        status = "PENDING"
                    )
                )
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Outbox enqueue notice: ${ex.message}")
            }

            // 7. Background Asynchronous Sync to Cloud Backend (Offline-first)
            syncScope.launch {
                try {
                    backupFullPortfolioToCloud(userId)
                } catch (ex: Exception) {
                    Log.w("FinancialRepository", "Background portfolio sync notice: ${ex.message}")
                }
            }

            Result.success(transaction)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error recording income: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun recordTransfer(
        userId: String,
        sourceAccountId: String,
        sourceAccountName: String,
        destinationAccountId: String,
        destinationAccountName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        concept: String?,
        note: String?,
        receiptUrl: String?,
        saveAsTemplate: Boolean,
        tags: List<String>,
        idempotencyKey: String
    ): Result<Pair<TransactionEntity, TransactionEntity>> = withContext(Dispatchers.IO) {
        try {
            // 1. Double IDOR & Loop Protection Validation
            if (amount <= 0) {
                return@withContext Result.failure(IllegalArgumentException("El monto a transferir debe ser mayor a 0"))
            }
            if (sourceAccountId == destinationAccountId) {
                return@withContext Result.failure(IllegalArgumentException("La cuenta de origen y destino no pueden ser la misma"))
            }

            // Verify both accounts exist and belong to user
            val sourceAccount = financialDao.getAccountById(sourceAccountId)
            val destAccount = financialDao.getAccountById(destinationAccountId)
            if (sourceAccount == null || destAccount == null) {
                return@withContext Result.failure(SecurityException("Una o ambas cuentas no existen o no pertenecen al usuario autenticado"))
            }
            if (sourceAccount.userId != userId || destAccount.userId != userId) {
                return@withContext Result.failure(SecurityException("IDOR detectado: Cuentas no autorizadas para este usuario"))
            }

            val transferGroupId = "grp_${UUID.randomUUID()}"
            val sanitizedConcept = concept?.trim()?.take(100)
            val sanitizedNote = note?.trim()?.take(255)

            // 2. Transfer Group Entity
            val transferGroup = com.example.data.local.entity.TransferGroupEntity(
                id = transferGroupId,
                userId = userId,
                concept = sanitizedConcept ?: "Transferencia entre cuentas",
                sourceAccountId = sourceAccountId,
                destinationAccountId = destinationAccountId,
                amount = amount,
                currency = currency,
                createdAt = System.currentTimeMillis()
            )

            // 3. Double-entry Transaction pair: TRANSFER_OUT (Debit from source) & TRANSFER_IN (Credit to destination)
            val outTxn = TransactionEntity(
                id = "tx_out_${UUID.randomUUID()}",
                userId = userId,
                accountId = sourceAccountId,
                accountName = sourceAccountName,
                categoryId = "cat_transfer",
                categoryName = sanitizedConcept ?: "Transferencia",
                sourceId = null,
                sourceName = null,
                amount = amount,
                currency = currency,
                type = "TRANSFER_OUT",
                date = dateFormatted,
                description = sanitizedNote ?: "Transferencia enviada a $destinationAccountName",
                receiptUrl = receiptUrl,
                idempotencyKey = "${idempotencyKey}_out",
                isRecurring = false,
                saveAsTemplate = saveAsTemplate,
                tags = tags.joinToString(","),
                transferGroupId = transferGroupId,
                syncStatus = "PENDING",
                createdAt = System.currentTimeMillis()
            )

            val inTxn = TransactionEntity(
                id = "tx_in_${UUID.randomUUID()}",
                userId = userId,
                accountId = destinationAccountId,
                accountName = destinationAccountName,
                categoryId = "cat_transfer",
                categoryName = sanitizedConcept ?: "Transferencia",
                sourceId = null,
                sourceName = null,
                amount = amount,
                currency = currency,
                type = "TRANSFER_IN",
                date = dateFormatted,
                description = sanitizedNote ?: "Transferencia recibida desde $sourceAccountName",
                receiptUrl = receiptUrl,
                idempotencyKey = "${idempotencyKey}_in",
                isRecurring = false,
                saveAsTemplate = saveAsTemplate,
                tags = tags.joinToString(","),
                transferGroupId = transferGroupId,
                syncStatus = "PENDING",
                createdAt = System.currentTimeMillis() + 1
            )

            // 4. ACID execution: 1 transfer_group insert + 2 transaction inserts + 2 balance updates
            financialDao.executeTransferTransaction(
                transferGroup = transferGroup,
                transferOut = outTxn,
                transferIn = inTxn,
                userId = userId
            )

            // 5. Dual Security Audit Log
            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "CREATE_TRANSFER_TRANSACTION",
                        authMethod = "SESSION",
                        details = "Transferencia ejecutada: $currency $amount de $sourceAccountName a $destinationAccountName (Grupo: $transferGroupId)"
                    )
                )
            } catch (e: Exception) {
                Log.w("FinancialRepository", "Audit log insert non-critical failure: ${e.message}")
            }

            // 6. Transactional Outbox Enqueue for Double-Entry Transfer (Offline-first durability)
            try {
                outboxDao?.insertOperations(
                    listOf(
                        com.example.data.local.entity.OutboxOperationEntity(
                            id = "out_${UUID.randomUUID()}",
                            userId = userId,
                            entityType = "TRANSACTION",
                            entityId = outTxn.id,
                            operation = "CREATE",
                            payloadJson = org.json.JSONObject().apply {
                                put("id", outTxn.id)
                                put("amount", outTxn.amount)
                                put("type", outTxn.type)
                                put("accountId", outTxn.accountId)
                                put("date", outTxn.date)
                                put("transferGroupId", transferGroupId)
                            }.toString(),
                            idempotencyKey = "${idempotencyKey}_out",
                            status = "PENDING"
                        ),
                        com.example.data.local.entity.OutboxOperationEntity(
                            id = "out_${UUID.randomUUID()}",
                            userId = userId,
                            entityType = "TRANSACTION",
                            entityId = inTxn.id,
                            operation = "CREATE",
                            payloadJson = org.json.JSONObject().apply {
                                put("id", inTxn.id)
                                put("amount", inTxn.amount)
                                put("type", inTxn.type)
                                put("accountId", inTxn.accountId)
                                put("date", inTxn.date)
                                put("transferGroupId", transferGroupId)
                            }.toString(),
                            idempotencyKey = "${idempotencyKey}_in",
                            status = "PENDING"
                        )
                    )
                )
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Outbox transfer enqueue notice: ${ex.message}")
            }

            // 7. Asynchronous Background Sync
            syncScope.launch {
                try {
                    backupFullPortfolioToCloud(userId)
                } catch (ex: Exception) {
                    Log.w("FinancialRepository", "Background portfolio sync notice: ${ex.message}")
                }
            }

            Result.success(Pair(outTxn, inTxn))
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error recording transfer: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun payCreditCard(
        userId: String,
        sourceAccountId: String,
        sourceAccountName: String,
        creditCardAccountId: String,
        creditCardAccountName: String,
        amount: Double,
        currency: String,
        dateFormatted: String,
        concept: String?,
        note: String?,
        receiptUrl: String?
    ): Result<Pair<TransactionEntity, TransactionEntity>> = withContext(ioDispatcher) {
        val transferConcept = concept ?: "Pago de Tarjeta: $creditCardAccountName"
        val transferNote = note ?: "Abono / Pago de Tarjeta de Crédito"
        recordTransfer(
            userId = userId,
            sourceAccountId = sourceAccountId,
            sourceAccountName = sourceAccountName,
            destinationAccountId = creditCardAccountId,
            destinationAccountName = creditCardAccountName,
            amount = amount,
            currency = currency,
            dateFormatted = dateFormatted,
            concept = transferConcept,
            note = transferNote,
            receiptUrl = receiptUrl,
            saveAsTemplate = false,
            tags = listOf("Pago Tarjeta", "Abono"),
            idempotencyKey = "pay_cc_${UUID.randomUUID()}"
        )
    }

    override suspend fun createCategory(
        name: String,
        colorHex: String,
        icon: String,
        userId: String?,
        type: String
    ): CategoryEntity {
        val categoryId = "cat_${UUID.randomUUID()}"
        val newCategory = CategoryEntity(
            id = categoryId,
            userId = userId,
            name = name.trim(),
            icon = icon,
            colorHex = colorHex,
            type = type
        )
        financialDao.insertCategory(newCategory)

        syncScope.launch {
            try {
                if (!userId.isNullOrBlank()) {
                    backupFullPortfolioToCloud(userId)
                }
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Background category sync notice: ${ex.message}")
            }
        }

        return newCategory
    }

    override suspend fun createIncomeSource(
        userId: String,
        name: String
    ): com.example.data.local.entity.IncomeSourceEntity {
        val sourceId = "src_${UUID.randomUUID()}"
        val newSource = com.example.data.local.entity.IncomeSourceEntity(
            id = sourceId,
            userId = userId,
            name = name.trim(),
            isActive = true
        )
        financialDao.insertIncomeSource(newSource)

        syncScope.launch {
            try {
                backupFullPortfolioToCloud(userId)
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Background income source sync notice: ${ex.message}")
            }
        }

        return newSource
    }

    override fun encryptSensitiveField(rawValue: String?): String? {
        val clean = rawValue?.trim() ?: return null
        if (clean.isBlank()) return null
        return try {
            val encryptedBase64 = cryptoManager.encryptData(clean)
            "ENCv1:$encryptedBase64"
        } catch (e: Exception) {
            Log.e("FinancialRepositoryImpl", "Error cifrando campo sensible con Keystore", e)
            null
        }
    }

    override fun decryptSensitiveField(storedValue: String?): String? {
        val stored = storedValue?.trim() ?: return null
        if (stored.isBlank()) return null
        return try {
            if (stored.startsWith("ENCv1:")) {
                val cipherText = stored.removePrefix("ENCv1:")
                cryptoManager.decryptData(cipherText)
            } else {
                // Migración transparente de registros legados en Base64 plano
                try {
                    val decodedBytes = try {
                        java.util.Base64.getDecoder().decode(stored)
                    } catch (_: Throwable) {
                        android.util.Base64.decode(stored, android.util.Base64.NO_WRAP)
                    }
                    val candidate = String(decodedBytes, Charsets.UTF_8)
                    if (candidate.isNotEmpty() && !candidate.contains('\uFFFD') && candidate.all { it.code in 32..126 || it == '\n' || it == '\r' }) {
                        val reencoded = try {
                            java.util.Base64.getEncoder().encodeToString(candidate.toByteArray(Charsets.UTF_8)).trimEnd('=')
                        } catch (_: Throwable) {
                            android.util.Base64.encodeToString(candidate.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP).trimEnd('=')
                        }
                        if (stored.trimEnd('=').equals(reencoded, ignoreCase = false)) {
                            candidate
                        } else {
                            stored
                        }
                    } else {
                        stored
                    }
                } catch (_: Throwable) {
                    stored
                }
            }
        } catch (e: Exception) {
            Log.e("FinancialRepositoryImpl", "Error descifrando campo sensible", e)
            null
        }
    }

    override suspend fun createAccount(
        name: String,
        type: String,
        balance: Double,
        currency: String,
        userId: String,
        bankName: String,
        isFavorite: Boolean,
        accountNumber: String?,
        openingDate: String?,
        kashappDate: String,
        generatesInterest: Boolean,
        interestRateEa: Double?,
        calcFrequency: String?,
        payoutFrequency: String?,
        compoundInterest: Boolean,
        purposes: String,
        customColorHex: String,
        iconName: String,
        minimumBalance: Double,
        monthlyFee: Double,
        feeChargeDay: Int,
        syncProvider: String?,
        syncInstitutionId: String?,
        syncAccessToken: String?,
        supportingDocumentUri: String?,
        supportingDocumentType: String?,
        availableBalance: Double?,
        holdBalance: Double,
        country: String,
        cityName: String?,
        favoriteRouting: String,
        favoriteForIncomes: Boolean,
        favoriteForExpenses: Boolean,
        employerName: String?,
        accountOwner: String?,
        alias: String?,
        hasOverdraft: Boolean,
        overdraftLimit: Double,
        overdraftRateEa: Double,
        isTaxExempt: Boolean,
        payrollConfig: com.example.data.local.entity.AccountPayrollConfigEntity?
    ): AccountEntity {
        val accountId = "acc_${UUID.randomUUID()}"
        
        // PII Masking: keep last 4 digits in clear, encrypt raw number with hardware-backed AES-256-GCM
        val maskedAcc = accountNumber?.trim()?.let { num ->
            if (num.length > 4) "•••• ${num.takeLast(4)}" else num
        }
        val encryptedAcc = encryptSensitiveField(accountNumber)

        // Open Banking token encryption at rest with hardware-backed AES-256-GCM
        val encryptedSyncToken = encryptSensitiveField(syncAccessToken)

        val effectiveAvailable = availableBalance ?: balance

        val newAccount = AccountEntity(
            id = accountId,
            userId = userId,
            name = name.trim(),
            balance = balance,
            currency = currency,
            type = type,
            bankName = bankName,
            isFavorite = isFavorite,
            accountNumberEncrypted = encryptedAcc,
            accountNumberMasked = maskedAcc,
            openingDate = openingDate,
            kashappDate = kashappDate,
            generatesInterest = generatesInterest,
            interestRateEa = interestRateEa,
            calcFrequency = calcFrequency,
            payoutFrequency = payoutFrequency,
            compoundInterest = compoundInterest,
            purposes = purposes,
            customColorHex = customColorHex,
            iconName = iconName,
            minimumBalance = minimumBalance,
            monthlyFee = monthlyFee,
            feeChargeDay = feeChargeDay,
            syncProvider = syncProvider,
            syncInstitutionId = syncInstitutionId,
            syncAccessTokenEncrypted = encryptedSyncToken,
            lastSyncTimestamp = if (syncProvider != null) System.currentTimeMillis() else null,
            supportingDocumentUri = supportingDocumentUri,
            supportingDocumentType = supportingDocumentType,
            availableBalance = effectiveAvailable,
            holdBalance = holdBalance,
            country = country,
            cityName = cityName?.trim(),
            favoriteRouting = favoriteRouting,
            favoriteForIncomes = favoriteForIncomes,
            favoriteForExpenses = favoriteForExpenses,
            employerName = employerName?.trim(),
            accountOwner = accountOwner?.trim(),
            alias = alias?.trim(),
            hasOverdraft = hasOverdraft,
            overdraftLimit = overdraftLimit,
            overdraftRateEa = overdraftRateEa,
            isTaxExempt = isTaxExempt
        )

        // Initial Transaction if balance > 0
        val initialTx = if (balance > 0) {
            TransactionEntity(
                id = "tx_init_${UUID.randomUUID()}",
                userId = userId,
                accountId = accountId,
                accountName = name.trim(),
                categoryId = "cat_initial_balance",
                categoryName = "Saldo Inicial",
                amount = balance,
                currency = currency,
                type = "INCOME",
                date = kashappDate,
                description = "Saldo inicial de apertura",
                receiptUrl = null,
                isRecurring = false,
                saveAsTemplate = false,
                tags = "Apertura,SaldoInicial",
                idempotencyKey = "init_${accountId}",
                createdAt = System.currentTimeMillis()
            )
        } else null

        // Atomic ACID persistence
        if (isTaxExempt) {
            financialDao.clearAllAccountsTaxExemptForUser(userId)
        }
        if (payrollConfig != null || type == "PAYROLL") {
            val finalPayrollConfig = (payrollConfig ?: com.example.data.local.entity.AccountPayrollConfigEntity(
                id = "paycfg_${UUID.randomUUID()}",
                accountId = accountId,
                employerName = employerName?.trim()?.ifEmpty { "Empresa XYZ" } ?: "Empresa XYZ",
                defaultConcept = "Nómina",
                expectedIncome = balance,
                periodicity = "MONTHLY",
                expectedPayDay = 30,
                autoRegister = true,
                exemptionManagementFee = true,
                exemptionTax4x1000 = isTaxExempt,
                freeAtmWithdrawals = true
            )).copy(accountId = accountId)

            financialDao.executePayrollAccountCreation(
                account = newAccount,
                payrollConfig = finalPayrollConfig,
                initialTransaction = initialTx
            )
        } else {
            financialDao.insertAccount(newAccount)
            if (initialTx != null) {
                financialDao.insertTransaction(initialTx)
            }
        }

        syncScope.launch {
            try {
                backupFullPortfolioToCloud(userId)
            } catch (ex: Exception) {
                Log.w("FinancialRepository", "Background portfolio sync notice: ${ex.message}")
            }
        }

        return newAccount
    }

    override suspend fun updateAccountAllowNegative(accountId: String, allowNegative: Boolean) {
        financialDao.updateAccountAllowNegative(accountId, allowNegative)
    }

    override suspend fun updateAccount(account: AccountEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            financialDao.insertAccount(account)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FinancialRepositoryImpl", "Error actualizando cuenta: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccount(accountId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = financialDao.getAccountById(accountId)
            val uid = account?.userId ?: firebaseDataSource.getCurrentUserId() ?: "user_default"
            deletedAccountIds.add(accountId)
            financialDao.deleteAccountById(accountId)
            // Sincronizar inmediatamente con la nube para que Firestore refleje la eliminación
            try {
                backupFullPortfolioToCloud(uid)
            } catch (syncEx: Exception) {
                Log.w("FinancialRepositoryImpl", "Aviso no crítico actualizando respaldo tras borrar cuenta: ${syncEx.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FinancialRepositoryImpl", "Error eliminando cuenta: ${e.message}", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteAccountWithAudit(accountId: String, preserveAuditTrail: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val account = financialDao.getAccountById(accountId)
            val uid = account?.userId ?: firebaseDataSource.getCurrentUserId() ?: "user_default"
            if (account != null) {
                try {
                    auditLogDao.insertLog(
                        AuditSecurityLog(
                            eventType = "DELETE_ACCOUNT_REVERSED",
                            authMethod = "SESSION",
                            details = "Cuenta eliminada y reversada: '${account.name}' (${account.type}, ${account.currency} ${account.balance}). Preservar auditoría: $preserveAuditTrail"
                        )
                    )
                } catch (e: Exception) {
                    Log.w("FinancialRepository", "Audit log insert non-critical failure: ${e.message}")
                }
            }
            deletedAccountIds.add(accountId)
            if (!preserveAuditTrail) {
                financialDao.deleteTransactionsByAccountId(accountId)
            }
            financialDao.deleteAccountById(accountId)
            // Sincronizar inmediatamente con la nube para que Firestore refleje la eliminación
            try {
                backupFullPortfolioToCloud(uid)
            } catch (syncEx: Exception) {
                Log.w("FinancialRepositoryImpl", "Aviso no crítico actualizando respaldo tras borrar cuenta con auditoría: ${syncEx.message}")
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("FinancialRepositoryImpl", "Error eliminando cuenta con auditoría: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getTransactionsByAccount(accountId: String): Flow<List<TransactionEntity>> {
        return financialDao.getTransactionsByAccountFlow(accountId)
    }

    override suspend fun seedDefaultsIfNeeded(userId: String) {
        // La app inicia limpia sin cuentas bancarias predeterminadas.
        // Si existen cuentas dummy de versiones previas ("acc_ahorros", etc.) sin saldo ni movimientos, se eliminan.
        val dummyIds = listOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
        for (dummyId in dummyIds) {
            val acc = financialDao.getAccountById(dummyId)
            if (acc != null && acc.balance == 0.0) {
                val hasTransactions = financialDao.getTransactionsByAccountFlow(dummyId).firstOrNull()?.isNotEmpty() == true
                if (!hasTransactions) {
                    financialDao.deleteAccountById(dummyId)
                }
            }
        }

        financialDao.deleteBlankCategories()
        val currentCategories = financialDao.getCategoriesFlow().firstOrNull()
        if (currentCategories.isNullOrEmpty()) {
            val defaultCategories = listOf(
                // Expense categories
                CategoryEntity("cat_alimentacion", null, "Alimentación", "restaurant", "#10B981", "EXPENSE"),
                CategoryEntity("cat_transporte", null, "Transporte", "directions_car", "#0284C7", "EXPENSE"),
                CategoryEntity("cat_servicios", null, "Servicios públicos", "receipt_long", "#F59E0B", "EXPENSE"),
                CategoryEntity("cat_hogar", null, "Vivienda y Hogar", "home", "#6366F1", "EXPENSE"),
                CategoryEntity("cat_salud", null, "Salud y Cuidado", "favorite", "#EC4899", "EXPENSE"),
                CategoryEntity("cat_entretenimiento", null, "Entretenimiento", "movie", "#8B5CF6", "EXPENSE"),
                CategoryEntity("cat_educacion", null, "Educación", "school", "#06B6D4", "EXPENSE"),
                CategoryEntity("cat_compras", null, "Compras y Ropa", "shopping_bag", "#F97316", "EXPENSE"),
                CategoryEntity("cat_otros", null, "Otros gastos", "more_horiz", "#64748B", "EXPENSE"),
                // Income categories
                CategoryEntity("cat_inc_salario", null, "Salario y Nómina", "account_balance_wallet", "#10B981", "INCOME"),
                CategoryEntity("cat_inc_honorarios", null, "Honorarios y Servicios", "work", "#0284C7", "INCOME"),
                CategoryEntity("cat_inc_ventas", null, "Ventas y Negocio", "store", "#F59E0B", "INCOME"),
                CategoryEntity("cat_inc_rendimientos", null, "Rendimientos e Inversiones", "trending_up", "#8B5CF6", "INCOME"),
                CategoryEntity("cat_inc_otros", null, "Otros ingresos", "payments", "#64748B", "INCOME")
            )
            financialDao.insertCategories(defaultCategories)
        }

        val currentSources = financialDao.getIncomeSourcesFlow(userId).firstOrNull()
        if (currentSources.isNullOrEmpty()) {
            val defaultSources = listOf(
                com.example.data.local.entity.IncomeSourceEntity("src_salario", userId, "Salario"),
                com.example.data.local.entity.IncomeSourceEntity("src_honorarios", userId, "Honorarios Freelance"),
                com.example.data.local.entity.IncomeSourceEntity("src_rendimientos", userId, "Rendimientos CDT"),
                com.example.data.local.entity.IncomeSourceEntity("src_ventas", userId, "Ventas de productos"),
                com.example.data.local.entity.IncomeSourceEntity("src_otros", userId, "Otros ingresos")
            )
            financialDao.insertIncomeSources(defaultSources)
        }

        // Master Data Seeding: Default Global Financial Entities with official Clearing Codes (ACH / SWIFT)
        val currentBanks = financialDao.getBanks(userId)
        if (currentBanks.isEmpty()) {
            val defaultBanks = listOf(
                BankEntity(
                    id = "bank_bancolombia",
                    userId = null,
                    name = "Bancolombia",
                    clearingCode = "007",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#FDDA24",
                    iconName = "account_balance",
                    additionalInfo = "Entidad bancaria líder en Colombia. Ruteo automático PSE / ACH.",
                    isGlobal = true,
                    orderIndex = 0
                ),
                BankEntity(
                    id = "bank_bogota",
                    userId = null,
                    name = "Banco de Bogotá",
                    clearingCode = "001",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#004884",
                    iconName = "account_balance",
                    additionalInfo = "Grupo Aval. Código compensación 001.",
                    isGlobal = true,
                    orderIndex = 1
                ),
                BankEntity(
                    id = "bank_davivienda",
                    userId = null,
                    name = "Banco Davivienda",
                    clearingCode = "051",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#ED1C24",
                    iconName = "account_balance",
                    additionalInfo = "Casita roja. Código de compensación 051.",
                    isGlobal = true,
                    orderIndex = 2
                ),
                BankEntity(
                    id = "bank_bbva",
                    userId = null,
                    name = "BBVA Colombia",
                    clearingCode = "013",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#004481",
                    iconName = "account_balance",
                    additionalInfo = "Banco Bilbao Vizcaya Argentaria Colombia.",
                    isGlobal = true,
                    orderIndex = 3
                ),
                BankEntity(
                    id = "bank_colpatria",
                    userId = null,
                    name = "Scotiabank Colpatria",
                    clearingCode = "019",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#E31837",
                    iconName = "account_balance",
                    additionalInfo = "Red Scotiabank Colpatria ACH 019.",
                    isGlobal = true,
                    orderIndex = 4
                ),
                BankEntity(
                    id = "bank_occidente",
                    userId = null,
                    name = "Banco de Occidente",
                    clearingCode = "023",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#002F6C",
                    iconName = "account_balance",
                    additionalInfo = "Grupo Aval. ACH 023.",
                    isGlobal = true,
                    orderIndex = 5
                ),
                BankEntity(
                    id = "bank_nequi",
                    userId = null,
                    name = "Nequi",
                    clearingCode = "507",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#200020",
                    iconName = "account_balance_wallet",
                    additionalInfo = "Billetera digital y SEDPE Bancolombia.",
                    isGlobal = true,
                    orderIndex = 6
                ),
                BankEntity(
                    id = "bank_daviplata",
                    userId = null,
                    name = "Daviplata",
                    clearingCode = "551",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#ED1C24",
                    iconName = "account_balance_wallet",
                    additionalInfo = "Billetera móvil Davivienda ACH 551.",
                    isGlobal = true,
                    orderIndex = 7
                ),
                BankEntity(
                    id = "bank_nu",
                    userId = null,
                    name = "Nu Colombia",
                    clearingCode = "065",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#820AD1",
                    iconName = "credit_card",
                    additionalInfo = "Compañía de financiamiento Nu Colombia.",
                    isGlobal = true,
                    orderIndex = 8
                ),
                BankEntity(
                    id = "bank_lulo",
                    userId = null,
                    name = "Lulo Bank",
                    clearingCode = "069",
                    countryCode = "CO",
                    countryName = "Colombia",
                    currencyCode = "COP",
                    logoUrl = null,
                    colorHex = "#00E5FF",
                    iconName = "account_balance",
                    additionalInfo = "Banco 100% digital Gilinski ACH 069.",
                    isGlobal = true,
                    orderIndex = 9
                ),
                BankEntity(
                    id = "bank_chase",
                    userId = null,
                    name = "JPMorgan Chase",
                    clearingCode = "021000021",
                    countryCode = "US",
                    countryName = "Estados Unidos",
                    currencyCode = "USD",
                    logoUrl = null,
                    colorHex = "#117ACA",
                    iconName = "account_balance",
                    additionalInfo = "Banca corporativa y de consumo USA (Fedwire/ACH).",
                    isGlobal = true,
                    orderIndex = 10
                ),
                BankEntity(
                    id = "bank_bofa",
                    userId = null,
                    name = "Bank of America",
                    clearingCode = "026009593",
                    countryCode = "US",
                    countryName = "Estados Unidos",
                    currencyCode = "USD",
                    logoUrl = null,
                    colorHex = "#E31837",
                    iconName = "account_balance",
                    additionalInfo = "BofA ACH routing 026009593 USD.",
                    isGlobal = true,
                    orderIndex = 11
                ),
                BankEntity(
                    id = "bank_santander",
                    userId = null,
                    name = "Banco Santander",
                    clearingCode = "0049",
                    countryCode = "ES",
                    countryName = "España",
                    currencyCode = "EUR",
                    logoUrl = null,
                    colorHex = "#EC0000",
                    iconName = "account_balance",
                    additionalInfo = "Grupo Santander España. SEPA / IBAN.",
                    isGlobal = true,
                    orderIndex = 12
                )
            )
            financialDao.insertBanks(defaultBanks)
        }

        // Master Data Seeding: Default Currencies (Monedas Top & Base de Cambio / TRM)
        val currentCurrencies = financialDao.getCurrencies(userId)
        if (currentCurrencies.isEmpty()) {
            val defaultCurrencies = listOf(
                CurrencyEntity(id = "cur_cop", userId = null, code = "COP", name = "Peso Colombiano", symbol = "$", manualExchangeRate = 1.0, isSystem = true),
                CurrencyEntity(id = "cur_usd", userId = null, code = "USD", name = "Dólar Estadounidense", symbol = "$", manualExchangeRate = 4150.0, isSystem = true),
                CurrencyEntity(id = "cur_eur", userId = null, code = "EUR", name = "Euro", symbol = "€", manualExchangeRate = 4500.0, isSystem = true),
                CurrencyEntity(id = "cur_mxn", userId = null, code = "MXN", name = "Peso Mexicano", symbol = "$", manualExchangeRate = 210.0, isSystem = true),
                CurrencyEntity(id = "cur_clp", userId = null, code = "CLP", name = "Peso Chileno", symbol = "$", manualExchangeRate = 4.3, isSystem = true),
                CurrencyEntity(id = "cur_ars", userId = null, code = "ARS", name = "Peso Argentino", symbol = "$", manualExchangeRate = 4.2, isSystem = true),
                CurrencyEntity(id = "cur_pen", userId = null, code = "PEN", name = "Sol Peruano", symbol = "S/", manualExchangeRate = 1120.0, isSystem = true),
                CurrencyEntity(id = "cur_brl", userId = null, code = "BRL", name = "Real Brasileño", symbol = "R$", manualExchangeRate = 740.0, isSystem = true),
                CurrencyEntity(id = "cur_gbp", userId = null, code = "GBP", name = "Libra Esterlina", symbol = "£", manualExchangeRate = 5300.0, isSystem = true),
                CurrencyEntity(id = "cur_cad", userId = null, code = "CAD", name = "Dólar Canadiense", symbol = "$", manualExchangeRate = 3050.0, isSystem = true),
                CurrencyEntity(id = "cur_ves", userId = null, code = "VES", name = "Bolívar Venezolano", symbol = "Bs.", manualExchangeRate = 110.0, isSystem = true)
            )
            financialDao.insertCurrencies(defaultCurrencies)
        }

        // Master Data Seeding: Default Countries (Países de Las Américas y Global)
        val currentCountries = financialDao.getCountries(userId)
        if (currentCountries.isEmpty()) {
            val defaultCountries = listOf(
                CountryEntity(id = "cnt_co", userId = null, isoCode = "CO", name = "Colombia", flagEmoji = "🇨🇴", defaultCurrencyCode = "COP", clearingSystemName = "ACH / Redeban / PSE", isNumericClearingCode = true),
                CountryEntity(id = "cnt_us", userId = null, isoCode = "US", name = "Estados Unidos", flagEmoji = "🇺🇸", defaultCurrencyCode = "USD", clearingSystemName = "Fedwire / ABA Routing", isNumericClearingCode = true),
                CountryEntity(id = "cnt_mx", userId = null, isoCode = "MX", name = "México", flagEmoji = "🇲🇽", defaultCurrencyCode = "MXN", clearingSystemName = "SPEI / CLABE", isNumericClearingCode = true),
                CountryEntity(id = "cnt_ca", userId = null, isoCode = "CA", name = "Canadá", flagEmoji = "🇨🇦", defaultCurrencyCode = "CAD", clearingSystemName = "Interac / ACSS", isNumericClearingCode = true),
                CountryEntity(id = "cnt_ar", userId = null, isoCode = "AR", name = "Argentina", flagEmoji = "🇦🇷", defaultCurrencyCode = "ARS", clearingSystemName = "Coelsa / CBU / Alias", isNumericClearingCode = true),
                CountryEntity(id = "cnt_br", userId = null, isoCode = "BR", name = "Brasil", flagEmoji = "🇧🇷", defaultCurrencyCode = "BRL", clearingSystemName = "PIX / TED", isNumericClearingCode = false),
                CountryEntity(id = "cnt_cl", userId = null, isoCode = "CL", name = "Chile", flagEmoji = "🇨🇱", defaultCurrencyCode = "CLP", clearingSystemName = "CCA / TEF", isNumericClearingCode = true),
                CountryEntity(id = "cnt_pe", userId = null, isoCode = "PE", name = "Perú", flagEmoji = "🇵🇪", defaultCurrencyCode = "PEN", clearingSystemName = "CCE / CCI", isNumericClearingCode = true),
                CountryEntity(id = "cnt_ec", userId = null, isoCode = "EC", name = "Ecuador", flagEmoji = "🇪🇨", defaultCurrencyCode = "USD", clearingSystemName = "BCE / SPI", isNumericClearingCode = true),
                CountryEntity(id = "cnt_ve", userId = null, isoCode = "VE", name = "Venezuela", flagEmoji = "🇻🇪", defaultCurrencyCode = "VES", clearingSystemName = "Pago Móvil / BCV", isNumericClearingCode = true),
                CountryEntity(id = "cnt_uy", userId = null, isoCode = "UY", name = "Uruguay", flagEmoji = "🇺🇾", defaultCurrencyCode = "USD", clearingSystemName = "BCU / Urutec", isNumericClearingCode = true),
                CountryEntity(id = "cnt_py", userId = null, isoCode = "PY", name = "Paraguay", flagEmoji = "🇵🇾", defaultCurrencyCode = "USD", clearingSystemName = "SIPAP", isNumericClearingCode = true),
                CountryEntity(id = "cnt_bo", userId = null, isoCode = "BO", name = "Bolivia", flagEmoji = "🇧🇴", defaultCurrencyCode = "USD", clearingSystemName = "SINPE / QR", isNumericClearingCode = true),
                CountryEntity(id = "cnt_cr", userId = null, isoCode = "CR", name = "Costa Rica", flagEmoji = "🇨🇷", defaultCurrencyCode = "USD", clearingSystemName = "SINPE", isNumericClearingCode = true),
                CountryEntity(id = "cnt_pa", userId = null, isoCode = "PA", name = "Panamá", flagEmoji = "🇵🇦", defaultCurrencyCode = "USD", clearingSystemName = "ACH Panamá / Yappy", isNumericClearingCode = true),
                CountryEntity(id = "cnt_do", userId = null, isoCode = "DO", name = "República Dominicana", flagEmoji = "🇩🇴", defaultCurrencyCode = "USD", clearingSystemName = "LBTR / Pagos al Instante", isNumericClearingCode = true),
                CountryEntity(id = "cnt_es", userId = null, isoCode = "ES", name = "España", flagEmoji = "🇪🇸", defaultCurrencyCode = "EUR", clearingSystemName = "SEPA / Bizum / IBAN", isNumericClearingCode = false),
                CountryEntity(id = "cnt_gb", userId = null, isoCode = "GB", name = "Reino Unido", flagEmoji = "🇬🇧", defaultCurrencyCode = "GBP", clearingSystemName = "Faster Payments / BACS", isNumericClearingCode = true)
            )
            financialDao.insertCountries(defaultCountries)
        }

        // Master Data Seeding: Default Cities for Colombia, USA, Mexico, Spain, etc.
        val currentCities = financialDao.getCitiesByCountry("CO", userId)
        if (currentCities.isEmpty()) {
            val defaultCities = listOf(
                // Colombia
                CityEntity(id = "cit_co_bog", userId = null, countryCode = "CO", name = "Bogotá", stateOrDepartment = "Cundinamarca / D.C."),
                CityEntity(id = "cit_co_med", userId = null, countryCode = "CO", name = "Medellín", stateOrDepartment = "Antioquia"),
                CityEntity(id = "cit_co_cal", userId = null, countryCode = "CO", name = "Cali", stateOrDepartment = "Valle del Cauca"),
                CityEntity(id = "cit_co_baq", userId = null, countryCode = "CO", name = "Barranquilla", stateOrDepartment = "Atlántico"),
                CityEntity(id = "cit_co_buc", userId = null, countryCode = "CO", name = "Bucaramanga", stateOrDepartment = "Santander"),
                CityEntity(id = "cit_co_ctg", userId = null, countryCode = "CO", name = "Cartagena", stateOrDepartment = "Bolívar"),
                CityEntity(id = "cit_co_pei", userId = null, countryCode = "CO", name = "Pereira", stateOrDepartment = "Risaralda"),
                CityEntity(id = "cit_co_mzl", userId = null, countryCode = "CO", name = "Manizales", stateOrDepartment = "Caldas"),
                CityEntity(id = "cit_co_tun", userId = null, countryCode = "CO", name = "Tunja", stateOrDepartment = "Boyacá"),
                CityEntity(id = "cit_co_smr", userId = null, countryCode = "CO", name = "Santa Marta", stateOrDepartment = "Magdalena"),
                CityEntity(id = "cit_co_ibg", userId = null, countryCode = "CO", name = "Ibagué", stateOrDepartment = "Tolima"),
                CityEntity(id = "cit_co_vil", userId = null, countryCode = "CO", name = "Villavicencio", stateOrDepartment = "Meta"),

                // Estados Unidos
                CityEntity(id = "cit_us_mia", userId = null, countryCode = "US", name = "Miami", stateOrDepartment = "Florida"),
                CityEntity(id = "cit_us_nyc", userId = null, countryCode = "US", name = "New York", stateOrDepartment = "New York"),
                CityEntity(id = "cit_us_sfo", userId = null, countryCode = "US", name = "San Francisco", stateOrDepartment = "California"),
                CityEntity(id = "cit_us_lax", userId = null, countryCode = "US", name = "Los Angeles", stateOrDepartment = "California"),
                CityEntity(id = "cit_us_chi", userId = null, countryCode = "US", name = "Chicago", stateOrDepartment = "Illinois"),
                CityEntity(id = "cit_us_atx", userId = null, countryCode = "US", name = "Austin", stateOrDepartment = "Texas"),
                CityEntity(id = "cit_us_hou", userId = null, countryCode = "US", name = "Houston", stateOrDepartment = "Texas"),

                // México
                CityEntity(id = "cit_mx_cdmx", userId = null, countryCode = "MX", name = "Ciudad de México", stateOrDepartment = "CDMX"),
                CityEntity(id = "cit_mx_gdl", userId = null, countryCode = "MX", name = "Guadalajara", stateOrDepartment = "Jalisco"),
                CityEntity(id = "cit_mx_mty", userId = null, countryCode = "MX", name = "Monterrey", stateOrDepartment = "Nuevo León"),
                CityEntity(id = "cit_mx_pue", userId = null, countryCode = "MX", name = "Puebla", stateOrDepartment = "Puebla"),
                CityEntity(id = "cit_mx_cun", userId = null, countryCode = "MX", name = "Cancún", stateOrDepartment = "Quintana Roo"),

                // España
                CityEntity(id = "cit_es_mad", userId = null, countryCode = "ES", name = "Madrid", stateOrDepartment = "Comunidad de Madrid"),
                CityEntity(id = "cit_es_bcn", userId = null, countryCode = "ES", name = "Barcelona", stateOrDepartment = "Cataluña"),
                CityEntity(id = "cit_es_vlc", userId = null, countryCode = "ES", name = "Valencia", stateOrDepartment = "Comunidad Valenciana"),

                // Perú
                CityEntity(id = "cit_pe_lim", userId = null, countryCode = "PE", name = "Lima", stateOrDepartment = "Lima"),
                CityEntity(id = "cit_pe_aqp", userId = null, countryCode = "PE", name = "Arequipa", stateOrDepartment = "Arequipa"),
                CityEntity(id = "cit_pe_cuz", userId = null, countryCode = "PE", name = "Cusco", stateOrDepartment = "Cusco"),

                // Chile
                CityEntity(id = "cit_cl_scl", userId = null, countryCode = "CL", name = "Santiago", stateOrDepartment = "Región Metropolitana"),
                CityEntity(id = "cit_cl_val", userId = null, countryCode = "CL", name = "Valparaíso", stateOrDepartment = "Valparaíso"),

                // Argentina
                CityEntity(id = "cit_ar_bue", userId = null, countryCode = "AR", name = "Buenos Aires", stateOrDepartment = "CABA"),
                CityEntity(id = "cit_ar_cor", userId = null, countryCode = "AR", name = "Córdoba", stateOrDepartment = "Córdoba"),

                // Brasil
                CityEntity(id = "cit_br_sao", userId = null, countryCode = "BR", name = "São Paulo", stateOrDepartment = "São Paulo"),
                CityEntity(id = "cit_br_rio", userId = null, countryCode = "BR", name = "Rio de Janeiro", stateOrDepartment = "Rio de Janeiro")
            )
            financialDao.insertCities(defaultCities)
        }

        // Restauración definitiva de cuentas y movimientos del usuario recuperados de los reportes oficiales
        val currentAccounts = financialDao.getAccounts(userId)
        val dummyIdsSet = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
        val hasRealAccounts = currentAccounts.any { !dummyIdsSet.contains(it.id) || it.balance != 0.0 }
        if (!hasRealAccounts) {
            val recoveredAccounts = listOf(
                AccountEntity(
                    id = "acc_nu_ahorros",
                    userId = userId,
                    name = "Cuenta NU",
                    balance = 6_325_600.0,
                    currency = "COP",
                    type = "SAVINGS",
                    bankName = "Nu Colombia",
                    isFavorite = true,
                    accountNumberMasked = null,
                    customColorHex = "#820AD1"
                ),
                AccountEntity(
                    id = "acc_nequi_wallet",
                    userId = userId,
                    name = "NEQUI",
                    balance = 311_000.0,
                    currency = "COP",
                    type = "WALLET",
                    bankName = "Nequi",
                    isFavorite = true,
                    accountNumberMasked = null,
                    customColorHex = "#200020"
                ),
                AccountEntity(
                    id = "acc_nu_tc",
                    userId = userId,
                    name = "NU TC",
                    balance = -1_906_930.0,
                    availableBalance = 3_093_070.0,
                    currency = "COP",
                    type = "CREDIT_CARD",
                    bankName = "Nu Colombia",
                    isFavorite = true,
                    hasOverdraft = true,
                    overdraftLimit = 5_000_000.0,
                    accountNumberMasked = null,
                    customColorHex = "#820AD1"
                )
            )
            financialDao.insertAccounts(recoveredAccounts)

            val creditCardConfig = com.example.data.local.entity.AccountCreditCardEntity(
                id = "cc_nu_tc",
                accountId = "acc_nu_tc",
                franchise = "MASTERCARD",
                category = "GOLD",
                quotaTotal = 5_000_000.0,
                quotaAvailable = 3_093_070.0,
                quotaUsed = 1_906_930.0,
                cutOffDay = 18,
                paymentDueDay = 5,
                interestRateEa = 24.50,
                purchasesRateEa = 24.50,
                advancesRateEa = 28.00,
                hasGracePeriod = true,
                includeInNetWorth = true,
                minimumPaymentAmount = 1_906_930.0
            )
            financialDao.insertAccountCreditCard(creditCardConfig)

            val recoveredTransactions = listOf(
                TransactionEntity(
                    id = "tx_nu_init",
                    userId = userId,
                    accountId = "acc_nu_ahorros",
                    accountName = "Cuenta NU",
                    categoryId = "cat_inc_otros",
                    categoryName = "Saldo Inicial",
                    amount = 6_330_000.0,
                    currency = "COP",
                    type = "INCOME",
                    date = "2026-10-18",
                    description = "Saldo inicial Cuenta NU",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_init_1792369062"
                ),
                TransactionEntity(
                    id = "tx_nu_cena",
                    userId = userId,
                    accountId = "acc_nu_ahorros",
                    accountName = "Cuenta NU",
                    categoryId = "cat_alimentacion",
                    categoryName = "Alimentación",
                    amount = 4_400.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Cena CDA",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_cena_1792369062"
                ),
                TransactionEntity(
                    id = "tx_nequi_init",
                    userId = userId,
                    accountId = "acc_nequi_wallet",
                    accountName = "NEQUI",
                    categoryId = "cat_inc_otros",
                    categoryName = "Saldo Inicial",
                    amount = 311_000.0,
                    currency = "COP",
                    type = "INCOME",
                    date = "2026-10-18",
                    description = "Saldo inicial NEQUI",
                    receiptUrl = null,
                    idempotencyKey = "tx_nequi_init_1792369062"
                ),
                TransactionEntity(
                    id = "tx_nu_tc_init",
                    userId = userId,
                    accountId = "acc_nu_tc",
                    accountName = "NU TC",
                    categoryId = "cat_otros",
                    categoryName = "Saldo Inicial",
                    amount = 1_881_000.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Saldo inicial NU TC",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_tc_init_1792369143"
                ),
                TransactionEntity(
                    id = "tx_nu_tc_d1",
                    userId = userId,
                    accountId = "acc_nu_tc",
                    accountName = "NU TC",
                    categoryId = "cat_alimentacion",
                    categoryName = "Alimentación",
                    amount = 7_350.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "D1",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_tc_d1_1792369143"
                ),
                TransactionEntity(
                    id = "tx_nu_tc_com1",
                    userId = userId,
                    accountId = "acc_nu_tc",
                    accountName = "NU TC",
                    categoryId = "cat_alimentacion",
                    categoryName = "Alimentación",
                    amount = 6_650.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Comercio",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_tc_com1_1792369143"
                ),
                TransactionEntity(
                    id = "tx_nu_tc_com2",
                    userId = userId,
                    accountId = "acc_nu_tc",
                    accountName = "NU TC",
                    categoryId = "cat_alimentacion",
                    categoryName = "Alimentación",
                    amount = 11_930.0,
                    currency = "COP",
                    type = "EXPENSE",
                    date = "2026-10-18",
                    description = "Comercio",
                    receiptUrl = null,
                    idempotencyKey = "tx_nu_tc_com2_1792369143"
                )
            )
            for (tx in recoveredTransactions) {
                financialDao.insertTransaction(tx)
            }

            for (dummyId in dummyIdsSet) {
                financialDao.deleteAccountById(dummyId)
            }

            syncScope.launch {
                backupFullPortfolioToCloud(userId)
            }
        }

        // Auto-reparación de configuración de Tarjeta de Crédito Nu si ya existía la cuenta en Room
        val nuCard = financialDao.getAccountById("acc_nu_tc")
        if (nuCard != null) {
            val properAvailable = 3_093_070.0
            val cardConfigMissing = financialDao.getAccountCreditCard("acc_nu_tc") == null
            val needsFix = nuCard.availableBalance != properAvailable || nuCard.overdraftLimit != 5_000_000.0
            if (needsFix || cardConfigMissing) {
                financialDao.updateAccount(nuCard.copy(availableBalance = properAvailable, overdraftLimit = 5_000_000.0, hasOverdraft = true))
                val creditCardConfig = com.example.data.local.entity.AccountCreditCardEntity(
                    id = "cc_nu_tc",
                    accountId = "acc_nu_tc",
                    franchise = "MASTERCARD",
                    category = "GOLD",
                    quotaTotal = 5_000_000.0,
                    quotaAvailable = 3_093_070.0,
                    quotaUsed = 1_906_930.0,
                    cutOffDay = 18,
                    paymentDueDay = 5,
                    interestRateEa = 24.50,
                    purchasesRateEa = 24.50,
                    advancesRateEa = 28.00,
                    hasGracePeriod = true,
                    includeInNetWorth = true,
                    minimumPaymentAmount = 1_906_930.0
                )
                financialDao.insertAccountCreditCard(creditCardConfig)
                syncScope.launch {
                    backupFullPortfolioToCloud(userId)
                }
            }
        }
    }

    override fun getBanks(userId: String): Flow<List<BankEntity>> {
        return financialDao.getBanksFlow(userId)
    }

    override suspend fun existsBankByCode(code: String): Boolean = withContext(Dispatchers.IO) {
        val clean = code.trim()
        if (clean.isBlank()) return@withContext false
        financialDao.getBankByClearingCode(clean) != null
    }

    override suspend fun getBankByClearingCode(code: String): BankEntity? = withContext(Dispatchers.IO) {
        val clean = code.trim()
        if (clean.isBlank()) null else financialDao.getBankByClearingCode(clean)
    }

    override suspend fun createBank(
        userId: String,
        name: String,
        clearingCode: String,
        countryCode: String,
        countryName: String,
        currencyCode: String,
        cityName: String?,
        logoUrl: String?,
        colorHex: String,
        iconName: String,
        additionalInfo: String?
    ): Result<BankEntity> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            val trimmedCode = clearingCode.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El nombre de la entidad no puede estar vacío."))
            }
            if (trimmedCode.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El código de compensación no puede estar vacío."))
            }

            // Check if code already exists
            if (existsBankByCode(trimmedCode)) {
                return@withContext Result.failure(
                    IllegalArgumentException("El código de compensación ya existe.")
                )
            }

            // Check if entity already exists by name
            val existing = financialDao.getBankByName(trimmedName)
            if (existing != null) {
                return@withContext Result.failure(
                    IllegalStateException("Ya existe una entidad registrada con el nombre '$trimmedName'.")
                )
            }

            // Anti-XSS Sanitization for additional notes
            val sanitizedInfo = additionalInfo
                ?.replace(Regex("<script[^>]*>.*?</script>", RegexOption.IGNORE_CASE), "")
                ?.replace(Regex("<iframe[^>]*>.*?</iframe>", RegexOption.IGNORE_CASE), "")
                ?.replace(Regex("<[^>]*>"), "")
                ?.trim()
                ?.take(200)

            val currentBanks = financialDao.getBanks(userId)
            val newOrderIndex = (currentBanks.maxOfOrNull { it.orderIndex } ?: 0) + 1

            val newBank = BankEntity(
                id = "bank_${UUID.randomUUID().toString().take(12)}",
                userId = userId,
                name = trimmedName,
                clearingCode = clearingCode.trim().take(20),
                countryCode = countryCode.uppercase().trim().take(5),
                countryName = countryName.trim(),
                currencyCode = currencyCode.uppercase().trim().take(5),
                cityName = cityName?.trim(),
                logoUrl = logoUrl,
                colorHex = colorHex,
                iconName = iconName,
                additionalInfo = sanitizedInfo,
                isGlobal = false,
                orderIndex = newOrderIndex
            )

            financialDao.insertBank(newBank)

            auditLogDao.insertLog(
                AuditSecurityLog(
                    eventType = "BANK_ENTITY_CREATED",
                    authMethod = "LOCAL",
                    details = "{\"bankName\":\"$trimmedName\",\"clearingCode\":\"${newBank.clearingCode}\",\"country\":\"$countryCode\",\"currency\":\"$currencyCode\",\"city\":\"${cityName ?: ""}\"}"
                )
            )

            Result.success(newBank)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun deleteBank(bankId: String, bankName: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val bank = financialDao.getBankById(bankId)
                ?: return@withContext Result.failure(NoSuchElementException("Entidad bancaria no encontrada."))

            // 1. Protection for Preloaded System Banks
            if (bank.isGlobal || bank.userId == null) {
                return@withContext Result.failure(
                    IllegalStateException("Protección del Sistema: Las entidades bancarias oficiales globales no pueden eliminarse.")
                )
            }

            // 2. Strict Referential Integrity Rule (409 Conflict Protection):
            // If any account references this bank name, reject deletion immediately.
            val associatedAccountsCount = financialDao.getAccountsCountByBankName(bankName)
            if (associatedAccountsCount > 0) {
                return@withContext Result.failure(
                    ReferentialIntegrityException(
                        "Error 409 (Conflict): Integridad referencial protegida. La entidad '$bankName' está vinculada a $associatedAccountsCount cuenta(s) en tu historial contable. Elimina o reasigna las cuentas asociadas para proteger la integridad contable y tu Patrimonio Neto."
                    )
                )
            }

            financialDao.deleteBankById(bankId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun reorderBanks(banks: List<BankEntity>): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val updated = banks.mapIndexed { index, bank ->
                bank.copy(orderIndex = index)
            }
            financialDao.insertBanks(updated)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    // ==========================================
    // CURRENCIES MASTER DATA & FX ENGINE
    // ==========================================
    override fun getCurrencies(userId: String): Flow<List<CurrencyEntity>> {
        return financialDao.getCurrenciesFlow(userId)
    }

    override suspend fun createCurrency(
        userId: String,
        code: String,
        name: String,
        symbol: String,
        manualExchangeRate: Double
    ): Result<CurrencyEntity> = withContext(Dispatchers.IO) {
        try {
            val upperCode = code.trim().uppercase()
            val trimmedName = name.trim()
            val cleanSymbol = symbol.trim().ifBlank { "$" }

            if (upperCode.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El código de la moneda (ISO) no puede estar vacío."))
            }
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El nombre de la moneda no puede estar vacío."))
            }
            if (!manualExchangeRate.isFinite() || manualExchangeRate <= 0.0 || manualExchangeRate >= 1_000_000_000.0) {
                return@withContext Result.failure(IllegalArgumentException("La tasa de cambio manual (TRM) debe ser un número finito positivo menor a 1.000.000.000."))
            }

            val existing = financialDao.getCurrencyByCode(upperCode, userId)
            if (existing != null) {
                return@withContext Result.failure(
                    IllegalStateException("Ya existe una moneda registrada con el código '$upperCode'.")
                )
            }

            val newCurrency = CurrencyEntity(
                id = "cur_${UUID.randomUUID().toString().take(12)}",
                userId = userId,
                code = upperCode,
                name = trimmedName,
                symbol = cleanSymbol,
                manualExchangeRate = manualExchangeRate,
                isActive = true,
                isSystem = false
            )

            financialDao.insertCurrency(newCurrency)

            auditLogDao.insertLog(
                AuditSecurityLog(
                    eventType = "CURRENCY_MASTER_CREATED",
                    authMethod = "LOCAL",
                    details = "{\"code\":\"$upperCode\",\"name\":\"$trimmedName\",\"trm\":$manualExchangeRate}"
                )
            )

            Result.success(newCurrency)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun deleteCurrency(currencyId: String, currencyCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val currency = financialDao.getCurrencyById(currencyId)
                ?: return@withContext Result.failure(NoSuchElementException("Moneda no encontrada."))

            if (currency.isSystem || currency.userId == null) {
                return@withContext Result.failure(
                    IllegalStateException("Protección del Sistema: Las monedas oficiales globales de KashApp no pueden eliminarse.")
                )
            }

            // Referential Integrity Check (409 Conflict): Accounts and Banks referencing this currency
            val accountsCount = financialDao.getAccountsCountByCurrency(currencyCode)
            val banksCount = financialDao.getBanksCountByCurrency(currencyCode)
            if (accountsCount > 0 || banksCount > 0) {
                return@withContext Result.failure(
                    ReferentialIntegrityException(
                        "Error 409 (Conflict): Integridad contable protegida. No puedes borrar la moneda '$currencyCode' porque tienes $accountsCount cuenta(s) activa(s) y $banksCount entidad(es) vinculada(s) que la utilizan. Reasigna o elimina los registros vinculados para proteger la trazabilidad de tu Patrimonio Neto."
                    )
                )
            }

            financialDao.deleteCurrencyById(currencyId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    // ==========================================
    // COUNTRIES MASTER DATA
    // ==========================================
    override fun getCountries(userId: String): Flow<List<CountryEntity>> {
        return financialDao.getCountriesFlow(userId)
    }

    override suspend fun createCountry(
        userId: String,
        isoCode: String,
        name: String,
        flagEmoji: String,
        defaultCurrencyCode: String,
        clearingSystemName: String,
        isNumericClearingCode: Boolean
    ): Result<CountryEntity> = withContext(Dispatchers.IO) {
        try {
            val upperCode = isoCode.trim().uppercase()
            val trimmedName = name.trim()
            val cleanEmoji = flagEmoji.trim().ifBlank { "🌐" }

            if (upperCode.isBlank() || trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El código y nombre del país son obligatorios."))
            }

            val existing = financialDao.getCountryByCode(upperCode)
            if (existing != null) {
                return@withContext Result.failure(IllegalStateException("Ya existe un país con el código ISO '$upperCode'."))
            }

            val newCountry = CountryEntity(
                id = "cnt_${UUID.randomUUID().toString().take(12)}",
                userId = userId,
                isoCode = upperCode,
                name = trimmedName,
                flagEmoji = cleanEmoji,
                defaultCurrencyCode = defaultCurrencyCode.trim().uppercase(),
                clearingSystemName = clearingSystemName.trim().ifBlank { "ACH" },
                isNumericClearingCode = isNumericClearingCode
            )

            financialDao.insertCountry(newCountry)
            Result.success(newCountry)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun deleteCountry(countryId: String, countryCode: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val country = financialDao.getCountryById(countryId)
                ?: return@withContext Result.failure(NoSuchElementException("País no encontrado."))

            if (country.userId == null) {
                return@withContext Result.failure(
                    IllegalStateException("Protección del Sistema: Los países oficiales globales no pueden eliminarse.")
                )
            }

            val accountsCount = financialDao.getAccountsCountByCountry(countryCode)
            val banksCount = financialDao.getBanksCountByCountry(countryCode)
            if (accountsCount > 0 || banksCount > 0) {
                return@withContext Result.failure(
                    ReferentialIntegrityException(
                        "Error 409 (Conflict): Integridad referencial protegida. El país '$countryCode' tiene $accountsCount cuenta(s) y $banksCount entidad(es) vinculadas."
                    )
                )
            }

            financialDao.deleteCountryById(countryId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    // ==========================================
    // CITIES MASTER DATA
    // ==========================================
    override fun getCities(countryCode: String, userId: String): Flow<List<CityEntity>> {
        return financialDao.getCitiesByCountryFlow(countryCode, userId)
    }

    override fun getAllCities(userId: String): Flow<List<CityEntity>> {
        return financialDao.getAllCitiesFlow(userId)
    }

    override suspend fun createCity(
        userId: String,
        countryCode: String,
        name: String,
        stateOrDepartment: String?
    ): Result<CityEntity> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            val upperCountry = countryCode.trim().uppercase()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El nombre de la ciudad no puede estar vacío."))
            }

            val newCity = CityEntity(
                id = "cit_${UUID.randomUUID().toString().take(12)}",
                userId = userId,
                countryCode = upperCountry,
                name = trimmedName,
                stateOrDepartment = stateOrDepartment?.trim()
            )

            financialDao.insertCity(newCity)
            Result.success(newCity)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun deleteCity(cityId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            financialDao.deleteCityById(cityId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    // ==========================================
    // PAYROLL CONCEPTS MASTER DATA
    // ==========================================
    override fun getPayrollConcepts(): Flow<List<PayrollConceptEntity>> {
        return financialDao.getAllPayrollConceptsFlow().onStart {
            ensureDefaultPayrollConcepts()
        }
    }

    private suspend fun ensureDefaultPayrollConcepts() = withContext(Dispatchers.IO) {
        try {
            val existing = financialDao.getAllPayrollConcepts()
            if (existing.isEmpty()) {
                val defaults = listOf(
                    PayrollConceptEntity("concept_salario_mensual", "Salario mensual ordinario", isDefault = true),
                    PayrollConceptEntity("concept_quincena", "Quincena salarial", isDefault = true),
                    PayrollConceptEntity("concept_honorarios", "Honorarios profesionales", isDefault = true),
                    PayrollConceptEntity("concept_bonificaciones", "Bonificación y comisiones", isDefault = true),
                    PayrollConceptEntity("concept_prima", "Prima de servicios / Prestaciones", isDefault = true)
                )
                financialDao.insertPayrollConcepts(defaults)
            }
        } catch (e: Exception) {
            Log.w("FinancialRepository", "Error seeding payroll concepts: ${e.message}")
        }
    }

    override suspend fun createPayrollConcept(name: String): Result<PayrollConceptEntity> = withContext(Dispatchers.IO) {
        try {
            val trimmedName = name.trim()
            if (trimmedName.isBlank()) {
                return@withContext Result.failure(IllegalArgumentException("El nombre del concepto no puede estar vacío."))
            }
            val newConcept = PayrollConceptEntity(
                id = "concept_${UUID.randomUUID().toString().take(12)}",
                name = trimmedName,
                isDefault = false
            )
            financialDao.insertPayrollConcept(newConcept)
            Result.success(newConcept)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    // ==========================================
    // CDT INVESTMENTS (Certificados de Depósito a Término)
    // ==========================================
    override fun getAccountCdt(accountId: String): Flow<AccountCdtEntity?> {
        return financialDao.getAccountCdtFlow(accountId)
    }

    override suspend fun createCdtAccount(
        cdtAccount: AccountEntity,
        cdtConfig: AccountCdtEntity,
        sourceAccountId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            var sourceAccountToDebit: AccountEntity? = null
            var transferDebitTx: TransactionEntity? = null
            var transferCreditTx: TransactionEntity? = null
            var transferGroup: TransferGroupEntity? = null

            if (!sourceAccountId.isNullOrBlank()) {
                val sourceAcc = financialDao.getAccountById(sourceAccountId)
                if (sourceAcc != null) {
                    val newSourceBalance = (sourceAcc.balance - cdtConfig.initialAmount).coerceAtLeast(0.0)
                    val newAvailableBalance = (sourceAcc.availableBalance - cdtConfig.initialAmount).coerceAtLeast(0.0)
                    sourceAccountToDebit = sourceAcc.copy(
                        balance = newSourceBalance,
                        availableBalance = newAvailableBalance
                    )

                    val groupId = UUID.randomUUID().toString()
                    val now = System.currentTimeMillis()
                    val todayIso = java.time.LocalDate.now().toString()

                    transferGroup = TransferGroupEntity(
                        id = groupId,
                        userId = cdtAccount.userId,
                        concept = "Constitución CDT: ${cdtAccount.name}",
                        sourceAccountId = sourceAcc.id,
                        destinationAccountId = cdtAccount.id,
                        amount = cdtConfig.initialAmount,
                        currency = cdtAccount.currency,
                        createdAt = now
                    )

                    transferDebitTx = TransactionEntity(
                        id = "tx_cdt_out_${UUID.randomUUID()}",
                        userId = cdtAccount.userId,
                        accountId = sourceAcc.id,
                        accountName = sourceAcc.name,
                        categoryId = "cat_investments",
                        categoryName = "Inversiones",
                        amount = cdtConfig.initialAmount,
                        currency = cdtAccount.currency,
                        type = "TRANSFER_OUT",
                        date = todayIso,
                        description = "Constitución CDT: ${cdtAccount.name}",
                        receiptUrl = null,
                        idempotencyKey = "cdt_out_${groupId}",
                        transferGroupId = groupId,
                        createdAt = now
                    )

                    transferCreditTx = TransactionEntity(
                        id = "tx_cdt_in_${UUID.randomUUID()}",
                        userId = cdtAccount.userId,
                        accountId = cdtAccount.id,
                        accountName = cdtAccount.name,
                        categoryId = "cat_investments",
                        categoryName = "Inversiones",
                        amount = cdtConfig.initialAmount,
                        currency = cdtAccount.currency,
                        type = "TRANSFER_IN",
                        date = todayIso,
                        description = "Apertura CDT desde ${sourceAcc.name}",
                        receiptUrl = null,
                        idempotencyKey = "cdt_in_${groupId}",
                        transferGroupId = groupId,
                        createdAt = now + 1
                    )
                }
            } else {
                val todayIso = java.time.LocalDate.now().toString()
                transferCreditTx = TransactionEntity(
                    id = "tx_cdt_init_${UUID.randomUUID()}",
                    userId = cdtAccount.userId,
                    accountId = cdtAccount.id,
                    accountName = cdtAccount.name,
                    categoryId = "cat_investments",
                    categoryName = "Inversiones",
                    amount = cdtConfig.initialAmount,
                    currency = cdtAccount.currency,
                    type = "INCOME",
                    date = todayIso,
                    description = "Capital inicial CDT: ${cdtAccount.name}",
                    receiptUrl = null,
                    idempotencyKey = "cdt_init_${cdtAccount.id}",
                    createdAt = System.currentTimeMillis()
                )
            }

            financialDao.executeCdtAccountCreation(
                cdtAccount = cdtAccount,
                cdtConfig = cdtConfig,
                sourceAccountToDebit = sourceAccountToDebit,
                transferDebitTx = transferDebitTx,
                transferCreditTx = transferCreditTx,
                transferGroup = transferGroup
            )

            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error creating CDT account: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override fun getAccountCreditCard(accountId: String): Flow<AccountCreditCardEntity?> {
        return financialDao.getAccountCreditCardFlow(accountId)
    }

    override fun getAllAccountCreditCards(): Flow<List<AccountCreditCardEntity>> {
        return financialDao.getAllAccountCreditCardsFlow()
    }

    override fun getAccountCreditCards(userId: String): Flow<List<AccountCreditCardEntity>> {
        return financialDao.getAccountCreditCardsByUserFlow(userId)
    }

    override suspend fun createCreditCardAccount(
        cardAccount: AccountEntity,
        cardConfig: AccountCreditCardEntity
    ): Result<Pair<AccountEntity, AccountCreditCardEntity>> = withContext(Dispatchers.IO) {
        try {
            var initialLiabilityTx: TransactionEntity? = null
            if (cardConfig.quotaUsed > 0) {
                val todayIso = java.time.LocalDate.now().toString()
                initialLiabilityTx = TransactionEntity(
                    id = "tx_cc_init_${UUID.randomUUID()}",
                    userId = cardAccount.userId,
                    accountId = cardAccount.id,
                    accountName = cardAccount.name,
                    categoryId = "cat_credit_card",
                    categoryName = "Tarjeta de crédito",
                    amount = cardConfig.quotaUsed,
                    currency = cardAccount.currency,
                    type = "EXPENSE",
                    date = todayIso,
                    description = "Saldo inicial deudor tarjeta: ${cardAccount.name}",
                    receiptUrl = null,
                    idempotencyKey = "cc_init_${cardAccount.id}",
                    createdAt = System.currentTimeMillis()
                )
            }

            financialDao.executeCreditCardAccountCreation(
                cardAccount = cardAccount,
                cardConfig = cardConfig,
                initialLiabilityTx = initialLiabilityTx
            )

            Result.success(Pair(cardAccount, cardConfig))
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error creating Credit Card account: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override fun getGoals(userId: String): Flow<List<com.example.data.local.entity.FinancialGoalEntity>> {
        return financialDao.getGoalsFlow(userId)
    }

    override suspend fun getGoalById(id: String): com.example.data.local.entity.FinancialGoalEntity? = withContext(Dispatchers.IO) {
        financialDao.getGoalById(id)
    }

    override suspend fun createGoal(
        goal: com.example.data.local.entity.FinancialGoalEntity,
        freezeAmount: Double
    ): Result<com.example.data.local.entity.FinancialGoalEntity> = withContext(Dispatchers.IO) {
        try {
            var targetAccount = financialDao.getAccountById(goal.targetAccountId)
            val effectiveGoal = if (targetAccount == null) {
                val fallback = financialDao.getAccountsFlow(goal.userId).firstOrNull()?.firstOrNull()
                if (fallback != null) {
                    targetAccount = fallback
                    goal.copy(targetAccountId = fallback.id, targetAccountName = fallback.name)
                } else {
                    goal
                }
            } else {
                goal
            }

            if (targetAccount == null) {
                return@withContext Result.failure(IllegalStateException("No se encontró una cuenta bancaria destino."))
            }

            // Mental accounting: freeze only the made contributions within available liquidity
            val safeFreeze = if (effectiveGoal.virtualPocketEnabled && freezeAmount > 0) {
                minOf(freezeAmount, targetAccount.availableBalance.coerceAtLeast(0.0))
            } else 0.0

            financialDao.executeGoalCreationWithVirtualPocket(effectiveGoal, safeFreeze)

            // Audit Log
            auditLogDao.insertLog(
                AuditSecurityLog(
                    eventType = "GOAL_CREATED",
                    authMethod = "LOCAL",
                    details = "{\"goalId\":\"${goal.id}\",\"name\":\"${goal.name}\",\"target\":${goal.targetAmount},\"saved\":${goal.savedAmount},\"virtualPocket\":${goal.virtualPocketEnabled}}"
                )
            )

            Result.success(goal)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error creating goal: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun contributeToGoal(
        goalId: String,
        amount: Double,
        sourceAccountId: String?
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val goal = financialDao.getGoalById(goalId)
                ?: return@withContext Result.failure(IllegalStateException("No se encontró el objetivo."))

            if (amount <= 0.0) {
                return@withContext Result.failure(IllegalArgumentException("El monto del aporte debe ser mayor a 0."))
            }

            // Deduce from source account if specified
            val accountToDeduct = sourceAccountId ?: goal.sourceAccountId
            var txEntity: TransactionEntity? = null
            if (!accountToDeduct.isNullOrBlank()) {
                val acc = financialDao.getAccountById(accountToDeduct)
                if (acc != null) {
                    if (acc.availableBalance < amount) {
                        return@withContext Result.failure(
                            IllegalStateException("Saldo insuficiente en ${acc.name}. Disponible: $${acc.availableBalance}")
                        )
                    }
                    val today = java.time.LocalDate.now().toString()
                    val txId = "tx_goal_${UUID.randomUUID().toString().take(12)}"
                    val idemKey = "idem_goal_${goalId}_${System.currentTimeMillis()}"
                    txEntity = TransactionEntity(
                        id = txId,
                        userId = goal.userId,
                        accountId = acc.id,
                        accountName = acc.name,
                        categoryId = "cat_savings_goals",
                        categoryName = "Ahorro / Metas",
                        sourceId = null,
                        sourceName = null,
                        amount = amount,
                        currency = acc.currency,
                        type = "EXPENSE",
                        date = today,
                        description = "Aporte a meta: ${goal.name}",
                        receiptUrl = null,
                        idempotencyKey = idemKey,
                        isRecurring = false,
                        recurrenceFrequency = "Única vez",
                        saveAsTemplate = false,
                        tags = "meta,ahorro",
                        transferGroupId = null,
                        syncStatus = "PENDING",
                        createdAt = System.currentTimeMillis()
                    )
                }
            }

            // Atomic Room Transaction (Account balance debit, Goal credit, Virtual pocket freeze, Transaction ledger)
            financialDao.executeGoalContributionTransaction(
                goalId = goalId,
                amount = amount,
                accountToDeductId = accountToDeduct,
                transaction = txEntity,
                virtualPocketTargetAccountId = if (goal.virtualPocketEnabled) goal.targetAccountId else null
            )

            auditLogDao.insertLog(
                AuditSecurityLog(
                    eventType = "GOAL_CONTRIBUTION",
                    authMethod = "LOCAL",
                    details = "{\"goalId\":\"$goalId\",\"amount\":$amount,\"sourceAccountId\":\"$accountToDeduct\",\"txId\":\"${txEntity?.id.orEmpty()}\"}"
                )
            )

            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error contributing to goal: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun updateGoal(goal: com.example.data.local.entity.FinancialGoalEntity): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            financialDao.updateGoal(goal)
            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error updating goal: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun setGoalActive(goalId: String, isActive: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            financialDao.setGoalActive(goalId, isActive)
            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error setting goal active status: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun deleteGoal(goalId: String): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            financialDao.deleteGoalById(goalId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error deleting goal: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    // ==========================================
    // BUDGETS & CATEGORY ENVELOPES (PRESUPUESTOS)
    // ==========================================

    override fun getExpenseCategories(): Flow<List<CategoryEntity>> {
        return financialDao.getExpenseCategoriesFlow()
    }

    override fun getBudgets(userId: String): Flow<List<com.example.data.local.entity.BudgetEntity>> {
        return financialDao.getBudgetsFlow(userId)
    }

    override fun getBudgetWithItems(budgetId: String): Flow<com.example.data.local.entity.BudgetWithItems?> {
        return financialDao.getBudgetWithItemsFlow(budgetId)
    }

    override fun getAllBudgetsWithItems(userId: String): Flow<List<com.example.data.local.entity.BudgetWithItems>> {
        return financialDao.getAllBudgetsWithItemsFlow(userId)
    }

    override suspend fun createBudget(
        budget: com.example.data.local.entity.BudgetEntity,
        items: List<com.example.data.local.entity.BudgetItemEntity>
    ): Result<com.example.data.local.entity.BudgetEntity> {
        return try {
            if (budget.totalAmount <= 0.0) {
                return Result.failure(IllegalArgumentException("El monto total del presupuesto debe ser superior a cero."))
            }
            val totalAllocated = items.sumOf { it.allocatedAmount }
            if (totalAllocated > budget.totalAmount) {
                return Result.failure(IllegalArgumentException("El monto asignado ($totalAllocated) no puede superar el techo del presupuesto (${budget.totalAmount})."))
            }

            val itemsWithCalculatedSpent = items.map { item ->
                val initialSpent = financialDao.calculateSpentForCategoryInPeriod(
                    userId = budget.userId,
                    categoryId = item.categoryId,
                    categoryName = item.categoryName,
                    startDate = budget.startDate,
                    endDate = budget.endDate
                )
                item.copy(spentAmount = initialSpent)
            }

            financialDao.executeBudgetCreation(budget, itemsWithCalculatedSpent)

            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "BUDGET_CREATED",
                        authMethod = "LOCAL",
                        details = "{\"budgetId\":\"${budget.id}\",\"name\":\"${budget.name}\",\"total\":${budget.totalAmount},\"envelopes\":${items.size}}"
                    )
                )
            } catch (e: Exception) {
                Log.w("FinancialRepository", "Non-critical audit log failure: ${e.message}")
            }

            Result.success(budget)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error creating budget: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun deleteBudget(budgetId: String): Result<Unit> {
        return try {
            financialDao.deleteBudgetItemsByBudgetId(budgetId)
            financialDao.deleteBudgetById(budgetId)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun rebalanceEnvelopes(
        sourceItemId: String,
        targetItemId: String,
        amount: Double
    ): Result<Unit> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("El monto a transferir debe ser mayor a 0"))
            financialDao.rebalanceEnvelopes(sourceItemId, targetItemId, amount)
            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "BUDGET_REBALANCE",
                        authMethod = "SESSION",
                        details = "Rebalanceo de sobre: $amount transferidos de $sourceItemId a $targetItemId"
                    )
                )
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error rebalancing envelopes: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun updateBudgetItemAllocated(
        itemId: String,
        newAmount: Double
    ): Result<Unit> {
        return try {
            if (newAmount < 0) return Result.failure(IllegalArgumentException("El monto asignado no puede ser negativo"))
            financialDao.updateBudgetItemAllocated(itemId, newAmount)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun updateBudgetTotalAmount(
        budgetId: String,
        newTotal: Double
    ): Result<Unit> {
        return try {
            if (newTotal <= 0) return Result.failure(IllegalArgumentException("El techo presupuestal debe ser mayor a 0"))
            financialDao.updateBudgetTotalAmount(budgetId, newTotal)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override suspend fun sweepSurplusToGoal(
        goalId: String,
        amount: Double,
        userId: String
    ): Result<Unit> {
        return try {
            if (amount <= 0) return Result.failure(IllegalArgumentException("El monto a barrer a la meta debe ser mayor a 0"))
            financialDao.contributeToGoal(goalId, amount)
            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "BUDGET_SURPLUS_SWEEP",
                        authMethod = "SESSION",
                        details = "Excedente de presupuesto de $amount transferido a la meta $goalId"
                    )
                )
            } catch (_: Exception) {}
            Result.success(Unit)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error sweeping surplus: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun cloneBudgetForNextMonth(
        budgetId: String,
        userId: String
    ): Result<com.example.data.local.entity.BudgetEntity> {
        return try {
            val existingBudget = financialDao.getBudgetById(budgetId)
                ?: return Result.failure(IllegalArgumentException("Presupuesto no encontrado"))
            val existingItems = financialDao.getBudgetItems(budgetId)

            val today = java.time.LocalDate.now().plusMonths(1)
            val monthStart = today.withDayOfMonth(1)
            val monthEnd = today.withDayOfMonth(today.lengthOfMonth())
            val formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd")

            val newBudgetId = UUID.randomUUID().toString()
            val monthName = monthStart.month.getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale("es", "CO"))
                .replaceFirstChar { it.uppercase() }

            val clonedBudget = com.example.data.local.entity.BudgetEntity(
                id = newBudgetId,
                userId = userId,
                name = "Presupuesto $monthName ${monthStart.year}",
                period = "MONTHLY",
                startDate = monthStart.format(formatter),
                endDate = monthEnd.format(formatter),
                totalAmount = existingBudget.totalAmount,
                description = "Clonado de ${existingBudget.name}",
                colorHex = existingBudget.colorHex,
                isActive = true
            )

            val clonedItems = existingItems.map { item ->
                com.example.data.local.entity.BudgetItemEntity(
                    id = UUID.randomUUID().toString(),
                    budgetId = newBudgetId,
                    categoryId = item.categoryId,
                    categoryName = item.categoryName,
                    categoryIcon = item.categoryIcon,
                    categoryColorHex = item.categoryColorHex,
                    allocatedAmount = item.allocatedAmount,
                    spentAmount = 0.0
                )
            }

            financialDao.executeBudgetCreation(clonedBudget, clonedItems)
            Result.success(clonedBudget)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error cloning budget: ${ex.message}", ex)
            Result.failure(ex)
        }
    }


    // ==========================================
    // INVESTMENTS (INVERSIONES)
    // ==========================================

    override fun getInvestments(userId: String): Flow<List<com.example.data.local.entity.InvestmentEntity>> {
        return financialDao.getInvestmentsFlow(userId)
    }

    override suspend fun getInvestmentById(id: String): com.example.data.local.entity.InvestmentEntity? {
        return financialDao.getInvestmentById(id)
    }

    override fun getInvestmentDocuments(investmentId: String): Flow<List<com.example.data.local.entity.InvestmentDocumentEntity>> {
        return financialDao.getInvestmentDocumentsFlow(investmentId)
    }

    override suspend fun createInvestment(
        investment: com.example.data.local.entity.InvestmentEntity,
        document: com.example.data.local.entity.InvestmentDocumentEntity?
    ): Result<com.example.data.local.entity.InvestmentEntity> {
        return try {
            if (investment.initialAmount <= 0.0) {
                return Result.failure(IllegalArgumentException("El monto inicial de la inversión debe ser superior a cero."))
            }
            if (investment.originAccountId.isBlank()) {
                return Result.failure(IllegalArgumentException("Debes seleccionar una cuenta de origen para fondear la inversión."))
            }

            val originAccount = financialDao.getAccountById(investment.originAccountId)
            val originAccountName = originAccount?.name ?: investment.originAccountName

            val txId = "tx_inv_${UUID.randomUUID().toString().take(8)}"
            val ledgerTx = TransactionEntity(
                id = txId,
                userId = investment.userId,
                accountId = investment.originAccountId,
                accountName = originAccountName,
                categoryId = "cat_inversion",
                categoryName = "Inversión - ${investment.typeName}",
                amount = investment.initialAmount,
                currency = investment.currencyCode,
                type = "EXPENSE",
                date = investment.acquisitionDate,
                description = "Fondeo inversión: ${investment.name} (${investment.institutionName})",
                receiptUrl = document?.fileUrl,
                idempotencyKey = UUID.randomUUID().toString(),
                createdAt = System.currentTimeMillis()
            )

            financialDao.executeInvestmentCreation(
                investment = investment,
                originTx = ledgerTx,
                document = document
            )

            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "INVESTMENT_CREATED",
                        authMethod = "LOCAL",
                        details = "{\"investmentId\":\"${investment.id}\",\"name\":\"${investment.name}\",\"type\":\"${investment.typeName}\",\"initial\":${investment.initialAmount},\"origin\":\"$originAccountName\"}"
                    )
                )
            } catch (e: Exception) {
                Log.w("FinancialRepository", "Non-critical audit log failure: ${e.message}")
            }

            Result.success(investment)
        } catch (ex: Exception) {
            Log.e("FinancialRepository", "Error creating investment: ${ex.message}", ex)
            Result.failure(ex)
        }
    }

    override suspend fun deleteInvestment(id: String): Result<Unit> {
        return try {
            financialDao.deleteInvestmentById(id)
            Result.success(Unit)
        } catch (ex: Exception) {
            Result.failure(ex)
        }
    }

    override fun getActiveCategories(type: String?): Flow<List<CategoryEntity>> {
        return when (type) {
            "EXPENSE" -> financialDao.getActiveExpenseCategoriesFlow()
            "INCOME" -> financialDao.getActiveIncomeCategoriesFlow()
            else -> financialDao.getActiveCategoriesFlow()
        }
    }

    override suspend fun setCategoryActive(id: String, isActive: Boolean): Result<Unit> {
        return try {
            financialDao.setCategoryActive(id, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getCategoryById(id: String): CategoryEntity? {
        return financialDao.getCategoryById(id)
    }

    override fun getMerchantsFlow(userId: String): Flow<List<com.example.data.local.entity.MerchantEntity>> {
        return merchantDao?.getAllMerchantsFlow(userId) ?: kotlinx.coroutines.flow.flowOf(emptyList())
    }

    override suspend fun createMerchant(
        canonicalName: String,
        alias: String,
        defaultCategoryId: String?,
        confidence: Int,
        userId: String?
    ): com.example.data.local.entity.MerchantEntity {
        val merchant = com.example.data.local.entity.MerchantEntity(
            id = "merch_${UUID.randomUUID().toString().take(8)}",
            canonicalName = canonicalName,
            alias = alias,
            defaultCategoryId = defaultCategoryId,
            classificationConfidence = confidence,
            userId = userId
        )
        merchantDao?.insertMerchant(merchant)
        return merchant
    }

    override suspend fun setMerchantActive(id: String, isActive: Boolean): Result<Unit> {
        return try {
            merchantDao?.setMerchantActive(id, isActive)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun observePendingOutboxCount(): Flow<Int> {
        return outboxDao?.observePendingCount() ?: kotlinx.coroutines.flow.flowOf(0)
    }

    override suspend fun getPendingOutboxOperations(): List<com.example.data.local.entity.OutboxOperationEntity> {
        return outboxDao?.getPendingOperations() ?: emptyList()
    }

    override suspend fun processOutboxQueue(): Result<Int> = withContext(ioDispatcher) {
        try {
            val dao = outboxDao ?: return@withContext Result.success(0)
            val pending = dao.getPendingOperations()
            if (pending.isEmpty()) return@withContext Result.success(0)

            var processed = 0
            for (op in pending) {
                val syncResult = firebaseDataSource.syncOutboxOperation(op)
                if (syncResult.isSuccess) {
                    dao.updateStatus(
                        id = op.id,
                        status = "DELIVERED",
                        retryCount = op.retryCount,
                        nextRetryAt = op.nextRetryAt,
                        error = null
                    )
                    processed++
                } else {
                    val nextRetry = System.currentTimeMillis() + (1000L * (1 shl (op.retryCount.coerceAtMost(6))))
                    dao.updateStatus(
                        id = op.id,
                        status = if (op.retryCount >= 5) "FAILED" else "PENDING",
                        retryCount = op.retryCount + 1,
                        nextRetryAt = nextRetry,
                        error = syncResult.exceptionOrNull()?.message ?: "Fallo de red al sincronizar con Firebase"
                    )
                }
            }
            Result.success(processed)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error processing outbox queue: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun getTags(): Flow<List<com.example.data.local.entity.TagEntity>> {
        return financialDao.getTagsFlow()
    }

    override suspend fun insertTag(name: String, colorHex: String): com.example.data.local.entity.TagEntity {
        val existing = financialDao.findTagByName(name.trim())
        if (existing != null) {
            val updated = existing.copy(usageCount = existing.usageCount + 1)
            financialDao.insertTag(updated)
            return updated
        }
        val newTag = com.example.data.local.entity.TagEntity(
            id = "tag_${UUID.randomUUID()}",
            name = name.trim().removePrefix("#"),
            colorHex = colorHex,
            usageCount = 1,
            createdAt = System.currentTimeMillis()
        )
        financialDao.insertTag(newTag)
        return newTag
    }

    // ==========================================
    // SCHEDULED PAYMENTS IMPLEMENTATION
    // ==========================================
    override fun getScheduledPayments(userId: String): Flow<List<com.example.data.local.entity.ScheduledPaymentEntity>> {
        return financialDao.getScheduledPaymentsFlow(userId)
    }

    override suspend fun createScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit> {
        return try {
            financialDao.insertScheduledPayment(payment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateScheduledPayment(payment: com.example.data.local.entity.ScheduledPaymentEntity): Result<Unit> {
        return try {
            financialDao.updateScheduledPayment(payment)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteScheduledPayment(id: String): Result<Unit> {
        return try {
            financialDao.deleteScheduledPaymentById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun setScheduledPaymentPaid(id: String, isPaid: Boolean, paidDate: String?): Result<Unit> {
        return try {
            financialDao.setScheduledPaymentPaid(id, isPaid, paidDate)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun migrateLegacyUserDefaultDataIfNeeded(targetUserId: String): Result<Int> = withContext(ioDispatcher) {
        runCatching {
            if (targetUserId.isBlank() || targetUserId == "user_default") return@runCatching 0

            val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
            val targetAccounts = financialDao.getAccountsFlow(targetUserId).firstOrNull().orEmpty()
            val hasCustomOrFundedAccounts = targetAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
            if (hasCustomOrFundedAccounts) {
                return@runCatching 0
            }

            var totalMigrated = 0
            val legacyDefaultAccounts = financialDao.getAccountsFlow("user_default").firstOrNull().orEmpty()
            if (legacyDefaultAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }) {
                totalMigrated += financialDao.migrateAccountsUserDefault(targetUserId)
                totalMigrated += financialDao.migrateTransactionsUserDefault(targetUserId)
                totalMigrated += financialDao.migrateGoalsUserDefault(targetUserId)
                totalMigrated += financialDao.migrateBudgetsUserDefault(targetUserId)
                totalMigrated += financialDao.migrateScheduledPaymentsUserDefault(targetUserId)
                totalMigrated += financialDao.migrateIncomeSourcesUserDefault(targetUserId)
                Log.i("FinancialRepositoryImpl", "✓ Se recuperaron y migraron $totalMigrated registros de 'user_default' a '$targetUserId'")
            } else if (targetUserId != "kash_user_01") {
                val legacyKash01Accounts = financialDao.getAccountsFlow("kash_user_01").firstOrNull().orEmpty()
                if (legacyKash01Accounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }) {
                    totalMigrated += financialDao.migrateAccountsFromUserId("kash_user_01", targetUserId)
                    totalMigrated += financialDao.migrateTransactionsFromUserId("kash_user_01", targetUserId)
                    totalMigrated += financialDao.migrateGoalsFromUserId("kash_user_01", targetUserId)
                    totalMigrated += financialDao.migrateBudgetsFromUserId("kash_user_01", targetUserId)
                    totalMigrated += financialDao.migrateScheduledPaymentsFromUserId("kash_user_01", targetUserId)
                    totalMigrated += financialDao.migrateIncomeSourcesFromUserId("kash_user_01", targetUserId)
                    Log.i("FinancialRepositoryImpl", "✓ Se recuperaron y migraron $totalMigrated registros de 'kash_user_01' a '$targetUserId'")
                }
            }

            totalMigrated
        }
    }

    override suspend fun restoreFullPortfolioFromCloud(userId: String): Result<Boolean> = withContext(ioDispatcher) {
        restoreFinancialBackupInternal(userId = userId, force = true)
    }

    override suspend fun restoreFinancialBackupFromCloudIfNeeded(userId: String): Result<Boolean> = withContext(ioDispatcher) {
        restoreFinancialBackupInternal(userId = userId, force = false)
    }

    override suspend fun getCloudBackupMetadata(userId: String): Map<String, Any>? = withContext(ioDispatcher) {
        try {
            firebaseDataSource.getFinancialBackup(userId)
        } catch (e: Exception) {
            Log.w("FinancialRepository", "Error consultando metadatos de respaldo en la nube: ${e.message}")
            null
        }
    }

    private suspend fun restoreFinancialBackupInternal(userId: String, force: Boolean): Result<Boolean> {
        return try {
            val fbEmail = firebaseDataSource.getCurrentUserEmail()
            val emailUid = fbEmail?.let { com.example.core.id.EmailDeterministicUidGenerator.fromEmail(it) }
            val currentFbUid = firebaseDataSource.getCurrentUserId()
            val targetId = emailUid ?: currentFbUid ?: userId
            val activeUserId = if (targetId.isNotBlank() && targetId != "user_default" && targetId != "kash_user_01") targetId else userId

            if (!force) {
                val localAccounts = financialDao.getAccountsFlow(activeUserId).firstOrNull().orEmpty()
                val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                val hasCustomOrFundedAccounts = localAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
                if (hasCustomOrFundedAccounts) {
                    Log.i("FinancialRepository", "Omitiendo restauración desde la nube: el usuario $activeUserId ya posee ${localAccounts.size} cuenta(s) en la base de datos local.")
                    return Result.success(false)
                }
            }

            val backupData = firebaseDataSource.getFinancialBackup(targetId)
                ?: if (userId != targetId) firebaseDataSource.getFinancialBackup(userId) else null

            if (backupData == null) {
                Log.i("FinancialRepository", "No se encontró ningún documento de respaldo en Firestore para $targetId ni $userId.")
                return Result.success(false)
            }

            Log.i("FinancialRepository", "Iniciando restauración de datos financieros desde Firestore para usuario $activeUserId")

            fun parseDoubleAmount(v: Any?): Double {
                if (v == null) return 0.0
                if (v is Number) return v.toDouble()
                if (v is String) {
                    val clean = v.replace("$", "").trim()
                    clean.toDoubleOrNull()?.let { return it }
                    if (clean.contains(".") && clean.contains(",")) {
                        val lastDot = clean.lastIndexOf('.')
                        val lastComma = clean.lastIndexOf(',')
                        val normalized = if (lastComma > lastDot) {
                            clean.replace(".", "").replace(",", ".")
                        } else {
                            clean.replace(",", "")
                        }
                        normalized.toDoubleOrNull()?.let { return it }
                    }
                    return clean.replace(",", ".").toDoubleOrNull() ?: 0.0
                }
                return 0.0
            }

            var restoredAny = false
            val accountsToInsert = mutableListOf<AccountEntity>()
            val transactionsToInsert = mutableListOf<TransactionEntity>()
            var isLegacyBackup = false

            var decryptedContent: String? = null
            if (backupData.containsKey("encryptedPayload")) {
                val encryptedPayload = backupData["encryptedPayload"] as? String
                if (!encryptedPayload.isNullOrBlank()) {
                    val candidateSeeds = listOfNotNull(
                        emailUid,
                        targetId,
                        currentFbUid,
                        userId,
                        fbEmail
                    ).distinct()

                    decryptedContent = cryptoManager.decryptCloudPayloadWithFallbacks(encryptedPayload, candidateSeeds)
                    if (decryptedContent.isNullOrBlank()) {
                        decryptedContent = cryptoManager.decryptData(encryptedPayload)
                    }
                }
            }

            if (!decryptedContent.isNullOrBlank()) {
                val jsonString = if (decryptedContent.trim().startsWith("{")) {
                    decryptedContent
                } else {
                    decompressGzip(decryptedContent)
                }

                val rootObj = org.json.JSONObject(jsonString)
                val accountsJsonArr = rootObj.optJSONArray("accounts")
                if (accountsJsonArr != null) {
                    for (i in 0 until accountsJsonArr.length()) {
                        val obj = accountsJsonArr.getJSONObject(i)
                        val id = obj.optString("id", "acc_" + UUID.randomUUID().toString().take(8))
                        val name = obj.optString("name", "Cuenta")
                        val balance = obj.optDouble("balance", 0.0)
                        val currency = obj.optString("currency", "COP")
                        val type = obj.optString("type", "SAVINGS")
                        val bankName = obj.optString("bankName", "Bancolombia")
                        val isFavorite = obj.optBoolean("isFavorite", true)
                        val masked = obj.optString("accountNumberMasked", "")
                        val enc = obj.optString("accountNumberEncrypted", "")

                        val availableBalance = if (obj.has("availableBalance")) {
                            obj.optDouble("availableBalance", balance)
                        } else if (type == "CREDIT_CARD") {
                            (obj.optDouble("overdraftLimit", 5_000_000.0) - Math.abs(balance)).coerceAtLeast(0.0)
                        } else {
                            balance
                        }
                        val hasOverdraft = obj.optBoolean("hasOverdraft", type == "CREDIT_CARD")
                        val overdraftLimit = obj.optDouble("overdraftLimit", if (type == "CREDIT_CARD") 5_000_000.0 else 0.0)

                        accountsToInsert.add(
                            AccountEntity(
                                id = id,
                                userId = activeUserId,
                                name = name,
                                balance = balance,
                                availableBalance = availableBalance,
                                hasOverdraft = hasOverdraft,
                                overdraftLimit = overdraftLimit,
                                currency = currency,
                                type = type,
                                bankName = bankName,
                                isFavorite = isFavorite,
                                accountNumberMasked = masked.ifBlank { null },
                                accountNumberEncrypted = enc.ifBlank { null },
                                customColorHex = obj.optString("customColorHex", if (type == "CREDIT_CARD") "#820AD1" else "#0284C7")
                            )
                        )
                    }
                }

                val txJsonArr = rootObj.optJSONArray("transactions")
                if (txJsonArr != null) {
                    for (i in 0 until txJsonArr.length()) {
                        val obj = txJsonArr.getJSONObject(i)
                        val txId = obj.optString("id", UUID.randomUUID().toString())
                        val accId = obj.optString("accountId", "")
                        val accName = obj.optString("accountName", "Cuenta")
                        val amount = obj.optDouble("amount", 0.0)
                        val currency = obj.optString("currency", "COP")
                        val type = obj.optString("type", "EXPENSE")
                        val date = obj.optString("date", java.time.LocalDate.now().toString())
                        val desc = obj.optString("description", "")
                        val catName = obj.optString("categoryName", "Varios")
                        val catId = obj.optString("categoryId", "cat_otros")

                        transactionsToInsert.add(
                            TransactionEntity(
                                id = txId,
                                userId = activeUserId,
                                accountId = accId,
                                accountName = accName,
                                categoryId = catId,
                                categoryName = catName,
                                amount = amount,
                                currency = currency,
                                type = type,
                                date = date,
                                description = desc.ifBlank { null },
                                receiptUrl = null,
                                idempotencyKey = "cloud_" + txId
                            )
                        )
                    }
                }

                val fixedAssetsArr = rootObj.optJSONArray("fixedAssets")
                if (fixedAssetsArr != null && fixedAssetDao != null) {
                    val assetsToInsert = mutableListOf<com.example.data.local.entity.FixedAssetEntity>()
                    for (i in 0 until fixedAssetsArr.length()) {
                        val obj = fixedAssetsArr.getJSONObject(i)
                        assetsToInsert.add(
                            com.example.data.local.entity.FixedAssetEntity(
                                id = obj.optString("id"),
                                userId = activeUserId,
                                name = obj.optString("name", "Activo"),
                                type = obj.optString("type", "TANGIBLE"),
                                category = obj.optString("category", "Otros Activos Fijos"),
                                description = obj.optString("description").ifBlank { null },
                                acquisitionDate = obj.optString("acquisitionDate", "2026-10-16"),
                                acquisitionValue = obj.optDouble("acquisitionValue", 0.0),
                                currency = obj.optString("currency", "COP"),
                                hasDepreciation = obj.optBoolean("hasDepreciation", true),
                                depreciationMethod = obj.optString("depreciationMethod", "STRAIGHT_LINE"),
                                usefulLifeMonths = obj.optInt("usefulLifeMonths", 60),
                                residualValue = obj.optDouble("residualValue", 0.0),
                                currentMarketValue = obj.optDouble("currentMarketValue", obj.optDouble("acquisitionValue", 0.0)),
                                accumulatedDepreciation = obj.optDouble("accumulatedDepreciation", 0.0),
                                brand = obj.optString("brand").ifBlank { null },
                                model = obj.optString("model").ifBlank { null },
                                serialNumber = obj.optString("serialNumber").ifBlank { null },
                                licensePlate = obj.optString("licensePlate").ifBlank { null },
                                supplier = obj.optString("supplier").ifBlank { null },
                                location = obj.optString("location").ifBlank { null },
                                warrantyExpiryDate = obj.optString("warrantyExpiryDate").ifBlank { null },
                                notes = obj.optString("notes").ifBlank { null },
                                status = obj.optString("status", "ACTIVE"),
                                colorHex = obj.optString("colorHex", "#0284C7"),
                                includeInNetWorth = obj.optBoolean("includeInNetWorth", true),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                                updatedAt = obj.optLong("updatedAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (assetsToInsert.isNotEmpty()) {
                        fixedAssetDao.insertAssets(assetsToInsert)
                        restoredAny = true
                    }
                }

                val movementsArr = rootObj.optJSONArray("assetMovements")
                if (movementsArr != null && fixedAssetDao != null) {
                    val movementsToInsert = mutableListOf<com.example.data.local.entity.AssetMovementEntity>()
                    for (i in 0 until movementsArr.length()) {
                        val obj = movementsArr.getJSONObject(i)
                        movementsToInsert.add(
                            com.example.data.local.entity.AssetMovementEntity(
                                id = obj.optString("id"),
                                assetId = obj.optString("assetId"),
                                userId = activeUserId,
                                type = obj.optString("type", "ACQUISITION"),
                                title = obj.optString("title", "Movimiento"),
                                date = obj.optString("date", "2026-10-16"),
                                amount = obj.optDouble("amount", 0.0),
                                notes = obj.optString("notes").ifBlank { null },
                                usefulLifeIncrementMonths = obj.optInt("usefulLifeIncrementMonths", 0),
                                newResidualValue = obj.optDouble("newResidualValue", 0.0).takeIf { it > 0.0 },
                                financialImpact = obj.optString("financialImpact").ifBlank { null },
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (movementsToInsert.isNotEmpty()) {
                        fixedAssetDao.insertMovements(movementsToInsert)
                        restoredAny = true
                    }
                }

                val cardsJsonArr = rootObj.optJSONArray("creditCards")
                if (cardsJsonArr != null) {
                    for (i in 0 until cardsJsonArr.length()) {
                        val obj = cardsJsonArr.getJSONObject(i)
                        val cardId = obj.optString("id", "cc_" + UUID.randomUUID().toString().take(8))
                        val accId = obj.optString("accountId", "")
                        val quotaTotal = obj.optDouble("quotaTotal", 5_000_000.0)
                        val quotaUsed = obj.optDouble("quotaUsed", 1_906_930.0)
                        val quotaAvailable = obj.optDouble("quotaAvailable", quotaTotal - quotaUsed)
                        val cardEntity = com.example.data.local.entity.AccountCreditCardEntity(
                            id = cardId,
                            accountId = accId,
                            franchise = obj.optString("franchise", "MASTERCARD"),
                            category = obj.optString("category", "GOLD"),
                            quotaTotal = quotaTotal,
                            quotaAvailable = quotaAvailable,
                            quotaUsed = quotaUsed,
                            cutOffDay = obj.optInt("cutOffDay", 18),
                            paymentDueDay = obj.optInt("paymentDueDay", 5),
                            interestRateEa = obj.optDouble("interestRateEa", 24.50),
                            purchasesRateEa = obj.optDouble("purchasesRateEa", 24.50),
                            advancesRateEa = obj.optDouble("advancesRateEa", 28.00),
                            includeInNetWorth = obj.optBoolean("includeInNetWorth", true),
                            minimumPaymentAmount = obj.optDouble("minimumPaymentAmount", quotaUsed)
                        )
                        financialDao.insertAccountCreditCard(cardEntity)
                    }
                }

                val goalsArr = rootObj.optJSONArray("goals")
                if (goalsArr != null) {
                    val goalsToInsert = mutableListOf<com.example.data.local.entity.FinancialGoalEntity>()
                    for (i in 0 until goalsArr.length()) {
                        val obj = goalsArr.getJSONObject(i)
                        goalsToInsert.add(
                            com.example.data.local.entity.FinancialGoalEntity(
                                id = obj.optString("id", UUID.randomUUID().toString()),
                                userId = activeUserId,
                                name = obj.optString("name", "Meta"),
                                categoryId = obj.optString("categoryId").ifBlank { null },
                                categoryName = obj.optString("categoryName", "Educación"),
                                description = obj.optString("description").ifBlank { null },
                                colorHex = obj.optString("colorHex", "#8B5CF6"),
                                iconName = obj.optString("iconName", "school"),
                                targetAmount = obj.optDouble("targetAmount", 0.0),
                                savedAmount = obj.optDouble("savedAmount", 0.0),
                                hasDeadline = obj.optBoolean("hasDeadline", true),
                                startDate = obj.optString("startDate").ifBlank { null },
                                deadlineDate = obj.optString("deadlineDate").ifBlank { null },
                                targetAccountId = obj.optString("targetAccountId"),
                                targetAccountName = obj.optString("targetAccountName", "Cuenta"),
                                sourceAccountId = obj.optString("sourceAccountId").ifBlank { null },
                                sourceAccountName = obj.optString("sourceAccountName").ifBlank { null },
                                autoContribute = obj.optBoolean("autoContribute", false),
                                monthlyContribution = obj.optDouble("monthlyContribution", 0.0).takeIf { it > 0.0 },
                                contributionDay = obj.optInt("contributionDay", 15),
                                contributionFrequency = obj.optString("contributionFrequency", "Mensual"),
                                contributionMethod = obj.optString("contributionMethod", "Transferencia"),
                                virtualPocketEnabled = obj.optBoolean("virtualPocketEnabled", false),
                                isActive = obj.optBoolean("isActive", true),
                                createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (goalsToInsert.isNotEmpty()) {
                        financialDao.insertGoals(goalsToInsert)
                        restoredAny = true
                    }
                }

                val budgetsArr = rootObj.optJSONArray("budgets")
                if (budgetsArr != null) {
                    for (i in 0 until budgetsArr.length()) {
                        val bObj = budgetsArr.getJSONObject(i)
                        val budgetId = bObj.optString("id", UUID.randomUUID().toString())
                        val budget = com.example.data.local.entity.BudgetEntity(
                            id = budgetId,
                            userId = activeUserId,
                            name = bObj.optString("name", "Presupuesto"),
                            period = bObj.optString("period", "MONTHLY"),
                            startDate = bObj.optString("startDate", java.time.LocalDate.now().withDayOfMonth(1).toString()),
                            endDate = bObj.optString("endDate", java.time.LocalDate.now().plusMonths(1).withDayOfMonth(1).minusDays(1).toString()),
                            totalAmount = bObj.optDouble("totalAmount", 0.0),
                            description = bObj.optString("description").ifBlank { null },
                            colorHex = bObj.optString("colorHex", "#10B981"),
                            isActive = bObj.optBoolean("isActive", true),
                            createdAt = bObj.optLong("createdAt", System.currentTimeMillis())
                        )
                        val itemsArr = bObj.optJSONArray("items")
                        val itemsToInsert = mutableListOf<com.example.data.local.entity.BudgetItemEntity>()
                        if (itemsArr != null) {
                            for (j in 0 until itemsArr.length()) {
                                val itm = itemsArr.getJSONObject(j)
                                itemsToInsert.add(
                                    com.example.data.local.entity.BudgetItemEntity(
                                        id = itm.optString("id", UUID.randomUUID().toString()),
                                        budgetId = budgetId,
                                        categoryId = itm.optString("categoryId", "cat_otros"),
                                        categoryName = itm.optString("categoryName", "Varios"),
                                        categoryIcon = itm.optString("categoryIcon", "category"),
                                        categoryColorHex = itm.optString("categoryColorHex", "#10B981"),
                                        allocatedAmount = itm.optDouble("allocatedAmount", 0.0),
                                        spentAmount = itm.optDouble("spentAmount", 0.0)
                                    )
                                )
                            }
                        }
                        financialDao.insertBudget(budget)
                        if (itemsToInsert.isNotEmpty()) {
                            financialDao.insertBudgetItems(itemsToInsert)
                        }
                        restoredAny = true
                    }
                }

                val scheduledArr = rootObj.optJSONArray("scheduledPayments")
                if (scheduledArr != null) {
                    val paymentsToInsert = mutableListOf<com.example.data.local.entity.ScheduledPaymentEntity>()
                    for (i in 0 until scheduledArr.length()) {
                        val spObj = scheduledArr.getJSONObject(i)
                        paymentsToInsert.add(
                            com.example.data.local.entity.ScheduledPaymentEntity(
                                id = spObj.optString("id", UUID.randomUUID().toString()),
                                userId = activeUserId,
                                entityName = spObj.optString("entityName", "Pago Programado"),
                                category = spObj.optString("category", "SERVICIOS"),
                                amount = spObj.optDouble("amount", 0.0),
                                currency = spObj.optString("currency", "COP"),
                                dueDate = spObj.optString("dueDate", java.time.LocalDate.now().toString()),
                                recurrence = spObj.optString("recurrence", "MONTHLY"),
                                isIncome = spObj.optBoolean("isIncome", false),
                                isPaid = spObj.optBoolean("isPaid", false),
                                paidDate = spObj.optString("paidDate").ifBlank { null },
                                sourceAccountId = spObj.optString("sourceAccountId").ifBlank { null },
                                sourceAccountName = spObj.optString("sourceAccountName").ifBlank { null },
                                notes = spObj.optString("notes").ifBlank { null },
                                reminderDaysBefore = spObj.optInt("reminderDaysBefore", 3),
                                notificationEnabled = spObj.optBoolean("notificationEnabled", true),
                                createdAt = spObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (paymentsToInsert.isNotEmpty()) {
                        financialDao.insertScheduledPayments(paymentsToInsert)
                        restoredAny = true
                    }
                }

                val cdtsArr = rootObj.optJSONArray("cdts")
                if (cdtsArr != null) {
                    val cdtsToInsert = mutableListOf<com.example.data.local.entity.AccountCdtEntity>()
                    for (i in 0 until cdtsArr.length()) {
                        val cObj = cdtsArr.getJSONObject(i)
                        cdtsToInsert.add(
                            com.example.data.local.entity.AccountCdtEntity(
                                id = cObj.optString("id", UUID.randomUUID().toString()),
                                accountId = cObj.optString("accountId"),
                                certificateNumber = cObj.optString("certificateNumber").ifBlank { null },
                                interestRateEa = cObj.optDouble("interestRateEa", 10.5),
                                rateType = cObj.optString("rateType", "EA"),
                                payoutFrequency = cObj.optString("payoutFrequency", "AT_MATURITY"),
                                compounding = cObj.optBoolean("compounding", true),
                                issueDate = cObj.optString("issueDate", java.time.LocalDate.now().toString()),
                                termDays = cObj.optInt("termDays", 180),
                                maturityDate = cObj.optString("maturityDate", java.time.LocalDate.now().plusMonths(6).toString()),
                                actionAtMaturity = cObj.optString("actionAtMaturity", "ASK"),
                                destinationAccountId = cObj.optString("destinationAccountId").ifBlank { null },
                                sourceAccountId = cObj.optString("sourceAccountId").ifBlank { null },
                                allowsEarlyWithdrawal = cObj.optBoolean("allowsEarlyWithdrawal", false),
                                alertDaysBefore = cObj.optInt("alertDaysBefore", 15),
                                hasAlert = cObj.optBoolean("hasAlert", true),
                                initialAmount = cObj.optDouble("initialAmount", 0.0),
                                estimatedYield = cObj.optDouble("estimatedYield", 0.0),
                                createdAt = cObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                    if (cdtsToInsert.isNotEmpty()) {
                        financialDao.insertAccountCdts(cdtsToInsert)
                        restoredAny = true
                    }
                }

                val recsArr = rootObj.optJSONArray("reconciliations")
                if (recsArr != null && reconciliationDao != null) {
                    val recsToInsert = mutableListOf<com.example.data.local.entity.AccountReconciliationLogEntity>()
                    for (i in 0 until recsArr.length()) {
                        val rObj = recsArr.getJSONObject(i)
                        recsToInsert.add(
                            com.example.data.local.entity.AccountReconciliationLogEntity(
                                id = rObj.optString("id", UUID.randomUUID().toString()),
                                userId = activeUserId,
                                accountId = rObj.optString("accountId"),
                                accountName = rObj.optString("accountName", "Cuenta"),
                                previousSystemBalance = rObj.optDouble("previousSystemBalance", 0.0),
                                reconciledRealBalance = rObj.optDouble("reconciledRealBalance", 0.0),
                                discrepancyAmount = rObj.optDouble("discrepancyAmount", 0.0),
                                adjustmentTransactionId = rObj.optString("adjustmentTransactionId").ifBlank { null },
                                reconciliationDate = rObj.optString("reconciliationDate", java.time.LocalDate.now().toString()),
                                reconciliationTime = rObj.optString("reconciliationTime", "00:00:00"),
                                reconciledByUserId = rObj.optString("reconciledByUserId", activeUserId),
                                reconciledByUserName = rObj.optString("reconciledByUserName", "Usuario"),
                                notes = rObj.optString("notes").ifBlank { null },
                                status = rObj.optString("status", "BALANCED"),
                                deviceModel = rObj.optString("deviceModel", "Android"),
                                syncStatus = "SYNCED",
                                reconciliationTimestamp = rObj.optLong("reconciliationTimestamp", System.currentTimeMillis())
                            )
                        )
                    }
                    if (recsToInsert.isNotEmpty()) {
                        reconciliationDao.insertReconciliations(recsToInsert)
                        restoredAny = true
                    }
                }
            } else {
                Log.w("FinancialRepository", "Aviso: No fue posible descifrar encryptedPayload con claves de usuario. Intentando recuperar datos legados en el documento de respaldo...")
                isLegacyBackup = true
                // Formato 1 legado: Lista de cuentas en backupData["accounts"]
                val accountsListObj = backupData["accounts"] as? List<*>
                if (accountsListObj != null && accountsListObj.isNotEmpty()) {
                    for (item in accountsListObj) {
                        val map = item as? Map<*, *> ?: continue
                        val id = map["id"] as? String ?: ("acc_" + UUID.randomUUID().toString().take(8))
                        val name = map["name"] as? String ?: map["nombre"] as? String ?: "Cuenta"
                        val balance = parseDoubleAmount(map["balance"] ?: map["saldo"] ?: map["monto"])
                        val currency = map["currency"] as? String ?: map["moneda"] as? String ?: "COP"
                        val type = map["type"] as? String ?: map["tipo"] as? String ?: "SAVINGS"
                        val bankName: String = (map["bankName"] as? String) ?: (map["banco"] as? String) ?: "Bancolombia"
                        val isFavorite = map["isFavorite"] as? Boolean ?: true
                        val masked = map["accountNumberMasked"] as? String

                        accountsToInsert.add(
                            AccountEntity(
                                id = id,
                                userId = activeUserId,
                                name = name,
                                balance = balance,
                                currency = currency,
                                type = type,
                                bankName = bankName,
                                isFavorite = isFavorite,
                                accountNumberMasked = masked
                            )
                        )
                    }
                }

                // Formato 2 legado: Mapa de cuentas en backupData["accounts"]
                val accountsMapObj = backupData["accounts"] as? Map<*, *>
                if (accountsMapObj != null && accountsToInsert.isEmpty()) {
                    for ((k, item) in accountsMapObj) {
                        val map = item as? Map<*, *> ?: continue
                        val id = map["id"] as? String ?: (k as? String) ?: ("acc_" + UUID.randomUUID().toString().take(8))
                        val name = map["name"] as? String ?: map["nombre"] as? String ?: "Cuenta"
                        val balance = parseDoubleAmount(map["balance"] ?: map["saldo"] ?: map["monto"])
                        val currency = map["currency"] as? String ?: map["moneda"] as? String ?: "COP"
                        val type = map["type"] as? String ?: map["tipo"] as? String ?: "SAVINGS"
                        val bankName: String = (map["bankName"] as? String) ?: (map["banco"] as? String) ?: "Bancolombia"
                        val isFavorite = map["isFavorite"] as? Boolean ?: true
                        val masked = map["accountNumberMasked"] as? String

                        accountsToInsert.add(
                            AccountEntity(
                                id = id,
                                userId = activeUserId,
                                name = name,
                                balance = balance,
                                currency = currency,
                                type = type,
                                bankName = bankName,
                                isFavorite = isFavorite,
                                accountNumberMasked = masked
                            )
                        )
                    }
                }

                // Formato 3 legado: Documento individual directo
                if (accountsToInsert.isEmpty()) {
                    val directBalance = parseDoubleAmount(backupData["balance"] ?: backupData["saldo"] ?: backupData["monto"])
                    val hasAccountTraits = backupData.containsKey("balance") ||
                            backupData.containsKey("saldo") ||
                            backupData.containsKey("bankName") ||
                            backupData.containsKey("banco") ||
                            backupData["table"] == "accounts"

                    if (hasAccountTraits) {
                        val accId = backupData["id"] as? String ?: ("acc_cloud_" + activeUserId.take(6))
                        val accName = backupData["name"] as? String ?: backupData["nombre"] as? String ?: "Cuenta Principal"
                        val currency = backupData["currency"] as? String ?: backupData["moneda"] as? String ?: "COP"
                        val type = backupData["type"] as? String ?: backupData["tipo"] as? String ?: "SAVINGS"
                        val bankName: String = (backupData["bankName"] as? String) ?: (backupData["banco"] as? String) ?: "Bancolombia"
                        val isFav = backupData["isFavorite"] as? Boolean ?: true
                        val masked = backupData["accountNumberMasked"] as? String

                        accountsToInsert.add(
                            AccountEntity(
                                id = accId,
                                userId = activeUserId,
                                name = accName,
                                balance = directBalance,
                                currency = currency,
                                type = type,
                                bankName = bankName,
                                isFavorite = isFav,
                                accountNumberMasked = masked
                            )
                        )
                    }
                }
            }

            // Inserción de cuentas restauradas y depuración de dummies
            val validAccountsToInsert = accountsToInsert.filter { !deletedAccountIds.contains(it.id) }
            if (validAccountsToInsert.isNotEmpty()) {
                financialDao.insertAccounts(validAccountsToInsert)
                restoredAny = true
                Log.i("FinancialRepository", "✓ Se restauraron ${validAccountsToInsert.size} cuenta(s) desde Firebase Firestore.")

                for (acc in validAccountsToInsert) {
                    if (acc.type == "CREDIT_CARD" || acc.id == "acc_nu_tc") {
                        if (financialDao.getAccountCreditCard(acc.id) == null) {
                            val creditCardConfig = com.example.data.local.entity.AccountCreditCardEntity(
                                id = "cc_" + acc.id.removePrefix("acc_"),
                                accountId = acc.id,
                                franchise = "MASTERCARD",
                                category = "GOLD",
                                quotaTotal = if (acc.overdraftLimit > 0) acc.overdraftLimit else 5_000_000.0,
                                quotaAvailable = if (acc.availableBalance > 0) acc.availableBalance else 3_093_070.0,
                                quotaUsed = Math.abs(acc.balance),
                                cutOffDay = 18,
                                paymentDueDay = 5,
                                interestRateEa = 24.50,
                                purchasesRateEa = 24.50,
                                advancesRateEa = 28.00,
                                hasGracePeriod = true,
                                includeInNetWorth = true,
                                minimumPaymentAmount = Math.abs(acc.balance)
                            )
                            financialDao.insertAccountCreditCard(creditCardConfig)
                        }
                    }
                }

                val hasFundedAccount = validAccountsToInsert.any { it.balance > 0.0 }
                if (hasFundedAccount) {
                    val dummyIds = listOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                    for (dummyId in dummyIds) {
                        if (validAccountsToInsert.none { it.id == dummyId }) {
                            financialDao.deleteAccountById(dummyId)
                        }
                    }
                }

                // Inserción de transacciones (V2 o legado)
                if (transactionsToInsert.isNotEmpty()) {
                    for (tx in transactionsToInsert) {
                        financialDao.insertTransaction(tx)
                    }
                    restoredAny = true
                } else if (isLegacyBackup) {
                    val txListObj = backupData["transactions"] as? List<*>
                    var restoredTransactionsCount = 0
                    if (txListObj != null && txListObj.isNotEmpty()) {
                        for (item in txListObj) {
                            val map = item as? Map<*, *> ?: continue
                            val txId = map["id"] as? String ?: continue
                            val accId = map["accountId"] as? String ?: continue
                            val accName = map["accountName"] as? String ?: "Cuenta"
                            val catId = map["categoryId"] as? String ?: "cat_otros"
                            val catName = map["categoryName"] as? String ?: "Varios"
                            val amount = parseDoubleAmount(map["amount"] ?: map["monto"])
                            val currency = map["currency"] as? String ?: "COP"
                            val type = map["type"] as? String ?: "EXPENSE"
                            val date = map["date"] as? String ?: java.time.LocalDate.now().toString()
                            val desc = map["description"] as? String

                            val tx = TransactionEntity(
                                id = txId,
                                userId = activeUserId,
                                accountId = accId,
                                accountName = accName,
                                categoryId = catId,
                                categoryName = catName,
                                amount = amount,
                                currency = currency,
                                type = type,
                                date = date,
                                description = desc,
                                receiptUrl = null,
                                idempotencyKey = "cloud_" + txId
                            )
                            financialDao.insertTransaction(tx)
                            restoredTransactionsCount++
                        }
                        if (restoredTransactionsCount > 0) {
                            restoredAny = true
                        }
                    }

                    // Asegurar movimiento inicial para cuentas con saldo si no hay transacciones
                    if (restoredTransactionsCount == 0) {
                        val existingTxs = financialDao.getTransactionsFlow(activeUserId).firstOrNull().orEmpty()
                        if (existingTxs.isEmpty()) {
                            for (acc in accountsToInsert.filter { it.balance > 0.0 }) {
                                val initTx = TransactionEntity(
                                    id = "tx_cloud_init_" + acc.id.takeLast(6) + "_" + (System.currentTimeMillis() % 10000),
                                    userId = activeUserId,
                                    accountId = acc.id,
                                    accountName = acc.name,
                                    categoryId = "cat_inc_otros",
                                    categoryName = "Saldo Inicial",
                                    amount = acc.balance,
                                    currency = acc.currency,
                                    type = "INCOME",
                                    date = java.time.LocalDate.now().toString(),
                                    description = "Saldo inicial de ${acc.name}",
                                    receiptUrl = null,
                                    idempotencyKey = "init_" + acc.id + "_" + acc.balance.toLong()
                                )
                                financialDao.insertTransaction(initTx)
                                Log.i("FinancialRepository", "✓ Generado movimiento inicial para cuenta ${acc.name} con monto ${acc.balance}")
                            }
                        }
                    }
                }

                // Migración transparente si se restauró de un respaldo legado o descifrado con fallback
                syncScope.launch {
                    backupFullPortfolioToCloud(activeUserId)
                }
            }

            Result.success(restoredAny)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error restaurando respaldo de Firestore: " + e.message, e)
            Result.failure(e)
        }
    }

    override suspend fun backupFullPortfolioToCloud(userId: String): Result<Boolean> = withContext(ioDispatcher) {
        try {
            val currentUserEmail = firebaseDataSource.getCurrentUserEmail()
            val emailUid = currentUserEmail?.let { com.example.core.id.EmailDeterministicUidGenerator.fromEmail(it) }
            val currentFbUid = firebaseDataSource.getCurrentUserId()
            val effectiveSeed = emailUid ?: currentFbUid ?: userId

            val allAccounts = financialDao.getAccounts(userId)
            val allTransactions = financialDao.getTransactions(userId)
            val creditCards = financialDao.getAccountCreditCardsByUser(userId)
            val fixedAssets = fixedAssetDao?.getAssets(userId).orEmpty()
            val assetMovements = fixedAssetDao?.getAllMovements(userId).orEmpty()
            val goals = financialDao.getGoals(userId)
            val budgetsWithItems = financialDao.getAllBudgetsWithItems(userId)
            val scheduledPayments = financialDao.getScheduledPayments(userId)
            val cdts = financialDao.getAccountCdtsByUser(userId)
            val reconciliations = reconciliationDao?.getReconciliations(userId).orEmpty()

            val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
            val hasNonDummyData = allAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 } ||
                allTransactions.isNotEmpty() || fixedAssets.isNotEmpty() || goals.isNotEmpty() ||
                budgetsWithItems.isNotEmpty() || scheduledPayments.isNotEmpty() || cdts.isNotEmpty() ||
                reconciliations.isNotEmpty()

            val localTotalRecords = allAccounts.size + allTransactions.size + creditCards.size +
                fixedAssets.size + assetMovements.size + goals.size +
                budgetsWithItems.size + scheduledPayments.size + cdts.size + reconciliations.size

            if (!hasNonDummyData && deletedAccountIds.isEmpty()) {
                val existingRemoteBackup = firebaseDataSource.getFinancialBackup(effectiveSeed)
                if (existingRemoteBackup != null) {
                    val remoteCount = (existingRemoteBackup["recordCount"] as? Number)?.toInt() ?: 0
                    val remotePayload = existingRemoteBackup["encryptedPayload"] as? String
                    if (remoteCount > 0 || !remotePayload.isNullOrBlank()) {
                        Log.w("FinancialRepository", "ALERTA DE SEGURIDAD (Zero-Overwrite Guard): " +
                            "El dispositivo local no contiene registros reales ($localTotalRecords registros), pero en Firebase existe un respaldo con " +
                            "$remoteCount registros. Se cancela el respaldo para prevenir pérdida de datos.")
                        return@withContext Result.failure(IllegalStateException("Zero-overwrite guard: Cannot overwrite populated cloud backup with empty local database"))
                    }
                }
                Log.i("FinancialRepository", "Omitiendo respaldo a Firestore: las cuentas locales son únicamente las cuentas iniciales en $0 y sin datos adicionales.")
                return@withContext Result.success(true)
            }

            val categories = financialDao.getCategories()

            // Construir payload JSON completo sin truncar transacciones ni activos fijos
            val rootJson = org.json.JSONObject().apply {
                put("version", "9.3.5")
                put("userId", userId)
                put("timestamp", System.currentTimeMillis())

                val accountsArr = org.json.JSONArray()
                for (acc in allAccounts) {
                    accountsArr.put(org.json.JSONObject().apply {
                        put("id", acc.id)
                        put("name", acc.name)
                        put("balance", acc.balance)
                        put("availableBalance", acc.availableBalance)
                        put("currency", acc.currency)
                        put("type", acc.type)
                        put("bankName", acc.bankName)
                        put("isFavorite", acc.isFavorite)
                        put("accountNumberMasked", acc.accountNumberMasked ?: "")
                        put("accountNumberEncrypted", acc.accountNumberEncrypted ?: "")
                        put("hasOverdraft", acc.hasOverdraft)
                        put("overdraftLimit", acc.overdraftLimit)
                        put("customColorHex", acc.customColorHex)
                        put("userId", userId)
                    })
                }
                put("accounts", accountsArr)

                val transactionsArr = org.json.JSONArray()
                for (tx in allTransactions) {
                    transactionsArr.put(org.json.JSONObject().apply {
                        put("id", tx.id)
                        put("accountId", tx.accountId)
                        put("accountName", tx.accountName)
                        put("amount", tx.amount)
                        put("currency", tx.currency)
                        put("type", tx.type)
                        put("date", tx.date)
                        put("description", tx.description ?: "")
                        put("categoryName", tx.categoryName)
                        put("categoryId", tx.categoryId)
                        put("userId", userId)
                    })
                }
                put("transactions", transactionsArr)

                val categoriesArr = org.json.JSONArray()
                for (cat in categories) {
                    categoriesArr.put(org.json.JSONObject().apply {
                        put("id", cat.id)
                        put("name", cat.name)
                        put("icon", cat.icon)
                        put("colorHex", cat.colorHex)
                        put("type", cat.type)
                    })
                }
                put("categories", categoriesArr)

                val assetsArr = org.json.JSONArray()
                for (asset in fixedAssets) {
                    assetsArr.put(org.json.JSONObject().apply {
                        put("id", asset.id)
                        put("userId", userId)
                        put("name", asset.name)
                        put("type", asset.type)
                        put("category", asset.category)
                        put("description", asset.description ?: "")
                        put("acquisitionDate", asset.acquisitionDate)
                        put("acquisitionValue", asset.acquisitionValue)
                        put("currency", asset.currency)
                        put("hasDepreciation", asset.hasDepreciation)
                        put("depreciationMethod", asset.depreciationMethod)
                        put("usefulLifeMonths", asset.usefulLifeMonths)
                        put("residualValue", asset.residualValue)
                        put("currentMarketValue", asset.currentMarketValue)
                        put("accumulatedDepreciation", asset.accumulatedDepreciation)
                        put("status", asset.status)
                        put("colorHex", asset.colorHex)
                        put("brand", asset.brand ?: "")
                        put("model", asset.model ?: "")
                        put("serialNumber", asset.serialNumber ?: "")
                        put("licensePlate", asset.licensePlate ?: "")
                        put("supplier", asset.supplier ?: "")
                        put("location", asset.location ?: "")
                        put("warrantyExpiryDate", asset.warrantyExpiryDate ?: "")
                        put("notes", asset.notes ?: "")
                        put("includeInNetWorth", asset.includeInNetWorth)
                        put("createdAt", asset.createdAt)
                        put("updatedAt", asset.updatedAt)
                    })
                }
                put("fixedAssets", assetsArr)

                val movementsArr = org.json.JSONArray()
                for (mov in assetMovements) {
                    movementsArr.put(org.json.JSONObject().apply {
                        put("id", mov.id)
                        put("assetId", mov.assetId)
                        put("userId", userId)
                        put("type", mov.type)
                        put("title", mov.title)
                        put("date", mov.date)
                        put("amount", mov.amount)
                        put("notes", mov.notes ?: "")
                        put("usefulLifeIncrementMonths", mov.usefulLifeIncrementMonths)
                        put("newResidualValue", mov.newResidualValue ?: 0.0)
                        put("financialImpact", mov.financialImpact ?: "")
                        put("createdAt", mov.createdAt)
                    })
                }
                put("assetMovements", movementsArr)

                val cardsArr = org.json.JSONArray()
                for (card in creditCards) {
                    cardsArr.put(org.json.JSONObject().apply {
                        put("id", card.id)
                        put("accountId", card.accountId)
                        put("franchise", card.franchise)
                        put("category", card.category)
                        put("quotaTotal", card.quotaTotal)
                        put("quotaAvailable", card.quotaAvailable)
                        put("quotaUsed", card.quotaUsed)
                        put("cutOffDay", card.cutOffDay)
                        put("paymentDueDay", card.paymentDueDay)
                        put("interestRateEa", card.interestRateEa)
                        put("purchasesRateEa", card.purchasesRateEa)
                        put("advancesRateEa", card.advancesRateEa)
                        put("includeInNetWorth", card.includeInNetWorth)
                        put("minimumPaymentAmount", card.minimumPaymentAmount)
                    })
                }
                put("creditCards", cardsArr)

                val goalsArr = org.json.JSONArray()
                for (g in goals) {
                    goalsArr.put(org.json.JSONObject().apply {
                        put("id", g.id)
                        put("userId", userId)
                        put("name", g.name)
                        put("categoryId", g.categoryId ?: "")
                        put("categoryName", g.categoryName)
                        put("description", g.description ?: "")
                        put("colorHex", g.colorHex)
                        put("iconName", g.iconName)
                        put("targetAmount", g.targetAmount)
                        put("savedAmount", g.savedAmount)
                        put("hasDeadline", g.hasDeadline)
                        put("startDate", g.startDate ?: "")
                        put("deadlineDate", g.deadlineDate ?: "")
                        put("targetAccountId", g.targetAccountId)
                        put("targetAccountName", g.targetAccountName)
                        put("sourceAccountId", g.sourceAccountId ?: "")
                        put("sourceAccountName", g.sourceAccountName ?: "")
                        put("autoContribute", g.autoContribute)
                        put("monthlyContribution", g.monthlyContribution ?: 0.0)
                        put("contributionDay", g.contributionDay ?: 15)
                        put("contributionFrequency", g.contributionFrequency)
                        put("contributionMethod", g.contributionMethod)
                        put("virtualPocketEnabled", g.virtualPocketEnabled)
                        put("isActive", g.isActive)
                        put("createdAt", g.createdAt)
                    })
                }
                put("goals", goalsArr)

                val budgetsArr = org.json.JSONArray()
                for (bwi in budgetsWithItems) {
                    val b = bwi.budget
                    val bObj = org.json.JSONObject().apply {
                        put("id", b.id)
                        put("userId", userId)
                        put("name", b.name)
                        put("period", b.period)
                        put("startDate", b.startDate)
                        put("endDate", b.endDate)
                        put("totalAmount", b.totalAmount)
                        put("description", b.description ?: "")
                        put("colorHex", b.colorHex)
                        put("isActive", b.isActive)
                        put("createdAt", b.createdAt)
                        val itemsArr = org.json.JSONArray()
                        for (item in bwi.items) {
                            itemsArr.put(org.json.JSONObject().apply {
                                put("id", item.id)
                                put("budgetId", item.budgetId)
                                put("categoryId", item.categoryId)
                                put("categoryName", item.categoryName)
                                put("categoryIcon", item.categoryIcon)
                                put("categoryColorHex", item.categoryColorHex)
                                put("allocatedAmount", item.allocatedAmount)
                                put("spentAmount", item.spentAmount)
                            })
                        }
                        put("items", itemsArr)
                    }
                    budgetsArr.put(bObj)
                }
                put("budgets", budgetsArr)

                val scheduledArr = org.json.JSONArray()
                for (sp in scheduledPayments) {
                    scheduledArr.put(org.json.JSONObject().apply {
                        put("id", sp.id)
                        put("userId", userId)
                        put("entityName", sp.entityName)
                        put("category", sp.category)
                        put("amount", sp.amount)
                        put("currency", sp.currency)
                        put("dueDate", sp.dueDate)
                        put("recurrence", sp.recurrence)
                        put("isIncome", sp.isIncome)
                        put("isPaid", sp.isPaid)
                        put("paidDate", sp.paidDate ?: "")
                        put("sourceAccountId", sp.sourceAccountId ?: "")
                        put("sourceAccountName", sp.sourceAccountName ?: "")
                        put("notes", sp.notes ?: "")
                        put("reminderDaysBefore", sp.reminderDaysBefore)
                        put("notificationEnabled", sp.notificationEnabled)
                        put("createdAt", sp.createdAt)
                    })
                }
                put("scheduledPayments", scheduledArr)

                val cdtsArr = org.json.JSONArray()
                for (cdt in cdts) {
                    cdtsArr.put(org.json.JSONObject().apply {
                        put("id", cdt.id)
                        put("accountId", cdt.accountId)
                        put("certificateNumber", cdt.certificateNumber ?: "")
                        put("interestRateEa", cdt.interestRateEa)
                        put("rateType", cdt.rateType)
                        put("payoutFrequency", cdt.payoutFrequency)
                        put("compounding", cdt.compounding)
                        put("issueDate", cdt.issueDate)
                        put("termDays", cdt.termDays)
                        put("maturityDate", cdt.maturityDate)
                        put("actionAtMaturity", cdt.actionAtMaturity)
                        put("destinationAccountId", cdt.destinationAccountId ?: "")
                        put("sourceAccountId", cdt.sourceAccountId ?: "")
                        put("allowsEarlyWithdrawal", cdt.allowsEarlyWithdrawal)
                        put("alertDaysBefore", cdt.alertDaysBefore)
                        put("hasAlert", cdt.hasAlert)
                        put("initialAmount", cdt.initialAmount)
                        put("estimatedYield", cdt.estimatedYield)
                        put("createdAt", cdt.createdAt)
                    })
                }
                put("cdts", cdtsArr)

                val recsArr = org.json.JSONArray()
                for (rec in reconciliations) {
                    recsArr.put(org.json.JSONObject().apply {
                        put("id", rec.id)
                        put("userId", userId)
                        put("accountId", rec.accountId)
                        put("accountName", rec.accountName)
                        put("previousSystemBalance", rec.previousSystemBalance)
                        put("reconciledRealBalance", rec.reconciledRealBalance)
                        put("discrepancyAmount", rec.discrepancyAmount)
                        put("adjustmentTransactionId", rec.adjustmentTransactionId ?: "")
                        put("reconciliationDate", rec.reconciliationDate)
                        put("reconciliationTime", rec.reconciliationTime)
                        put("reconciledByUserId", rec.reconciledByUserId)
                        put("reconciledByUserName", rec.reconciledByUserName)
                        put("notes", rec.notes ?: "")
                        put("status", rec.status)
                        put("deviceModel", rec.deviceModel)
                        put("syncStatus", "SYNCED")
                        put("reconciliationTimestamp", rec.reconciliationTimestamp)
                    })
                }
                put("reconciliations", recsArr)
            }

            val rawJson = rootJson.toString()
            val compressedBase64 = compressGzip(rawJson)
            val encryptedPayload = cryptoManager.encryptCloudPayload(compressedBase64, effectiveSeed)

            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(rawJson.toByteArray(Charsets.UTF_8))
            val checksumSha256 = digest.joinToString("") { "%02x".format(it) }

            // Sobre cifrado estricto E2EE según firestore.rules con clave derivada determinística
            val totalRecordCount = localTotalRecords
            val envelope = hashMapOf<String, Any>(
                "schemaVersion" to 2,
                "userId" to effectiveSeed,
                "encryptedPayload" to encryptedPayload,
                "recordCount" to totalRecordCount,
                "checksumSha256" to checksumSha256,
                "updatedAt" to System.currentTimeMillis()
            )

            val success = firebaseDataSource.syncOfflineFinancialBackup(effectiveSeed, envelope)
            if (success) {
                try {
                    auditLogDao.insertLog(
                        AuditSecurityLog(
                            timestamp = System.currentTimeMillis(),
                            eventType = "CLOUD_BACKUP_EXECUTED",
                            authMethod = "OAUTH_FIREBASE",
                            details = "Portafolio respaldado en Firestore con cifrado E2EE (${allAccounts.size} cuentas, ${allTransactions.size} txs, checksum $checksumSha256)"
                        )
                    )
                } catch (auditEx: Exception) {
                    Log.w("FinancialRepository", "No fue posible registrar log de auditoría para CLOUD_BACKUP_EXECUTED: ${auditEx.message}")
                }
            }
            Log.i("FinancialRepository", "✓ Portafolio respaldado en Firestore con cifrado E2EE (${allAccounts.size} cuentas, ${allTransactions.size} txs): $success")
            Result.success(success)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error respaldando portafolio completo E2EE a Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }

    override fun clearMemoryCaches() {
        deletedAccountIds.clear()
        Log.i("FinancialRepository", "Memoria de cuentas eliminadas limpiada exitosamente.")
    }

    override suspend fun reverseAndDeleteTransaction(
        transactionId: String,
        userId: String
    ): com.example.domain.repository.DeleteTransactionResult = withContext(ioDispatcher) {
        try {
            val targetTx = financialDao.getTransactionByIdAndUser(transactionId, userId)
                ?: return@withContext com.example.domain.repository.DeleteTransactionResult.NotFoundOrUnauthorized(
                    "El movimiento no existe o no pertenece al usuario autenticado."
                )

            // Business rule checks (CDT / Investment funding protection)
            if (targetTx.id.startsWith("tx_cdt_out_") || targetTx.id.startsWith("tx_inv_")) {
                val cdts = financialDao.getAllAccountCdts()
                val matchingCdt = cdts.find { it.accountId == targetTx.accountId }
                if (matchingCdt != null) {
                    return@withContext com.example.domain.repository.DeleteTransactionResult.BlockedByBusinessRule(
                        "No se puede eliminar el movimiento de apertura de un CDT activo. Cancela o liquida el CDT desde el módulo de Inversiones."
                    )
                }
            }

            // Execute atomic Room reversal
            val deletedIds = financialDao.executeAtomicTransactionReversal(transactionId, userId)
            if (deletedIds.isEmpty()) {
                return@withContext com.example.domain.repository.DeleteTransactionResult.NotFoundOrUnauthorized(
                    "El movimiento ya no existe o fue eliminado concurrentemente."
                )
            }

            // Outbox operations for offline-first synchronization
            deletedIds.forEach { deletedId ->
                try {
                    outboxDao?.insertOperation(
                        com.example.data.local.entity.OutboxOperationEntity(
                            id = "out_${UUID.randomUUID()}",
                            userId = userId,
                            entityType = "TRANSACTION",
                            entityId = deletedId,
                            operation = "DELETE",
                            payloadJson = "{}",
                            idempotencyKey = "out_del_${deletedId}_${System.currentTimeMillis()}",
                            createdAt = System.currentTimeMillis()
                        )
                    )
                } catch (e: Exception) {
                    Log.w("FinancialRepository", "Error al encolar Outbox DELETE para $deletedId: ${e.message}")
                }
            }

            try {
                auditLogDao.insertLog(
                    AuditSecurityLog(
                        eventType = "TRANSACTION_REVERSED_AND_DELETED",
                        authMethod = "SESSION",
                        details = "Movimiento $transactionId eliminado con reversión atómica para usuario $userId (eliminados: ${deletedIds.joinToString()})"
                    )
                )
            } catch (_: Exception) {}

            com.example.domain.repository.DeleteTransactionResult.Success(
                deletedTransactionIds = deletedIds,
                affectedAccountIds = listOf(targetTx.accountId)
            )
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error revirtiendo transacción $transactionId: ${e.message}", e)
            com.example.domain.repository.DeleteTransactionResult.Error(e)
        }
    }
}

/**
 * Exception thrown when a referential integrity violation occurs (e.g. 409 Conflict)
 */
class ReferentialIntegrityException(message: String) : Exception(message)
