package com.example.domain.usecase

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import com.example.domain.repository.FinancialRepository
import java.util.UUID

/**
 * UseCase for creating a Credit Card (Pasivo rotativo) in KashApp.
 *
 * Enforces:
 * 1. Master Equation Rule: Total = Available + Used + Retained
 * 2. Proper Liability Accounting: Account balance stored as negative used quota (-quotaUsed)
 * 3. Atomic persistence of AccountEntity and AccountCreditCardEntity
 */
class CreateCreditCardUseCase(
    private val financialRepository: FinancialRepository,
    private val validateCreditLimitsUseCase: ValidateCreditLimitsUseCase = ValidateCreditLimitsUseCase(),
    private val calculateNextBillingDatesUseCase: CalculateNextBillingDatesUseCase = CalculateNextBillingDatesUseCase()
) {

    suspend operator fun invoke(
        userId: String,
        name: String,
        bankId: String,
        bankName: String,
        currencyCode: String,
        franchise: String, // "VISA", "MASTERCARD", "AMEX", "DINERS"
        category: String, // "CLASSIC", "GOLD", "PLATINUM", "SIGNATURE", "BLACK", "INFINITE"
        quotaTotal: Double,
        quotaAvailable: Double,
        quotaUsed: Double,
        quotaRetained: Double = 0.0,
        cutOffDay: Int,
        paymentDueDay: Int,
        interestRateEa: Double,
        purchasesRateEa: Double,
        advancesRateEa: Double,
        hasGracePeriod: Boolean = true,
        lateInterestRateMonthly: Double = 2.5,
        currentInterestAmount: Double = 0.0,
        paymentStrategy: String = "MINIMUM_PAYMENT",
        minimumPaymentAmount: Double = 0.0,
        autoPayEnabled: Boolean = false,
        autoPaySourceAccountId: String? = null,
        autoPaySourceAccountName: String? = null,
        customPayDay: Int = 15,
        scheduledPayEnabled: Boolean = false,
        scheduledPayDate: String = "2026-10-15",
        extraPaymentsEnabled: Boolean = false,
        extraPaymentReduces: String = "BOTH",
        totalInstallments: Int = 48,
        paidInstallments: Int = 0,
        installmentValue: Double = 0.0,
        amortizationSystem: String = "FRENCH",
        includeInNetWorth: Boolean = true,
        financesAsset: Boolean = false,
        relatedAssetName: String? = null,
        relatedAssetValue: Double = 0.0,
        isFavorite: Boolean = false,
        supportingDocumentUri: String? = null,
        supportingDocumentType: String? = null,
        notes: String? = null
    ): Result<Pair<AccountEntity, AccountCreditCardEntity>> {
        val validation = com.example.domain.validation.FormValidator.validateCreditCard(
            cardName = name,
            quotaTotal = quotaTotal,
            cutOffDay = cutOffDay,
            paymentDueDay = paymentDueDay,
            franchise = franchise
        )
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            return Result.failure(IllegalArgumentException(validation.firstError))
        }

        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(name)
        val cleanBankName = com.example.domain.validation.FormValidator.sanitizeConcept(bankName)
        val cleanCurrency = com.example.domain.validation.FormValidator.sanitizeCurrency(currencyCode)
        val cleanNotes = com.example.domain.validation.FormValidator.sanitizeDescription(notes)

        // Validate Master Equation
        val limitValidation = validateCreditLimitsUseCase(
            totalQuota = quotaTotal,
            availableQuota = quotaAvailable,
            usedQuota = quotaUsed,
            retainedQuota = quotaRetained
        )

        val finalAvailable = if (!limitValidation.isValid) {
            validateCreditLimitsUseCase.calculateAvailableFromTotalAndUsed(quotaTotal, quotaUsed, quotaRetained)
        } else {
            quotaAvailable
        }

        val accountId = UUID.randomUUID().toString()
        val account = AccountEntity(
            id = accountId,
            userId = userId,
            name = cleanName,
            type = "CREDIT_CARD",
            currency = cleanCurrency,
            balance = -quotaUsed, // Stored as negative debt / liability
            availableBalance = finalAvailable,
            bankName = cleanBankName,
            customColorHex = "#1E293B",
            iconName = "credit_card",
            accountNumberMasked = null,
            hasOverdraft = true,
            overdraftLimit = quotaTotal,
            feeChargeDay = cutOffDay,
            supportingDocumentUri = supportingDocumentUri,
            supportingDocumentType = supportingDocumentType
        )

        val creditCardEntity = AccountCreditCardEntity(
            id = UUID.randomUUID().toString(),
            accountId = accountId,
            franchise = franchise,
            category = category,
            quotaTotal = quotaTotal,
            quotaAvailable = finalAvailable,
            quotaUsed = quotaUsed,
            quotaRetained = quotaRetained,
            cutOffDay = cutOffDay,
            paymentDueDay = paymentDueDay,
            interestRateEa = interestRateEa,
            purchasesRateEa = purchasesRateEa,
            advancesRateEa = advancesRateEa,
            hasGracePeriod = hasGracePeriod,
            lateInterestRateMonthly = lateInterestRateMonthly,
            currentInterestAmount = currentInterestAmount,
            interestCutoffDate = "2026-09-25",
            paymentStrategy = paymentStrategy,
            minimumPaymentAmount = minimumPaymentAmount,
            autoPayEnabled = autoPayEnabled,
            autoPaySourceAccountId = autoPaySourceAccountId,
            autoPaySourceAccountName = autoPaySourceAccountName,
            customPayDay = customPayDay,
            scheduledPayEnabled = scheduledPayEnabled,
            scheduledPayDate = scheduledPayDate,
            extraPaymentsEnabled = extraPaymentsEnabled,
            extraPaymentReduces = extraPaymentReduces,
            totalInstallments = totalInstallments,
            paidInstallments = paidInstallments,
            pendingInstallments = (totalInstallments - paidInstallments).coerceAtLeast(0),
            installmentValue = installmentValue,
            amortizationSystem = amortizationSystem,
            nextInstallmentDate = scheduledPayDate,
            includeInNetWorth = includeInNetWorth,
            financesAsset = financesAsset,
            relatedAssetName = relatedAssetName,
            relatedAssetValue = relatedAssetValue,
            notes = notes,
            isFavorite = isFavorite
        )

        return financialRepository.createCreditCardAccount(account, creditCardEntity)
    }
}
