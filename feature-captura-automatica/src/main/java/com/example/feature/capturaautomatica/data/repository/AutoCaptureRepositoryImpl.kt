package com.example.feature.capturaautomatica.data.repository

import com.example.data.local.dao.AuditSecurityLogDao
import com.example.data.local.dao.AutoCaptureDao
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.MerchantDao
import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.AuditSecurityLog
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.MerchantEntity
import com.example.data.local.entity.PendingAutoCaptureEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.capturaautomatica.domain.model.SupportedColombianBank
import com.example.feature.capturaautomatica.domain.parser.ColombianBankNotificationParser
import com.example.feature.capturaautomatica.domain.repository.AutoCaptureRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

class AutoCaptureRepositoryImpl(
    private val autoCaptureDao: AutoCaptureDao,
    private val financialDao: FinancialDao? = null,
    private val merchantDao: MerchantDao? = null,
    private val auditSecurityLogDao: AuditSecurityLogDao? = null,
    private val parser: ColombianBankNotificationParser = ColombianBankNotificationParser(),
    private val accountProvider: (suspend (userId: String) -> List<AccountEntity>)? = null,
    private val categoryProvider: (suspend () -> List<CategoryEntity>)? = null,
    private val recordExpense: (suspend (
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        amount: Double,
        dateFormatted: String,
        description: String?,
        idempotencyKey: String
    ) -> Result<String>)? = null,
    private val recordIncome: (suspend (
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        amount: Double,
        dateFormatted: String,
        description: String?,
        idempotencyKey: String
    ) -> Result<String>)? = null,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : AutoCaptureRepository {

    override fun observePendingCaptures(userId: String): Flow<List<PendingAutoCaptureEntity>> {
        return autoCaptureDao.observePendingCaptures(userId)
    }

    override fun observePendingCount(userId: String): Flow<Int> {
        return autoCaptureDao.observePendingCount(userId)
    }

    override suspend fun getCaptureById(id: String, userId: String): PendingAutoCaptureEntity? {
        return withContext(ioDispatcher) {
            autoCaptureDao.getCaptureById(id, userId)
        }
    }

    override suspend fun processNotification(
        packageName: String,
        title: String?,
        text: String?,
        postTime: Long,
        userId: String
    ): Result<PendingAutoCaptureEntity?> = withContext(ioDispatcher) {
        runCatching {
            val parsed = parser.parse(packageName, title, text, postTime) ?: return@runCatching null

            // Deduplicación por idempotencyKey
            if (autoCaptureDao.existsByIdempotencyKey(parsed.idempotencyKey)) {
                return@runCatching null
            }

            // 1. Resolver cuenta en base de datos
            val userAccounts = fetchAccounts(userId)
            val matchedAccount = resolveAccount(userAccounts, parsed.lastFourDigits, parsed.detectedBank, parsed.transactionType)

            // 2. Resolver categoría vía MerchantDao o heurística local
            val userCategories = fetchCategories()
            val (matchedCategoryId, matchedCategoryName) = resolveCategory(userId, parsed.merchantOrCounterparty, parsed.transactionType, userCategories)

            val captureEntity = PendingAutoCaptureEntity(
                id = "cap_${UUID.randomUUID().toString().take(12)}",
                userId = userId,
                sourcePackage = parsed.sourcePackage,
                detectedBankName = parsed.detectedBank.displayName,
                transactionType = parsed.transactionType,
                amount = parsed.amount,
                currency = parsed.currency,
                merchantOrCounterparty = parsed.merchantOrCounterparty,
                lastFourDigits = parsed.lastFourDigits,
                matchedAccountId = matchedAccount?.id,
                matchedAccountName = matchedAccount?.name,
                matchedCategoryId = matchedCategoryId,
                matchedCategoryName = matchedCategoryName,
                confidenceScore = parsed.confidenceScore,
                status = "PENDING",
                linkedTransactionId = null,
                idempotencyKey = parsed.idempotencyKey,
                notificationTimestamp = parsed.notificationTimestamp,
                createdAt = System.currentTimeMillis()
            )

            val rowId = autoCaptureDao.insertPendingCapture(captureEntity)
            if (rowId > 0L) {
                captureEntity
            } else {
                null
            }
        }
    }

    override suspend fun confirmCapture(
        captureId: String,
        userId: String,
        customAccountId: String?,
        customAccountName: String?,
        customCategoryId: String?,
        customCategoryName: String?
    ): Result<String> = withContext(ioDispatcher) {
        runCatching {
            val capture = autoCaptureDao.getCaptureById(captureId, userId)
                ?: throw IllegalArgumentException("No se encontró la captura con ID $captureId")

            if (capture.status != "PENDING") {
                throw IllegalStateException("La captura ya fue procesada anteriormente (${capture.status})")
            }

            val userAccounts = fetchAccounts(userId)
            val resolvedAccount = if (!customAccountId.isNullOrBlank()) {
                userAccounts.find { it.id == customAccountId }
                    ?: throw IllegalArgumentException("Cuenta seleccionada no válida")
            } else {
                userAccounts.find { it.id == capture.matchedAccountId }
                    ?: userAccounts.firstOrNull()
                    ?: throw IllegalStateException("El usuario no tiene cuentas disponibles para registrar el movimiento")
            }

            val userCategories = fetchCategories()
            val resolvedCategory = if (!customCategoryId.isNullOrBlank()) {
                userCategories.find { it.id == customCategoryId }
            } else {
                userCategories.find { it.id == capture.matchedCategoryId } ?: userCategories.firstOrNull()
            }

            val finalCategoryId = resolvedCategory?.id ?: "cat_default"
            val finalCategoryName = customCategoryName?.takeIf { it.isNotBlank() }
                ?: resolvedCategory?.name
                ?: capture.matchedCategoryName
                ?: "Varios"

            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.ROOT).format(Date(capture.notificationTimestamp))
            val description = "Captura automática: ${capture.merchantOrCounterparty} (${capture.detectedBankName})"

            val txId: String = if (capture.transactionType == "INCOME") {
                val incomeResult = recordIncome?.invoke(
                    userId,
                    resolvedAccount.id,
                    resolvedAccount.name,
                    finalCategoryId,
                    finalCategoryName,
                    capture.amount,
                    dateStr,
                    description,
                    capture.idempotencyKey
                )
                incomeResult?.getOrThrow() ?: fallbackInsertTransaction(
                    userId = userId,
                    account = resolvedAccount,
                    categoryId = finalCategoryId,
                    categoryName = finalCategoryName,
                    amount = capture.amount,
                    type = "INCOME",
                    dateStr = dateStr,
                    description = description,
                    idempotencyKey = capture.idempotencyKey
                )
            } else {
                val expenseResult = recordExpense?.invoke(
                    userId,
                    resolvedAccount.id,
                    resolvedAccount.name,
                    finalCategoryId,
                    finalCategoryName,
                    capture.amount,
                    dateStr,
                    description,
                    capture.idempotencyKey
                )
                expenseResult?.getOrThrow() ?: fallbackInsertTransaction(
                    userId = userId,
                    account = resolvedAccount,
                    categoryId = finalCategoryId,
                    categoryName = finalCategoryName,
                    amount = capture.amount,
                    type = "EXPENSE",
                    dateStr = dateStr,
                    description = description,
                    idempotencyKey = capture.idempotencyKey
                )
            }

            // Aprender el comercio para futuras compras si no existía en MerchantDao
            learnMerchantIfNew(userId, capture.merchantOrCounterparty, finalCategoryId, finalCategoryName)

            // Marcar la captura como confirmada
            autoCaptureDao.updateCaptureStatus(captureId, userId, "CONFIRMED", txId)

            auditSecurityLogDao?.insertLog(
                AuditSecurityLog(
                    eventType = "AUTO_CAPTURE_CONFIRMED",
                    authMethod = "BANK_NOTIFICATION",
                    details = "Confirmado ${capture.transactionType} por $${capture.amount.toLong()} en ${capture.merchantOrCounterparty} (Tx: $txId)"
                )
            )

            txId
        }
    }

    override suspend fun dismissCapture(captureId: String, userId: String): Result<Unit> = withContext(ioDispatcher) {
        runCatching {
            val capture = autoCaptureDao.getCaptureById(captureId, userId)
                ?: throw IllegalArgumentException("No se encontró la captura con ID $captureId")

            autoCaptureDao.updateCaptureStatus(captureId, userId, "DISMISSED", null)

            auditSecurityLogDao?.insertLog(
                AuditSecurityLog(
                    eventType = "AUTO_CAPTURE_DISMISSED",
                    authMethod = "BANK_NOTIFICATION",
                    details = "Descartado ${capture.transactionType} por $${capture.amount.toLong()} de ${capture.detectedBankName}"
                )
            )
            Unit
        }
    }

    override suspend fun simulateTestCapture(
        userId: String,
        bankName: String
    ): Result<PendingAutoCaptureEntity> = withContext(ioDispatcher) {
        val now = System.currentTimeMillis()
        val (pkg, title, text) = when (bankName.lowercase(Locale.ROOT)) {
            "nequi" -> Triple("com.nequi.MobileApp", "Nequi", "Pagaste $22.000,00 en CREPES Y WAFFLES con tu Nequi")
            "nequi bre-b", "bre-b", "breb" -> Triple("com.nequi.MobileApp", "Te enviaron plata por Bre-B", "Te enviaron $5. Entra a tu app y revisa tu saldo.")
            "nu", "nu colombia" -> Triple("com.nu.production", "Nu", "Compra aprobada por $ 42.000,00 en DIDI RIDES con tu tarjeta ...9104")
            "daviplata" -> Triple("com.davivienda.daviplataapp", "DaviPlata", "Compra por $45.000 en ALMACENES EXITO")
            "davivienda" -> Triple("com.davivienda.daviviendaapp", "Davivienda", "Davivienda informa compra por $68.000 en RESTAURANTE con tarjeta terminada en 1234")
            "banco de bogota", "bogota" -> Triple("com.bancodebogota.bancamovil", "Banco de Bogotá", "Banco de Bogotá le informa Compra por $89.000 en HOME CENTER")
            "falabella", "banco falabella" -> Triple("cl.falabella.bancafalabella.co", "Banco Falabella", "Compra aprobada por $120.000 en FALABELLA con tu tarjeta CMR")
            "dale", "dale!" -> Triple("co.com.dale.app", "Dale!", "Te enviaron $25.000 por Bre-B de ANDRES")
            "rappipay" -> Triple("co.com.rappipay.app", "RappiPay", "Pagaste $35.000 en RAPPI con tu RappiCard")
            else -> Triple("com.todo1.mobile", "Bancolombia", "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO 14:22 ${SimpleDateFormat("dd/MM/yyyy", Locale.ROOT).format(Date(now))} T.Deb *4821")
        }

        val res = processNotification(pkg, title, text, now, userId)
        val entity = res.getOrNull()
        if (entity != null) {
            Result.success(entity)
        } else {
            Result.failure(res.exceptionOrNull() ?: IllegalStateException("No fue posible simular la captura bancaria"))
        }
    }

    private fun resolveAccount(
        userAccounts: List<AccountEntity>,
        lastFourDigits: String?,
        bank: SupportedColombianBank,
        txType: String
    ): AccountEntity? {
        if (userAccounts.isEmpty()) return null

        // 1. Coincidencia por últimos 4 dígitos
        if (!lastFourDigits.isNullOrBlank()) {
            val matchDigits = userAccounts.find {
                it.accountNumberMasked?.endsWith(lastFourDigits) == true ||
                it.name.contains(lastFourDigits)
            }
            if (matchDigits != null) return matchDigits
        }

        // 2. Coincidencia por nombre de banco
        val bankAccounts = userAccounts.filter {
            it.bankName.equals(bank.bankNameInKashApp, ignoreCase = true) ||
            it.name.contains(bank.displayName, ignoreCase = true)
        }

        if (bankAccounts.isNotEmpty()) {
            val favorite = if (txType == "INCOME") {
                bankAccounts.find { it.favoriteForIncomes }
            } else {
                bankAccounts.find { it.favoriteForExpenses }
            }
            return favorite ?: bankAccounts.first()
        }

        // 3. Fallback a favorita general o primera cuenta
        val generalFavorite = if (txType == "INCOME") {
            userAccounts.find { it.favoriteForIncomes }
        } else {
            userAccounts.find { it.favoriteForExpenses }
        }
        return generalFavorite ?: userAccounts.firstOrNull()
    }

    private suspend fun resolveCategory(
        userId: String,
        merchantName: String,
        txType: String,
        userCategories: List<CategoryEntity>
    ): Pair<String?, String?> {
        // 1. Buscar en MerchantDao si el comercio ya fue clasificado
        val normalized = merchantName.trim()
        val existingMerchant = merchantDao?.findMerchantByNameOrAlias(normalized)
        if (existingMerchant != null && !existingMerchant.defaultCategoryId.isNullOrBlank()) {
            return existingMerchant.defaultCategoryId to existingMerchant.defaultCategoryName
        }

        // 2. Diccionario semántico colombiano
        val inferredName = inferCategoryConcept(normalized, txType)
        val matchedCategory = userCategories.find {
            it.name.contains(inferredName, ignoreCase = true) ||
            inferredName.contains(it.name, ignoreCase = true)
        }

        if (matchedCategory != null) {
            return matchedCategory.id to matchedCategory.name
        }

        val fallback = userCategories.firstOrNull()
        return fallback?.id to (fallback?.name ?: inferredName)
    }

    private fun inferCategoryConcept(merchant: String, txType: String): String {
        if (txType == "INCOME") return "Ingresos"
        val upper = merchant.uppercase(Locale.ROOT)
        return when {
            upper.contains("D1") || upper.contains("ARA") || upper.contains("EXITO") ||
            upper.contains("CARULLA") || upper.contains("JUMBO") || upper.contains("OLIMPICA") ||
            upper.contains("SUPERINTER") || upper.contains("ALKOSTO") || upper.contains("SURTIMAX") -> "Mercado"

            upper.contains("UBER") || upper.contains("DIDI") || upper.contains("CABIFY") ||
            upper.contains("TERPEL") || upper.contains("PRIMAX") || upper.contains("BIOMAX") ||
            upper.contains("FLYPASS") || upper.contains("PEAJE") -> "Transporte"

            upper.contains("RAPPI") || upper.contains("CREPES") || upper.contains("JUAN VALDEZ") ||
            upper.contains("FRISBY") || upper.contains("CORRAL") || upper.contains("MCDONALD") ||
            upper.contains("SUBWAY") || upper.contains("RESTAURANTE") || upper.contains("CAFE") ||
            upper.contains("PANADERIA") || upper.contains("PIZZA") -> "Restaurantes"

            upper.contains("CLARO") || upper.contains("MOVISTAR") || upper.contains("TIGO") ||
            upper.contains("ENEL") || upper.contains("EPM") || upper.contains("VANTI") ||
            upper.contains("ACUEDUCTO") || upper.contains("ETB") -> "Servicios"

            upper.contains("DROGUERIA") || upper.contains("CRUZ VERDE") || upper.contains("FARMACIA") ||
            upper.contains("REBAJA") || upper.contains("PASTEUR") -> "Salud"

            upper.contains("CINE") || upper.contains("SPOTIFY") || upper.contains("NETFLIX") -> "Entretenimiento"

            else -> "Alimentación"
        }
    }

    private suspend fun learnMerchantIfNew(
        userId: String,
        merchantName: String,
        categoryId: String,
        categoryName: String
    ) {
        val normalized = merchantName.trim()
        if (normalized.length < 3) return
        val existing = merchantDao?.findMerchantByNameOrAlias(normalized)
        if (existing == null) {
            merchantDao?.insertMerchant(
                MerchantEntity(
                    id = "merch_${UUID.randomUUID().toString().take(12)}",
                    userId = userId,
                    canonicalName = normalized,
                    alias = normalized.lowercase(Locale.ROOT),
                    defaultCategoryId = categoryId,
                    defaultCategoryName = categoryName,
                    classificationConfidence = 95,
                    isActive = true
                )
            )
        }
    }

    private suspend fun fetchAccounts(userId: String): List<AccountEntity> {
        return accountProvider?.invoke(userId) ?: financialDao?.getAccounts(userId) ?: emptyList()
    }

    private suspend fun fetchCategories(): List<CategoryEntity> {
        return categoryProvider?.invoke() ?: financialDao?.getCategories() ?: emptyList()
    }

    private suspend fun fallbackInsertTransaction(
        userId: String,
        account: AccountEntity,
        categoryId: String,
        categoryName: String,
        amount: Double,
        type: String,
        dateStr: String,
        description: String,
        idempotencyKey: String
    ): String {
        val dao = financialDao ?: throw IllegalStateException("FinancialDao o recordExpense/recordIncome deben estar configurados.")
        val txId = "tx_${UUID.randomUUID().toString().take(12)}"
        val tx = TransactionEntity(
            id = txId,
            userId = userId,
            accountId = account.id,
            accountName = account.name,
            categoryId = categoryId,
            categoryName = categoryName,
            amount = amount,
            currency = account.currency,
            type = type,
            date = dateStr,
            description = description,
            receiptUrl = null,
            idempotencyKey = idempotencyKey,
            syncStatus = "PENDING"
        )
        dao.insertTransaction(tx)

        if (type == "INCOME") {
            dao.addAccountBalance(account.id, amount)
        } else {
            dao.deductAccountBalance(account.id, amount)
        }

        return txId
    }
}
