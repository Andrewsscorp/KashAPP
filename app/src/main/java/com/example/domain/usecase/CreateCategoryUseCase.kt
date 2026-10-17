package com.example.domain.usecase

import com.example.core.error.DomainError
import com.example.core.error.PersistenceErrorTranslator
import com.example.core.id.IdGenerator
import com.example.core.id.SequentialIdGenerator
import com.example.core.result.Outcome
import com.example.core.text.TextNormalizer
import com.example.data.mapper.CategoryMapper
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.repository.FinancialRepository

/**
 * UseCase implementing atomic, validated creation of categories in Room SSOT.
 */
class CreateCategoryUseCase(
    private val financialRepository: FinancialRepository,
    private val idGenerator: IdGenerator = SequentialIdGenerator()
) {
    suspend operator fun invoke(
        name: String,
        colorHex: String = "#0284C7",
        icon: String = "category",
        type: CategoryType = CategoryType.EXPENSE,
        userId: String? = null
    ): Outcome<Category, DomainError> {
        val sanitizedName = TextNormalizer.sanitizeDisplayName(name)

        if (sanitizedName.isBlank()) {
            return Outcome.Failure(DomainError.Validation.Blank)
        }
        if (sanitizedName.length < 2) {
            return Outcome.Failure(DomainError.Validation.TooShort(min = 2, current = sanitizedName.length))
        }
        if (sanitizedName.length > 50) {
            return Outcome.Failure(DomainError.Validation.TooLong(max = 50, current = sanitizedName.length))
        }

        return try {
            val entity = financialRepository.createCategory(
                name = sanitizedName,
                colorHex = colorHex,
                icon = icon,
                userId = userId,
                type = type.name
            )
            Outcome.Success(CategoryMapper.toDomain(entity))
        } catch (t: Throwable) {
            Outcome.Failure(PersistenceErrorTranslator.translate(t, entityType = "Categoría", name = sanitizedName))
        }
    }
}
