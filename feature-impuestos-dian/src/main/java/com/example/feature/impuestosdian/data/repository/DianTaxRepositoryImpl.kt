package com.example.feature.impuestosdian.data.repository

import android.content.Context
import com.example.data.local.dao.FinancialDao
import com.example.data.local.dao.FixedAssetDao
import com.example.feature.impuestosdian.domain.engine.DianTaxCalendarCalculator
import com.example.feature.impuestosdian.domain.engine.DianUvtThresholdEngine
import com.example.feature.impuestosdian.domain.engine.GmfFourPerThousandEngine
import com.example.feature.impuestosdian.domain.engine.InternationalTaxEngine
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import java.time.LocalDate
import java.time.YearMonth

/**
 * Gestor reactivo de Configuración Tributaria compartido entre todos los módulos de KashApp
 * (Radar Tributario, Creación/Edición de Cuentas, Registro de Gastos/Ingresos, OCR e Informes).
 */
object TaxConfigurationStore {
    private const val PREFS_NAME = "kashapp_tax_config_prefs"
    private val activeProfileFlow = MutableStateFlow(TaxConfigurationProfile(userId = "user_default"))

    fun observeProfile(context: Context, userId: String): StateFlow<TaxConfigurationProfile> {
        val loaded = loadProfile(context, userId)
        if (activeProfileFlow.value.userId != userId || activeProfileFlow.value != loaded) {
            activeProfileFlow.value = loaded
        }
        return activeProfileFlow.asStateFlow()
    }

