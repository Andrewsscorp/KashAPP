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
            amount = 116_000.0,
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

        val txSafe = TransactionEntity(
            id = "tx_safe", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "c1", categoryName = "Salario", amount = limitCop * 0.699, currency = "COP",
            type = "INCOME", date = "2026-03-01", description = "Safe", receiptUrl = null, idempotencyKey = "k_safe"
        )
        val statusSafe = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txSafe), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.SAFE, statusSafe.alertLevel)

        val txWarn = txSafe.copy(id = "tx_warn", amount = limitCop * 0.70, idempotencyKey = "k_warn")
        val statusWarn = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txWarn), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.WARNING, statusWarn.alertLevel)

        val txWarn99 = txSafe.copy(id = "tx_warn99", amount = limitCop * 0.999, idempotencyKey = "k_warn99")
        val statusWarn99 = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(txWarn99), emptyList(), emptyList())
            .first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(TaxAlertLevel.WARNING, statusWarn99.alertLevel)

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

        assertEquals(1, gmfResults.size)
        val normStatus = gmfResults.first()
        assertEquals("acc_norm", normStatus.accountId)
        assertEquals(0.0, normStatus.exemptLimitUvt, 0.01)
        assertEquals(5_000_000.0, normStatus.taxableExcessCop, 0.01)
        assertEquals(20_000.0, normStatus.estimatedGmfTaxPaidCop, 0.01)
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
            currentMarketValue = 0.0,
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
        assertEquals(68_000_000.0, wealth.accumulatedCop, 0.01)
        assertFalse(wealth.accumulatedCop > 100_000_000.0)
    }

    @Test
    fun `TEST-DIAN-12 Inversiones de anos anteriores suman a Patrimonio Bruto pero NO inflan Consignaciones del ano actual`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 10_000_000.0, type = "SAVINGS")
        val oldCdt2024 = InvestmentEntity(
            id = "inv_old",
            userId = "u1",
            name = "CDT Antiguo 2024",
            typeId = "RENTA_FIJA",
            typeName = "CDT",
            acquisitionDate = "2024-11-10",
            status = "ACTIVE",
            initialAmount = 60_000_000.0,
            currentValue = 68_000_000.0,
            originAccountId = "acc_1",
            originAccountName = "Ahorros"
        )
        val newCdt2026 = InvestmentEntity(
            id = "inv_new",
            userId = "u1",
            name = "CDT Nuevo 2026",
            typeId = "RENTA_FIJA",
            typeName = "CDT",
            acquisitionDate = "2026-04-01",
            status = "ACTIVE",
            initialAmount = 15_000_000.0,
            currentValue = 16_000_000.0,
            originAccountId = "acc_1",
            originAccountName = "Ahorros"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc),
            transactionsInYear = emptyList(),
            fixedAssets = emptyList(),
            investments = listOf(oldCdt2024, newCdt2026)
        )

        val wealth = statuses.first { it.type == DianCriterionType.PATRIMONIO_BRUTO }
        val deposits = statuses.first { it.type == DianCriterionType.CONSIGNACIONES_E_INVERSIONES }

        // Patrimonio Bruto suma cuenta ($10M) + CDT 2024 ($68M) + CDT 2026 ($16M) = $94M
        assertEquals(94_000_000.0, wealth.accumulatedCop, 0.01)
        // Consignaciones e Inversiones 2026 solo suma el capital constituido en 2026 ($15M), NO el CDT de 2024
        assertEquals(15_000_000.0, deposits.accumulatedCop, 0.01)
        assertEquals(TaxAlertLevel.SAFE, deposits.alertLevel)
    }

    @Test
    fun `TEST-DIAN-13 Bolsa compartida de 350 UVT entre multiples cuentas cuando enforceSingleExemptAccount es false`() {
        val acc1 = AccountEntity(id = "acc_1", userId = "u1", name = "Bancolombia", bankName = "Bancolombia", balance = 30_000_000.0, type = "SAVINGS", isTaxExempt = false)
        val acc2 = AccountEntity(id = "acc_2", userId = "u1", name = "Davivienda", bankName = "Davivienda", balance = 30_000_000.0, type = "SAVINGS", isTaxExempt = false)

        val tx1 = TransactionEntity(
            id = "tx_1", userId = "u1", accountId = "acc_1", accountName = "Bancolombia",
            categoryId = "c1", categoryName = "Pagos", amount = 12_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-10-05", description = "Salida 1", receiptUrl = null, idempotencyKey = "k1"
        )
        val tx2 = TransactionEntity(
            id = "tx_2", userId = "u1", accountId = "acc_2", accountName = "Davivienda",
            categoryId = "c1", categoryName = "Pagos", amount = 8_330_900.0, currency = "COP",
            type = "EXPENSE", date = "2026-10-12", description = "Salida 2", receiptUrl = null, idempotencyKey = "k2"
        )

        val statuses = gmfEngine.evaluateMonthlyGmf(
            yearMonth = "2026-10",
            uvtValueCop = uvt2026,
            accounts = listOf(acc1, acc2),
            monthlyTransactions = listOf(tx1, tx2),
            jurisdiction = TaxJurisdiction.CO_DIAN,
            enforceSingleExemptAccount = false
        )

        val st1 = statuses.first { it.accountId == "acc_1" }
        val st2 = statuses.first { it.accountId == "acc_2" }

        assertTrue(st1.sharesUnified350UvtPool)
        assertTrue(st2.sharesUnified350UvtPool)
        assertEquals(0.0, st1.estimatedGmfTaxPaidCop, 0.01)
        assertEquals(2_000_000.0, st2.taxableExcessCop, 0.01)
        assertEquals(8_000.0, st2.estimatedGmfTaxPaidCop, 0.01)
    }

    @Test
    fun `TEST-DIAN-14 Calculo anual real de 12 meses de 4x1000 evita distorsion de multiplicar un solo mes por 12`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros No Exenta", bankName = "BBVA", balance = 50_000_000.0, type = "SAVINGS", isTaxExempt = false)
        val txMarch = TransactionEntity(
            id = "tx_mar", userId = "u1", accountId = "acc_1", accountName = "Ahorros No Exenta",
            categoryId = "c1", categoryName = "Gasto", amount = 5_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-03-10", description = "Marzo", receiptUrl = null, idempotencyKey = "k_mar"
        )
        val txJuly = txMarch.copy(id = "tx_jul", amount = 10_000_000.0, date = "2026-07-15", idempotencyKey = "k_jul")

        val annualGmf = gmfEngine.calculateAnnualGmfPaid(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc),
            transactionsInYear = listOf(txMarch, txJuly)
        )

        assertEquals(60_000.0, annualGmf, 0.01)
    }

    @Test
    fun `TEST-DIAN-15 Switch autoTagOcrReceiptsAsElectronicInvoice en false exige etiqueta explicita FE_DIAN`() {
        val bankAcc = AccountEntity(id = "acc_b", userId = "u1", name = "Ahorros", balance = 20_000_000.0, type = "SAVINGS")
        val txOnlyReceiptPhoto = TransactionEntity(
            id = "tx_photo", userId = "u1", accountId = "acc_b", accountName = "Ahorros",
            categoryId = "c1", categoryName = "Almuerzo", amount = 1_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-05-01", description = "Recibo cámara sin tag FE",
            receiptUrl = "content://media/external/images/101", tags = "", idempotencyKey = "k_photo"
        )
        val txExplicitFe = txOnlyReceiptPhoto.copy(
            id = "tx_fe", amount = 4_000_000.0, tags = "FE_DIAN", idempotencyKey = "k_fe"
        )

        val whenSwitchOn = thresholdEngine.calculateElectronicInvoiceBenefit(
            fiscalYear = 2026, uvtValueCop = uvt2026, accounts = listOf(bankAcc),
            transactionsInYear = listOf(txOnlyReceiptPhoto, txExplicitFe),
            autoTagOcrReceiptsAsElectronicInvoice = true
        )
        assertEquals(2, whenSwitchOn.eligibleTransactionsCount)
        assertEquals(50_000.0, whenSwitchOn.appliedDeductionCop, 0.01)

        val whenSwitchOff = thresholdEngine.calculateElectronicInvoiceBenefit(
            fiscalYear = 2026, uvtValueCop = uvt2026, accounts = listOf(bankAcc),
            transactionsInYear = listOf(txOnlyReceiptPhoto, txExplicitFe),
            autoTagOcrReceiptsAsElectronicInvoice = false
        )
        assertEquals(1, whenSwitchOff.eligibleTransactionsCount)
        assertEquals(40_000.0, whenSwitchOff.appliedDeductionCop, 0.01)
    }
}
