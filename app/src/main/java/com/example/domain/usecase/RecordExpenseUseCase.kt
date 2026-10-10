package com.example.domain.usecase

import com.example.data.local.entity.TransactionEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.validation.FormValidator
import java.util.UUID

/**
 * UseCase to sanitize, validate and record an expense with idempotency and ACID consistency.
 */
class RecordExpenseUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend operator fun invoke(
        userId: String,
        accountId: String,
        accountName: String,
        categoryId: String,
        categoryName: String,
        amount: Double,
        currency: String = "COP",
        dateFormatted: String,
        description: String?,
        receiptUrl: String? = null,
        isRecurring: Boolean = false,
        saveAsTemplate: Boolean = false,
        tags: List<String> = emptyList(),
        idempotencyKey: String = UUID.randomUUID().toString()
    ): Result<TransactionEntity> {
        val validation = FormValidator.validateExpense(
            amount = amount,
            accountId = accountId,
            categoryId = categoryId,
            description = description
        )

        if (validation is FormValidator.ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.firstError))
        }

        val cleanDescription = FormValidator.sanitizeDescription(description)
        val cleanAccountName = FormValidator.sanitizeConcept(accountName)
        val cleanCategoryName = FormValidator.sanitizeConcept(categoryName)
        val cleanCurrency = FormValidator.sanitizeCurrency(currency)
        val cleanTags = FormValidator.sanitizeTags(tags)

        return financialRepository.recordExpense(
            userId = userId,
            accountId = accountId.trim(),
            accountName = cleanAccountName,
            categoryId = categoryId.trim(),
            categoryName = cleanCategoryName,
            amount = amount,
            currency = cleanCurrency,
            dateFormatted = dateFormatted.trim(),
            description = cleanDescription,
            receiptUrl = receiptUrl?.trim(),
            isRecurring = isRecurring,
            saveAsTemplate = saveAsTemplate,
            tags = cleanTags,
            idempotencyKey = idempotencyKey.trim()
        )
    }
}
