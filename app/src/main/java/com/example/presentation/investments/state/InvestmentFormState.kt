package com.example.presentation.investments.state

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.BankEntity
import com.example.domain.usecase.InvestmentRoiResult

sealed class InvestmentKeyboardTarget {
    object InitialAmount : InvestmentKeyboardTarget()
    object CurrentValue : InvestmentKeyboardTarget()
    object Contributions : InvestmentKeyboardTarget()
    object Withdrawals : InvestmentKeyboardTarget()
    object YieldRate : InvestmentKeyboardTarget()
    object NetWorthValue : InvestmentKeyboardTarget()
}

data class InvestmentTypeOption(
    val id: String,
    val name: String,
    val iconName: String
)

data class InvestmentFormState(
    // 1. Información básica
    val name: String = "",
    val typeId: String = "ACCIONES",
    val typeName: String = "Acciones",
    val institutionId: String? = null,
    val institutionName: String = "Bancolombia",
    val currencyCode: String = "COP",
    val acquisitionDate: String = "2026-10-01",
    val status: String = "ACTIVE", // "ACTIVE", "CLOSED", "PAUSED"

    // 2. Capital
    val initialAmount: Double = 0.0,
    val currentValue: Double = 0.0,
    val contributions: Double = 0.0,
    val withdrawals: Double = 0.0,
    val roiResult: InvestmentRoiResult? = null,

    // 3. Rendimiento
    val generatesYield: Boolean = false,
    val yieldRate: Double = 0.0,
    val rateType: String = "EA", // "EA", "NOMINAL", "MENSUAL", "FIJA", "VARIABLE"
    val yieldPeriodicity: String = "MONTHLY", // "MONTHLY", "QUARTERLY", "SEMIANNUAL", "ANNUALLY", "MATURITY"
    val valueUpdateDate: String = "2026-10-01",

    // 4. Cuenta asociada
    val originAccountId: String = "",
    val originAccountName: String = "",
    val yieldAccountId: String? = null,
    val yieldAccountName: String? = null,
    val availableAccounts: List<AccountEntity> = emptyList(),
    val availableInstitutions: List<BankEntity> = emptyList(),

    // 5. Patrimonio
    val includeInNetWorth: Boolean = true,
    val netWorthValue: Double = 0.0,

    // 6. Documentos
    val selectedDocUri: String? = null,
    val selectedDocName: String? = null,
    val selectedDocSize: Long = 0L,
    val selectedDocCategory: String = "COMPROBANTE", // "COMPROBANTE", "EXTRACTO", "DOCUMENTO", "OTROS"

    // 7. Personalización
    val iconName: String = "trending_up",
    val colorHex: String = "#0284C7",
    val isFavorite: Boolean = false,
    val tags: String = "",
    val notes: String = "",

    // UI & In-App Keyboard
    val activeKeyboardTarget: InvestmentKeyboardTarget? = null,
    val numericInputBuffer: String = "",
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val isCreateInstitutionDialogOpen: Boolean = false,
    val isFormSubmittable: Boolean = false
)
