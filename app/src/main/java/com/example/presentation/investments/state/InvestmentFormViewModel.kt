package com.example.presentation.investments.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.entity.BankEntity
import com.example.data.local.entity.InvestmentDocumentEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.domain.repository.FinancialRepository
import com.example.domain.usecase.CalculateInvestmentRoiUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.UUID

class InvestmentFormViewModel(
    private val financialRepository: FinancialRepository,
    private val calculateRoiUseCase: CalculateInvestmentRoiUseCase = CalculateInvestmentRoiUseCase()
) : ViewModel() {

    private val _uiState = MutableStateFlow(InvestmentFormState())
    val uiState: StateFlow<InvestmentFormState> = _uiState.asStateFlow()

    init {
        setupDefaultDates()
        loadAccountsAndBanks()
    }

    private fun setupDefaultDates() {
        val today = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        _uiState.update {
            it.copy(
                acquisitionDate = today,
                valueUpdateDate = today
            )
        }
    }

    private fun loadAccountsAndBanks() {
        viewModelScope.launch {
            financialRepository.getAccounts("user_default").collect { accounts ->
                _uiState.update { state ->
                    val defaultOrigin = if (state.originAccountId.isBlank()) {
                        accounts.firstOrNull { it.type.equals("SAVINGS", ignoreCase = true) || it.type.equals("CHECKING", ignoreCase = true) }
                            ?: accounts.firstOrNull()
                    } else null

                    state.copy(
                        availableAccounts = accounts,
                        originAccountId = defaultOrigin?.id ?: state.originAccountId,
                        originAccountName = defaultOrigin?.name ?: state.originAccountName
                    )
                }
                recalculate()
            }
        }

        viewModelScope.launch {
            financialRepository.getBanks("user_default").collect { banks ->
                _uiState.update { state ->
                    val defaultBank = if (state.institutionId == null) {
                        banks.firstOrNull { it.name.contains("Bancolombia", ignoreCase = true) } ?: banks.firstOrNull()
                    } else null

                    state.copy(
                        availableInstitutions = banks,
                        institutionId = defaultBank?.id ?: state.institutionId,
                        institutionName = defaultBank?.name ?: state.institutionName
                    )
                }
            }
        }
    }

    fun onNameChanged(newName: String) {
        _uiState.update { it.copy(name = newName.take(100)) }
        recalculate()
    }

    fun onTypeSelected(typeId: String, typeName: String, defaultIcon: String) {
        _uiState.update {
            it.copy(
                typeId = typeId,
                typeName = typeName,
                iconName = defaultIcon
            )
        }
        recalculate()
    }

    fun onInstitutionSelected(institutionId: String?, institutionName: String) {
        _uiState.update {
            it.copy(
                institutionId = institutionId,
                institutionName = institutionName
            )
        }
    }

    fun onCurrencySelected(currencyCode: String) {
        _uiState.update { it.copy(currencyCode = currencyCode) }
        recalculate()
    }

    fun onAcquisitionDateSelected(date: String) {
        _uiState.update { it.copy(acquisitionDate = date) }
    }

    fun onStatusSelected(status: String) {
        _uiState.update { it.copy(status = status) }
    }

    fun onGeneratesYieldChanged(enabled: Boolean) {
        _uiState.update { it.copy(generatesYield = enabled) }
        recalculate()
    }

    fun onRateTypeSelected(rateType: String) {
        _uiState.update { it.copy(rateType = rateType) }
    }

    fun onYieldPeriodicitySelected(periodicity: String) {
        _uiState.update { it.copy(yieldPeriodicity = periodicity) }
    }

    fun onValueUpdateDateSelected(date: String) {
        _uiState.update { it.copy(valueUpdateDate = date) }
    }

    fun onOriginAccountSelected(accountId: String, accountName: String) {
        _uiState.update {
            it.copy(
                originAccountId = accountId,
                originAccountName = accountName
            )
        }
        recalculate()
    }

    fun onYieldAccountSelected(accountId: String?, accountName: String?) {
        _uiState.update {
            it.copy(
                yieldAccountId = accountId,
                yieldAccountName = accountName
            )
        }
    }

    fun onIncludeInNetWorthChanged(include: Boolean) {
        _uiState.update { it.copy(includeInNetWorth = include) }
        recalculate()
    }

    fun onDocumentSelected(uri: String?, name: String?, size: Long) {
        _uiState.update {
            it.copy(
                selectedDocUri = uri,
                selectedDocName = name,
                selectedDocSize = size
            )
        }
    }

    fun onDocumentCategorySelected(category: String) {
        _uiState.update { it.copy(selectedDocCategory = category) }
    }

    fun onIconSelected(iconName: String) {
        _uiState.update { it.copy(iconName = iconName) }
    }

    fun onColorSelected(colorHex: String) {
        _uiState.update { it.copy(colorHex = colorHex) }
    }

    fun onFavoriteChanged(isFav: Boolean) {
        _uiState.update { it.copy(isFavorite = isFav) }
    }

    fun onTagsChanged(tags: String) {
        _uiState.update { it.copy(tags = tags.take(200)) }
    }

    fun onNotesChanged(notes: String) {
        _uiState.update { it.copy(notes = notes.take(500)) }
    }

    // In-App Keyboard Handling
    fun openKeyboard(target: InvestmentKeyboardTarget) {
        val initialAmount = when (target) {
            is InvestmentKeyboardTarget.InitialAmount -> _uiState.value.initialAmount
            is InvestmentKeyboardTarget.CurrentValue -> _uiState.value.currentValue
            is InvestmentKeyboardTarget.Contributions -> _uiState.value.contributions
            is InvestmentKeyboardTarget.Withdrawals -> _uiState.value.withdrawals
            is InvestmentKeyboardTarget.YieldRate -> _uiState.value.yieldRate
            is InvestmentKeyboardTarget.NetWorthValue -> _uiState.value.netWorthValue
        }
        val buffer = if (initialAmount > 0.0) {
            if (target is InvestmentKeyboardTarget.YieldRate) initialAmount.toString() else initialAmount.toLong().toString()
        } else ""

        _uiState.update {
            it.copy(
                activeKeyboardTarget = target,
                numericInputBuffer = buffer
            )
        }
    }

    fun closeKeyboard() {
        _uiState.update {
            it.copy(
                activeKeyboardTarget = null,
                numericInputBuffer = ""
            )
        }
    }

    fun onDigitClick(digit: String) {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentBuffer = _uiState.value.numericInputBuffer
        if (currentBuffer.length >= 12) return

        val newBuffer = if (currentBuffer == "0" && digit != "00") digit else currentBuffer + digit
        val parsedValue = newBuffer.toDoubleOrNull() ?: 0.0
        applyTargetValue(target, parsedValue, newBuffer)
    }

    fun onQuickAdd(amountToAdd: Double) {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentValue = when (target) {
            is InvestmentKeyboardTarget.InitialAmount -> _uiState.value.initialAmount
            is InvestmentKeyboardTarget.CurrentValue -> _uiState.value.currentValue
            is InvestmentKeyboardTarget.Contributions -> _uiState.value.contributions
            is InvestmentKeyboardTarget.Withdrawals -> _uiState.value.withdrawals
            is InvestmentKeyboardTarget.YieldRate -> _uiState.value.yieldRate
            is InvestmentKeyboardTarget.NetWorthValue -> _uiState.value.netWorthValue
        }
        val newValue = currentValue + amountToAdd
        val newBuffer = newValue.toLong().toString()
        applyTargetValue(target, newValue, newBuffer)
    }

    fun onBackspace() {
        val target = _uiState.value.activeKeyboardTarget ?: return
        val currentBuffer = _uiState.value.numericInputBuffer
        val newBuffer = if (currentBuffer.isNotEmpty()) currentBuffer.dropLast(1) else ""
        val parsedValue = newBuffer.toDoubleOrNull() ?: 0.0
        applyTargetValue(target, parsedValue, newBuffer)
    }

    fun onClear() {
        val target = _uiState.value.activeKeyboardTarget ?: return
        applyTargetValue(target, 0.0, "")
    }

    private fun applyTargetValue(target: InvestmentKeyboardTarget, value: Double, buffer: String) {
        _uiState.update { state ->
            when (target) {
                is InvestmentKeyboardTarget.InitialAmount -> {
                    // Auto-suggest current value if it was zero
                    val updatedCurrentVal = if (state.currentValue <= 0.0) value else state.currentValue
                    state.copy(
                        initialAmount = value,
                        currentValue = updatedCurrentVal,
                        numericInputBuffer = buffer
                    )
                }
                is InvestmentKeyboardTarget.CurrentValue -> state.copy(currentValue = value, numericInputBuffer = buffer)
                is InvestmentKeyboardTarget.Contributions -> state.copy(contributions = value, numericInputBuffer = buffer)
                is InvestmentKeyboardTarget.Withdrawals -> state.copy(withdrawals = value, numericInputBuffer = buffer)
                is InvestmentKeyboardTarget.YieldRate -> state.copy(yieldRate = value, numericInputBuffer = buffer)
                is InvestmentKeyboardTarget.NetWorthValue -> state.copy(netWorthValue = value, numericInputBuffer = buffer)
            }
        }
        recalculate()
    }

    private fun recalculate() {
        val state = _uiState.value
        val roi = calculateRoiUseCase(
            initialAmount = state.initialAmount,
            currentValue = state.currentValue,
            contributions = state.contributions,
            withdrawals = state.withdrawals
        )

        val netWorth = if (state.currentValue > 0.0) state.currentValue else state.initialAmount
        val isValid = state.name.trim().isNotBlank() &&
                state.initialAmount > 0.0 &&
                state.originAccountId.isNotBlank()

        _uiState.update {
            it.copy(
                roiResult = roi,
                netWorthValue = netWorth,
                isFormSubmittable = isValid
            )
        }
    }

    fun openCreateInstitutionDialog(open: Boolean) {
        _uiState.update { it.copy(isCreateInstitutionDialogOpen = open) }
    }

    fun createCustomInstitution(name: String, clearingCode: String) {
        if (name.isBlank()) return
        viewModelScope.launch {
            val code = clearingCode.ifBlank { name.take(4).uppercase() }
            val result = financialRepository.createBank(
                userId = "user_default",
                name = name.trim(),
                clearingCode = code,
                countryCode = "CO",
                currencyCode = "COP"
            )
            result.onSuccess { bank ->
                _uiState.update {
                    it.copy(
                        institutionId = bank.id,
                        institutionName = bank.name,
                        isCreateInstitutionDialogOpen = false
                    )
                }
            }
        }
    }

    fun saveInvestment() {
        val state = _uiState.value
        if (state.isSaving) return

        val validation = com.example.domain.validation.FormValidator.validateInvestment(
            name = state.name,
            initialAmount = state.initialAmount,
            originAccountId = state.originAccountId,
            typeName = state.typeName
        )
        if (validation is com.example.domain.validation.FormValidator.ValidationResult.Invalid) {
            _uiState.update { it.copy(errorMessage = validation.firstError) }
            return
        }

        val cleanName = com.example.domain.validation.FormValidator.sanitizeConcept(state.name)
        val cleanInstitution = com.example.domain.validation.FormValidator.sanitizeConcept(state.institutionName)
        val cleanCurrency = com.example.domain.validation.FormValidator.sanitizeCurrency(state.currencyCode)
        val cleanNotes = com.example.domain.validation.FormValidator.sanitizeDescription(state.notes)
        val cleanTags = com.example.domain.validation.FormValidator.sanitizeTags(state.tags.split(",")).joinToString(",")
        val cleanColorHex = com.example.domain.validation.FormValidator.sanitizeColorHex(state.colorHex, "#0284C7")

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, errorMessage = null) }
            val investmentId = UUID.randomUUID().toString()

            val investment = InvestmentEntity(
                id = investmentId,
                userId = "user_default",
                name = cleanName,
                typeId = state.typeId,
                typeName = state.typeName,
                institutionId = state.institutionId,
                institutionName = cleanInstitution,
                currencyCode = cleanCurrency,
                acquisitionDate = state.acquisitionDate,
                status = state.status,
                initialAmount = state.initialAmount,
                currentValue = if (state.currentValue > 0.0) state.currentValue else state.initialAmount,
                contributions = state.contributions,
                withdrawals = state.withdrawals,
                generatesYield = state.generatesYield,
                yieldRate = if (state.generatesYield) state.yieldRate else null,
                rateType = if (state.generatesYield) state.rateType else null,
                yieldPeriodicity = if (state.generatesYield) state.yieldPeriodicity else null,
                valueUpdateDate = state.valueUpdateDate,
                originAccountId = state.originAccountId,
                originAccountName = state.originAccountName,
                yieldAccountId = state.yieldAccountId,
                yieldAccountName = state.yieldAccountName,
                includeInNetWorth = state.includeInNetWorth,
                netWorthValue = state.netWorthValue,
                iconName = state.iconName,
                colorHex = cleanColorHex,
                isFavorite = state.isFavorite,
                tags = cleanTags,
                notes = cleanNotes.takeIf { it.isNotBlank() },
                documentUri = state.selectedDocUri,
                documentCategory = state.selectedDocCategory
            )

            val document = state.selectedDocUri?.let { uri ->
                InvestmentDocumentEntity(
                    id = UUID.randomUUID().toString(),
                    investmentId = investmentId,
                    docCategory = state.selectedDocCategory,
                    fileUrl = uri,
                    fileName = state.selectedDocName ?: "comprobante_inversion.pdf",
                    fileSize = state.selectedDocSize
                )
            }

            val result = financialRepository.createInvestment(investment, document)
            result.fold(
                onSuccess = {
                    _uiState.update { it.copy(isSaving = false, saveSuccess = true) }
                },
                onFailure = { ex ->
                    _uiState.update { it.copy(isSaving = false, errorMessage = ex.message ?: "Error al guardar la inversión.") }
                }
            )
        }
    }

    companion object {
        fun provideFactory(financialRepository: FinancialRepository): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    return InvestmentFormViewModel(financialRepository) as T
                }
            }
    }
}
