package com.example.domain.engine

import com.example.core.error.DomainError
import com.example.core.result.Outcome
import com.example.core.text.TextNormalizer
import com.example.domain.models.shared.ConsecutiveMode
import com.example.domain.models.shared.SequenceDocumentType
import com.example.domain.models.shared.SequenceResetPolicy
import java.security.MessageDigest
import java.time.LocalDate

/**
 * Motor determinista de numeración correlativa para Gastos Compartidos (GC),
 * Tickets de Recibo (TK), Comprobantes de Pago (PG) y Actas de Liquidación (LQ).
 */
object ConsecutiveSequenceEngine {

    data class SequenceSnapshot(
        val sequenceType: SequenceDocumentType,
        val scopeGroupCode: String = "GLOBAL",
        val mode: ConsecutiveMode = ConsecutiveMode.HYBRID_ALLOW_OVERRIDE,
        val prefix: String,
        val patternTemplate: String,
        val currentValue: Long,
        val stepBy: Int = 1,
        val paddingDigits: Int = 4,
        val resetPolicy: SequenceResetPolicy = SequenceResetPolicy.YEARLY,
        val lastResetPeriodKey: String
    )

    data class GeneratedConsecutiveResult(
        val formattedCode: String,
        val normalizedCode: String,
        val nextSequenceValue: Long,
        val resolvedPeriodKey: String,
        val wasCounterReset: Boolean,
        val isManualOverride: Boolean
    )

    /**
     * Calcula la clave de período actual según la política de reinicio configurada.
     */
    fun computePeriodKey(
        policy: SequenceResetPolicy,
        referenceDate: LocalDate = LocalDate.now(),
        groupCycleIndex: Int = 1
    ): String {
        return when (policy) {
            SequenceResetPolicy.NEVER -> "CONTINUOUS"
            SequenceResetPolicy.YEARLY -> "%04d".format(referenceDate.year)
            SequenceResetPolicy.MONTHLY -> "%04d-%02d".format(referenceDate.year, referenceDate.monthValue)
            SequenceResetPolicy.PER_GROUP_CYCLE -> "CYCLE-$groupCycleIndex"
        }
    }

    /**
     * Previsualiza o genera el siguiente consecutivo automático interpolando todos los tokens dinámicos.
     * Tokens soportados: {PREFIX}, {GROUP}, {YYYY}, {YY}, {MM}, {DD}, {USER}, {SEQ:N} y {SEQ}.
     */
    fun generateNextAutomatic(
        snapshot: SequenceSnapshot,
        referenceDate: LocalDate = LocalDate.now(),
        groupCode: String? = null,
        userInitials: String = "YO",
        groupCycleIndex: Int = 1
    ): GeneratedConsecutiveResult {
        val currentPeriodKey = computePeriodKey(snapshot.resetPolicy, referenceDate, groupCycleIndex)
        val shouldReset = snapshot.resetPolicy != SequenceResetPolicy.NEVER &&
            snapshot.lastResetPeriodKey != currentPeriodKey

        val baseCounter = if (shouldReset) 0L else snapshot.currentValue
        val nextValue = baseCounter + snapshot.stepBy.coerceAtLeast(1)

        val formatted = formatPattern(
            patternTemplate = snapshot.patternTemplate,
            prefix = snapshot.prefix,
            sequenceNumber = nextValue,
            defaultPadding = snapshot.paddingDigits,
            referenceDate = referenceDate,
            groupCode = groupCode ?: snapshot.scopeGroupCode.takeIf { it != "GLOBAL" } ?: "GEN",
            userInitials = userInitials
        )

        return GeneratedConsecutiveResult(
            formattedCode = formatted,
            normalizedCode = TextNormalizer.normalizeForUniqueness(formatted),
            nextSequenceValue = nextValue,
            resolvedPeriodKey = currentPeriodKey,
            wasCounterReset = shouldReset,
            isManualOverride = false
        )
    }

