package com.example.feature.impuestosdian.domain.engine

import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.ElectronicInvoiceBenefitSummary
import com.example.feature.impuestosdian.domain.model.InternationalTaxSummary
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlin.math.max

/**
 * Motor de Impuestos Generales Multi-País (Colombia DIAN, México SAT, EE.UU. IRS, España AEAT,
 * Perú SUNAT, Chile SII, Argentina ARCA e Internacional Personalizado).
 */
class InternationalTaxEngine {

    private val exemptVatCategoryKeywords = listOf(
        "salud", "educación", "educacion", "arriendo", "hipoteca", "servicios públicos", "servicios publicos", "nómina", "nomina", "impuesto"
    )

    fun evaluateGeneralTaxes(
        profile: TaxConfigurationProfile,
        transactionsInYear: List<TransactionEntity>,
        investments: List<InvestmentEntity>,
        annualBankTaxPaid: Double,
        electronicInvoiceBenefit: ElectronicInvoiceBenefitSummary
    ): InternationalTaxSummary {
        val yearPrefix = "${profile.fiscalYear}-"
        val validTx = transactionsInYear.filter {
            it.date.startsWith(yearPrefix) &&
                !it.categoryId.equals("cat_initial_balance", ignoreCase = true) &&
                !it.categoryId.equals("cat_opening_balance", ignoreCase = true) &&
                !it.id.startsWith("tx_init_", ignoreCase = true) &&
                it.transferGroupId.isNullOrBlank()
        }

        val totalIncome = validTx
            .filter { it.type.equals("INCOME", ignoreCase = true) }
            .sumOf { max(0.0, it.amount) }

        val taxableVatExpenses = validTx
            .filter { tx ->
                tx.type.equals("EXPENSE", ignoreCase = true) &&
                    exemptVatCategoryKeywords.none { kw -> tx.categoryName.lowercase().contains(kw) }
            }
            .sumOf { max(0.0, it.amount) }

        val vatRate = profile.vatOrSalesTaxRate.coerceAtLeast(0.0)
        // IVA implícito = Gasto - (Gasto / (1 + tasaIVA))
        val estimatedVatPaid = if (vatRate > 0.0) {
            taxableVatExpenses - (taxableVatExpenses / (1.0 + vatRate))
        } else {
            0.0
        }

        // Rendimientos positivos en inversiones activas
        val positiveInvestmentYields = investments
            .filter { it.status.equals("ACTIVE", ignoreCase = true) }
            .sumOf { inv -> max(0.0, inv.currentValue - inv.initialAmount) }

        val withholdingRate = profile.jurisdiction.investmentYieldWithholdingRate
        val estimatedInvestmentWithholding = positiveInvestmentYields * withholdingRate

        // En Colombia el 50% del 4x1000 certificado es deducible en Renta (Art. 115 E.T.)
        val deductibleBankTax = if (profile.jurisdiction == TaxJurisdiction.CO_DIAN) {
            annualBankTaxPaid * ColombianUvtCatalog.GMF_DEDUCTIBLE_RENTA_PERCENTAGE
        } else {
            0.0
        }

        val totalAllowableDeductions = electronicInvoiceBenefit.appliedDeductionCop + deductibleBankTax
        val estimatedTaxableNetIncome = max(0.0, totalIncome - totalAllowableDeductions)

        // Exención base anual antes de aplicar tasa efectiva estimada de impuesto de renta
        val annualExemptIncomeAmount = profile.jurisdiction.annualIncomeFilingThresholdUnits * profile.taxUnitValue
        val netIncomeAboveThreshold = max(0.0, estimatedTaxableNetIncome - annualExemptIncomeAmount)
        val estimatedAnnualIncomeTax = netIncomeAboveThreshold * profile.effectiveIncomeTaxRate.coerceAtLeast(0.0)

        val totalTaxBurden = estimatedVatPaid + annualBankTaxPaid + estimatedInvestmentWithholding + estimatedAnnualIncomeTax
        val effectiveTaxPressurePct = if (totalIncome > 0.0) {
            (totalTaxBurden / totalIncome) * 100.0
        } else {
            0.0
        }

        return InternationalTaxSummary(
            jurisdiction = profile.jurisdiction,
            currency = profile.currency,
            taxUnitName = profile.taxUnitName,
            estimatedVatOrSalesTaxPaid = estimatedVatPaid,
            vatRateApplied = vatRate,
            estimatedInvestmentWithholdingTax = estimatedInvestmentWithholding,
            deductibleBankTaxFiftyPercent = deductibleBankTax,
            totalAllowableDeductions = totalAllowableDeductions,
            estimatedTaxableNetIncome = estimatedTaxableNetIncome,
            estimatedAnnualIncomeTax = estimatedAnnualIncomeTax,
            totalEstimatedTaxBurden = totalTaxBurden,
            effectiveTaxPressurePercentage = effectiveTaxPressurePct
        )
    }
}
