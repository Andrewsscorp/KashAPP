package com.example.domain.usecase

import com.example.data.mapper.CategoryMapper
import com.example.domain.model.Category
import com.example.domain.model.CategoryType
import com.example.domain.repository.FinancialRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/**
 * Reactive UseCase observing categories from Room (SSOT) with deduplication and active filtering.
 */
class ObserveCategoriesUseCase(
    private val financialRepository: FinancialRepository
) {
    operator fun invoke(type: CategoryType = CategoryType.EXPENSE): Flow<List<Category>> {
        val typeStr = when (type) {
            CategoryType.EXPENSE -> "EXPENSE"
            CategoryType.INCOME -> "INCOME"
            CategoryType.BOTH -> null
        }
        return financialRepository.getActiveCategories(typeStr)
            .map { list -> list.map { CategoryMapper.toDomain(it) } }
            .distinctUntilChanged()
    }
}
