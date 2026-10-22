package com.example.presentation.credit_cards.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CalculateNextBillingDatesUseCase
import com.example.domain.usecase.CreateCreditCardUseCase
import com.example.domain.usecase.ValidateCreditLimitsUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate

/**
 * ViewModel managing the Credit Card creation state and financial logic.
 * Enforces:
 * - Master Equation: Cupo Total = Cupo Disponible + Cupo Utilizado + Cupo Retenido.
 * - Reactive billing cycle dates & weekend rollover alerts.
 * - Amortization calculation & liability accounting.
 */
class CreditCardFormViewModel(
    private val createCreditCardUseCase: CreateCreditCardUseCase,
    private val validateCreditLimitsUseCase: ValidateCreditLimitsUseCase = ValidateCreditLimitsUseCase(),
    private val calculateNextBillingDatesUseCase: CalculateNextBillingDatesUseCase = CalculateNextBillingDatesUseCase(),
    private val financialRepository: FinancialRepository,
    val userId: String = "kash_user_01"
) : ViewModel() {

    private val _uiState = MutableStateFlow(CreditCardFormState())
    val uiState: StateFlow<CreditCardFormState> = _uiState.asStateFlow()

    init {
        // Initial billing engine run
        recalculateLimitsAndBilling(
            total = _uiState.value.totalQuota,
            used = _uiState.value.usedQuota,
            cutOff = _uiState.value.cutOffDay,
            dueDay = _uiState.value.paymentDueDay
        )
        loadSourceAccounts()
    }

    private fun loadSourceAccounts() {
        viewModelScope.launch {
            try {
                financialRepository.getAccounts(userId).collect { accounts ->
                    val nonCreditAccounts = accounts
                        .filter { it.type != "CREDIT_CARD" }
                        .map { it.id to "${it.name} - ${it.bankName}" }
                    if (nonCreditAccounts.isNotEmpty()) {
                        _uiState.update { state ->
                            state.copy(
                                availableSourceAccounts = nonCreditAccounts,
                                sourceAccountId = nonCreditAccounts.first().first,
                                sourceAccount = nonCreditAccounts.first().second
                            )
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun resetForm() {
        _uiState.update {
            CreditCardFormState(
                debtName = "",
                totalQuota = 0.0,
                usedQuota = 0.0,
                availableQuota = 0.0,
                isSaving = false,
                saveSuccess = false,
                errorMessage = null
            )
        }
        recalculateLimitsAndBilling(
            total = 0.0,
            used = 0.0,
            cutOff = 15,
            dueDay = 30
        )
    }

    fun onDebtNameChanged(name: String) {
        _uiState.update { it.copy(debtName = name) }
    }

    fun onCreditorBankChanged(bank: String) {
        _uiState.update { it.copy(creditorBank = bank) }
    }

    fun onCurrencyChanged(code: String, name: String) {
        _uiState.update { it.copy(currencyCode = code, currencyName = name) }
    }

    fun onFranchiseChanged(franchise: String) {
        _uiState.update { it.copy(franchise = franchise) }
    }

    fun onCategoryChanged(category: String) {
        _uiState.update { it.copy(category = category) }
    }

    fun onDebtStatusChanged(status: String) {
        _uiState.update { it.copy(debtStatus = status) }
    }

    fun onAcquisitionDateChanged(date: String) {
        _uiState.update { it.copy(acquisitionDate = date) }
    }

    /**
     * Master Equation Rule:
     * When user changes Total Quota, auto-calculate Available Quota:
     * Available = Total - Used - Retained
     */
    fun onTotalQuotaChanged(newTotal: Double) {
        val used = _uiState.value.usedQuota
        val retained = _uiState.value.retainedQuota
        val autoAvailable = validateCreditLimitsUseCase.calculateAvailableFromTotalAndUsed(newTotal, used, retained)
        recalculateLimitsAndBilling(total = newTotal, used = used, available = autoAvailable)
    }

    /**
     * Reactive state listener for "Cupo Utilizado / ¿Cuánto debes hoy?":
     * When user enters e.g. $ 2.850.000 or $ 8.450.000,
     * available quota auto-balances and the progress bar animates in real time!
     */
    fun onUsedQuotaChanged(newUsed: Double) {
        val total = _uiState.value.totalQuota
        val retained = _uiState.value.retainedQuota
        val autoAvailable = validateCreditLimitsUseCase.calculateAvailableFromTotalAndUsed(total, newUsed, retained)
        recalculateLimitsAndBilling(total = total, used = newUsed, available = autoAvailable)
    }

    /**
     * When user directly adjusts Available Quota, auto-adjusts Used Quota:
     * Used = Total - Available - Retained
     */
    fun onAvailableQuotaChanged(newAvailable: Double) {
        val total = _uiState.value.totalQuota
        val retained = _uiState.value.retainedQuota
        val autoUsed = validateCreditLimitsUseCase.calculateUsedFromTotalAndAvailable(total, newAvailable, retained)
        recalculateLimitsAndBilling(total = total, used = autoUsed, available = newAvailable)
    }

    fun onCutOffDayChanged(day: Int) {
        val clamped = day.coerceIn(1, 31)
        _uiState.update { it.copy(cutOffDay = clamped) }
        recalculateBillingDates(clamped, _uiState.value.paymentDueDay)
    }

    fun onPaymentDueDayChanged(day: Int) {
        val clamped = day.coerceIn(1, 31)
        _uiState.update { it.copy(paymentDueDay = clamped) }
        recalculateBillingDates(_uiState.value.cutOffDay, clamped)
    }

    private fun recalculateLimitsAndBilling(
        total: Double,
        used: Double,
        available: Double = _uiState.value.availableQuota,
        cutOff: Int = _uiState.value.cutOffDay,
        dueDay: Int = _uiState.value.paymentDueDay
    ) {
        val validation = validateCreditLimitsUseCase(
            totalQuota = total,
            availableQuota = available,
            usedQuota = used,
            retainedQuota = _uiState.value.retainedQuota
        )

        val cycle = calculateNextBillingDatesUseCase(
            cutOffDay = cutOff,
            paymentDueDay = dueDay,
            referenceDate = LocalDate.of(2026, 9, 27),
            billedAmount = used,
            purchasesRateEa = _uiState.value.purchasesRateEa
        )

        _uiState.update {
            it.copy(
                totalQuota = total,
                usedQuota = used,
                availableQuota = available,
                limitValidation = validation,
                billingCycle = cycle,
                cutOffDate = cycle.nextCutOffDateFormatted
            )
        }
    }

    private fun recalculateBillingDates(cutOff: Int, dueDay: Int) {
        val cycle = calculateNextBillingDatesUseCase(
            cutOffDay = cutOff,
            paymentDueDay = dueDay,
            referenceDate = LocalDate.of(2026, 9, 27),
            billedAmount = _uiState.value.usedQuota,
            purchasesRateEa = _uiState.value.purchasesRateEa
        )
        _uiState.update { it.copy(billingCycle = cycle) }
    }

    // Intereses
    fun onGeneratesInterestChanged(enabled: Boolean) {
        _uiState.update { it.copy(generatesInterest = enabled) }
    }

    fun onInterestRateEaChanged(rate: Double) {
        _uiState.update { it.copy(interestRateEa = rate) }
    }

    fun onPurchasesRateEaChanged(rate: Double) {
        _uiState.update { it.copy(purchasesRateEa = rate) }
        recalculateBillingDates(_uiState.value.cutOffDay, _uiState.value.paymentDueDay)
    }

    fun onAdvancesRateEaChanged(rate: Double) {
        _uiState.update { it.copy(advancesRateEa = rate) }
    }

    fun onGracePeriodChanged(enabled: Boolean) {
        _uiState.update { it.copy(hasGracePeriod = enabled) }
    }

    fun onLateInterestMonthlyChanged(rate: Double) {
        _uiState.update { it.copy(lateInterestRateMonthly = rate) }
    }

    fun onCurrentInterestAmountChanged(amount: Double) {
        _uiState.update { it.copy(currentInterestAmount = amount) }
    }

    // Plazos y Cuotas
    fun onTotalInstallmentsChanged(total: Int) {
        val safeTotal = total.coerceAtLeast(1)
        val paid = _uiState.value.paidInstallments.coerceAtMost(safeTotal)
        val pending = safeTotal - paid
        _uiState.update {
            it.copy(
                totalInstallments = safeTotal,
                paidInstallments = paid,
                pendingInstallments = pending
            )
        }
        recalculateAmortization(safeTotal)
    }

    fun onPaidInstallmentsChanged(paid: Int) {
        val total = _uiState.value.totalInstallments
        val safePaid = paid.coerceIn(0, total)
        val pending = total - safePaid
        _uiState.update {
            it.copy(
                paidInstallments = safePaid,
                pendingInstallments = pending
            )
        }
    }

    fun onInstallmentValueChanged(value: Double) {
        _uiState.update { it.copy(installmentValue = value) }
    }

    fun onAmortizationSystemChanged(system: String) {
        _uiState.update { it.copy(amortizationSystem = system) }
    }

    private fun recalculateAmortization(installments: Int) {
        val projection = calculateNextBillingDatesUseCase.calculateInstallmentProjection(
            amount = _uiState.value.usedQuota,
            installments = installments,
            annualRateEa = _uiState.value.purchasesRateEa
        )
        _uiState.update {
            it.copy(
                installmentProjection = projection,
                installmentValue = projection.installmentMonthlyPayment
            )
        }
    }

    // Configuración de Pagos
    fun onPaymentMethodChanged(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun onSourceAccountChanged(id: String, name: String) {
        _uiState.update { it.copy(sourceAccountId = id, sourceAccount = name) }
    }

    fun onRegularPaymentDayChanged(day: Int) {
        _uiState.update { it.copy(regularPaymentDay = day.coerceIn(1, 31)) }
    }

    fun onMinimumPaymentEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(minimumPaymentEnabled = enabled) }
    }

    fun onMinimumPaymentAmountChanged(amount: Double) {
        _uiState.update { it.copy(minimumPaymentAmount = amount) }
    }

    fun onScheduledPaymentEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(scheduledPaymentEnabled = enabled) }
    }

    fun onScheduledPaymentDateChanged(date: String) {
        _uiState.update { it.copy(scheduledPaymentDate = date) }
    }

    fun onExtraPaymentsEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(extraPaymentsEnabled = enabled) }
    }

    fun onExtraPaymentReducesChanged(reduces: String) {
        _uiState.update { it.copy(extraPaymentReduces = reduces) }
    }

    // Impacto en el Patrimonio Neto
    fun onIncludeInNetWorthChanged(enabled: Boolean) {
        _uiState.update { it.copy(includeInNetWorth = enabled) }
    }

    fun onFinancesAssetChanged(enabled: Boolean) {
        _uiState.update { it.copy(financesAsset = enabled) }
    }

    fun onRelatedAssetChanged(asset: String) {
        _uiState.update { it.copy(relatedAsset = asset) }
    }

    fun onAssetValueChanged(value: Double) {
        _uiState.update { it.copy(assetValue = value) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes) }
    }

    // Documentos
    fun onDocumentAttached(uri: String, name: String, type: String, sizeKb: Long) {
        _uiState.update {
            it.copy(
                attachedDocumentUri = uri,
                attachedDocumentName = name,
                attachedDocumentType = type,
                attachedDocumentSizeKb = sizeKb
            )
        }
    }

    fun onRemoveDocument() {
        _uiState.update {
            it.copy(
                attachedDocumentUri = null,
                attachedDocumentName = null,
                attachedDocumentType = null,
                attachedDocumentSizeKb = 0L
            )
        }
    }

    // Recordatorios
    fun onRemindersEnabledChanged(enabled: Boolean) {
        _uiState.update { it.copy(remindersEnabled = enabled) }
    }

    fun onReminderDaysAheadChanged(days: Int) {
        _uiState.update { it.copy(reminderDaysAhead = days) }
    }

    fun onReminderTimeChanged(hour: Int, minute: Int) {
        _uiState.update { it.copy(reminderHour = hour, reminderMinute = minute) }
    }

    // Más detalles
    fun onReferenceNumberChanged(ref: String) {
        _uiState.update { it.copy(referenceNumber = ref) }
    }

    fun onCreditorPhoneChanged(phone: String) {
        _uiState.update { it.copy(creditorPhone = phone) }
    }

    fun onPaymentChannelChanged(channel: String) {
        _uiState.update { it.copy(paymentChannel = channel) }
    }

    fun onToggleTag(tag: String) {
        _uiState.update { state ->
            val updated = if (state.selectedTags.contains(tag)) {
                state.selectedTags - tag
            } else {
                state.selectedTags + tag
            }
            state.copy(selectedTags = updated)
        }
    }

    fun onInsuranceMonthlyFeeChanged(fee: Double) {
        _uiState.update { it.copy(insuranceMonthlyFee = fee) }
    }

    fun onFavoriteToggle() {
        _uiState.update { it.copy(isFavorite = !it.isFavorite) }
    }

    // Accordions
    fun toggleAccordion(section: String) {
        _uiState.update { state ->
            when (section) {
                "basic" -> state.copy(isBasicInfoExpanded = !state.isBasicInfoExpanded)
                "balance" -> state.copy(isBalanceExpanded = !state.isBalanceExpanded)
                "interest" -> state.copy(isInterestExpanded = !state.isInterestExpanded)
                "installments" -> state.copy(isInstallmentsExpanded = !state.isInstallmentsExpanded)
                "payment" -> state.copy(isPaymentConfigExpanded = !state.isPaymentConfigExpanded)
                "networth" -> state.copy(isNetWorthExpanded = !state.isNetWorthExpanded)
                "documents" -> state.copy(isDocumentsExpanded = !state.isDocumentsExpanded)
                "reminders" -> state.copy(isRemindersExpanded = !state.isRemindersExpanded)
                "favorites" -> state.copy(isFavoritesExpanded = !state.isFavoritesExpanded)
                "details" -> state.copy(isDetailsExpanded = !state.isDetailsExpanded)
                else -> state
            }
        }
    }

    fun saveCreditCard(
        userId: String = this.userId,
        context: android.content.Context? = null
    ) {
        val s = _uiState.value
        val nameToSave = s.debtName.ifBlank { "Deuda ${s.creditorBank}" }
        _uiState.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            val combinedNotes = buildString {
                if (s.referenceNumber.isNotBlank()) append("Ref/Pagaré: ${s.referenceNumber}. ")
                if (s.creditorPhone.isNotBlank()) append("Tel: ${s.creditorPhone}. ")
                if (s.paymentChannel.isNotBlank()) append("Canal: ${s.paymentChannel}. ")
                if (s.selectedTags.isNotEmpty()) append("Tags: ${s.selectedTags.joinToString(", ")}. ")
                if (s.insuranceMonthlyFee > 0) append("Seguro: $${s.insuranceMonthlyFee}. ")
                if (s.notes.isNotBlank()) append("\n${s.notes}")
            }.trim().ifBlank { null }

            val result = createCreditCardUseCase(
                userId = userId,
                name = nameToSave,
                bankId = "bank_${s.creditorBank.lowercase().replace(" ", "_")}",
                bankName = s.creditorBank,
                currencyCode = s.currencyCode,
                franchise = s.franchise,
                category = s.category,
                quotaTotal = s.totalQuota,
                quotaAvailable = s.availableQuota,
                quotaUsed = s.usedQuota,
                quotaRetained = s.retainedQuota,
                cutOffDay = s.cutOffDay,
                paymentDueDay = s.paymentDueDay,
                interestRateEa = s.interestRateEa,
                purchasesRateEa = s.purchasesRateEa,
                advancesRateEa = s.advancesRateEa,
                hasGracePeriod = s.hasGracePeriod,
                lateInterestRateMonthly = s.lateInterestRateMonthly,
                currentInterestAmount = s.currentInterestAmount,
                paymentStrategy = if (s.minimumPaymentEnabled) "MINIMUM_PAYMENT" else "TOTAL_PAYMENT",
                minimumPaymentAmount = s.minimumPaymentAmount,
                autoPayEnabled = s.paymentMethod == "Débito automático",
                autoPaySourceAccountId = s.sourceAccountId,
                autoPaySourceAccountName = s.sourceAccount,
                customPayDay = s.regularPaymentDay,
                scheduledPayEnabled = s.scheduledPaymentEnabled,
                scheduledPayDate = s.scheduledPaymentDate,
                extraPaymentsEnabled = s.extraPaymentsEnabled,
                extraPaymentReduces = s.extraPaymentReduces,
                totalInstallments = s.totalInstallments,
                paidInstallments = s.paidInstallments,
                installmentValue = s.installmentValue,
                amortizationSystem = s.amortizationSystem,
                includeInNetWorth = s.includeInNetWorth,
                financesAsset = s.financesAsset,
                relatedAssetName = if (s.financesAsset) s.relatedAsset else null,
                relatedAssetValue = if (s.financesAsset) s.assetValue else 0.0,
                isFavorite = s.isFavorite,
                supportingDocumentUri = s.attachedDocumentUri,
                supportingDocumentType = s.attachedDocumentType,
                notes = combinedNotes
            )

            result.onSuccess { (createdAccount, createdCc) ->
                if (context != null && s.remindersEnabled) {
                    try {
                        val amount = if (s.installmentValue > 0) s.installmentValue else (if (s.usedQuota > 0) s.usedQuota else s.totalQuota)
                        com.example.util.DebtReminderNotificationManager.scheduleReminder(
                            context = context,
                            debtId = createdAccount.id,
                            debtName = createdAccount.name,
                            amount = amount,
                            dueDay = s.paymentDueDay,
                            daysAhead = s.reminderDaysAhead,
                            hourOfDay = s.reminderHour,
                            minute = s.reminderMinute
                        )
                    } catch (t: Throwable) {
                        android.util.Log.w("CreditCardFormViewModel", "Could not schedule reminder: ${t.message}")
                    }
                }
                _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
            }.onFailure { ex ->
                _uiState.update { it.copy(isSaving = false, errorMessage = ex.message ?: "Error al guardar tarjeta de crédito") }
            }
        }
    }

    companion object {
        fun provideFactory(
            createCreditCardUseCase: CreateCreditCardUseCase,
            validateCreditLimitsUseCase: ValidateCreditLimitsUseCase,
            calculateNextBillingDatesUseCase: CalculateNextBillingDatesUseCase,
            financialRepository: FinancialRepository,
            userId: String = "kash_user_01"
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return CreditCardFormViewModel(
                    createCreditCardUseCase = createCreditCardUseCase,
                    validateCreditLimitsUseCase = validateCreditLimitsUseCase,
                    calculateNextBillingDatesUseCase = calculateNextBillingDatesUseCase,
                    financialRepository = financialRepository,
                    userId = userId
                ) as T
            }
        }
    }
}
