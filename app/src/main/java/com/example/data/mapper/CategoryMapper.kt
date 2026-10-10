package com.example.data.mapper

import com.example.data.local.entity.CategoryEntity
import com.example.domain.model.Category
import com.example.domain.model.CategoryType

/**
 * Bidirectional mappers between Room persistence entity and Domain model.
 */
object CategoryMapper {

    fun toDomain(entity: CategoryEntity): Category {
        return Category(
            id = entity.id,
            name = entity.name,
            nameNormalized = com.example.core.text.TextNormalizer.normalize(entity.name),
            icon = entity.icon,
            colorHex = entity.colorHex,
            type = CategoryType.fromString(entity.type),
            isActive = entity.isActive,
            sortOrder = entity.sortOrder,
            userId = entity.userId
        )
    }

    fun toEntity(domain: Category): CategoryEntity {
        return CategoryEntity(
            id = domain.id,
            userId = domain.userId,
            name = domain.name,
            icon = domain.icon,
            colorHex = domain.colorHex,
            type = domain.type.name,
            isActive = domain.isActive,
            sortOrder = domain.sortOrder,
            updatedAt = System.currentTimeMillis()
        )
    }
}
