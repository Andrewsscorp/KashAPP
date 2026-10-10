package com.example

import com.example.data.local.entity.BankEntity
import com.example.domain.usecase.BankCatalogRepository
import com.example.domain.usecase.EntityFormFlow
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class EntityFormFlowTest {

    private lateinit var mockCatalogRepo: FakeBankCatalogRepository
    private lateinit var flow: EntityFormFlow

    @Before
    fun setUp() {
        mockCatalogRepo = FakeBankCatalogRepository()
        flow = EntityFormFlow(mockCatalogRepo)
    }

    @Test
    fun `form flow searches catalog after name input`() {
        // User inputs bank name first in updated architecture
        val suggestions = flow.processNameInput("Bancolom")

        assertEquals(1, mockCatalogRepo.searchCallCount)
        assertEquals("Bancolom", mockCatalogRepo.lastQuery)
        assertEquals(1, suggestions.size)
        assertEquals("Bancolombia", suggestions[0].name)
        assertEquals("007", suggestions[0].clearingCode)
    }
}

class FakeBankCatalogRepository : BankCatalogRepository {
    var searchCallCount = 0
    var lastQuery: String? = null

    private val officialBanks = listOf(
        BankEntity(
            id = "001",
            userId = null,
            name = "Bancolombia",
            clearingCode = "007",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP"
        ),
        BankEntity(
            id = "002",
            userId = null,
            name = "Banco Davivienda",
            clearingCode = "051",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP"
        )
    )

    override fun searchOfficialBanks(query: String): List<BankEntity> {
        searchCallCount++
        lastQuery = query
        return officialBanks.filter { it.name.contains(query, ignoreCase = true) }
    }
}
