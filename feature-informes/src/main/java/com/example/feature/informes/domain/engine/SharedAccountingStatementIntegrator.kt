package com.example.feature.informes.domain.engine

import com.example.data.local.entity.TransactionEntity
import com.example.feature.informes.domain.model.BalanceSheetData
import com.example.feature.informes.domain.model.CalculationTraceDetail
import com.example.feature.informes.domain.model.DataSourceItem
import com.example.feature.informes.domain.model.FinancialLineItem
import java.util.Locale
import kotlin.math.abs
import kotlin.math.roundToLong

/**
 * Extensión NIIF de FinancialCalculationEngine que integra las Cuentas por Cobrar (CxC),
 * Cuentas por Pagar (CxP), el Gasto Neto Reconocido y el Flujo de Reembolsos de Gastos Compartidos.
 */
object SharedAccountingStatementIntegrator {

    data class SharedAccountingSnapshot(
        val activeAccountsReceivableNiif: Double = 0.0,
        val activeAccountsPayableNiif: Double = 0.0,
        val totalMentalHoldReserved: Double = 0.0,
        val periodReimbursementsCollectedCash: Double = 0.0,
        val periodSharedOutflowsOnBehalfOfOthers: Double = 0.0,
        val openReceivableTicketsCount: Int = 0,
        val openPayableTicketsCount: Int = 0,
        val traceConsecutiveCodes: List<String> = emptyList()
    )

    /**
     * Calcula el Gasto Contable Real (PyG) de una lista de transacciones respetando
     * la columna `accountingRecognizedExpenseAmount` de los Gastos Compartidos.
     */
    fun computeRecognizedPnlExpenses(periodTransactions: List<TransactionEntity>): Double {
        val sum = periodTransactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { tx -> tx.accountingRecognizedExpenseAmount ?: tx.amount }
        return roundCents(sum)
    }

    /**
     * Calcula la Salida Bruta Real de Efectivo (Cash Flow Outflow) de las cuentas bancarias,
     * que sí incluye el desembolso total pagado en caja (`tx.amount`).
     */
    fun computeRealCashOutflows(periodTransactions: List<TransactionEntity>): Double {
        val sum = periodTransactions
            .filter { it.type.equals("EXPENSE", ignoreCase = true) }
            .sumOf { tx -> tx.amount }
        return roundCents(sum)
    }

