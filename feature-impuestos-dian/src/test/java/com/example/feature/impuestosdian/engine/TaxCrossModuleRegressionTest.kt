package com.example.feature.impuestosdian.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.InvestmentEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.engine.DianUvtThresholdEngine
import com.example.feature.impuestosdian.domain.engine.GmfFourPerThousandEngine
import com.example.feature.impuestosdian.domain.engine.InternationalTaxEngine
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianCriterionType
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TaxCrossModuleRegressionTest {

    private val thresholdEngine = DianUvtThresholdEngine()
    private val gmfEngine = GmfFourPerThousandEngine()
    private val internationalEngine = InternationalTaxEngine()
    private val uvt2026 = ColombianUvtCatalog.UVT_2026_DEFAULT_COP

    @Test
    fun `TEST-REG-01 Creacion de cuenta con saldo inicial y transferencias propias NO inflan Ingresos Brutos ni Consignaciones`() {
        val acc1 = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros Principal", balance = 40_000_000.0, type = "SAVINGS")
        val acc2 = AccountEntity(id = "acc_2", userId = "u1", name = "Cuenta Corriente", balance = 40_000_000.0, type = "CHECKING")

        val txInit = TransactionEntity(
            id = "tx_init_001", userId = "u1", accountId = "acc_1", accountName = "Ahorros Principal",
            categoryId = "cat_initial_balance", categoryName = "Saldo Inicial", amount = 40_000_000.0,
            currency = "COP", type = "INCOME", date = "2026-01-02", description = "Saldo inicial de apertura",
            receiptUrl = null, tags = "Apertura,SaldoInicial", idempotencyKey = "init_acc_1"
        )
        val txTransferOut = TransactionEntity(
            id = "tx_tr_out", userId = "u1", accountId = "acc_1", accountName = "Ahorros Principal",
            categoryId = "cat_transfer", categoryName = "Transferencia", amount = 40_000_000.0,
            currency = "COP", type = "TRANSFER_OUT", date = "2026-02-10", description = "Traslado entre cuentas propias",
            receiptUrl = null, idempotencyKey = "tr_out_1"
        )
        val txTransferIn = TransactionEntity(
            id = "tx_tr_in", userId = "u1", accountId = "acc_2", accountName = "Cuenta Corriente",
            categoryId = "cat_transfer", categoryName = "Transferencia", amount = 40_000_000.0,
            currency = "COP", type = "TRANSFER_IN", date = "2026-02-10", description = "Recepción traslado propio",
            receiptUrl = null, idempotencyKey = "tr_in_1"
        )
        val txRealIncome = TransactionEntity(
            id = "tx_real_inc", userId = "u1", accountId = "acc_1", accountName = "Ahorros Principal",
            categoryId = "cat_salary", categoryName = "Honorarios", amount = 15_000_000.0,
            currency = "COP", type = "INCOME", date = "2026-03-15", description = "Pago cliente",
            receiptUrl = null, idempotencyKey = "real_inc_1"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc1, acc2),
            transactionsInYear = listOf(txInit, txTransferOut, txTransferIn, txRealIncome),
            fixedAssets = emptyList(),
            investments = emptyList()
        )

        val grossIncome = statuses.first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        val deposits = statuses.first { it.type == DianCriterionType.CONSIGNACIONES_E_INVERSIONES }
        val totalPurchases = statuses.first { it.type == DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES }

        assertEquals(15_000_000.0, grossIncome.accumulatedCop, 0.01)
        assertEquals(15_000_000.0, deposits.accumulatedCop, 0.01)
        assertEquals(0.0, totalPurchases.accumulatedCop, 0.01)

        val coProfile = TaxConfigurationProfile(userId = "u1", jurisdiction = TaxJurisdiction.CO_DIAN, fiscalYear = 2026)
        val feBenefit = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc1, acc2), listOf(txInit, txRealIncome))
        val intlSummary = internationalEngine.evaluateGeneralTaxes(
            profile = coProfile,
            transactionsInYear = listOf(txInit, txTransferOut, txTransferIn, txRealIncome),
            investments = emptyList(),
            annualBankTaxPaid = 0.0,
            electronicInvoiceBenefit = feBenefit
        )
        assertEquals(15_000_000.0, intlSummary.estimatedTaxableNetIncome, 0.01)
    }

    @Test
    fun `TEST-REG-02 Cambio de cuenta exenta de 350 UVT recalcula 4x1000 sin duplicar exencion`() {
        val accA = AccountEntity(id = "acc_a", userId = "u1", name = "Bancolombia A", bankName = "Bancolombia", balance = 30_000_000.0, type = "SAVINGS", isTaxExempt = true)
        val accB = AccountEntity(id = "acc_b", userId = "u1", name = "Davivienda B", bankName = "Davivienda", balance = 30_000_000.0, type = "SAVINGS", isTaxExempt = false)

        val txOutA = TransactionEntity(
            id = "tx_a", userId = "u1", accountId = "acc_a", accountName = "Bancolombia A",
            categoryId = "c1", categoryName = "Pagos", amount = 10_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-10-10", description = "Salida A", receiptUrl = null, idempotencyKey = "ka"
        )
        val txOutB = TransactionEntity(
            id = "tx_b", userId = "u1", accountId = "acc_b", accountName = "Davivienda B",
            categoryId = "c1", categoryName = "Pagos", amount = 10_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-10-12", description = "Salida B", receiptUrl = null, idempotencyKey = "kb"
        )

        val beforeSwitch = gmfEngine.evaluateMonthlyGmf("2026-10", uvt2026, listOf(accA, accB), listOf(txOutA, txOutB))
        assertEquals(0.0, beforeSwitch.first { it.accountId == "acc_a" }.estimatedGmfTaxPaidCop, 0.01)
        assertEquals(40_000.0, beforeSwitch.first { it.accountId == "acc_b" }.estimatedGmfTaxPaidCop, 0.01)

        val accAAfter = accA.copy(isTaxExempt = false)
        val accBAfter = accB.copy(isTaxExempt = true)
        val afterSwitch = gmfEngine.evaluateMonthlyGmf("2026-10", uvt2026, listOf(accAAfter, accBAfter), listOf(txOutA, txOutB))

        assertEquals(1, afterSwitch.count { it.isMarkedTaxExempt350Uvt })
        assertEquals(40_000.0, afterSwitch.first { it.accountId == "acc_a" }.estimatedGmfTaxPaidCop, 0.01)
        assertEquals(0.0, afterSwitch.first { it.accountId == "acc_b" }.estimatedGmfTaxPaidCop, 0.01)
    }

    @Test
    fun `TEST-REG-03 Eliminacion de gasto con Factura Electronica actualiza topes de Compras y Beneficio del 1 por ciento`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 50_000_000.0, type = "SAVINGS")
        val tx1 = TransactionEntity(
            id = "tx_1", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "c1", categoryName = "Electrodomésticos", amount = 50_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-06-01", description = "Compra 1 con FE_DIAN",
            receiptUrl = "fe1.pdf", tags = "FE_DIAN", idempotencyKey = "k1"
        )
        val tx2 = TransactionEntity(
            id = "tx_2", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "c1", categoryName = "Viajes", amount = 30_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-06-15", description = "Compra 2 con FE_DIAN",
            receiptUrl = "fe2.pdf", tags = "FE_DIAN", idempotencyKey = "k2"
        )

        val statusesWithBoth = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(tx1, tx2), emptyList(), emptyList())
        val benefitWithBoth = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc), listOf(tx1, tx2))
        assertEquals(80_000_000.0, statusesWithBoth.first { it.type == DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES }.accumulatedCop, 0.01)
        assertEquals(800_000.0, benefitWithBoth.appliedDeductionCop, 0.01)

        val statusesAfterDelete = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(tx1), emptyList(), emptyList())
        val benefitAfterDelete = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc), listOf(tx1))
        assertEquals(50_000_000.0, statusesAfterDelete.first { it.type == DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES }.accumulatedCop, 0.01)
        assertEquals(500_000.0, benefitAfterDelete.appliedDeductionCop, 0.01)
    }

    @Test
    fun `TEST-REG-04 Transferencia con transferGroupId hacia CDT propio NO duplica el Tope 5 de Consignaciones`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 10_000_000.0, type = "SAVINGS")
        val txIncomeSalary = TransactionEntity(
            id = "tx_sal", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "cat_sal", categoryName = "Salario", amount = 30_000_000.0, currency = "COP",
            type = "INCOME", date = "2026-01-15", description = "Salario Enero", receiptUrl = null, idempotencyKey = "k_sal"
        )
        val cdtFromSavings = InvestmentEntity(
            id = "inv_cdt", userId = "u1", name = "CDT Propio", typeId = "RENTA_FIJA", typeName = "CDT",
            acquisitionDate = "2026-01-20", status = "ACTIVE", initialAmount = 20_000_000.0,
            currentValue = 21_000_000.0, originAccountId = "acc_1", originAccountName = "Ahorros",
            tags = "INTERNAL_TRANSFER"
        )

        val statuses = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc),
            transactionsInYear = listOf(txIncomeSalary),
            fixedAssets = emptyList(),
            investments = listOf(cdtFromSavings)
        )

        val deposits = statuses.first { it.type == DianCriterionType.CONSIGNACIONES_E_INVERSIONES }
        assertEquals(30_000_000.0, deposits.accumulatedCop, 0.01)
    }

    @Test
    fun `TEST-REG-05 Flujo anual completo combina 4x1000 de 12 meses 1 por ciento FE y Cedula General DIAN`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros No Exenta", bankName = "Bancolombia", balance = 40_000_000.0, type = "SAVINGS", isTaxExempt = false)
        val txIncome = TransactionEntity(
            id = "tx_inc", userId = "u1", accountId = "acc_1", accountName = "Ahorros No Exenta",
            categoryId = "cat_sal", categoryName = "Salario", amount = 120_000_000.0, currency = "COP",
            type = "INCOME", date = "2026-02-10", description = "Ingresos anuales", receiptUrl = null, idempotencyKey = "k_inc"
        )
        val txExpenseFe = TransactionEntity(
            id = "tx_exp_fe", userId = "u1", accountId = "acc_1", accountName = "Ahorros No Exenta",
            categoryId = "cat_comp", categoryName = "Compras", amount = 20_000_000.0, currency = "COP",
            type = "EXPENSE", date = "2026-04-15", description = "Compra con Factura Electrónica",
            receiptUrl = "fe_2026.pdf", tags = "FE_DIAN", idempotencyKey = "k_exp_fe"
        )

        val annualGmf = gmfEngine.calculateAnnualGmfPaid(2026, uvt2026, listOf(acc), listOf(txIncome, txExpenseFe))
        assertEquals(80_000.0, annualGmf, 0.01)

        val feBenefit = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc), listOf(txIncome, txExpenseFe))
        assertEquals(200_000.0, feBenefit.appliedDeductionCop, 0.01)

        val profile = TaxConfigurationProfile(
            userId = "u1",
            jurisdiction = TaxJurisdiction.CO_DIAN,
            fiscalYear = 2026,
            taxUnitValue = uvt2026,
            useProgressiveCedulaGeneralInColombia = true,
            dependentsCount = 1
        )
        val summary = internationalEngine.evaluateGeneralTaxes(
            profile = profile,
            transactionsInYear = listOf(txIncome, txExpenseFe),
            investments = emptyList(),
            annualBankTaxPaid = annualGmf,
            electronicInvoiceBenefit = feBenefit
        )

        assertEquals(40_000.0, summary.deductibleBankTaxFiftyPercent, 0.01)
        assertEquals(72.0 * uvt2026, summary.dependentsDeductionAmount, 0.01)
        assertTrue(summary.estimatedTaxableNetIncome < 120_000_000.0)
        assertTrue(summary.estimatedAnnualIncomeTax > 0.0)
    }

    @Test
    fun `TEST-REG-06 Movimientos de otros anos fiscales 2025 o 2027 son ignorados estrictamente en el reporte 2026`() {
        val acc = AccountEntity(id = "acc_1", userId = "u1", name = "Ahorros", balance = 10_000_000.0, type = "SAVINGS")
        val tx2025 = TransactionEntity(
            id = "tx_2025", userId = "u1", accountId = "acc_1", accountName = "Ahorros",
            categoryId = "cat_sal", categoryName = "Salario", amount = 90_000_000.0, currency = "COP",
            type = "INCOME", date = "2025-12-31", description = "Ingreso año anterior", receiptUrl = null, idempotencyKey = "k_2025"
        )
        val tx2027 = tx2025.copy(id = "tx_2027", date = "2027-01-01", idempotencyKey = "k_2027")

        val statuses2026 = thresholdEngine.evaluateAnnualThresholds(
            fiscalYear = 2026,
            uvtValueCop = uvt2026,
            accounts = listOf(acc),
            transactionsInYear = listOf(tx2025, tx2027),
            fixedAssets = emptyList(),
            investments = emptyList()
        )

        val income2026 = statuses2026.first { it.type == DianCriterionType.INGRESOS_BRUTOS }
        assertEquals(0.0, income2026.accumulatedCop, 0.01)
    }
}
