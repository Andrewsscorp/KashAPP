package com.example

import com.example.data.local.entity.BankEntity
import com.example.domain.usecase.CreateBankEntityUseCase
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

class CreateBankEntityUseCaseTest {

    private lateinit var fakeFinancialRepository: FakeFinancialRepository
    private lateinit var createBankEntityUseCase: CreateBankEntityUseCase

    @Before
    fun setUp() {
        fakeFinancialRepository = FakeFinancialRepository()
        createBankEntityUseCase = CreateBankEntityUseCase(fakeFinancialRepository)

        // Seed existing bank with code "001"
        fakeFinancialRepository.seedBank(
            BankEntity(
                id = "bank_001",
                userId = null,
                name = "Banco de Bogotá",
                clearingCode = "001",
                countryCode = "CO",
                countryName = "Colombia",
                currencyCode = "COP"
            )
        )
    }

    @Test
    fun `create entity successfully saves bank`() = runBlocking {
        val result = createBankEntityUseCase.execute(
            name = "Bancolombia",
            code = "007",
            countryCode = "CO",
            countryName = "Colombia",
            currencyCode = "COP"
        )

        assertNotNull(result)
        assertEquals("Bancolombia", result.name)
        assertEquals("007", result.clearingCode)
        assertEquals(true, fakeFinancialRepository.existsBankByCode("007"))
    }

    @Test
    fun `create entity prevents duplicate clearing codes`() = runBlocking {
        try {
            createBankEntityUseCase.execute(
                name = "Banco de Bogotá Duplicado",
                code = "001" // Already exists
            )
            fail("Should have thrown IllegalArgumentException for duplicate clearing code")
        } catch (e: IllegalArgumentException) {
            assertEquals("El código de compensación ya existe.", e.message)
        }
    }

    @Test
    fun `create entity fails if name or code is blank`() = runBlocking {
        try {
            createBankEntityUseCase.execute(
                name = "",
                code = "055"
            )
            fail("Should have thrown IllegalArgumentException for blank name")
        } catch (e: IllegalArgumentException) {
            assertEquals("El nombre del banco no puede estar vacío.", e.message)
        }

        try {
            createBankEntityUseCase.execute(
                name = "Neobanco",
                code = "   "
            )
            fail("Should have thrown IllegalArgumentException for blank code")
        } catch (e: IllegalArgumentException) {
            assertEquals("El código de compensación no puede estar vacío.", e.message)
        }
    }
}