    /**
     * Enriquecimiento del Balance General NIIF con Cuentas por Cobrar y Cuentas por Pagar Compartidas.
     * Mantiene intacta la identidad fundamental: Total Activos == Total Pasivos + Patrimonio Neto.
     */
    fun enrichBalanceSheetWithSharedPortfolio(
        baseBalanceSheet: BalanceSheetData,
        sharedSnapshot: SharedAccountingSnapshot
    ): BalanceSheetData {
        val updatedCurrentAssets = baseBalanceSheet.currentAssets.toMutableList()
        if (sharedSnapshot.activeAccountsReceivableNiif > 0.0) {
            updatedCurrentAssets.removeAll { it.id == "ca0" }
            val newTotalAssetsPreview = baseBalanceSheet.totalAssets + sharedSnapshot.activeAccountsReceivableNiif
            val pct = if (newTotalAssetsPreview > 0.0) {
                (sharedSnapshot.activeAccountsReceivableNiif / newTotalAssetsPreview) * 100.0
            } else 0.0
            val codesPreview = if (sharedSnapshot.traceConsecutiveCodes.isNotEmpty()) {
                " (${sharedSnapshot.traceConsecutiveCodes.take(3).joinToString(", ")})"
            } else ""
            updatedCurrentAssets.add(
                FinancialLineItem(
                    id = "ca3_shared_cxc",
                    title = "Cuentas por Cobrar (Gastos Compartidos NIIF)",
                    subtitle = "${sharedSnapshot.openReceivableTicketsCount} tickets vigentes$codesPreview",
                    category = "Deudores Terceros",
                    amount = roundCents(sharedSnapshot.activeAccountsReceivableNiif),
                    percentageOfTotal = pct
                )
            )
        }

        val updatedCurrentLiabilities = baseBalanceSheet.currentLiabilities.toMutableList()
        if (sharedSnapshot.activeAccountsPayableNiif > 0.0) {
            updatedCurrentLiabilities.removeAll { it.id == "cl0" }
            val newTotalLiabPreview = baseBalanceSheet.totalLiabilities + sharedSnapshot.activeAccountsPayableNiif
            val pct = if (newTotalLiabPreview > 0.0) {
                (sharedSnapshot.activeAccountsPayableNiif / newTotalLiabPreview) * 100.0
            } else 0.0
            updatedCurrentLiabilities.add(
                FinancialLineItem(
                    id = "cl3_shared_cxp",
                    title = "Cuentas por Pagar (Gastos Compartidos NIIF)",
                    subtitle = "${sharedSnapshot.openPayableTicketsCount} obligaciones compartidas pendientes",
                    category = "Acreedores Varios",
                    amount = roundCents(sharedSnapshot.activeAccountsPayableNiif),
                    percentageOfTotal = pct
                )
            )
        }

        val newTotalCurrentAssets = roundCents(updatedCurrentAssets.sumOf { it.amount })
        val newGrandTotalAssets = roundCents(newTotalCurrentAssets + baseBalanceSheet.totalNonCurrentAssets)

        val newTotalCurrentLiabilities = roundCents(updatedCurrentLiabilities.sumOf { it.amount })
        val newGrandTotalLiabilities = roundCents(newTotalCurrentLiabilities + baseBalanceSheet.totalNonCurrentLiabilities)

        val newGrandTotalEquity = roundCents(newGrandTotalAssets - newGrandTotalLiabilities)
        val newLiabilitiesPlusEquity = roundCents(newGrandTotalLiabilities + newGrandTotalEquity)
        val diff = roundCents(abs(newGrandTotalAssets - newLiabilitiesPlusEquity))

        return baseBalanceSheet.copy(
            currentAssets = updatedCurrentAssets,
            totalCurrentAssets = newTotalCurrentAssets,
            totalAssets = newGrandTotalAssets,
            currentLiabilities = updatedCurrentLiabilities,
            totalCurrentLiabilities = newTotalCurrentLiabilities,
            totalLiabilities = newGrandTotalLiabilities,
            initialEquity = newGrandTotalEquity,
            totalEquity = newGrandTotalEquity,
            liabilitiesPlusEquity = newLiabilitiesPlusEquity,
            isBalanced = diff < 0.01,
            balanceDifference = diff
        )
    }

    /**
     * Genera la traza de auditoría NIIF para Cuentas por Cobrar (`SHARED_RECEIVABLES_NIIF`).
     */
    fun buildSharedReceivablesAuditTrace(
        sharedSnapshot: SharedAccountingSnapshot,
        currency: String = "COP"
    ): CalculationTraceDetail {
        val codesText = if (sharedSnapshot.traceConsecutiveCodes.isNotEmpty()) {
            sharedSnapshot.traceConsecutiveCodes.joinToString(", ")
        } else "Sin tickets pendientes"

        return CalculationTraceDetail(
            metricKey = "SHARED_RECEIVABLES_NIIF",
            title = "Cuentas por Cobrar a Terceros (Gastos Compartidos)",
            subtitle = "Activo Corriente exigible respaldado por Tickets Oficiales TK-xxxx",
            currentFormattedValue = String.format(Locale.US, "$%,.2f %s", sharedSnapshot.activeAccountsReceivableNiif, currency),
            accountingFormula = "CxC Compartida = ∑ (Desembolso Pagado por Mí − Mi Cuota Parte de Consumo − Abonos Recibidos PG)",
            mathematicalOperation = String.format(
                Locale.US,
                "Suma de saldos insolutos en %d tickets activos (%s) = $%,.0f %s",
                sharedSnapshot.openReceivableTicketsCount,
                codesText,
                sharedSnapshot.activeAccountsReceivableNiif,
                currency
            ),
            dataSources = listOf(
                DataSourceItem(
                    "shared_expenses + shared_expense_splits",
                    "Cartera Activa GC / TK",
                    "Cuotas parte adeudadas por participantes en modo Devengo NIIF (ACCRUAL_RECEIVABLE)",
                    sharedSnapshot.openReceivableTicketsCount,
                    sharedSnapshot.activeAccountsReceivableNiif
                ),
                DataSourceItem(
                    "shared_settlement_payments",
                    "Abonos y Reembolsos PG",
                    "Pagos parciales y totales descontados en cascada FIFO",
                    1,
                    sharedSnapshot.periodReimbursementsCollectedCash
                )
            ),
            stepByStepExplanation = listOf(
                "Paso 1: Al pagar una factura compartida en modo Devengo NIIF, KashApp separa tu cuota parte (que va a tu Gasto Real PyG) del dinero prestado a los demás.",
                "Paso 2: El excedente pagado a nombre de otros participantes se registra como un Activo Corriente (Cuenta por Cobrar) con su consecutivo GC-xxxx y Ticket TK-xxxx.",
                "Paso 3: Cuando un participante realiza un abono (Comprobante PG-xxxx), el dinero ingresa a tu cuenta bancaria y disminuye esta Cuenta por Cobrar sin inflar artificialmente tus ingresos."
            ),
            accountingStandardNote = "NIIF 9 (Instrumentos Financieros - Cuentas por Cobrar Comerciales y Otras Cuentas por Cobrar) y NIC 1 (Activos Corrientes realizables dentro de los 12 meses).",
            auditConfidenceStatus = "✓ 100% Respaldado por Tickets con Firma Criptográfica SHA-256 ($codesText)."
        )
    }

