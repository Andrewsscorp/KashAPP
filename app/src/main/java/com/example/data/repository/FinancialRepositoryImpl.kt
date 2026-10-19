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

            // 6. Background Asynchronous Sync to Firebase Firestore (Offline-first)
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

            // 6. Background Asynchronous Sync to Cloud Backend (Offline-first)
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

            // 6. Asynchronous Background Sync
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
            if (manualExchangeRate <= 0.0) {
                return@withContext Result.failure(IllegalArgumentException("La tasa de cambio manual (TRM) debe ser superior a cero."))
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

    override suspend fun processOutboxQueue(): Result<Int> {
        return try {
            val pending = outboxDao?.getPendingOperations() ?: emptyList()
            var processed = 0
            for (op in pending) {
                outboxDao?.updateStatus(
                    id = op.id,
                    status = "DELIVERED",
                    retryCount = op.retryCount,
                    nextRetryAt = op.nextRetryAt,
                    error = null
                )
                processed++
            }
            Result.success(processed)
        } catch (e: Exception) {
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
            if (!force) {
                val localAccounts = financialDao.getAccountsFlow(userId).firstOrNull().orEmpty()
                val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
                val hasCustomOrFundedAccounts = localAccounts.any { !dummyIds.contains(it.id) || it.balance > 0.0 }
                if (hasCustomOrFundedAccounts) {
                    Log.i("FinancialRepository", "Omitiendo restauración desde la nube: el usuario $userId ya posee ${localAccounts.size} cuenta(s) en la base de datos local.")
                    return Result.success(false)
                }
            }

            val backupData = firebaseDataSource.getFinancialBackup(userId)
            if (backupData == null) {
                return Result.success(false)
            }

            Log.i("FinancialRepository", "Iniciando restauración de datos financieros desde Firestore para usuario $userId")

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

            if (backupData.containsKey("encryptedPayload")) {
                val encryptedPayload = backupData["encryptedPayload"] as? String
                if (encryptedPayload.isNullOrBlank()) {
                    Log.w("FinancialRepository", "Respaldo cifrado tiene payload vacío.")
                    return Result.success(false)
                }

                val decryptedContent = cryptoManager.decryptData(encryptedPayload)
                if (decryptedContent.isNullOrBlank()) {
                    Log.e("FinancialRepository", "Fallo al descifrar el payload del respaldo en la nube.")
                    return Result.failure(IllegalStateException("No fue posible descifrar el respaldo de Firestore."))
                }

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

                        accountsToInsert.add(
                            AccountEntity(
                                id = id,
                                userId = userId,
                                name = name,
                                balance = balance,
                                currency = currency,
                                type = type,
                                bankName = bankName,
                                isFavorite = isFavorite,
                                accountNumberMasked = masked.ifBlank { null },
                                accountNumberEncrypted = enc.ifBlank { null }
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
                                userId = userId,
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
            } else {
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
                                userId = userId,
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
                                userId = userId,
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
                        val accId = backupData["id"] as? String ?: ("acc_cloud_" + userId.take(6))
                        val accName = backupData["name"] as? String ?: backupData["nombre"] as? String ?: "Cuenta Principal"
                        val currency = backupData["currency"] as? String ?: backupData["moneda"] as? String ?: "COP"
                        val type = backupData["type"] as? String ?: backupData["tipo"] as? String ?: "SAVINGS"
                        val bankName: String = (backupData["bankName"] as? String) ?: (backupData["banco"] as? String) ?: "Bancolombia"
                        val isFav = backupData["isFavorite"] as? Boolean ?: true
                        val masked = backupData["accountNumberMasked"] as? String

                        accountsToInsert.add(
                            AccountEntity(
                                id = accId,
                                userId = userId,
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
                                userId = userId,
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
                        val existingTxs = financialDao.getTransactionsFlow(userId).firstOrNull().orEmpty()
                        if (existingTxs.isEmpty()) {
                            for (acc in accountsToInsert.filter { it.balance > 0.0 }) {
                                val initTx = TransactionEntity(
                                    id = "tx_cloud_init_" + acc.id.takeLast(6) + "_" + (System.currentTimeMillis() % 10000),
                                    userId = userId,
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

                // Migración transparente si se restauró de un respaldo legado
                if (isLegacyBackup) {
                    syncScope.launch {
                        backupFullPortfolioToCloud(userId)
                    }
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
            val allAccounts = financialDao.getAccounts(userId)
            val allTransactions = financialDao.getTransactions(userId)

            val dummyIds = setOf("acc_ahorros", "acc_corriente", "acc_billetera", "acc_efectivo")
            val isAllEmptyDummy = (allAccounts.isEmpty() || allAccounts.all { it.balance == 0.0 && dummyIds.contains(it.id) }) && allTransactions.isEmpty()
            if (isAllEmptyDummy && deletedAccountIds.isEmpty()) {
                Log.i("FinancialRepository", "Omitiendo respaldo a Firestore: las cuentas locales son únicamente las cuentas iniciales en $0.")
                return@withContext Result.success(true)
            }

            val categories = financialDao.getCategories()

            // Construir payload JSON completo sin truncar transacciones
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
            }

            val rawJson = rootJson.toString()
            val compressedBase64 = compressGzip(rawJson)
            val encryptedPayload = cryptoManager.encryptData(compressedBase64)

            val digest = java.security.MessageDigest.getInstance("SHA-256").digest(rawJson.toByteArray(Charsets.UTF_8))
            val checksumSha256 = digest.joinToString("") { "%02x".format(it) }

            // Sobre cifrado estricto E2EE según firestore.rules
            val envelope = hashMapOf<String, Any>(
                "schemaVersion" to 2,
                "userId" to userId,
                "encryptedPayload" to encryptedPayload,
                "recordCount" to (allAccounts.size + allTransactions.size),
                "checksumSha256" to checksumSha256,
                "updatedAt" to System.currentTimeMillis()
            )

            val success = firebaseDataSource.syncOfflineFinancialBackup(userId, envelope)
            Log.i("FinancialRepository", "✓ Portafolio respaldado en Firestore con cifrado E2EE (${allAccounts.size} cuentas, ${allTransactions.size} txs): $success")
            Result.success(success)
        } catch (e: Exception) {
            Log.e("FinancialRepository", "Error respaldando portafolio completo E2EE a Firestore: ${e.message}", e)
            Result.failure(e)
        }
    }
}

/**
 * Exception thrown when a referential integrity violation occurs (e.g. 409 Conflict)
 */
class ReferentialIntegrityException(message: String) : Exception(message)
