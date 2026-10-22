package com.example.feature.impuestosdian.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.impuestosdian.domain.engine.DianUvtThresholdEngine
import com.example.feature.impuestosdian.domain.engine.GmfFourPerThousandEngine
import com.example.feature.impuestosdian.domain.engine.InternationalTaxEngine
import com.example.feature.impuestosdian.domain.model.ColombianUvtCatalog
import com.example.feature.impuestosdian.domain.model.DianCriterionType
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

        // 1. Transacción de apertura (Saldo Inicial) por $40.000.000
        val txInit = TransactionEntity(
            id = "tx_init_001",
            userId = "u1",
            accountId = "acc_1",
            accountName = "Ahorros Principal",
            categoryId = "cat_initial_balance",
            categoryName = "Saldo Inicial",
            amount = 40_000_000.0,
            currency = "COP",
            type = "INCOME",
            date = "2026-01-02",
            description = "Saldo inicial de apertura",
            receiptUrl = null,
            tags = "Apertura,SaldoInicial",
            idempotencyKey = "init_acc_1"
        )

        // 2. Transferencia entre cuentas propias por $40.000.000 (TRANSFER_IN y TRANSFER_OUT)
        val txTransferOut = TransactionEntity(
            id = "tx_tr_out",
            userId = "u1",
            accountId = "acc_1",
            accountName = "Ahorros Principal",
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            amount = 40_000_000.0,
            currency = "COP",
            type = "TRANSFER_OUT",
            date = "2026-02-10",
            description = "Traslado entre cuentas propias",
            receiptUrl = null,
            idempotencyKey = "tr_out_1"
        )
        val txTransferIn = TransactionEntity(
            id = "tx_tr_in",
            userId = "u1",
            accountId = "acc_2",
            accountName = "Cuenta Corriente",
            categoryId = "cat_transfer",
            categoryName = "Transferencia",
            amount = 40_000_000.0,
            currency = "COP",
            type = "TRANSFER_IN",
            date = "2026-02-10",
            description = "Recepción traslado propio",
            receiptUrl = null,
            idempotencyKey = "tr_in_1"
        )

        // 3. Ingreso real de honorarios por $15.000.000
        val txRealIncome = TransactionEntity(
            id = "tx_real_inc",
            userId = "u1",
            accountId = "acc_1",
            accountName = "Ahorros Principal",
            categoryId = "cat_salary",
            categoryName = "Honorarios",
            amount = 15_000_000.0,
            currency = "COP",
            type = "INCOME",
            date = "2026-03-15",
            description = "Pago cliente",
            receiptUrl = null,
            idempotencyKey = "real_inc_1"
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

        // Solo los $15.000.000 reales deben contar como Ingresos Brutos y Consignaciones (no los $40M de apertura ni los $40M de transferencia)
        assertEquals(15_000_000.0, grossIncome.accumulatedCop, 0.01)
        assertEquals(15_000_000.0, deposits.accumulatedCop, 0.01)
        // TRANSFER_OUT no cuenta como compra/consumo (Art. 594-3 E.T.), pero sí aplica para 4x1000 en el motor GMF
        assertEquals(0.0, totalPurchases.accumulatedCop, 0.01)

        // Verificar que en el motor de impuestos internacionales tampoco se infle la renta líquida con el saldo inicial
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

        // Estado 1: Cuenta A exenta, Cuenta B gravada
        val beforeSwitch = gmfEngine.evaluateMonthlyGmf("2026-10", uvt2026, listOf(accA, accB), listOf(txOutA, txOutB))
        val statusABefore = beforeSwitch.first { it.accountId == "acc_a" }
        val statusBBefore = beforeSwitch.first { it.accountId == "acc_b" }
        assertTrue(statusABefore.isMarkedTaxExempt350Uvt)
        assertFalse(statusBBefore.isMarkedTaxExempt350Uvt)
        assertEquals(0.0, statusABefore.estimatedGmfTaxPaidCop, 0.01) // 10M < 18.33M (350 UVT)
        assertEquals(40_000.0, statusBBefore.estimatedGmfTaxPaidCop, 0.01) // 10M * 0.004

        // Estado 2: Usuario traslada la marca de 350 UVT a la Cuenta B (regla de única cuenta exenta desmarca A)
        val accAAfter = accA.copy(isTaxExempt = false)
        val accBAfter = accB.copy(isTaxExempt = true)
        val afterSwitch = gmfEngine.evaluateMonthlyGmf("2026-10", uvt2026, listOf(accAAfter, accBAfter), listOf(txOutA, txOutB))

        // Solo 1 cuenta puede tener isMarkedTaxExempt350Uvt == true
        assertEquals(1, afterSwitch.count { it.isMarkedTaxExempt350Uvt })
        val statusAAfter = afterSwitch.first { it.accountId == "acc_a" }
        val statusBAfter = afterSwitch.first { it.accountId == "acc_b" }
        assertEquals(40_000.0, statusAAfter.estimatedGmfTaxPaidCop, 0.01)
        assertEquals(0.0, statusBAfter.estimatedGmfTaxPaidCop, 0.01)
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

        // Con ambos gastos ($80M > $73.32M tope de 1,400 UVT)
        val statusesWithBoth = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(tx1, tx2), emptyList(), emptyList())
        val benefitWithBoth = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc), listOf(tx1, tx2))
        val purchasesBoth = statusesWithBoth.first { it.type == DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES }
        assertEquals(80_000_000.0, purchasesBoth.accumulatedCop, 0.01)
        assertEquals(800_000.0, benefitWithBoth.appliedDeductionCop, 0.01)

        // Al eliminar tx2, el acumulado baja a $50M y la deducción del 1% baja a $500.000
        val statusesAfterDelete = thresholdEngine.evaluateAnnualThresholds(2026, uvt2026, listOf(acc), listOf(tx1), emptyList(), emptyList())
        val benefitAfterDelete = thresholdEngine.calculateElectronicInvoiceBenefit(2026, uvt2026, listOf(acc), listOf(tx1))
        val purchasesAfter = statusesAfterDelete.first { it.type == DianCriterionType.COMPRAS_Y_CONSUMOS_TOTALES }
        assertEquals(50_000_000.0, purchasesAfter.accumulatedCop, 0.01)
        assertEquals(500_000.0, benefitAfterDelete.appliedDeductionCop, 0.01)
    }
}
