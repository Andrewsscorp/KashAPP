package com.example.domain.models

import java.math.BigDecimal

enum class FinancialRubroType(
    val title: String,
    val subtitle: String,
    val defaultIconName: String,
    val primaryColorHex: String,
    val bgSoftColorHex: String,
    val categoryLabel: String
) {
    AVAILABLE_CASH(
        title = "Dinero Disponible",
        subtitle = "Liquidez inmediata, cuentas bancarias y efectivo",
        defaultIconName = "account_balance",
        primaryColorHex = "#0284C7",
        bgSoftColorHex = "#E0F2FE",
        categoryLabel = "Tipo de Cuenta"
    ),
    INVESTMENTS(
        title = "Inversiones y CDTs",
        subtitle = "Renta fija, CDTs, fondos de inversión y acciones",
        defaultIconName = "trending_up",
        primaryColorHex = "#8B5CF6",
        bgSoftColorHex = "#F3E8FF",
        categoryLabel = "Tipo de Inversión"
    ),
    DEBTS(
        title = "Deudas y Pasivos",
        subtitle = "Tarjetas de crédito diferidas, préstamos y obligaciones",
        defaultIconName = "credit_card",
        primaryColorHex = "#EF4444",
        bgSoftColorHex = "#FEE2E2",
        categoryLabel = "Tipo de Obligación"
    ),
    ASSETS(
        title = "Activos Totales",
        subtitle = "Patrimonio neto, bienes tangibles e intangibles",
        defaultIconName = "account_balance_wallet",
        primaryColorHex = "#10B981",
        bgSoftColorHex = "#DCFCE7",
        categoryLabel = "Clase de Activo"
    )
}

data class CardSubPurchaseItem(
    val id: String,
    val cardId: String,
    val cardName: String,
    val merchant: String,
    val concept: String,
    val originalAmount: Double,
    val currentBalance: Double,
    val totalInstallments: Int,
    val paidInstallments: Int,
    val rateEa: Double,
    val monthlyPayment: Double,
    val totalInterestEstimated: Double,
    val interestPaid: Double,
    val nextPaymentDueDate: String,
    val category: String,
    val dateFormatted: String
)

data class AccountMovementDisplay(
    val id: String,
    val title: String,
    val category: String,
    val dateFormatted: String,
    val amount: Double,
    val percentageOfTotal: Double,
    val details: String = ""
)

data class RubroItemDisplay(
    val id: String,
    val name: String,
    val institution: String,
    val category: String,
    val amount: BigDecimal,
    val currency: String,
    val rateEa: Double? = null,
    val quotaLimit: Double? = null,
    val availableQuota: Double? = null,
    val status: String = "Activa",
    val colorHex: String = "#0284C7",
    val subPurchases: List<CardSubPurchaseItem> = emptyList(),
    val accountMovements: List<AccountMovementDisplay> = emptyList()
)
