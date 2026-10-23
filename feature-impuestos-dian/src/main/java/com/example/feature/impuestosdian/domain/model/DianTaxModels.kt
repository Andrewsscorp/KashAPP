package com.example.feature.impuestosdian.domain.model

enum class TaxAlertLevel {
    SAFE,       // < 70% del tope (Verde / Azul Seguro)
    WARNING,    // Entre 70% y 99.9% del tope (Ámbar — Alerta preventiva)
    EXCEEDED    // >= 100% del tope (Rojo — Obligado a declarar por este tope)
}

enum class DianCriterionType {
    PATRIMONIO_BRUTO,
    INGRESOS_BRUTOS,
    CONSUMOS_TARJETA_CREDITO,
    COMPRAS_Y_CONSUMOS_TOTALES,
    CONSIGNACIONES_E_INVERSIONES
}

/**
 * Perfil de Configuración Tributaria vinculado transversalmente con:
 * - Creación/Edición de Cuentas (`AccountEntity.isTaxExempt`, topes mensuales y tasas bancarias)
 * - Creación de Gastos e Ingresos (`TransactionEntity`, IVA/IGV/Sales Tax y Factura Electrónica)
 * - Activos Fijos (`:feature-activos`) e Inversiones (`InvestmentEntity`)
 * - Informes Financieros (`:feature-informes`)
 */
data class TaxConfigurationProfile(
    val userId: String,
    val jurisdiction: TaxJurisdiction = TaxJurisdiction.CO_DIAN,
    val fiscalYear: Int = 2026,
    val currency: String = jurisdiction.defaultCurrency,
    val taxUnitName: String = jurisdiction.taxUnitName,
    val taxUnitValue: Double = jurisdiction.defaultTaxUnitValue,
    val taxIdLastDigits: String = "01",
    val vatOrSalesTaxRate: Double = jurisdiction.standardVatOrSalesTaxRate,
    val financialTransactionTaxRate: Double = jurisdiction.financialTransactionTaxRate,
    val electronicInvoiceDeductionRate: Double = jurisdiction.electronicInvoiceDeductionRate,
    val effectiveIncomeTaxRate: Double = jurisdiction.effectiveIncomeTaxEstimateRate,
    val enforceSingleExemptAccountInColombia: Boolean = true,
    val autoTagOcrReceiptsAsElectronicInvoice: Boolean = true,
    val dependentsCount: Int = 0,
    val mandatoryIncrgoRate: Double = ColombianUvtCatalog.DEFAULT_MANDATORY_INCRGO_RATE,
    val useProgressiveCedulaGeneralInColombia: Boolean = false
)

/**
 * Guía dinámica inyectada en el formulario de Creación/Edición de Cuentas.
 */
data class AccountTaxCreationHint(
    val jurisdictionCode: String,
    val switchLabel: String,
    val switchSubtitle: String,
    val enforcesSingleExemptAccountRule: Boolean,
    val financialTaxRate: Double
)

/**
 * Vista previa de impacto fiscal al registrar un movimiento en Gastos/Ingresos/OCR.
 */
data class TransactionTaxImpactPreview(
    val estimatedIncludedVatAmount: Double,
    val estimatedBankTransactionTaxAmount: Double,
    val estimatedTaxDeductionBenefitAmount: Double,
    val summaryMessage: String
)

data class DianThresholdStatus(
    val type: DianCriterionType,
    val title: String,
    val legalReference: String,
    val accumulatedCop: Double,
    val accumulatedUvt: Double,
    val limitUvt: Double,
    val limitCop: Double,
    val consumptionPercentage: Double,
    val remainingCopBeforeLimit: Double,
    val alertLevel: TaxAlertLevel
)

data class AccountGmfMonthlyStatus(
    val accountId: String,
    val accountName: String,
    val bankName: String,
    val accountType: String,
    val isMarkedTaxExempt350Uvt: Boolean,
    val isLowAmountDigitalWallet65Uvt: Boolean,
    val monthlyOutflowsCop: Double,
    val monthlyOutflowsUvt: Double,
    val exemptLimitUvt: Double,
    val exemptLimitCop: Double,
    val remainingExemptCop: Double,
    val taxableExcessCop: Double,
    val estimatedGmfTaxPaidCop: Double,
    val gmfAlertLevel: TaxAlertLevel,
    val sharesUnified350UvtPool: Boolean = false
)

data class ElectronicInvoiceBenefitSummary(
    val eligiblePurchasesTotalCop: Double,
    val deductionOnePercentCop: Double,
    val deductionOnePercentUvt: Double,
    val maxAllowedDeductionUvt: Double = ColombianUvtCatalog.ELECTRONIC_INVOICE_MAX_DEDUCTION_UVT,
    val maxAllowedDeductionCop: Double,
    val appliedDeductionCop: Double,
    val remainingBenefitCop: Double,
    val eligibleTransactionsCount: Int
)

/**
 * Resumen de Impuestos Generales aplicable tanto a Colombia como a cualquier país seleccionado
 * (IVA/VAT/Sales Tax en compras, Retención en inversiones, Renta estimada y Carga Fiscal Total).
 */
data class InternationalTaxSummary(
    val jurisdiction: TaxJurisdiction,
    val currency: String,
    val taxUnitName: String,
    val estimatedVatOrSalesTaxPaid: Double,
    val vatRateApplied: Double,
    val estimatedInvestmentWithholdingTax: Double,
    val deductibleBankTaxFiftyPercent: Double,
    val totalAllowableDeductions: Double,
    val estimatedTaxableNetIncome: Double,
    val estimatedAnnualIncomeTax: Double,
    val totalEstimatedTaxBurden: Double,
    val effectiveTaxPressurePercentage: Double,
    val mandatoryIncrgoAmount: Double = 0.0,
    val laborExempt25PercentAmount: Double = 0.0,
    val dependentsDeductionAmount: Double = 0.0,
    val cappedGeneralDeductionsAmount: Double = 0.0
)

data class DianTaxRadarReport(
    val userId: String,
    val fiscalYear: Int,
    val currentYearMonth: String,
    val uvtValueCop: Double,
    val lastTwoDigitsNitOrCedula: String,
    val estimatedFilingDeadlineDescription: String,
    val thresholds: List<DianThresholdStatus>,
    val isObligatedToDeclareRenta: Boolean,
    val exceededThresholdsCount: Int,
    val warningThresholdsCount: Int,
    val accountGmfStatuses: List<AccountGmfMonthlyStatus>,
    val totalMonthlyGmfTaxCop: Double,
    val totalAnnualGmfTaxCop: Double = totalMonthlyGmfTaxCop,
    val hasMarked350UvtAccount: Boolean,
    val optimizationRecommendation: String,
    val electronicInvoiceBenefit: ElectronicInvoiceSummaryAlias,
    val configProfile: TaxConfigurationProfile = TaxConfigurationProfile(
        userId = userId,
        fiscalYear = fiscalYear,
        taxUnitValue = uvtValueCop,
        taxIdLastDigits = lastTwoDigitsNitOrCedula
    ),
    val internationalTaxSummary: InternationalTaxSummary? = null
)

typealias ElectronicInvoiceSummaryAlias = ElectronicInvoiceBenefitSummary
