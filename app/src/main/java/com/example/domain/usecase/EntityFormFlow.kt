package com.example.domain.usecase

import com.example.data.local.entity.BankEntity

/**
 * Interface representing catalog operations for bank search.
 */
interface BankCatalogRepository {
    fun searchOfficialBanks(query: String): List<BankEntity>
}

/**
 * Domain flow manager for the Entity Creation Form.
 * Reacts when the user inputs a name first to query suggestions from the official bank catalog.
 */
class EntityFormFlow(private val catalogRepo: BankCatalogRepository) {

    fun processNameInput(nameInput: String): List<BankEntity> {
        val trimmed = nameInput.trim()
        if (trimmed.isEmpty()) return emptyList()
        return catalogRepo.searchOfficialBanks(trimmed)
    }
}
