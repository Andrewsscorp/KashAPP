package com.example

import com.example.data.local.entity.AccountEntity
import com.example.data.repository.ReferentialIntegrityException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests verifying Master Data Management (MDM):
 * 1. Global state synchronization: created bank appears in reactive Flow.
 * 2. Uniqueness and validation of clearing code (ACH / SWIFT).
 * 3. Custom Currency creation with manualExchangeRate (TRM) and reactive emission.
 * 4. Referential integrity: blocking deletion of banks/currencies tied to active accounts (409 Conflict).
 */
class MasterDataSyncAndIntegrityTest {

    private lateinit var repository: FakeFinancialRepository

    @Before
    fun setUp() {
        repository = FakeFinancialRepository()
    }

    @Test
    fun `createBank saves bank and reactive flow emits updated list immediately`() = runBlocking {
        // Initial banks flow
        val initialList = repository.getBanks("user_default").first()
        assertEquals(0, initialList.size)

        // Create new bank entity
        val result = repository.createBank(
            userId = "user_default",
            name = "Banco Cooperativo de Tunja",
            clearingCode = "888",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP",
            cityName = "Tunja",
            logoUrl = null,
            colorHex = "#0284C7",
            iconName = "account_balance",
            additionalInfo = "Entidad comunitaria"
        )

        assertTrue("Bank creation must succeed", result.isSuccess)
        val created = result.getOrNull()
        assertNotNull(created)
        assertEquals("Banco Cooperativo de Tunja", created?.name)
        assertEquals("888", created?.clearingCode)

        // Reactive Flow verification: all observing screens receive the new bank
        val updatedList = repository.getBanks("user_default").first()
        assertEquals(1, updatedList.size)
        assertEquals("Banco Cooperativo de Tunja", updatedList[0].name)
        assertEquals("888", updatedList[0].clearingCode)
    }

    @Test
    fun `createBank prevents duplicate clearing codes`() = runBlocking {
        repository.createBank(
            userId = "user_default",
            name = "Bancolombia",
            clearingCode = "007",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP",
            cityName = null,
            logoUrl = null,
            colorHex = "#FDDA24",
            iconName = "account_balance",
            additionalInfo = null
        )

        val duplicateResult = repository.createBank(
            userId = "user_default",
            name = "Bancolombia Sucursal",
            clearingCode = "007", // Duplicate ACH code
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP",
            cityName = null,
            logoUrl = null,
            colorHex = "#FDDA24",
            iconName = "account_balance",
            additionalInfo = null
        )

        assertTrue(duplicateResult.isFailure)
        val error = duplicateResult.exceptionOrNull()
        assertEquals("El código de compensación ya existe.", error?.message)
    }

    @Test
    fun `deleteBank with linked accounts triggers 409 Conflict ReferentialIntegrityException`() = runBlocking {
        // Register bank
        val bankResult = repository.createBank(
            userId = "user_default",
            name = "Banco Davivienda",
            clearingCode = "051",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP",
            cityName = null,
            logoUrl = null,
            colorHex = "#ED1C24",
            iconName = "account_balance",
            additionalInfo = null
        )
        val bank = bankResult.getOrThrow()

        // Create an account linked to this bank
        repository.seedAccount(
            AccountEntity(
                id = "acc_davi_1",
                userId = "user_default",
                name = "Ahorros Davivienda",
                balance = 1500000.0,
                currency = "COP",
                type = "SAVINGS",
                bankName = "Banco Davivienda"
            )
        )

        // Attempt deletion of bank
        val deleteResult = repository.deleteBank(bank.id, bank.name)

        assertTrue("Deletion must fail due to referential integrity", deleteResult.isFailure)
        val exception = deleteResult.exceptionOrNull()
        assertTrue(exception is ReferentialIntegrityException)
        assertTrue(exception?.message?.contains("Error 409 (Conflict)") == true)
        assertTrue(exception?.message?.contains("vinculada a 1 cuenta(s)") == true)

        // Bank should still exist in reactive Flow
        val banksAfterFailedDelete = repository.getBanks("user_default").first()
        assertEquals(1, banksAfterFailedDelete.size)
    }

    @Test
    fun `createCurrency with manualExchangeRate stores currency and reactive flow emits it`() = runBlocking {
        val result = repository.createCurrency(
            userId = "user_default",
            code = "CAD",
            name = "Dólar Canadiense",
            symbol = "$",
            manualExchangeRate = 3050.50
        )

        assertTrue(result.isSuccess)
        val currency = result.getOrNull()
        assertNotNull(currency)
        assertEquals("CAD", currency?.code)
        assertEquals(3050.50, currency?.manualExchangeRate ?: 0.0, 0.001)

        val currenciesList = repository.getCurrencies("user_default").first()
        val found = currenciesList.find { it.code == "CAD" }
        assertNotNull("Custom currency must be found in reactive flow", found)
        assertEquals(3050.50, found?.manualExchangeRate ?: 0.0, 0.001)
    }

    @Test
    fun `deleteCurrency with linked accounts triggers 409 Conflict ReferentialIntegrityException`() = runBlocking {
        val currencyResult = repository.createCurrency(
            userId = "user_default",
            code = "EUR",
            name = "Euro",
            symbol = "€",
            manualExchangeRate = 4500.0
        )
        val currency = currencyResult.getOrThrow()

        // Link account with EUR
        repository.seedAccount(
            AccountEntity(
                id = "acc_eur_1",
                userId = "user_default",
                name = "Cuenta en Euros",
                balance = 5000.0,
                currency = "EUR",
                type = "SAVINGS"
            )
        )

        val deleteResult = repository.deleteCurrency(currency.id, currency.code)

        assertTrue(deleteResult.isFailure)
        val exception = deleteResult.exceptionOrNull()
        assertTrue(exception is ReferentialIntegrityException)
        assertTrue(exception?.message?.contains("Error 409 (Conflict)") == true)
        assertTrue(exception?.message?.contains("1 cuenta(s) activa(s)") == true)
    }
}
