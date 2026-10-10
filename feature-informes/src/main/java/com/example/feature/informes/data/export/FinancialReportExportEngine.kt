package com.example.feature.informes.data.export

import com.example.feature.informes.domain.model.*
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class FinancialReportExportEngine {

    fun generateReportText(
        config: ExportReportConfig,
        filter: FinancialPeriodFilter,
        summary: FinancialSummaryData,
        balanceSheet: BalanceSheetData,
        incomeStatement: IncomeStatementData,
        cashFlow: CashFlowStatementData,
        equityStatement: EquityStatementData,
        specializedReports: SpecializedReportsBundle,
        analysis: FinancialAnalysisData
    ): String {
        val now = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"))
        val sb = StringBuilder()

        sb.appendLine("================================================================================")
        sb.appendLine("                     KASHAPP - INFORMES FINANCIEROS CENTRAL                     ")
        sb.appendLine("================================================================================")
        sb.appendLine("TITULO: ${config.reportTitle}")
        sb.appendLine("EMISOR: ${config.authorOrCompany}")
        sb.appendLine("FECHA DE EMISIÓN: $now")
        sb.appendLine("PERÍODO: ${filter.periodLabel}")
        sb.appendLine("COMPARADO CON: ${filter.comparison.label}")
        sb.appendLine("FRECUENCIA: ${filter.frequency.label}")
        sb.appendLine("MONEDA PRINCIPAL: ${filter.currency}")
        sb.appendLine("NOTAS: ${config.customNotes}")
        sb.appendLine("--------------------------------------------------------------------------------\n")

        if (config.includeSummary) {
            sb.appendLine("1. RESUMEN EJECUTIVO Y PATRIMONIO")
            sb.appendLine("   - Patrimonio Neto: $%,.2f (%s)".format(summary.netWorth, summary.netWorthTrend))
            sb.appendLine("   - Total Activos: $%,.2f (%d registros)".format(summary.totalAssets, summary.assetCount))
            sb.appendLine("   - Total Pasivos: $%,.2f (%d deudas pendientes)".format(summary.totalLiabilities, summary.pendingDebtsCount))
            sb.appendLine("   - Ingresos Totales Período: $%,.2f".format(summary.totalIncome))
            sb.appendLine("   - Gastos Totales Período: $%,.2f".format(summary.totalExpenses))
            sb.appendLine("   - Resultado del Período (Superávit): $%,.2f".format(summary.periodResult))
            sb.appendLine("   - Flujo de Efectivo Neto: $%,.2f\n".format(summary.netCashFlow))
        }

        if (config.includeFinancialStatements) {
            sb.appendLine("2. ESTADOS FINANCIEROS OFICIALES")
            sb.appendLine("   2.1 ESTADO DE SITUACIÓN FINANCIERA (BALANCE GENERAL)")
            sb.appendLine("       * Total Activos Corrientes: $%,.2f".format(balanceSheet.totalCurrentAssets))
            sb.appendLine("       * Total Activos No Corrientes: $%,.2f".format(balanceSheet.totalNonCurrentAssets))
            sb.appendLine("       * TOTAL ACTIVOS: $%,.2f".format(balanceSheet.totalAssets))
            sb.appendLine("       * Total Pasivos Corrientes: $%,.2f".format(balanceSheet.totalCurrentLiabilities))
            sb.appendLine("       * Total Pasivos No Corrientes: $%,.2f".format(balanceSheet.totalNonCurrentLiabilities))
            sb.appendLine("       * TOTAL PASIVOS: $%,.2f".format(balanceSheet.totalLiabilities))
            sb.appendLine("       * TOTAL PATRIMONIO: $%,.2f".format(balanceSheet.totalEquity))
            sb.appendLine("       * TOTAL PASIVO + PATRIMONIO: $%,.2f".format(balanceSheet.liabilitiesPlusEquity))
            sb.appendLine("       * ESTADO CONTABLE: ${if (balanceSheet.isBalanced) "✓ ESTADO CUADRADO (Activos = Pasivos + Patrimonio)" else "⚠ DIFERENCIA DETECTADA: $%,.2f".format(balanceSheet.balanceDifference)}")
            sb.appendLine("")

            sb.appendLine("   2.2 ESTADO DE RESULTADOS")
            sb.appendLine("       * Total Ingresos: $%,.2f".format(incomeStatement.totalIncome))
            sb.appendLine("       * Gastos Operativos: $%,.2f".format(incomeStatement.operatingExpenses))
            sb.appendLine("       * Resultado Neto del Ejercicio: $%,.2f".format(incomeStatement.netResult))
            sb.appendLine("       * Margen de Ahorro Neto: %.1f%%".format(incomeStatement.savingsMarginPct))
            sb.appendLine("")

            sb.appendLine("   2.3 ESTADO DE FLUJO DE EFECTIVO")
            sb.appendLine("       * Saldo Inicial en Caja & Bancos: $%,.2f".format(cashFlow.initialBalance))
            sb.appendLine("       * (+) Total Entradas de Efectivo: $%,.2f".format(cashFlow.totalInflows))
            sb.appendLine("       * (-) Total Salidas de Efectivo: $%,.2f".format(cashFlow.totalOutflows))
            sb.appendLine("       * (=) Flujo Neto del Período: $%,.2f".format(cashFlow.netCashFlow))
            sb.appendLine("       * (=) Saldo Final: $%,.2f".format(cashFlow.finalBalance))
            sb.appendLine("       * (Transferencias internas aisladas de duplicidad: %d operaciones)".format(cashFlow.internalTransfersExcludedCount))
            sb.appendLine("")

            sb.appendLine("   2.4 ESTADO DE CAMBIOS EN EL PATRIMONIO")
            sb.appendLine("       * Patrimonio Inicial: $%,.2f".format(equityStatement.initialEquity))
            sb.appendLine("       * Resultado del Período: $%,.2f".format(equityStatement.periodResult))
            sb.appendLine("       * Aportes de Capital: $%,.2f".format(equityStatement.capitalContributions))
            sb.appendLine("       * Valorización / Revaluación: $%,.2f".format(equityStatement.valuationGainsOrLosses + equityStatement.assetRevaluations))
            sb.appendLine("       * PATRIMONIO FINAL: $%,.2f (Crecimiento: %.1f%%)\n".format(equityStatement.finalEquity, equityStatement.netEquityGrowthPct))
        }

        if (config.includeSpecializedReports) {
            sb.appendLine("3. INFORMES ESPECIALIZADOS")
            sb.appendLine("   - Cuentas Bancarias: %d cuentas consolidadas.".format(specializedReports.accountReports.size))
            specializedReports.accountReports.forEach { acc ->
                sb.appendLine("     * %s (%s): Saldo $%,.2f".format(acc.name, acc.bankName, acc.currentBalance))
            }
            sb.appendLine("   - Tarjetas de Crédito:")
            specializedReports.creditCardReports.forEach { card ->
                sb.appendLine("     * %s: Cupo $%,.2f | Utilizado $%,.2f (%.1f%%)".format(card.cardName, card.quotaTotal, card.quotaUsed, card.utilizationPct))
            }
            sb.appendLine("   - Inversiones y Rendimientos:")
            specializedReports.investmentReports.forEach { inv ->
                sb.appendLine("     * %s: Invertido $%,.2f | Actual $%,.2f (Retorno: %.1f%%)".format(inv.name, inv.investedCapital, inv.currentValue, inv.returnPercentage))
            }
            sb.appendLine("")
        }

        if (config.includeFinancialAnalysis) {
            sb.appendLine("4. ANÁLISIS FINANCIERO & RATIOS")
            sb.appendLine("   - Diagnóstico General: %s (%d/100)".format(analysis.overallHealthLabel, analysis.overallHealthScore))
            analysis.ratios.forEach { ratio ->
                sb.appendLine("   - %s: %s [Meta: %s] (%s)".format(ratio.name, ratio.formattedValue, ratio.benchmark, ratio.status.label))
                sb.appendLine("     Fórmula: %s | %s".format(ratio.formula, ratio.interpretation))
            }
            sb.appendLine("\n   - Proyecciones Futuras:")
            analysis.projections.forEach { p ->
                sb.appendLine("     * %s: Patrimonio Proyectado $%,.2f (Confianza: %d%%)".format(p.horizonLabel, p.projectedNetWorth, p.confidencePct))
            }
            sb.appendLine("")
        }

        sb.appendLine("================================================================================")
        sb.appendLine("                   Fin del Informe Financiero - KashApp Enterprise               ")
        sb.appendLine("================================================================================")

        return sb.toString()
    }

    fun generateCsv(
        summary: FinancialSummaryData,
        balanceSheet: BalanceSheetData,
        incomeStatement: IncomeStatementData,
        specializedReports: SpecializedReportsBundle
    ): String {
        val sb = StringBuilder()
        sb.appendLine("Sección;Concepto;Detalle;Monto COP;Porcentaje;Variación")
        sb.appendLine("Resumen;Patrimonio Neto;Activos menos Pasivos;${summary.netWorth};100%;${summary.netWorthVariationPct}%")
        sb.appendLine("Resumen;Total Activos;Cuentas, Inversiones y Fijos;${summary.totalAssets};-;${summary.assetsVariationPct}%")
        sb.appendLine("Resumen;Total Pasivos;Tarjetas, Créditos y Obligaciones;${summary.totalLiabilities};-;${summary.liabilitiesVariationPct}%")
        sb.appendLine("Resumen;Ingresos Período;Entradas registradas;${summary.totalIncome};-;${summary.incomeVariationPct}%")
        sb.appendLine("Resumen;Gastos Período;Salidas operativas;${summary.totalExpenses};-;${summary.expenseVariationPct}%")
        sb.appendLine("Resumen;Resultado Período;Superávit neto;${summary.periodResult};-;-")

        balanceSheet.currentAssets.forEach {
            sb.appendLine("Balance - Activos Corrientes;${it.title};${it.category};${it.amount};${it.percentageOfTotal}%;-")
        }
        balanceSheet.nonCurrentAssets.forEach {
            sb.appendLine("Balance - Activos No Corrientes;${it.title};${it.category};${it.amount};${it.percentageOfTotal}%;-")
        }
        balanceSheet.currentLiabilities.forEach {
            sb.appendLine("Balance - Pasivos Corrientes;${it.title};${it.category};${it.amount};${it.percentageOfTotal}%;-")
        }
        balanceSheet.nonCurrentLiabilities.forEach {
            sb.appendLine("Balance - Pasivos No Corrientes;${it.title};${it.category};${it.amount};${it.percentageOfTotal}%;-")
        }

        incomeStatement.incomeByCategory.forEach {
            sb.appendLine("Resultados - Ingreso;${it.name};${it.transactionCount} transacciones;${it.amount};${it.percentageOfTotal}%;${it.variationPct ?: 0.0}%")
        }
        incomeStatement.expensesByCategory.forEach {
            sb.appendLine("Resultados - Gasto;${it.name};${it.transactionCount} transacciones;${it.amount};${it.percentageOfTotal}%;${it.variationPct ?: 0.0}%")
        }

        specializedReports.investmentReports.forEach {
            sb.appendLine("Inversiones;${it.name};${it.entityOrPlatform};${it.currentValue};Retorno: ${it.returnPercentage}%;${it.variationAmount}")
        }

        return sb.toString()
    }
}
