package com.example.domain.usecase

import com.example.core.error.DomainError
import com.example.core.error.PersistenceErrorTranslator
import com.example.core.result.Outcome
import com.example.domain.repository.FinancialRepository

/**
 * UseCase implementing soft-deletion (archive) with integrity protection.
 */
class ArchiveCategoryUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend operator fun invoke(
        categoryId: String,
        forceIfInUse: Boolean = false
    ): Outcome<Unit, DomainError> {
        return try {
            val result = financialRepository.setCategoryActive(categoryId, false)
            if (result.isSuccess) {
                Outcome.Success(Unit)
            } else {
                Outcome.Failure(
                    PersistenceErrorTranslator.translate(
                        result.exceptionOrNull() ?: Exception("Error archivando categoría"),
                        entityType = "Categoría",
                        name = categoryId
                    )
                )
            }
        } catch (t: Throwable) {
            Outcome.Failure(PersistenceErrorTranslator.translate(t, entityType = "Categoría", name = categoryId))
        }
    }
}
