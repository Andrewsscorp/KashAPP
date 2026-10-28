package com.example.data.backup

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.CategoryEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.TransactionEntity
import java.time.LocalDate
import java.util.UUID

/**
 * Resultado auditable del proceso de sanitización de un lote de respaldo antes de su inserción ACID en Room.
 */
data class SanitizedPortfolioBatch(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val transactions: List<TransactionEntity>,
    val fixedAssets: List<FixedAssetEntity>,
    val discardedOrphanTransactions: Int,
    val repairedFieldsCount: Int
)

/**
 * Cortafuegos de sanitización defensiva para entidades provenientes de respaldos en la nube o archivos externos.
 * Previene inyección de nulos, NaN/Infinity, fechas corruptas y transacciones huérfanas.
 */
object BackupEntitySanitizer {

    private val ISO_DATE_REGEX = Regex("^\\d{4}-\\d{2}-\\d{2}$")
    private val HEX_COLOR_REGEX = Regex("^#[0-9A-Fa-f]{6,8}$")
    private val VALID_CURRENCIES = setOf("COP", "USD", "EUR", "MXN", "BRL", "GBP", "CLP", "PEN")
    private val VALID_TX_TYPES = setOf("EXPENSE", "INCOME", "TRANSFER", "GASTO", "INGRESO", "TRANSFERENCIA")

    fun sanitizeFiniteAmount(value: Double?, allowNegative: Boolean = false, defaultVal: Double = 0.0): Pair<Double, Boolean> {
        if (value == null || value.isNaN() || value.isInfinite()) {
            return defaultVal to true
        }
        if (!allowNegative && value < 0.0) {
            return 0.0 to true
        }
        return value to false
    }

    fun sanitizeIsoDate(rawDate: String?): Pair<String, Boolean> {
        val clean = rawDate?.trim().orEmpty()
        if (clean.matches(ISO_DATE_REGEX)) {
            return try {
                LocalDate.parse(clean)
                clean to false
            } catch (_: Exception) {
                LocalDate.now().toString() to true
            }
        }
        return LocalDate.now().toString() to true
    }

    fun sanitizeCurrency(rawCurrency: String?): String {
        val upper = rawCurrency?.trim()?.uppercase().orEmpty()
        return if (upper in VALID_CURRENCIES) upper else "COP"
    }

    fun sanitizeColorHex(rawColor: String?, fallback: String = "#0284C7"): String {
        val clean = rawColor?.trim().orEmpty()
        return if (clean.matches(HEX_COLOR_REGEX)) clean else fallback
    }

    fun sanitizeAccount(raw: AccountEntity, targetUserId: String): Pair<AccountEntity, Int> {
        var repairs = 0
        val safeId = raw.id.trim().ifBlank { repairs++; "acc_${UUID.randomUUID()}" }
        val safeName = raw.name.trim().ifBlank { repairs++; "Cuenta Recuperada" }.take(80)
        val (safeBalance, balRepaired) = sanitizeFiniteAmount(raw.balance, allowNegative = raw.hasOverdraft)
        if (balRepaired) repairs++
        val (safeAvailable, availRepaired) = sanitizeFiniteAmount(raw.availableBalance, allowNegative = raw.hasOverdraft, defaultVal = safeBalance)
        if (availRepaired) repairs++
        val (safeOverdraft, overRepaired) = sanitizeFiniteAmount(raw.overdraftLimit, allowNegative = false)
        if (overRepaired) repairs++

        return raw.copy(
            id = safeId,
            userId = targetUserId.ifBlank { "user_default" },
            name = safeName,
            balance = safeBalance,
            availableBalance = safeAvailable,
            overdraftLimit = safeOverdraft,
            currency = sanitizeCurrency(raw.currency),
            customColorHex = sanitizeColorHex(raw.customColorHex, fallback = "#0284C7")
        ) to repairs
    }

    fun sanitizeTransaction(
        raw: TransactionEntity,
        targetUserId: String,
        validAccountIds: Set<String>
    ): Pair<TransactionEntity?, Int> {
        // Rechazar transacciones huérfanas cuya cuenta padre no existe en el portafolio
        if (raw.accountId.isBlank() || raw.accountId !in validAccountIds) {
            return null to 1
        }

        var repairs = 0
        val (safeAmount, amountRepaired) = sanitizeFiniteAmount(raw.amount, allowNegative = false)
        if (amountRepaired || safeAmount <= 0.0) {
            return null to 1 // Descartar transacciones con monto <= 0 o NaN
        }

        val (safeDate, dateRepaired) = sanitizeIsoDate(raw.date)
        if (dateRepaired) repairs++

        val normalizedType = raw.type.trim().uppercase().let {
            if (it in VALID_TX_TYPES) it else { repairs++; "EXPENSE" }
        }

        val safeId = raw.id.trim().ifBlank { repairs++; "tx_${UUID.randomUUID()}" }
        val safeCategoryId = raw.categoryId.trim().ifBlank { repairs++; "cat_otros" }
        val safeCategoryName = raw.categoryName.trim().ifBlank { repairs++; "Otros gastos" }

        val sanitized = raw.copy(
            id = safeId,
            userId = targetUserId.ifBlank { "user_default" },
            amount = safeAmount,
            currency = sanitizeCurrency(raw.currency),
            type = normalizedType,
            date = safeDate,
            categoryId = safeCategoryId,
            categoryName = safeCategoryName,
            description = raw.description?.trim()?.take(250),
            idempotencyKey = raw.idempotencyKey.trim().ifBlank { "restored_$safeId" }
        )
        return sanitized to repairs
    }

    /**
     * Sanitiza un portafolio completo garantizando integridad referencial entre cuentas y transacciones.
     */
    fun sanitizePortfolioBatch(
        targetUserId: String,
        rawAccounts: List<AccountEntity>,
        rawCategories: List<CategoryEntity>,
        rawTransactions: List<TransactionEntity>,
        rawFixedAssets: List<FixedAssetEntity>
    ): SanitizedPortfolioBatch {
        var totalRepairs = 0
        var orphanCount = 0

        val cleanAccounts = rawAccounts.distinctBy { it.id }.map { acc ->
            val (sanitized, rep) = sanitizeAccount(acc, targetUserId)
            totalRepairs += rep
            sanitized
        }
        val validAccountIds = cleanAccounts.map { it.id }.toSet()

        val cleanCategories = rawCategories.distinctBy { it.id }.map { cat ->
            cat.copy(
                id = cat.id.trim().ifBlank { "cat_${UUID.randomUUID()}" },
                name = cat.name.trim().ifBlank { "Categoría" }.take(60),
                colorHex = sanitizeColorHex(cat.colorHex, "#64748B")
            )
        }

        val cleanTransactions = mutableListOf<TransactionEntity>()
        for (tx in rawTransactions.distinctBy { it.id }) {
            val (sanitizedTx, rep) = sanitizeTransaction(tx, targetUserId, validAccountIds)
            totalRepairs += rep
            if (sanitizedTx != null) {
                cleanTransactions.add(sanitizedTx)
            } else {
                orphanCount++
            }
        }

        return SanitizedPortfolioBatch(
            accounts = cleanAccounts,
            categories = cleanCategories,
            transactions = cleanTransactions,
            fixedAssets = rawFixedAssets,
            discardedOrphanTransactions = orphanCount,
            repairedFieldsCount = totalRepairs
        )
    }
}
