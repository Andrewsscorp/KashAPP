package com.example.domain.usecase

import com.example.data.local.entity.AccountCdtEntity
import com.example.data.local.entity.AccountEntity
import com.example.domain.repository.FinancialRepository
import java.time.LocalDate
import java.util.UUID

/**
 * Use case orchestrating CDT investment creation.
 * Enforces financial integrity:
 * - Locks liquidity (availableBalance = 0.0, holdBalance = initialAmount).
 * - Debits source account if funded from existing bank account.
 * - Computes yield and maturity date via CalculateCdtYieldUseCase.
 * - Atomically persists Account and AccountCdtEntity.
 */
class CreateCdtUseCase(
    private val financialRepository: FinancialRepository,
    private val calculateYieldUseCase: CalculateCdtYieldUseCase = CalculateCdtYieldUseCase()
) {

    suspend operator fun invoke(
        userId: String,
        name: String,
        bankName: String,
        currency: String = "COP",
        certificateNumber: String? = null,
        initialAmount: Double,
        interestRateEa: Double,
        rateType: String = "EA",
        payoutFrequency: String = "AT_MATURITY",
        compounding: Boolean = true,
        issueDate: LocalDate = LocalDate.now(),
        termDays: Int = 180,
        actionAtMaturity: String = "ASK",
        destinationAccountId: String? = null,
        sourceAccountId: String? = null,
        allowsEarlyWithdrawal: Boolean = false,
        hasAlert: Boolean = true,
        alertDaysBefore: Int = 15,
        customColorHex: String = "#0284C7",
        iconName: String = "ic_bank_default"
    ): Result<Pair<AccountEntity, AccountCdtEntity>> {
        val candidateName = name.trim().ifEmpty { "CDT $bankName $termDays días" }
        val validation = com.example.domain.validation.FormValidator.validateCdt(
            name = candidateName,
            bankName = bankName,
            initialAmount = initialAmount,
            interestRateEa = interestRateEa,
            termDays = termDays
        )
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.firstError))
        }

        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(candidateName)
        val cleanBankName = com.example.domain.validation.FormValidator.sanitizeConcept(bankName)
        val cleanCurrency = com.example.domain.validation.FormValidator.sanitizeCurrency(currency)
        val cleanCertificateNumber = certificateNumber?.let { com.example.domain.validation.FormValidator.sanitizeAccountNumber(it) }
        val cleanColorHex = com.example.domain.validation.FormValidator.sanitizeColorHex(customColorHex)

        // Calculate yield and maturity date
        val calculation = calculateYieldUseCase(
            initialAmount = initialAmount,
            interestRateEa = interestRateEa,
            termDays = termDays,
            issueDate = issueDate
        )

        val accountId = UUID.randomUUID().toString()
        val cdtId = UUID.randomUUID().toString()

        val cdtAccount = AccountEntity(
            id = accountId,
            userId = userId,
            name = cleanName,
            balance = initialAmount,
            availableBalance = 0.0, // Illiquid: locked until maturity
            holdBalance = initialAmount,
            currency = cleanCurrency,
            type = "CDT",
            bankName = cleanBankName,
            accountNumberEncrypted = financialRepository.encryptSensitiveField(cleanCertificateNumber),
            accountNumberMasked = cleanCertificateNumber?.let { if (it.length > 4) "•••• " + it.takeLast(4) else it },
            openingDate = issueDate.toString(),
            generatesInterest = true,
            interestRateEa = interestRateEa,
            payoutFrequency = payoutFrequency,
            compoundInterest = compounding,
            purposes = "Inversión, CDT",
            customColorHex = cleanColorHex,
            iconName = iconName
        )

        val cdtConfig = AccountCdtEntity(
            id = cdtId,
            accountId = accountId,
            certificateNumber = certificateNumber,
            interestRateEa = interestRateEa,
            rateType = rateType,
            payoutFrequency = payoutFrequency,
            compounding = compounding,
            issueDate = issueDate.toString(),
            termDays = termDays,
            maturityDate = calculation.maturityDate.toString(),
            actionAtMaturity = actionAtMaturity,
            destinationAccountId = destinationAccountId,
            sourceAccountId = sourceAccountId,
            allowsEarlyWithdrawal = allowsEarlyWithdrawal,
            alertDaysBefore = alertDaysBefore,
            hasAlert = hasAlert,
            initialAmount = initialAmount,
            estimatedYield = calculation.estimatedYield
        )

        return financialRepository.createCdtAccount(cdtAccount, cdtConfig, sourceAccountId)
            .map { Pair(cdtAccount, cdtConfig) }
    }
}
