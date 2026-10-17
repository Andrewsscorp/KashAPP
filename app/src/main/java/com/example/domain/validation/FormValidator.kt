package com.example.domain.validation

import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Global Enterprise FormValidator utility.
 * Sanitizes and validates user input fields across all financial modules
 * (Expenses, Incomes, Transfers, Accounts, Credit Cards, CDTs, Investments, Goals, Budgets)
 * prior to database persistence and state emission.
 */
object FormValidator {

    sealed class ValidationResult {
        object Valid : ValidationResult()
        data class Invalid(val errors: List<String>) : ValidationResult() {
            val firstError: String get() = errors.firstOrNull() ?: "Error de validación"
        }

        val isValid: Boolean get() = this is Valid
        val isInvalid: Boolean get() = this is Invalid
    }

    // ==========================================
    // 1. SANITIZATION UTILITIES
    // ==========================================

    /**
     * Trims leading/trailing whitespace, strips unsafe ASCII control chars,
     * normalizes internal spacing, and truncates to maxLength.
     */
    fun sanitizeText(input: String?, maxLength: Int = 255): String {
        if (input.isNullOrBlank()) return ""
        val cleaned = input
            .replace(Regex("[\\x00-\\x1F\\x7F]"), "") // Remove control characters
            .replace(Regex("\\s+"), " ")              // Normalize multiple whitespaces
            .trim()
        return if (cleaned.length > maxLength) cleaned.take(maxLength).trim() else cleaned
    }

    /**
     * Sanitizes transaction concepts and entity names.
     */
    fun sanitizeConcept(input: String?, maxLength: Int = 100): String {
        return sanitizeText(input, maxLength)
    }

    /**
     * Sanitizes long descriptions and notes.
     */
    fun sanitizeDescription(input: String?, maxLength: Int = 500): String {
        return sanitizeText(input, maxLength)
    }

    /**
     * Cleans list of tags, deduplicating and limiting tag length and count.
     */
    fun sanitizeTags(tags: List<String>?, maxTags: Int = 10): List<String> {
        if (tags.isNullOrEmpty()) return emptyList()
        return tags
            .map { sanitizeText(it, 30).lowercase(Locale.ROOT) }
            .filter { it.isNotBlank() && it.matches(Regex("^[a-zA-Z0-9áéíóúÁÉÍÓÚñÑ_\\-]+$")) }
            .distinct()
            .take(maxTags)
    }

    /**
     * Ensures uppercase ISO 3-letter currency code (e.g. COP, USD, EUR).
     */
    fun sanitizeCurrency(currency: String?, defaultCurrency: String = "COP"): String {
        val clean = sanitizeText(currency, 3).uppercase(Locale.ROOT)
        return if (clean.length == 3 && clean.all { it.isLetter() }) clean else defaultCurrency
    }

    /**
     * Parses and cleans raw monetary strings with currency masks ($ . ,).
     */
    fun sanitizeAmount(rawAmount: String?): Double? {
        if (rawAmount.isNullOrBlank()) return null
        val cleaned = rawAmount
            .replace("$", "")
            .replace(" ", "")
            .replace("\u00A0", "") // non-breaking space
            .replace(".", "")      // remove thousands separators
            .replace(",", ".")     // convert decimal comma to dot
            .trim()
        return cleaned.toDoubleOrNull()
    }