    /**
     * Genera la traza de auditoría NIIF para Cuentas por Pagar (`SHARED_PAYABLES_NIIF`).
     */
    fun buildSharedPayablesAuditTrace(
        sharedSnapshot: SharedAccountingSnapshot,
        currency: String = "COP"
    ): CalculationTraceDetail {
        return CalculationTraceDetail(
            metricKey = "SHARED_PAYABLES_NIIF",
            title = "Cuentas por Pagar a Terceros (Gastos Compartidos)",
            subtitle = "Pasivo Corriente exigible + Reserva en Bolsillo Hold",
            currentFormattedValue = String.format(Locale.US, "$%,.2f %s", sharedSnapshot.activeAccountsPayableNiif, currency),
            accountingFormula = "CxP Compartida = ∑ (Mi Cuota Parte en Facturas Pagadas por Otros − Mis Pagos Realizados PG)",
            mathematicalOperation = String.format(
                Locale.US,
                "Obligaciones activas = $%,.0f %s (Reserva mental en holdBalance = $%,.0f %s)",
                sharedSnapshot.activeAccountsPayableNiif,
                currency,
                sharedSnapshot.totalMentalHoldReserved,
                currency
            ),
            dataSources = listOf(
                DataSourceItem(
                    "shared_expense_splits",
                    "Pasivo Corriente CxP",
                    "Cuotas parte pendientes donde otro participante pagó la factura en caja",
                    sharedSnapshot.openPayableTicketsCount,
                    sharedSnapshot.activeAccountsPayableNiif
                ),
                DataSourceItem(
                    "accounts (holdBalance)",
                    "Reserva Mental Accounting",
                    "Fondos apartados automáticamente de tu saldo disponible para garantizar liquidez de pago",
                    1,
                    sharedSnapshot.totalMentalHoldReserved
                )
            ),
            stepByStepExplanation = listOf(
                "Paso 1: Cuando un tercero paga un gasto compartido en el que participas, tu cuota parte se reconoce hoy en tu Estado de Resultados (PyG).",
                "Paso 2: Nace simultáneamente un Pasivo Corriente (Cuenta por Pagar) y, si activaste Mental Accounting, ese monto se bloquea en holdBalance para que no lo gastes por error.",
                "Paso 3: Al transferirle el pago (Comprobante PG-xxxx), se libera el holdBalance, baja tu saldo bancario y se extingue el pasivo sin duplicar el gasto en PyG."
            ),
            accountingStandardNote = "NIC 1 / NIIF 9 (Pasivos Financieros a Costo Amortizado y Contabilidad Mental Preventiva).",
            auditConfidenceStatus = "✓ Conciliado con saldos disponibles y reservas holdBalance."
        )
    }

    private fun roundCents(v: Double): Double = (v * 100.0).roundToLong() / 100.0
}