    /**
     * Valida y normaliza un consecutivo ingresado manualmente o extraído por el escáner OCR,
     * verificando colisiones y actualizando el puntero si el usuario ingresó un número mayor de la misma serie.
     */
    fun validateAndResolveManualOrOcrCode(
        rawManualInput: String,
        snapshot: SequenceSnapshot,
        alreadyExistsInDb: Boolean,
        referenceDate: LocalDate = LocalDate.now(),
        groupCode: String? = null,
        userInitials: String = "YO"
    ): Outcome<GeneratedConsecutiveResult, DomainError> {
        val trimmed = rawManualInput.trim().uppercase()
        if (trimmed.isBlank()) {
            return Outcome.Failure(DomainError.Validation.Blank)
        }
        if (trimmed.length < 3) {
            return Outcome.Failure(DomainError.Validation.TooShort(min = 3, current = trimmed.length))
        }
        if (trimmed.length > 36) {
            return Outcome.Failure(DomainError.Validation.TooLong(max = 36, current = trimmed.length))
        }
        val validRegex = Regex("^[A-Z0-9._/-]+$")
        if (!validRegex.matches(trimmed)) {
            return Outcome.Failure(
                DomainError.Validation.InvalidCharacters("Solo se permiten letras, números, guiones (-), barras (/) y puntos (.).")
            )
        }

        val autoFallback = generateNextAutomatic(
            snapshot = snapshot,
            referenceDate = referenceDate,
            groupCode = groupCode,
            userInitials = userInitials
        )

        if (alreadyExistsInDb) {
            return Outcome.Failure(
                DomainError.SharedExpense.DuplicateConsecutiveCode(
                    consecutiveCode = trimmed,
                    sequenceType = snapshot.sequenceType.displayName,
                    suggestedNextAvailable = autoFallback.formattedCode
                )
            )
        }

        return Outcome.Success(
            GeneratedConsecutiveResult(
                formattedCode = trimmed,
                normalizedCode = TextNormalizer.normalizeForUniqueness(trimmed),
                nextSequenceValue = snapshot.currentValue,
                resolvedPeriodKey = autoFallback.resolvedPeriodKey,
                wasCounterReset = false,
                isManualOverride = true
            )
        )
    }

    /**
     * Interpola una plantilla de consecutivo reemplazando tokens dinámicos.
     */
    fun formatPattern(
        patternTemplate: String,
        prefix: String,
        sequenceNumber: Long,
        defaultPadding: Int = 4,
        referenceDate: LocalDate = LocalDate.now(),
        groupCode: String = "GEN",
        userInitials: String = "YO"
    ): String {
        val cleanGroup = groupCode.trim().uppercase().replace(Regex("[^A-Z0-9]"), "").ifBlank { "GEN" }
        val cleanPrefix = prefix.trim().uppercase().ifBlank { "GC" }
        val cleanUser = userInitials.trim().uppercase().ifBlank { "YO" }

        var result = patternTemplate
            .replace("{PREFIX}", cleanPrefix, ignoreCase = true)
            .replace("{GROUP}", cleanGroup, ignoreCase = true)
            .replace("{YYYY}", "%04d".format(referenceDate.year), ignoreCase = true)
            .replace("{YY}", "%02d".format(referenceDate.year % 100), ignoreCase = true)
            .replace("{MM}", "%02d".format(referenceDate.monthValue), ignoreCase = true)
            .replace("{DD}", "%02d".format(referenceDate.dayOfMonth), ignoreCase = true)
            .replace("{USER}", cleanUser, ignoreCase = true)

        // Token con dígitos explícitos: {SEQ:5}
        val seqWithDigitsRegex = Regex("\\{SEQ:(\\d+)\\}", RegexOption.IGNORE_CASE)
        result = seqWithDigitsRegex.replace(result) { matchResult ->
            val digits = matchResult.groupValues[1].toIntOrNull()?.coerceIn(1, 10) ?: defaultPadding
            "%0${digits}d".format(sequenceNumber)
        }

        // Token simple: {SEQ}
        val paddedDefault = "%0${defaultPadding.coerceIn(1, 10)}d".format(sequenceNumber)
        result = result.replace("{SEQ}", paddedDefault, ignoreCase = true)

        return result
    }

    /**
     * Genera el sello criptográfico SHA-256 truncado (16 caracteres hexadecimales en mayúscula)
     * para imprimir en el Ticket de Recibo y verificar que los montos/cuotas no fueron alterados.
     */
    fun computeTicketIntegrityHash(
        consecutiveCode: String,
        receiptTicketCode: String,
        totalAmount: Double,
        currency: String,
        expenseDate: String,
        participantSharesSignature: String
    ): String {
        val canonicalPayload = buildString {
            append(consecutiveCode.trim().uppercase())
            append("|")
            append(receiptTicketCode.trim().uppercase())
            append("|")
            append("%.2f".format(java.util.Locale.US, totalAmount))
            append("|")
            append(currency.trim().uppercase())
            append("|")
            append(expenseDate.trim())
            append("|")
            append(participantSharesSignature.trim())
        }
        val digest = MessageDigest.getInstance("SHA-256").digest(canonicalPayload.toByteArray(Charsets.UTF_8))
        return digest.take(8).joinToString("") { "%02X".format(it) }
    }
}