    /**
     * Parses raw percentage rate strings (e.g. "12,5%", "10.25").
     */
    fun sanitizeRate(rawRate: String?): Double? {
        if (rawRate.isNullOrBlank()) return null
        val cleaned = rawRate
            .replace("%", "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()
        return cleaned.toDoubleOrNull()
    }

    /**
     * Validates and sanitizes Hex Color strings (#RRGGBB or #AARRGGBB).
     */
    fun sanitizeColorHex(colorHex: String?, defaultHex: String = "#0284C7"): String {
        if (colorHex.isNullOrBlank()) return defaultHex
        val trimmed = colorHex.trim()
        val withHash = if (trimmed.startsWith("#")) trimmed else "#$trimmed"
        return if (withHash.matches(Regex("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{8})$"))) {
            withHash.uppercase(Locale.ROOT)
        } else {
            defaultHex
        }
    }

    /**
     * Cleans bank account / card numbers, keeping only digits and hyphens.
     */
    fun sanitizeAccountNumber(raw: String?, maxLength: Int = 30): String {
        if (raw.isNullOrBlank()) return ""
        val digitsOnly = raw.replace(Regex("[^0-9\\-]"), "")
        return digitsOnly.take(maxLength)
    }

    // ==========================================
    // 2. FIELD-LEVEL VALIDATORS
    // ==========================================

    fun validateName(name: String?, minLength: Int = 2, maxLength: Int = 100, fieldName: String = "Nombre"): ValidationResult {
        val clean = sanitizeText(name, maxLength)
        val errors = mutableListOf<String>()
        if (clean.length < minLength) {
            errors.add("$fieldName debe tener al menos $minLength caracteres.")
        }
        if (clean.length > maxLength) {
            errors.add("$fieldName no puede superar los $maxLength caracteres.")
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateAmount(
        amount: Double?,
        min: Double = 0.01,
        max: Double = 100_000_000_000.0,
        fieldName: String = "Monto"
    ): ValidationResult {
        val errors = mutableListOf<String>()
        if (amount == null || amount.isNaN() || amount.isInfinite()) {
            errors.add("$fieldName debe ser un número válido.")
        } else if (amount < min) {
            errors.add("$fieldName debe ser mayor a cero (mínimo $min).")
        } else if (amount > max) {
            errors.add("$fieldName excede el límite permitido.")
        }
        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    fun validateAccountSelection(accountId: String?, fieldName: String = "Cuenta"): ValidationResult {
        val clean = sanitizeText(accountId)
        return if (clean.isNotBlank()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf("Debes seleccionar una $fieldName válida."))
        }
    }

    fun validateCategorySelection(categoryId: String?, fieldName: String = "Categoría"): ValidationResult {
        val clean = sanitizeText(categoryId)
        return if (clean.isNotBlank()) {
            ValidationResult.Valid
        } else {
            ValidationResult.Invalid(listOf("Debes seleccionar una $fieldName."))
        }
    }

    // ==========================================
    // 3. MODULE-SPECIFIC VALIDATORS
    // ==========================================

    /**
     * Validates Expense transaction input.
     */
    fun validateExpense(
        amount: Double,
        accountId: String,
        categoryId: String,
        description: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val amountRes = validateAmount(amount, fieldName = "Monto del gasto")
        if (amountRes is ValidationResult.Invalid) errors.addAll(amountRes.errors)

        val accRes = validateAccountSelection(accountId, fieldName = "cuenta de origen")
        if (accRes is ValidationResult.Invalid) errors.addAll(accRes.errors)

        val catRes = validateCategorySelection(categoryId, fieldName = "categoría de gasto")
        if (catRes is ValidationResult.Invalid) errors.addAll(catRes.errors)

        val cleanDesc = sanitizeDescription(description)
        if (cleanDesc.length > 500) {
            errors.add("La descripción no puede exceder 500 caracteres.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Income transaction input.
     */
    fun validateIncome(
        amount: Double,
        accountId: String,
        categoryId: String,
        sourceName: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val amountRes = validateAmount(amount, fieldName = "Monto del ingreso")
        if (amountRes is ValidationResult.Invalid) errors.addAll(amountRes.errors)

        val accRes = validateAccountSelection(accountId, fieldName = "cuenta receptora")
        if (accRes is ValidationResult.Invalid) errors.addAll(accRes.errors)

        val catRes = validateCategorySelection(categoryId, fieldName = "categoría de ingreso")
        if (catRes is ValidationResult.Invalid) errors.addAll(catRes.errors)

        val cleanSource = sanitizeConcept(sourceName)
        if (cleanSource.isBlank()) {
            errors.add("Debes ingresar el nombre de la fuente o emisor del ingreso.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Double-Entry Transfer between two accounts.
     */
    fun validateTransfer(
        sourceAccountId: String,
        destinationAccountId: String,
        amount: Double,
        concept: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val amountRes = validateAmount(amount, fieldName = "Monto a transferir")
        if (amountRes is ValidationResult.Invalid) errors.addAll(amountRes.errors)

        val srcClean = sanitizeText(sourceAccountId)
        val dstClean = sanitizeText(destinationAccountId)

        if (srcClean.isBlank()) {
            errors.add("Debes seleccionar la cuenta de origen.")
        }
        if (dstClean.isBlank()) {
            errors.add("Debes seleccionar la cuenta de destino.")
        }
        if (srcClean.isNotBlank() && dstClean.isNotBlank() && srcClean == dstClean) {
            errors.add("La cuenta de origen y destino no pueden ser la misma.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Account creation input.
     */
    fun validateAccount(
        name: String,
        type: String,
        initialBalance: Double,
        currency: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(name, minLength = 2, maxLength = 60, fieldName = "Nombre de la cuenta")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        val cleanType = sanitizeText(type).uppercase(Locale.ROOT)
        val validTypes = setOf("SAVINGS", "CHECKING", "CASH", "WALLET", "PAYROLL", "CDT", "CREDIT_CARD", "INVESTMENT", "ASSET")
        if (!validTypes.contains(cleanType)) {
            errors.add("Tipo de cuenta no soportado ($cleanType).")
        }

        if (initialBalance < 0.0 && cleanType != "CREDIT_CARD") {
            errors.add("El saldo inicial no puede ser negativo.")
        }

        val cleanCurrency = sanitizeCurrency(currency)
        if (cleanCurrency.length != 3) {
            errors.add("Moneda inválida.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Credit Card (Revolving Debt) setup.
     */
    fun validateCreditCard(
        cardName: String,
        quotaTotal: Double,
        cutOffDay: Int,
        paymentDueDay: Int,
        franchise: String
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(cardName, minLength = 2, maxLength = 60, fieldName = "Nombre de la tarjeta")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        if (quotaTotal <= 0.0) {
            errors.add("El cupo total de la tarjeta debe ser superior a cero.")
        }

        if (cutOffDay !in 1..31) {
            errors.add("El día de corte debe estar entre 1 y 31.")
        }

        if (paymentDueDay !in 1..31) {
            errors.add("El día límite de pago debe estar entre 1 y 31.")
        }

        val cleanFranchise = sanitizeText(franchise).uppercase(Locale.ROOT)
        if (cleanFranchise.isBlank()) {
            errors.add("Debes seleccionar la franquicia de la tarjeta.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Goal (Mental Accounting Pocket) setup.
     */
    fun validateGoal(
        name: String,
        targetAmount: Double,
        categoryName: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(name, minLength = 2, maxLength = 50, fieldName = "Nombre del objetivo")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        val targetRes = validateAmount(targetAmount, min = 1000.0, fieldName = "Meta de ahorro")
        if (targetRes is ValidationResult.Invalid) errors.addAll(targetRes.errors)

        if (sanitizeText(categoryName).isBlank()) {
            errors.add("Debes seleccionar una categoría para el objetivo.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Budget and Zero-Based Envelope allocation.
     */
    fun validateBudget(
        name: String,
        totalAmount: Double,
        envelopesTotal: Double
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(name, minLength = 3, maxLength = 50, fieldName = "Nombre del presupuesto")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        val totalRes = validateAmount(totalAmount, min = 1000.0, fieldName = "Techo presupuestal")
        if (totalRes is ValidationResult.Invalid) errors.addAll(totalRes.errors)

        if (envelopesTotal > totalAmount + 0.01) {
            val overage = envelopesTotal - totalAmount
            errors.add("El presupuesto está sobreasignado por $ %,.0f. Reduce los montos de los sobres.".format(Locale.GERMANY, overage))
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Investment asset creation.
     */
    fun validateInvestment(
        name: String,
        initialAmount: Double,
        originAccountId: String,
        typeName: String?
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(name, minLength = 2, maxLength = 100, fieldName = "Nombre de la inversión")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        val amountRes = validateAmount(initialAmount, min = 100.0, fieldName = "Monto inicial invertido")
        if (amountRes is ValidationResult.Invalid) errors.addAll(amountRes.errors)

        val accRes = validateAccountSelection(originAccountId, fieldName = "cuenta de origen para el fondeo")
        if (accRes is ValidationResult.Invalid) errors.addAll(accRes.errors)

        if (sanitizeText(typeName).isBlank()) {
            errors.add("Debes seleccionar el tipo de inversión.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates CDT Certificate creation.
     */
    fun validateCdt(
        name: String,
        bankName: String,
        initialAmount: Double,
        interestRateEa: Double,
        termDays: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val nameRes = validateName(name, minLength = 2, maxLength = 80, fieldName = "Nombre del CDT")
        if (nameRes is ValidationResult.Invalid) errors.addAll(nameRes.errors)

        if (sanitizeText(bankName).isBlank()) {
            errors.add("Debes seleccionar la entidad financiera emisora.")
        }

        val amountRes = validateAmount(initialAmount, min = 50000.0, fieldName = "Monto inicial del CDT")
        if (amountRes is ValidationResult.Invalid) errors.addAll(amountRes.errors)

        if (interestRateEa <= 0.0 || interestRateEa > 100.0) {
            errors.add("La tasa de interés (E.A.) debe estar entre 0.01% y 100%.")
        }

        if (termDays < 30) {
            errors.add("El plazo mínimo para un CDT es de 30 días.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Bank registration.
     */
    fun validateBank(name: String, code: String): ValidationResult {
        val errors = mutableListOf<String>()
        val cleanName = sanitizeConcept(name)
        val cleanCode = sanitizeAccountNumber(code)

        if (name.isBlank() || cleanName.isBlank()) {
            errors.add("El nombre del banco no puede estar vacío.")
        } else if (cleanName.length < 2) {
            errors.add("El nombre de la entidad debe tener al menos 2 caracteres.")
        }
        if (code.isBlank() || cleanCode.isBlank()) {
            errors.add("El código de compensación no puede estar vacío.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Validates Payroll configuration.
     */
    fun validatePayroll(
        employerName: String?,
        expectedIncome: Double,
        payDay: Int
    ): ValidationResult {
        val errors = mutableListOf<String>()
        val cleanEmployer = sanitizeConcept(employerName)
        if (cleanEmployer.isBlank()) {
            errors.add("Debes ingresar el nombre de la empresa o empleador.")
        }

        val incomeRes = validateAmount(expectedIncome, min = 100.0, fieldName = "Ingreso esperado de nómina")
        if (incomeRes is ValidationResult.Invalid) errors.addAll(incomeRes.errors)

        if (payDay !in 1..31) {
            errors.add("El día de pago debe estar entre 1 y 31.")
        }

        return if (errors.isEmpty()) ValidationResult.Valid else ValidationResult.Invalid(errors)
    }

    /**
     * Cleans phone numbers, allowing digits, spaces and leading plus sign.
     */
    fun sanitizePhone(phone: String?): String {
        if (phone.isNullOrBlank()) return ""
        val trimmed = phone.trim()
        val hasPlus = trimmed.startsWith("+")
        val digitsOnly = trimmed.replace(Regex("[^0-9]"), "")
        return if (hasPlus) "+$digitsOnly" else digitsOnly
    }
}
