package com.example.feature.impuestosdian.domain.engine

import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.ElectronicInvoiceBenefitSummary
import com.example.feature.impuestosdian.domain.model.InternationalTaxSummary
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlin.math.max
import kotlin.math.min

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
        val estimatedVatPaid = if (vatRate > 0.0) {
            taxableVatExpenses - (taxableVatExpenses / (1.0 + vatRate))
        } else {
            0.0
        }

        val positiveInvestmentYields = investments
            .filter { it.status.equals("ACTIVE", ignoreCase = true) }
            .sumOf { inv -> max(0.0, inv.currentValue - inv.initialAmount) }

        val withholdingRate = profile.jurisdiction.investmentYieldWithholdingRate
        val estimatedInvestmentWithholding = positiveInvestmentYields * withholdingRate

        val deductibleBankTax = if (profile.jurisdiction == TaxJurisdiction.CO_DIAN) {
            annualBankTaxPaid * ColombianUvtCatalog.GMF_DEDUCTIBLE_RENTA_PERCENTAGE
        } else {
            0.0
        }

        val safeUvt = profile.taxUnitValue.coerceAtLeast(1.0)

        var mandatoryIncrgo = 0.0
        var laborExempt25 = 0.0
        var dependentsDeduction = 0.0
        var cappedGeneralDeductions = 0.0
        val totalAllowableDeductions: Double
        val estimatedTaxableNetIncome: Double
        val estimatedAnnualIncomeTax: Double

        if (profile.jurisdiction == TaxJurisdiction.CO_DIAN && profile.useProgressiveCedulaGeneralInColombia) {
            // 1. Ingresos No Constitutivos de Renta (INCRGO: Salud + Pensión obligatoria)
            mandatoryIncrgo = totalIncome * profile.mandatoryIncrgoRate.coerceIn(0.0, 0.30)
            val netIncomeAfterIncrgo = max(0.0, totalIncome - mandatoryIncrgo)

            // 2. Renta exenta laboral del 25% (Art. 206 Num. 10 E.T., tope 790 UVT)
            val baseFor25Percent = max(0.0, netIncomeAfterIncrgo - deductibleBankTax)
            val max790UvtCop = ColombianUvtCatalog.LABOR_EXEMPT_INCOME_MAX_UVT * safeUvt
            laborExempt25 = min(baseFor25Percent * ColombianUvtCatalog.LABOR_EXEMPT_INCOME_RATE, max790UvtCop)

            // 3. Límite global del 40% o 1.340 UVT (Art. 336 E.T. - Ley 2277/2022)
            val subjectToCap = deductibleBankTax + laborExempt25
            val fortyPercentCap = netIncomeAfterIncrgo * ColombianUvtCatalog.CEDULA_GENERAL_GLOBAL_CAP_RATE
            val max1340UvtCop = ColombianUvtCatalog.CEDULA_GENERAL_GLOBAL_MAX_UVT * safeUvt
            cappedGeneralDeductions = min(subjectToCap, min(fortyPercentCap, max1340UvtCop))

            // 4. Deducciones especiales que NO entran en el límite del 40% (1% Factura Electrónica y 72 UVT por dependiente)
            val validDependents = profile.dependentsCount.coerceIn(0, ColombianUvtCatalog.MAX_DEPENDENTS_COUNT)
            dependentsDeduction = validDependents * ColombianUvtCatalog.DEPENDENT_DEDUCTION_UVT_PER_PERSON * safeUvt

            val outsideCapDeductions = electronicInvoiceBenefit.appliedDeductionCop + dependentsDeduction
            totalAllowableDeductions = mandatoryIncrgo + cappedGeneralDeductions + outsideCapDeductions
            estimatedTaxableNetIncome = max(0.0, totalIncome - totalAllowableDeductions)

            // 5. Impuesto progresivo Art. 241 E.T. (0% hasta 1.090 UVT)
            val taxableUvt = estimatedTaxableNetIncome / safeUvt
            val taxInUvt = ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(taxableUvt)
            estimatedAnnualIncomeTax = taxInUvt * safeUvt
        } else {
            totalAllowableDeductions = electronicInvoiceBenefit.appliedDeductionCop + deductibleBankTax
            estimatedTaxableNetIncome = max(0.0, totalIncome - totalAllowableDeductions)
            val annualExemptIncomeAmount = profile.jurisdiction.annualIncomeFilingThresholdUnits * profile.taxUnitValue
            val netIncomeAboveThreshold = max(0.0, estimatedTaxableNetIncome - annualExemptIncomeAmount)
            estimatedAnnualIncomeTax = netIncomeAboveThreshold * profile.effectiveIncomeTaxRate.coerceAtLeast(0.0)
        }

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
            effectiveTaxPressurePercentage = effectiveTaxPressurePct,
            mandatoryIncrgoAmount = mandatoryIncrgo,
            laborExempt25PercentAmount = laborExempt25,
            dependentsDeductionAmount = dependentsDeduction,
            cappedGeneralDeductionsAmount = cappedGeneralDeductions
        )
    }
}
