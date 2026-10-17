package com.example.domain.usecase

import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.validation.FormValidator
import java.util.UUID

/**
 * UseCase to sanitize, validate and record an income transaction with ACID consistency
 * and idempotency key duplicate prevention.
 */
class RecordIncomeUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend operator fun invoke(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        sourceId: String?,
        sourceName: String,
        amount: Double,
        currency: String = "COP",
        dateFormatted: String,
        description: String?,
        receiptUrl: String? = null,
        isRecurring: Boolean = false,
        recurrenceFrequency: String = "Mensual",
        saveAsTemplate: Boolean = false,
        tags: List<String> = emptyList(),
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<TransactionEntity> {
        val validation = FormValidator.validateIncome(
            amount = amount,
            accountId = accountId,
            categoryId = categoryId,
            sourceName = sourceName
        )

        if (validation is FormValidator.ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.firstError))
        }

        val cleanDescription = FormValidator.sanitizeDescription(description)
        val cleanAccountName = FormValidator.sanitizeConcept(accountName)
        val cleanSourceName = FormValidator.sanitizeConcept(sourceName)
        val cleanCategoryName = FormValidator.sanitizeConcept(categoryName)
        val cleanCurrency = FormValidator.sanitizeCurrency(currency)
        val cleanTags = FormValidator.sanitizeTags(tags)

        return financialRepository.recordIncome(
            userId = userId,
            accountId = accountId.trim(),
            accountName = cleanAccountName,
            categoryId = categoryId.trim(),
            categoryName = cleanCategoryName,
            sourceId = sourceId?.trim(),
            sourceName = cleanSourceName,
            amount = amount,
            currency = cleanCurrency,
            dateFormatted = dateFormatted.trim(),
            description = cleanDescription,
            receiptUrl = receiptUrl?.trim(),
            isRecurring = isRecurring,
            recurrenceFrequency = recurrenceFrequency,
            saveAsTemplate = saveAsTemplate,
            tags = cleanTags,
            idempotencyKey = idempotencyKey.trim()
        )
    }
}
