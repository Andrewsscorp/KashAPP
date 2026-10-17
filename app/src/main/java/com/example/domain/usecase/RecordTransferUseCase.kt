package com.example.domain.usecase

import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.validation.FormValidator
import java.util.UUID

/**
 * UseCase to sanitize, validate and record double-entry transfers between two accounts.
 * Ensures amount > 0, source != destination, and executes atomic database commit.
 */
class RecordTransferUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend operator fun invoke(
        userId: String,
        sourceAccountId: String,
        sourceAccountName: String,
        destinationAccountId: String,
        destinationAccountName: String,
        amount: Double,
        currency: String = "COP",
        dateFormatted: String,
        concept: String? = null,
        note: String? = null,
        receiptUrl: String? = null,
        saveAsTemplate: Boolean = false,
        tags: List<String> = emptyList(),
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<Pair<TransactionEntity, TransactionEntity>> {
        val validation = FormValidator.validateTransfer(
            sourceAccountId = sourceAccountId,
            destinationAccountId = destinationAccountId,
            amount = amount,
            concept = concept
        )

        if (validation is FormValidator.ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.firstError))
        }

        val cleanConcept = FormValidator.sanitizeConcept(concept)
        val cleanNote = FormValidator.sanitizeDescription(note)
        val cleanSourceAccountName = FormValidator.sanitizeConcept(sourceAccountName)
        val cleanDestinationAccountName = FormValidator.sanitizeConcept(destinationAccountName)
        val cleanCurrency = FormValidator.sanitizeCurrency(currency)
        val cleanTags = FormValidator.sanitizeTags(tags)

        return financialRepository.recordTransfer(
            userId = userId,
            sourceAccountId = sourceAccountId.trim(),
            sourceAccountName = cleanSourceAccountName,
            destinationAccountId = destinationAccountId.trim(),
            destinationAccountName = cleanDestinationAccountName,
            amount = amount,
            currency = cleanCurrency,
            dateFormatted = dateFormatted.trim(),
            concept = cleanConcept,
            note = cleanNote,
            receiptUrl = receiptUrl?.trim(),
            saveAsTemplate = saveAsTemplate,
            tags = cleanTags,
            idempotencyKey = idempotencyKey.trim()
        )
    }
}
