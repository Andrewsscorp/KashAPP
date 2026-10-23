package com.example.feature.impuestosdian.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.engine.DianTaxCalendarCalculator
import com.example.feature.impuestosdian.domain.engine.DianUvtThresholdEngine
import com.example.feature.impuestosdian.domain.engine.GmfFourPerThousandEngine
import com.example.feature.impuestosdian.domain.engine.InternationalTaxEngine
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.ElectronicInvoiceBenefitSummary
import com.example.feature.impuestosdian.domain.model.GlobalTaxJurisdictionCatalog
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InternationalTaxAndJurisdictionsTest {

    private val thresholdEngine = DianUvtThresholdEngine()
    private val gmfEngine = GmfFourPerThousandEngine()
    private val internationalEngine = InternationalTaxEngine()
    private val uvt2026 = ColombianUvtCatalog.UVT_2026_DEFAULT_COP

    @Test
    fun `TEST-CAT-01 ColombianUvtCatalog retorna UVT oficial 2024 2025 2026 y fallback seguro`() {
        assertEquals(47_065.0, ColombianUvtCatalog.getDefaultUvtForYear(2024), 0.01)
        assertEquals(49_799.0, ColombianUvtCatalog.getDefaultUvtForYear(2025), 0.01)
        assertEquals(52_374.0, ColombianUvtCatalog.getDefaultUvtForYear(2026), 0.01)
        assertEquals(52_374.0, ColombianUvtCatalog.getDefaultUvtForYear(2030), 0.01)
    }

    @Test
    fun `TEST-CAT-02 Evaluacion completa de las 8 jurisdicciones fiscales internacionales`() {
        val allJurisdictions = TaxJurisdiction.entries
        assertEquals(8, allJurisdictions.size)

        allJurisdictions.forEach { jur ->
            val profile = TaxConfigurationProfile(
                userId = "user_intl",
                jurisdiction = jur,
                fiscalYear = 2026
            )

            val acc = AccountEntity(
                id = "acc_${jur.code}",
                userId = "user_intl",
                name = "Cuenta ${jur.countryName}",
                bankName = "Banco Nacional",
                balance = profile.taxUnitValue * 500.0,
                currency = profile.currency,
                type = "SAVINGS",
                isTaxExempt = true
            )

            val txIncome = TransactionEntity(
                id = "tx_in_${jur.code}",
                userId = "user_intl",
                accountId = acc.id,
                accountName = acc.name,
                categoryId = "cat_inc",
                categoryName = "Ingresos Profesionales",
                amount = profile.taxUnitValue * (jur.annualIncomeFilingThresholdUnits + 200.0),
                currency = profile.currency,
                type = "INCOME",
                date = "2026-05-15",
                description = "Honorarios en ${jur.countryName}",
                receiptUrl = null,
                idempotencyKey = "k_in_${jur.code}"
            )

            val txExpense = TransactionEntity(
                id = "tx_ex_${jur.code}",
                userId = "user_intl",
                accountId = acc.id,
                accountName = acc.name,
                categoryId = "cat_exp",
                categoryName = "Gastos Operativos",
                amount = profile.taxUnitValue * 100.0,
                currency = profile.currency,
                type = "EXPENSE",
                date = "2026-05-20",
                description = "Compra con soporte electrónico",
                receiptUrl = "invoice_${jur.code}.pdf",
                tags = "FE_DIAN",
                idempotencyKey = "k_ex_${jur.code}"
            )

            val inv = InvestmentEntity(
                id = "inv_${jur.code}",
                userId = "user_intl",
                name = "Depósito a Plazo ${jur.countryName}",
                typeId = "FIXED_INCOME",
                typeName = "CDT / Bono",
                acquisitionDate = "2026-01-15",
                status = "ACTIVE",
                initialAmount = profile.taxUnitValue * 200.0,
                currentValue = profile.taxUnitValue * 230.0,
                originAccountId = acc.id,
                originAccountName = acc.name
            )

            val thresholds = thresholdEngine.evaluateAnnualThresholds(
                fiscalYear = 2026,
                uvtValueCop = profile.taxUnitValue,
                accounts = listOf(acc),
                transactionsInYear = listOf(txIncome, txExpense),
                fixedAssets = emptyList(),
                investments = listOf(inv),
                jurisdiction = jur
            )
            assertEquals(5, thresholds.size)

            val gmfMonthly = gmfEngine.evaluateMonthlyGmf(
                yearMonth = "2026-05",
                uvtValueCop = profile.taxUnitValue,
                accounts = listOf(acc),
                monthlyTransactions = listOf(txExpense),
                jurisdiction = jur,
                customTaxRate = profile.financialTransactionTaxRate
            )
            assertEquals(1, gmfMonthly.size)

            val feBenefit = thresholdEngine.calculateElectronicInvoiceBenefit(
                fiscalYear = 2026,
                uvtValueCop = profile.taxUnitValue,
                accounts = listOf(acc),
                transactionsInYear = listOf(txIncome, txExpense),
                deductionRate = profile.electronicInvoiceDeductionRate,
                maxDeductionUnits = jur.electronicInvoiceMaxUnits
            )

            val summary = internationalEngine.evaluateGeneralTaxes(
                profile = profile,
                transactionsInYear = listOf(txIncome, txExpense),
                investments = listOf(inv),
                annualBankTaxPaid = gmfMonthly.sumOf { it.estimatedGmfTaxPaidCop },
                electronicInvoiceBenefit = feBenefit
            )

            assertEquals(jur, summary.jurisdiction)
            assertTrue(summary.estimatedAnnualIncomeTax > 0.0)
            assertTrue(summary.estimatedInvestmentWithholdingTax > 0.0)
            assertTrue(summary.estimatedTaxableNetIncome > 0.0)
            assertTrue(summary.effectiveTaxPressurePercentage > 0.0)
            assertTrue(summary.totalEstimatedTaxBurden > 0.0)
        }
    }

    @Test
    fun `TEST-CAT-03 Calendario Tributario DIAN para todos los rangos de digitos NIT y paises`() {
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "01", TaxJurisdiction.CO_DIAN).contains("Agosto de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "26", TaxJurisdiction.CO_DIAN).contains("Agosto de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "27", TaxJurisdiction.CO_DIAN).contains("Septiembre de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "61", TaxJurisdiction.CO_DIAN).contains("Cuarta semana de Septiembre de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "66", TaxJurisdiction.CO_DIAN).contains("Septiembre de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "67", TaxJurisdiction.CO_DIAN).contains("Octubre de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "99", TaxJurisdiction.CO_DIAN).contains("Octubre de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "00", TaxJurisdiction.CO_DIAN).contains("Octubre de 2027"))

        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.MX_SAT).contains("Abril de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.US_IRS).contains("15 de Abril de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.ES_AEAT).contains("30 de Junio de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.PE_SUNAT).contains("Mayo y Junio de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.CL_SII).contains("Abril de 2027"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.AR_ARCA).contains("Junio de 2027"))
    }

    @Test
    fun `TEST-CAT-04 Preview de Impacto Tributario en Gastos e Ingresos vinculado a Configuracion`() {
        val coProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.CO_DIAN, fiscalYear = 2026)

        val previewExemptWithInvoice = GlobalTaxJurisdictionCatalog.previewTransactionTaxImpact(
            profile = coProfile,
            amount = 1_190_000.0,
            isExpense = true,
            isCashAccount = false,
            isAccountTaxExempt = true,
            hasElectronicInvoiceSupport = true
        )

        assertEquals(190_000.0, previewExemptWithInvoice.estimatedIncludedVatAmount, 1.0)
        assertEquals(0.0, previewExemptWithInvoice.estimatedBankTransactionTaxAmount, 0.01)
        assertEquals(11_900.0, previewExemptWithInvoice.estimatedTaxDeductionBenefitAmount, 0.01)

        val previewNonExempt = GlobalTaxJurisdictionCatalog.previewTransactionTaxImpact(
            profile = coProfile,
            amount = 1_000_000.0,
            isExpense = true,
            isCashAccount = false,
            isAccountTaxExempt = false,
            hasElectronicInvoiceSupport = false
        )
        assertEquals(4_000.0, previewNonExempt.estimatedBankTransactionTaxAmount, 0.01)
        assertEquals(0.0, previewNonExempt.estimatedTaxDeductionBenefitAmount, 0.01)
        assertNotNull(previewNonExempt.summaryMessage)
    }

    @Test
    fun `TEST-CAT-05 Guia de Creacion de Cuentas vinculada segun Pais y Tipo de Cuenta`() {
        val coProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.CO_DIAN)
        val peProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.PE_SUNAT)
        val usProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.US_IRS)

        val coWalletHint = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(coProfile, "WALLET", false)
        assertTrue(coWalletHint.enforcesSingleExemptAccountRule)
        assertTrue(coWalletHint.switchSubtitle.contains("65 UVT"))

        val coExemptHint = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(coProfile, "SAVINGS", true)
        assertTrue(coExemptHint.enforcesSingleExemptAccountRule)
        assertTrue(coExemptHint.switchSubtitle.contains("350 UVT"))

        val peHint = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(peProfile, "PAYROLL", true)
        assertFalse(peHint.enforcesSingleExemptAccountRule)
        assertTrue(peHint.switchLabel.contains("ITF"))

        val usHint = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(usProfile, "CHECKING", false)
        assertEquals(0.0, usHint.financialTaxRate, 0.0001)
        assertTrue(usHint.switchLabel.contains("IRS"))
    }

    @Test
    fun `TEST-CAT-06 Tabla Progresiva Art 241 ET calcula con exactitud los 7 tramos UVT en Colombia`() {
        assertEquals(0.0, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(1_090.0), 0.001)
        assertEquals(115.90, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(1_700.0), 0.001)
        assertEquals(787.90, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(4_100.0), 0.001)
        assertEquals(2_296.00, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(8_670.0), 0.001)
        assertEquals(5_901.00, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(18_970.0), 0.001)
        assertEquals(10_352.10, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(31_000.0), 0.001)
        assertEquals(10_742.10, ColombianUvtCatalog.calculateProgressiveIncomeTaxUvt(32_000.0), 0.001)
    }

    @Test
    fun `TEST-CAT-07 Depuracion Cedula General aplica INCRGO 25 por ciento exento y limite global de 1340 UVT`() {
        val profile = TaxConfigurationProfile(
            userId = "u1",
            jurisdiction = TaxJurisdiction.CO_DIAN,
            fiscalYear = 2026,
            taxUnitValue = uvt2026,
            useProgressiveCedulaGeneralInColombia = true,
            mandatoryIncrgoRate = 0.08,
            dependentsCount = 2
        )
        val txHighIncome = TransactionEntity(
            id = "tx_hi", userId = "u1", accountId = "acc_1", accountName = "Bancolombia",
            categoryId = "cat_sal", categoryName = "Salario", amount = 10_000.0 * uvt2026,
            currency = "COP", type = "INCOME", date = "2026-06-01", description = "Salario anual",
            receiptUrl = null, idempotencyKey = "k_hi"
        )
        val emptyFe = ElectronicInvoiceBenefitSummary(0.0, 0.0, 0.0, 240.0, 240.0 * uvt2026, 0.0, 240.0 * uvt2026, 0)

        val summary = internationalEngine.evaluateGeneralTaxes(
            profile = profile,
            transactionsInYear = listOf(txHighIncome),
            investments = emptyList(),
            annualBankTaxPaid = 200.0 * uvt2026,
            electronicInvoiceBenefit = emptyFe
        )

        assertEquals(800.0 * uvt2026, summary.mandatoryIncrgoAmount, 1.0)
        assertEquals(790.0 * uvt2026, summary.laborExempt25PercentAmount, 1.0)
        assertEquals(144.0 * uvt2026, summary.dependentsDeductionAmount, 1.0)
        assertEquals(890.0 * uvt2026, summary.cappedGeneralDeductionsAmount, 1.0)
    }

    @Test
    fun `TEST-CAT-08 Tope absoluto de 1340 UVT recorta deducciones cuando superan el limite legal Ley 2277`() {
        val profile = TaxConfigurationProfile(
            userId = "u1",
            jurisdiction = TaxJurisdiction.CO_DIAN,
            fiscalYear = 2026,
            taxUnitValue = uvt2026,
            useProgressiveCedulaGeneralInColombia = true,
            mandatoryIncrgoRate = 0.0
        )
        val txIncome = TransactionEntity(
            id = "tx_inc", userId = "u1", accountId = "acc_1", accountName = "Bancolombia",
            categoryId = "cat_sal", categoryName = "Honorarios", amount = 10_000.0 * uvt2026,
            currency = "COP", type = "INCOME", date = "2026-06-01", description = "Honorarios",
            receiptUrl = null, idempotencyKey = "k_inc"
        )
        val emptyFe = ElectronicInvoiceBenefitSummary(0.0, 0.0, 0.0, 240.0, 240.0 * uvt2026, 0.0, 240.0 * uvt2026, 0)

        val summary = internationalEngine.evaluateGeneralTaxes(
            profile = profile,
            transactionsInYear = listOf(txIncome),
            investments = emptyList(),
            annualBankTaxPaid = 1_600.0 * uvt2026,
            electronicInvoiceBenefit = emptyFe
        )

        assertEquals(1_340.0 * uvt2026, summary.cappedGeneralDeductionsAmount, 1.0)
    }

    @Test
    fun `TEST-CAT-09 Categorias exentas de IVA Salud Educacion Arriendo Servicios no inflan IVA estimado`() {
        val coProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.CO_DIAN, fiscalYear = 2026)
        val txRent = TransactionEntity(
            id = "tx_rent", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "cat_rent", categoryName = "Arriendo y Vivienda", amount = 2_000_000.0,
            currency = "COP", type = "EXPENSE", date = "2026-02-05", description = "Canon arriendo",
            receiptUrl = null, idempotencyKey = "k_rent"
        )
        val txHealth = txRent.copy(id = "tx_health", categoryName = "Salud y Medicina", amount = 500_000.0, idempotencyKey = "k_hlth")
        val emptyFe = ElectronicInvoiceBenefitSummary(0.0, 0.0, 0.0, 240.0, 240.0 * uvt2026, 0.0, 240.0 * uvt2026, 0)

        val summary = internationalEngine.evaluateGeneralTaxes(
            profile = coProfile,
            transactionsInYear = listOf(txRent, txHealth),
            investments = emptyList(),
            annualBankTaxPaid = 0.0,
            electronicInvoiceBenefit = emptyFe
        )

        assertEquals(0.0, summary.estimatedVatOrSalesTaxPaid, 0.01)
    }
}
