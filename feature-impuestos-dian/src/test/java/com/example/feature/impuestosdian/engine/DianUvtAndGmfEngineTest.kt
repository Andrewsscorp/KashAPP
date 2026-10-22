package com.example.feature.impuestosdian.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.engine.DianTaxCalendarCalculator
import com.example.feature.impuestosdian.domain.engine.DianUvtThresholdEngine
import com.example.feature.impuestosdian.domain.engine.GmfFourPerThousandEngine
import com.example.feature.impuestosdian.domain.engine.InternationalTaxEngine
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianCriterionType
import com.example.feature.impuestosdian.domain.model.GlobalTaxJurisdictionCatalog
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DianUvtAndGmfEngineTest {

    private val thresholdEngine = DianUvtThresholdEngine()
    private val gmfEngine = GmfFourPerThousandEngine()
    private val internationalEngine = InternationalTaxEngine()
    private val uvt2026 = ColombianUvtCatalog.UVT_2026_DEFAULT_COP // 52_374.0

    @Test
    fun `TEST-DIAN-01 Tope de Ingresos Brutos supera 1400 UVT y marca EXCEEDED`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Bancolombia", balance = 10_000_000.0, type = "SAVINGS")
        val tx = TransactionEntity(
            id = "tx_1",
            userId = "u1",
            accountId = "acc_1",
            accountName = "Bancolombia",
            categoryId = "cat_salary",
            categoryName = "Salario",
            amount = 75_000_000.0,
            currency = "COP",
            type = "INCOME",
            date = "2026-06-15",
            description = "Honorarios anuales",
            receiptUrl = null,
            idempotencyKey = "idem_1"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc),
            transactionsInYear = listOf(tx),
            fixedAssets = emptyList(),
            investments = emptyList()
        )

        val incomeStatus = statuses.first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(75_000_000.0 / uvt2026, incomeStatus.accumulatedUvt, 0.1)
        assertEquals(TaxAlertLevel.EXCEEDED, incomeStatus.alertLevel)
        assertEquals(0.0, incomeStatus.remainingCopBeforeLimit, 0.01)
    }

    @Test
    fun `TEST-DIAN-02 Alerta preventiva WARNING en Consumos con Tarjeta de Credito`() {
        val ccAcc = AccountEntity(id = "acc_cc", userId = "u1", name = "Visa Infinite", balance = -58_000_000.0, type = "CREDIT_CARD")
        val tx = TransactionEntity(
            id = "tx_cc_1",
            userId = "u1",
            accountId = "acc_cc",
            accountName = "Visa Infinite",
            categoryId = "cat_shopping",
            categoryName = "Compras",
            amount = 58_000_000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-08-10",
            description = "Consumos TC",
            receiptUrl = null,
            idempotencyKey = "idem_cc_1"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(ccAcc),
            transactionsInYear = listOf(tx),
            fixedAssets = emptyList(),
            investments = emptyList()
        )

        val ccStatus = statuses.first { it.type == DianCriterionType.CONSUMOS_TARJETA_CREDITO }
        assertEquals(TaxAlertLevel.WARNING, ccStatus.alertLevel)
        assertEquals((1_400.0 * uvt2026) - 58_000_000.0, ccStatus.remainingCopBeforeLimit, 1.0)
    }

    @Test
    fun `TEST-DIAN-03 Tope de Patrimonio Bruto 4500 UVT combinando Cuentas Activos NIC 16 y CDTs`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 35_000_000.0, type = "SAVINGS")
        val ccDebt = AccountEntity(id = "acc_cc", userId = "u1", name = "TC", balance = -15_000_000.0, type = "CREDIT_CARD")
        val asset = FixedAssetEntity(
            id = "af_1",
            userId = "u1",
            name = "Apartamento",
            type = "TANGIBLE",
            category = "Inmuebles",
            acquisitionDate = "2025-01-10",
            acquisitionValue = 190_000_000.0,
            currentMarketValue = 190_000_000.0,
            status = "ACTIVE",
            includeInNetWorth = true
        )
        val cdt = InvestmentEntity(
            id = "inv_1",
            userId = "u1",
            name = "CDT Bancolombia",
            typeId = "RENTA_FIJA",
            typeName = "CDT",
            acquisitionDate = "2026-02-01",
            status = "ACTIVE",
            initialAmount = 20_000_000.0,
            currentValue = 20_000_000.0,
            originAccountId = "acc_1",
            originAccountName = "Ahorros"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc, ccDebt),
            transactionsInYear = emptyList(),
            fixedAssets = listOf(asset),
            investments = listOf(cdt)
        )

        val wealthStatus = statuses.first { it.type == DianCriterionType.PATRIMONIO_BRUTO }
        // Deudas de TC (-15M) no restan del Patrimonio Bruto (Estatuto Tributario Art. 261)
        assertEquals(245_000_000.0, wealthStatus.accumulatedCop, 0.01)
        assertEquals(TaxAlertLevel.EXCEEDED, wealthStatus.alertLevel)
    }

    @Test
    fun `TEST-DIAN-04 Calculo exacto del 4x1000 en Cuenta Exenta 350 UVT`() {
        val exemptAccount = AccountEntity(
            id = "acc_ex",
            userId = "u1",
            name = "Ahorros Exenta",
            bankName = "Bancolombia",
            balance = 50_000_000.0,
            type = "SAVINGS",
            isTaxExempt = true
        )
        val limit350Cop = 350.0 * uvt2026 // 18_330_900.0
        val outflowAmount = limit350Cop + 2_000_000.0 // 20_330_900.0

        val tx = TransactionEntity(
            id = "tx_out",
            userId = "u1",
            accountId = "acc_ex",
            accountName = "Ahorros Exenta",
            categoryId = "cat_exp",
            categoryName = "Pagos",
            amount = outflowAmount,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-15",
            description = "Pago",
            receiptUrl = null,
            idempotencyKey = "idem_out"
        )

        val gmfList = gmfEngine.evaluateMonthlyGmf(
            yearMonth = "2026-10",
            uvtValueCop = uvt2026,
            accounts = listOf(exemptAccount),
            monthlyTransactions = listOf(tx)
        )

        val status = gmfList.first()
        assertEquals(0.0, status.remainingExemptCop, 0.01)
        assertEquals(2_000_000.0, status.taxableExcessCop, 0.01)
        assertEquals(8_000.0, status.estimatedGmfTaxPaidCop, 0.01)
    }

    @Test
    fun `TEST-DIAN-05 Calculo exacto del 4x1000 en Billetera Digital Nequi 65 UVT`() {
        val nequi = AccountEntity(
            id = "acc_nq",
            userId = "u1",
            name = "Mi Nequi",
            bankName = "Nequi",
            balance = 5_000_000.0,
            type = "WALLET",
            isTaxExempt = false
        )
        val limit65Cop = 65.0 * uvt2026 // 3_404_310.0
        val outflowAmount = limit65Cop + 1_000_000.0 // 4_404_310.0

        val tx = TransactionEntity(
            id = "tx_nq",
            userId = "u1",
            accountId = "acc_nq",
            accountName = "Mi Nequi",
            categoryId = "cat_exp",
            categoryName = "Envíos",
            amount = outflowAmount,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-10-18",
            description = "Transferencia Nequi",
            receiptUrl = null,
            idempotencyKey = "idem_nq"
        )

        val gmfList = gmfEngine.evaluateMonthlyGmf(
            yearMonth = "2026-10",
            uvtValueCop = uvt2026,
            accounts = listOf(nequi),
            monthlyTransactions = listOf(tx)
        )

        val status = gmfList.first()
        assertEquals(65.0, status.exemptLimitUvt, 0.01)
        assertEquals(1_000_000.0, status.taxableExcessCop, 0.01)
        assertEquals(4_000.0, status.estimatedGmfTaxPaidCop, 0.01)
    }

    @Test
    fun `TEST-DIAN-06 Tope maximo de 240 UVT en deduccion del 1 por ciento por Factura Electronica`() {
        val bankAcc = AccountEntity(id = "acc_b", userId = "u1", name = "Cuenta Débito", balance = 90_000_000.0, type = "SAVINGS")
        val txNormal = TransactionEntity(
            id = "tx_fe_1",
            userId = "u1",
            accountId = "acc_b",
            accountName = "Cuenta Débito",
            categoryId = "cat_1",
            categoryName = "Tecnología",
            amount = 40_000_000.0,
            currency = "COP",
            type = "EXPENSE",
            date = "2026-05-10",
            description = "Compra con Factura Electrónica",
            receiptUrl = "https://dian.gov.co/fe/123",
            tags = "FE_DIAN",
            idempotencyKey = "idem_fe_1"
        )

        val benefit1 = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(bankAcc), listOf(txNormal))
        assertEquals(400_000.0, benefit1.appliedDeductionCop, 0.01)

        // Si las compras fueran $1.500M, se limita a 240 UVT ($12.569.760 COP)
        val txHuge = txNormal.copy(id = "tx_fe_2", amount = 1_500_000_000.0)
        val benefitCapped = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(bankAcc), listOf(txHuge))
        assertEquals(240.0 * uvt2026, benefitCapped.appliedDeductionCop, 0.01)
    }

    @Test
    fun `TEST-INTL-07 Calculo de Impuestos Generales Internacionales y Vinculacion con Cuentas`() {
        val mxProfile = TaxConfigurationProfile(
            userId = "u1",
            jurisdiction = TaxJurisdiction.MX_SAT,
            fiscalYear = 2026
        )
        val txIncome = TransactionEntity(
            id = "tx_mx_in",
            userId = "u1",
            accountId = "acc_mx",
            accountName = "BBVA México",
            categoryId = "cat_sal",
            categoryName = "Honorarios",
            amount = 500_000.0,
            currency = "MXN",
            type = "INCOME",
            date = "2026-04-10",
            description = "Ingresos",
            receiptUrl = null,
            idempotencyKey = "idem_mx_1"
        )
        val txExpense = TransactionEntity(
            id = "tx_mx_ex",
            userId = "u1",
            accountId = "acc_mx",
            accountName = "BBVA México",
            categoryId = "cat_comp",
            categoryName = "Equipos",
            amount = 116_000.0, // Incluye 16% IVA (16,000 MXN)
            currency = "MXN",
            type = "EXPENSE",
            date = "2026-04-12",
            description = "Laptop",
            receiptUrl = "cfdi.xml",
            idempotencyKey = "idem_mx_2"
        )

        val accMx = AccountEntity(id = "acc_mx", userId = "u1", name = "BBVA México", balance = 384_000.0, currency = "MXN", type = "SAVINGS")
        val feBenefit = thresholdEngine.calculateElectronicInvoiceBenefit(
            fiscalYear = 2026,
            uvtValueCop = mxProfile.taxUnitValue,
            accounts = listOf(accMx),
            transactionsInYear = listOf(txIncome, txExpense),
            deductionRate = mxProfile.electronicInvoiceDeductionRate,
            maxDeductionUnits = mxProfile.jurisdiction.electronicInvoiceMaxUnits
        )
        val intlSummary = internationalEngine.evaluateGeneralTaxes(
            profile = mxProfile,
            transactionsInYear = listOf(txIncome, txExpense),
            investments = emptyList(),
            annualBankTaxPaid = 0.0,
            electronicInvoiceBenefit = feBenefit
        )

        assertEquals(16_000.0, intlSummary.estimatedVatOrSalesTaxPaid, 1.0)
        assertTrue(intlSummary.estimatedAnnualIncomeTax > 0.0)

        val hintCo = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(
            profile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.CO_DIAN),
            accountType = "WALLET",
            isMarkedTaxExempt = false
        )
        assertTrue(hintCo.switchSubtitle.contains("65 UVT"))
        assertTrue(DianTaxCalendarCalculator.estimateDeadlineWindow(2026, "42", TaxJurisdiction.CO_DIAN).contains("Septiembre"))
    }

    @Test
    fun `TEST-DIAN-08 Fronteras exactas de niveles de alerta SAFE WARNING EXCEEDED`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 0.0, type = "SAVINGS")
        val limitCop = 1_400.0 * uvt2026 // 73,323,600.0

        // Caso A: 69.9% del tope -> SAFE
        val txSafe = TransactionEntity(
            id = "tx_safe", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "c1", categoryName = "Salario", amount = limitCop * 0.699, currency = "COP",
            type = "INCOME", date = "2026-03-01", description = "Safe", receiptUrl = null, idempotencyKey = "k_safe"
        )
        val statusSafe = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txSafe), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.SAFE, statusSafe.alertLevel)

        // Caso B: Exactamente 70.0% del tope -> WARNING
        val txWarn = txSafe.copy(id = "tx_warn", amount = limitCop * 0.70, idempotencyKey = "k_warn")
        val statusWarn = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txWarn), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.WARNING, statusWarn.alertLevel)

        // Caso C: 99.9% del tope -> WARNING
        val txWarn99 = txSafe.copy(id = "tx_warn99", amount = limitCop * 0.999, idempotencyKey = "k_warn99")
        val statusWarn99 = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txWarn99), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.WARNING, statusWarn99.alertLevel)

        // Caso D: Exactamente 100.0% del tope -> EXCEEDED
        val txExceeded = txSafe.copy(id = "tx_exc", amount = limitCop, idempotencyKey = "k_exc")
        val statusExceeded = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txExceeded), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.EXCEEDED, statusExceeded.alertLevel)
    }

    @Test
    fun `TEST-DIAN-09 Cuenta normal no exenta grava 100 por ciento de salidas y excluye cuentas de Efectivo y TC`() {
        val nonExemptSavings = AccountEntity(
            id = "acc_norm", userId = "u1", name = "Ahorros Secundaria", bankName = "Davivienda",
            balance = 20_000_000.0, type = "SAVINGS", isTaxExempt = false
        )
        val cashWallet = AccountEntity(
            id = "acc_cash", userId = "u1", name = "Efectivo Billetera", bankName = "Efectivo",
            balance = 500_000.0, type = "CASH", isTaxExempt = false
        )
        val creditCard = AccountEntity(
            id = "acc_cc", userId = "u1", name = "Tarjeta MasterCard", bankName = "Bancolombia",
            balance = -2_000_000.0, type = "CREDIT_CARD", isTaxExempt = false
        )

        val txSavings = TransactionEntity(
            id = "tx_s", userId = "u1", accountId = "acc_norm", accountName = "Ahorros Secundaria",
            categoryId = "c1", categoryName = "Arriendo", amount = 5_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-10-05", description = "Arriendo", receiptUrl = null, idempotencyKey = "ks"
        )
        val txCash = txSavings.copy(id = "tx_c", accountId = "acc_cash", amount = 200_000.0, idempotencyKey = "kc")
        val txCc = txSavings.copy(id = "tx_cc", accountId = "acc_cc", amount = 1_000_000.0, idempotencyKey = "kcc")

        val gmfResults = gmfEngine.evaluateMonthlyGmf(
            yearMonth = "2026-10",
            uvtValueCop = uvt2026,
            accounts = listOf(nonExemptSavings, cashWallet, creditCard),
            monthlyTransactions = listOf(txSavings, txCash, txCc)
        )

        // Solo la cuenta bancaria debe estar en el reporte GMF (Efectivo y TC excluidas)
        assertEquals(1, gmfResults.size)
        val normStatus = gmfResults.first()
        assertEquals("acc_norm", normStatus.accountId)
        assertEquals(0.0, normStatus.exemptLimitUvt, 0.01)
        assertEquals(5_000_000.0, normStatus.taxableExcessCop, 0.01)
        assertEquals(20_000.0, normStatus.estimatedGmfTaxPaidCop, 0.01) // 5M * 0.004 = 20,000
    }

    @Test
    fun `TEST-DIAN-10 Pagos en efectivo NO suman al beneficio del 1 por ciento aunque tengan factura`() {
        val cashAcc = AccountEntity(id = "acc_cash", userId = "u1", name = "Efectivo", balance = 1_000_000.0, type = "CASH")
        val bankAcc = AccountEntity(id = "acc_bank", userId = "u1", name = "Bancolombia", balance = 10_000_000.0, type = "SAVINGS")

        val txCashWithInvoice = TransactionEntity(
            id = "tx_cash_fe", userId = "u1", accountId = "acc_cash", accountName = "Efectivo",
            categoryId = "c1", categoryName = "Restaurante", amount = 2_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-07-01", description = "Compra en efectivo con FE_DIAN",
            receiptUrl = "fe_cash.pdf", tags = "FE_DIAN", idempotencyKey = "k_cash_fe"
        )
        val txBankWithInvoice = TransactionEntity(
            id = "tx_bank_fe", userId = "u1", accountId = "acc_bank", accountName = "Bancolombia",
            categoryId = "c1", categoryName = "Supermercado", amount = 3_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-07-02", description = "Compra con tarjeta débito y FE_DIAN",
            receiptUrl = "fe_bank.pdf", tags = "FE_DIAN", idempotencyKey = "k_bank_fe"
        )

        val benefit = thresholdEngine.calculateElectronicInvoiceBenefit(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(cashAcc, bankAcc),
            transactionsInYear = listOf(txCashWithInvoice, txBankWithInvoice)
        )

        // Art. 336 Num 5 E.T. exige pago por medio electrónico (tarjeta débito/crédito o transferencia), excluyendo efectivo
        assertEquals(1, benefit.eligibleTransactionsCount)
        assertEquals(3_000_000.0, benefit.eligiblePurchasesTotalCop, 0.01)
        assertEquals(30_000.0, benefit.appliedDeductionCop, 0.01)
    }

    @Test
    fun `TEST-DIAN-11 Patrimonio Bruto valora Activos Fijos con depreciacion lineal NIC 16 y respeta valor residual`() {
        val vehicleAsset = FixedAssetEntity(
            id = "af_car",
            userId = "u1",
            name = "Camioneta",
            type = "TANGIBLE",
            category = "Vehículos",
            acquisitionDate = "2024-01-01",
            acquisitionValue = 100_000_000.0,
            residualValue = 20_000_000.0,
            accumulatedDepreciation = 32_000_000.0,
            usefulLifeMonths = 60,
            depreciationMethod = "STRAIGHT_LINE",
            currentMarketValue = 0.0, // Sin avalúo comercial, usa valor en libros NIC 16 ($68M)
            status = "ACTIVE",
            includeInNetWorth = true
        )
        val disposedAsset = vehicleAsset.copy(id = "af_sold", status = "SOLD")

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = emptyList(),
            transactionsInYear = emptyList(),
            fixedAssets = listOf(vehicleAsset, disposedAsset),
            investments = emptyList()
        )

        val wealth = statuses.first { it.type == DianCriterionType.PATRIMONIO_BRUTO }
        // El activo vendido se excluye y el activo vigente vale costo ($100M) - depreciación ($32M) = $68M
        assertEquals(68_000_000.0, wealth.accumulatedCop, 0.01)
        assertFalse(wealth.accumulatedCop > 100_000_000.0)
    }
}
