package com.example.domain.usecase

import com.example.data.local.entity.BankEntity
import com.example.domain.repository.FinancialRepository

/**
 * Clean Architecture Use Case to register bank entities with duplicate validation.
 *
 * Enforces:
 * 1. Clearing code (código de compensación / ACH) uniqueness check.
 * 2. Name validation.
 * 3. Persistence through FinancialRepository.
 */
class CreateBankEntityUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend fun execute(
        name: String,
        code: String,
        countryCode: String = "CO",
        countryName: String = "Colombia",
        currencyCode: String = "COP",
        userId: String = "user_default"
    ): BankEntity {
        val validation = com.example.domain.validation.FormValidator.validateBank(name, code)
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            throw IllegalArgumentException(validation.firstError)
        }

        val cleanCode = com.example.domain.validation.FormValidator.sanitizeAccountNumber(code)
        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(name)
        val cleanCountryCode = com.example.domain.validation.FormValidator.sanitizeText(countryCode, 3).uppercase()
        val cleanCountryName = com.example.domain.validation.FormValidator.sanitizeConcept(countryName)
        val cleanCurrency = com.example.domain.validation.FormValidator.sanitizeCurrency(currencyCode)

        if (financialRepository.existsBankByCode(cleanCode)) {
            throw IllegalArgumentException("El código de compensación ya existe.")
        }

        val result = financialRepository.createBank(
            userId = userId,
            name = cleanName,
            clearingCode = cleanCode,
            countryCode = cleanCountryCode,
            countryName = cleanCountryName,
            currencyCode = cleanCurrency
        )

        return result.getOrThrow()
    }
}