    fun loadProfile(context: Context, userId: String): TaxConfigurationProfile {
        val safeUser = userId.ifBlank { "user_default" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val jurisdictionCode = prefs.getString("tax_jurisdiction_$safeUser", TaxJurisdiction.CO_DIAN.code)
        val jurisdiction = TaxJurisdiction.fromCode(jurisdictionCode)
        val fiscalYear = prefs.getInt("tax_fiscal_year_$safeUser", LocalDate.now().year)
        val defaultUnitVal = if (jurisdiction == TaxJurisdiction.CO_DIAN) {
            ColombianUvtCatalog.getDefaultUvtForYear(fiscalYear)
        } else {
            jurisdiction.defaultTaxUnitValue
        }

        val unitValue = prefs.getString("tax_unit_val_${safeUser}_${jurisdiction.code}_$fiscalYear", null)
            ?.toDoubleOrNull() ?: defaultUnitVal
        val currency = prefs.getString("tax_currency_${safeUser}_${jurisdiction.code}", null)
            ?: jurisdiction.defaultCurrency
        val taxUnitName = prefs.getString("tax_unit_name_${safeUser}_${jurisdiction.code}", null)
            ?: jurisdiction.taxUnitName
        val lastTwoDigits = prefs.getString("tax_last_digits_$safeUser", "01") ?: "01"
        val vatRate = prefs.getString("tax_vat_rate_${safeUser}_${jurisdiction.code}", null)
            ?.toDoubleOrNull() ?: jurisdiction.standardVatOrSalesTaxRate
        val bankTaxRate = prefs.getString("tax_bank_rate_${safeUser}_${jurisdiction.code}", null)
            ?.toDoubleOrNull() ?: jurisdiction.financialTransactionTaxRate
        val feRate = prefs.getString("tax_fe_rate_${safeUser}_${jurisdiction.code}", null)
            ?.toDoubleOrNull() ?: jurisdiction.electronicInvoiceDeductionRate
        val incomeTaxRate = prefs.getString("tax_income_rate_${safeUser}_${jurisdiction.code}", null)
            ?.toDoubleOrNull() ?: jurisdiction.effectiveIncomeTaxEstimateRate
        val singleExempt = prefs.getBoolean("tax_single_exempt_$safeUser", jurisdiction == TaxJurisdiction.CO_DIAN)
        val autoTagOcr = prefs.getBoolean("tax_auto_tag_ocr_$safeUser", true)
        val dependentsCount = prefs.getInt("tax_dependents_count_$safeUser", 0).coerceIn(0, ColombianUvtCatalog.MAX_DEPENDENTS_COUNT)
        val mandatoryIncrgoRate = prefs.getString("tax_incrgo_rate_$safeUser", null)
            ?.toDoubleOrNull() ?: ColombianUvtCatalog.DEFAULT_MANDATORY_INCRGO_RATE
        val useProgressiveCedula = prefs.getBoolean("tax_progressive_cedula_$safeUser", false)

        return TaxConfigurationProfile(
            userId = safeUser,
            jurisdiction = jurisdiction,
            fiscalYear = fiscalYear,
            currency = currency,
            taxUnitName = taxUnitName,
            taxUnitValue = unitValue,
            taxIdLastDigits = lastTwoDigits,
            vatOrSalesTaxRate = vatRate,
            financialTransactionTaxRate = bankTaxRate,
            electronicInvoiceDeductionRate = feRate,
            effectiveIncomeTaxRate = incomeTaxRate,
            enforceSingleExemptAccountInColombia = singleExempt,
            autoTagOcrReceiptsAsElectronicInvoice = autoTagOcr,
            dependentsCount = dependentsCount,
            mandatoryIncrgoRate = mandatoryIncrgoRate,
            useProgressiveCedulaGeneralInColombia = useProgressiveCedula
        )
    }

    fun saveProfile(context: Context, profile: TaxConfigurationProfile) {
        val safeUser = profile.userId.ifBlank { "user_default" }
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        prefs.edit()
            .putString("tax_jurisdiction_$safeUser", profile.jurisdiction.code)
            .putInt("tax_fiscal_year_$safeUser", profile.fiscalYear)
            .putString("tax_unit_val_${safeUser}_${profile.jurisdiction.code}_${profile.fiscalYear}", profile.taxUnitValue.toString())
            .putString("tax_currency_${safeUser}_${profile.jurisdiction.code}", profile.currency)
            .putString("tax_unit_name_${safeUser}_${profile.jurisdiction.code}", profile.taxUnitName)
            .putString("tax_last_digits_$safeUser", profile.taxIdLastDigits)
            .putString("tax_vat_rate_${safeUser}_${profile.jurisdiction.code}", profile.vatOrSalesTaxRate.toString())
            .putString("tax_bank_rate_${safeUser}_${profile.jurisdiction.code}", profile.financialTransactionTaxRate.toString())
            .putString("tax_fe_rate_${safeUser}_${profile.jurisdiction.code}", profile.electronicInvoiceDeductionRate.toString())
            .putString("tax_income_rate_${safeUser}_${profile.jurisdiction.code}", profile.effectiveIncomeTaxRate.toString())
            .putBoolean("tax_single_exempt_$safeUser", profile.enforceSingleExemptAccountInColombia)
            .putBoolean("tax_auto_tag_ocr_$safeUser", profile.autoTagOcrReceiptsAsElectronicInvoice)
            .putInt("tax_dependents_count_$safeUser", profile.dependentsCount.coerceIn(0, ColombianUvtCatalog.MAX_DEPENDENTS_COUNT))
            .putString("tax_incrgo_rate_$safeUser", profile.mandatoryIncrgoRate.toString())
            .putBoolean("tax_progressive_cedula_$safeUser", profile.useProgressiveCedulaGeneralInColombia)
            .apply()

        activeProfileFlow.value = profile
    }
}

class DianTaxRepositoryImpl(
    private val context: Context,
    private val financialDao: FinancialDao,
    private val fixedAssetDao: FixedAssetDao,
    private val thresholdEngine: DianUvtThresholdEngine = DianUvtThresholdEngine(),
    private val gmfEngine: GmfFourPerThousandEngine = GmfFourPerThousandEngine(),
    private val internationalEngine: InternationalTaxEngine = InternationalTaxEngine()
) {

    fun observeTaxRadarReport(userId: String): Flow<DianTaxRadarReport> {
        val dataFlow = combine(
            financialDao.getAccountsFlow(userId),
            financialDao.getTransactionsFlow(userId),
            fixedAssetDao.observeAssets(userId),
            financialDao.getInvestmentsFlow(userId)
        ) { accounts, transactions, assets, investments ->
            Quadruple(accounts, transactions, assets, investments)
        }

        return combine(
            dataFlow,
            TaxConfigurationStore.observeProfile(context, userId)
        ) { quad, profile ->
            val currentYearMonth = YearMonth.now().toString()
            val thresholds = thresholdEngine.evaluateAnnualThresholds(
                fiscalYear = profile.fiscalYear,
                uvtValueCop = profile.taxUnitValue,
                accounts = quad.first,
                transactionsInYear = quad.second,
                fixedAssets = quad.third,
                investments = quad.fourth,
                jurisdiction = profile.jurisdiction
            )

            val gmfStatuses = gmfEngine.evaluateMonthlyGmf(
                yearMonth = currentYearMonth,
                uvtValueCop = profile.taxUnitValue,
                accounts = quad.first,
                monthlyTransactions = quad.second,
                jurisdiction = profile.jurisdiction,
                customTaxRate = profile.financialTransactionTaxRate,
                enforceSingleExemptAccount = profile.enforceSingleExemptAccountInColombia
            )

            val totalMonthlyGmf = gmfStatuses.sumOf { it.estimatedGmfTaxPaidCop }
            val totalAnnualGmf = gmfEngine.calculateAnnualGmfPaid(
                fiscalYear = profile.fiscalYear,
                uvtValueCop = profile.taxUnitValue,
                accounts = quad.first,
                transactionsInYear = quad.second,
                jurisdiction = profile.jurisdiction,
                customTaxRate = profile.financialTransactionTaxRate,
                enforceSingleExemptAccount = profile.enforceSingleExemptAccountInColombia
            )
            val has350Uvt = gmfStatuses.any { it.isMarkedTaxExempt350Uvt }
            val advice = gmfEngine.buildGmfOptimizationAdvice(
                statuses = gmfStatuses,
                uvtValueCop = profile.taxUnitValue,
                jurisdiction = profile.jurisdiction
            )

            val feBenefit = thresholdEngine.calculateElectronicInvoiceBenefit(
                fiscalYear = profile.fiscalYear,
                uvtValueCop = profile.taxUnitValue,
                accounts = quad.first,
                transactionsInYear = quad.second,
                deductionRate = profile.electronicInvoiceDeductionRate,
                maxDeductionUnits = profile.jurisdiction.electronicInvoiceMaxUnits,
                autoTagOcrReceiptsAsElectronicInvoice = profile.autoTagOcrReceiptsAsElectronicInvoice
            )

            val intlSummary = internationalEngine.evaluateGeneralTaxes(
                profile = profile,
                transactionsInYear = quad.second,
                investments = quad.fourth,
                annualBankTaxPaid = totalAnnualGmf,
                electronicInvoiceBenefit = feBenefit
            )

            val exceededCount = thresholds.count { it.alertLevel == TaxAlertLevel.EXCEEDED }
            val warningCount = thresholds.count { it.alertLevel == TaxAlertLevel.WARNING }
            val deadlineDesc = DianTaxCalendarCalculator.estimateDeadlineWindow(
                fiscalYear = profile.fiscalYear,
                lastTwoDigitsInput = profile.taxIdLastDigits,
                jurisdiction = profile.jurisdiction
            )

            DianTaxRadarReport(
                userId = userId,
                fiscalYear = profile.fiscalYear,
                currentYearMonth = currentYearMonth,
                uvtValueCop = profile.taxUnitValue,
                lastTwoDigitsNitOrCedula = profile.taxIdLastDigits,
                estimatedFilingDeadlineDescription = deadlineDesc,
                thresholds = thresholds,
                isObligatedToDeclareRenta = exceededCount > 0,
                exceededThresholdsCount = exceededCount,
                warningThresholdsCount = warningCount,
                accountGmfStatuses = gmfStatuses,
                totalMonthlyGmfTaxCop = totalMonthlyGmf,
                totalAnnualGmfTaxCop = totalAnnualGmf,
                hasMarked350UvtAccount = has350Uvt,
                optimizationRecommendation = advice,
                electronicInvoiceBenefit = feBenefit,
                configProfile = profile,
                internationalTaxSummary = intlSummary
            )
        }
    }

    suspend fun toggleAccountTaxExempt(userId: String, accountId: String, isExempt: Boolean) {
        val profile = TaxConfigurationStore.loadProfile(context, userId)
        val enforceSingle = profile.jurisdiction == TaxJurisdiction.CO_DIAN && profile.enforceSingleExemptAccountInColombia
        financialDao.updateAccountTaxExemptWithSingleRule(
            accountId = accountId,
            userId = userId,
            isTaxExempt = isExempt,
            enforceSingleExempt = enforceSingle
        )
    }

    fun observeTaxConfiguration(userId: String): StateFlow<TaxConfigurationProfile> {
        return TaxConfigurationStore.observeProfile(context, userId)
    }

    fun updateTaxConfiguration(profile: TaxConfigurationProfile) {
        TaxConfigurationStore.saveProfile(context, profile)
    }

    private data class Quadruple<A, B, C, D>(
        val first: A,
        val second: B,
        val third: C,
        val fourth: D
    )
}
