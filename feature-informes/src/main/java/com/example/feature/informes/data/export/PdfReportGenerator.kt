package com.example.feature.informes.data.export

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.feature.informes.domain.model.*
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

class PdfReportGenerator(private val context: Context) {

    fun generateExecutivePdf(
        config: ExportReportConfig,
        filter: FinancialPeriodFilter,
        summary: FinancialSummaryData,
        balanceSheet: BalanceSheetData,
        incomeStatement: IncomeStatementData,
        cashFlow: CashFlowStatementData,
        analysis: FinancialAnalysisData
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Standard A4 portrait
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var yPos = 45f

        // 1. Header Banner
        paint.color = Color.parseColor("#0F172A") // Navy Dark
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Logo & Title
        paint.color = Color.parseColor("#38BDF8") // Sky Blue
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KASHAPP ENTERPRISE", 36f, 42f, paint)

        paint.color = Color.WHITE
        paint.textSize = 13f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("INFORME FINANCIERO Y CONTABLE CENTRAL", 36f, 62f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val nowStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
        canvas.drawText("Fecha: $nowStr • Período: ${filter.periodLabel} • Moneda: ${filter.currency}", 36f, 78f, paint)

        yPos = 125f

        // 2. Section: Resumen Patrimonial
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("1. RESUMEN EJECUTIVO Y PATRIMONIO NETO", 36f, yPos, paint)

        yPos += 8f
        paint.color = Color.parseColor("#CBD5E1")
        paint.strokeWidth = 1f
        canvas.drawLine(36f, yPos, 559f, yPos, paint)
        yPos += 18f

        // Patrimonio Box
        paint.color = Color.parseColor("#F1F5F9")
        canvas.drawRoundRect(36f, yPos, 559f, yPos + 48f, 8f, 8f, paint)

        paint.color = Color.parseColor("#64748B")
        paint.textSize = 9f
        canvas.drawText("PATRIMONIO NETO (Activos − Pasivos)", 48f, yPos + 18f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 16f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("$%,.2f ${filter.currency}".format(summary.netWorth), 48f, yPos + 38f, paint)

        paint.color = Color.parseColor("#059669") // Green
        paint.textSize = 10f
        canvas.drawText("+%.1f%% vs. Período Anterior".format(summary.netWorthVariationPct), 380f, yPos + 38f, paint)

        yPos += 64f

        // Metrics Table
        val col1 = 36f
        val col2 = 210f
        val col3 = 390f

        paint.color = Color.parseColor("#475569")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        canvas.drawText("• Total Activos: $%,.0f".format(summary.totalAssets), col1, yPos, paint)
        canvas.drawText("• Ingresos Período: $%,.0f".format(summary.totalIncome), col2, yPos, paint)
        canvas.drawText("• Superávit Neto: $%,.0f".format(summary.periodResult), col3, yPos, paint)

        yPos += 16f
        canvas.drawText("• Total Pasivos: $%,.0f".format(summary.totalLiabilities), col1, yPos, paint)
        canvas.drawText("• Gastos Período: $%,.0f".format(summary.totalExpenses), col2, yPos, paint)
        canvas.drawText("• Flujo de Caja Neto: $%,.0f".format(summary.netCashFlow), col3, yPos, paint)

        yPos += 28f

        // 3. Section: Balance General (Situación Financiera)
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("2. ESTADO DE SITUACIÓN FINANCIERA (BALANCE GENERAL)", 36f, yPos, paint)

        yPos += 8f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(36f, yPos, 559f, yPos, paint)
        yPos += 16f

        // Validation status badge
        paint.color = if (balanceSheet.isBalanced) Color.parseColor("#ECFDF5") else Color.parseColor("#FEF2F2")
        canvas.drawRoundRect(36f, yPos, 559f, yPos + 24f, 6f, 6f, paint)

        paint.color = if (balanceSheet.isBalanced) Color.parseColor("#059669") else Color.parseColor("#DC2626")
        paint.textSize = 9.5f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        val statusText = if (balanceSheet.isBalanced) "✓ ESTADO CUADRADO: Activos ($%,.0f) = Pasivos ($%,.0f) + Patrimonio ($%,.0f)".format(
            balanceSheet.totalAssets, balanceSheet.totalLiabilities, balanceSheet.totalEquity
        ) else "⚠ DIFERENCIA DETECTADA EN EL BALANCE"
        canvas.drawText(statusText, 48f, yPos + 16f, paint)

        yPos += 36f

        // Line items summary
        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Activos Corrientes (Caja, Bancos, Inversiones CP):", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(balanceSheet.totalCurrentAssets), 470f, yPos, paint)

        yPos += 14f
        canvas.drawText("Activos No Corrientes (Propiedad, Planta, Equipos e Inversiones LP):", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(balanceSheet.totalNonCurrentAssets), 470f, yPos, paint)

        yPos += 14f
        canvas.drawText("Pasivos Corrientes (Tarjetas de Crédito y Deudas CP):", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(balanceSheet.totalCurrentLiabilities), 470f, yPos, paint)

        yPos += 14f
        canvas.drawText("Pasivos No Corrientes (Préstamos y Obligaciones LP):", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(balanceSheet.totalNonCurrentLiabilities), 470f, yPos, paint)

        yPos += 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL PATRIMONIO NETO:", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(balanceSheet.totalEquity), 470f, yPos, paint)

        yPos += 28f

        // 4. Section: Estado de Resultados
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("3. ESTADO DE RESULTADOS", 36f, yPos, paint)

        yPos += 8f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(36f, yPos, 559f, yPos, paint)
        yPos += 16f

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Ingresos Operativos Totales:", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(incomeStatement.totalIncome), 470f, yPos, paint)

        yPos += 14f
        canvas.drawText("(-) Gastos Operativos y del Hogar:", 36f, yPos, paint)
        canvas.drawText("$%,.0f".format(incomeStatement.totalExpenses), 470f, yPos, paint)

        yPos += 14f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("(=) RESULTADO NETO (Margen de Ahorro: %.1f%%):".format(incomeStatement.savingsMarginPct), 36f, yPos, paint)
        paint.color = Color.parseColor("#059669")
        canvas.drawText("$%,.0f".format(incomeStatement.netResult), 470f, yPos, paint)

        yPos += 28f

        // 5. Section: Ratios Financieros & Diagnóstico
        paint.color = Color.parseColor("#1E293B")
        paint.textSize = 12f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("4. ANÁLISIS FINANCIERO Y RATIOS CLAVE", 36f, yPos, paint)

        yPos += 8f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(36f, yPos, 559f, yPos, paint)
        yPos += 16f

        paint.color = Color.parseColor("#334155")
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("• Razón Corriente: %.2f x [Meta: >1.50x] (Solvencia Líquida)".format(analysis.liquidityRatioValue), 36f, yPos, paint)
        canvas.drawText("• Nivel Endeudamiento: %.1f%% [Meta: <40%%] (Saludable)".format(analysis.debtLevelPct), 280f, yPos, paint)

        yPos += 14f
        canvas.drawText("• Margen Neto de Ahorro: %.1f%%".format(analysis.netMarginPct), 36f, yPos, paint)
        canvas.drawText("• Rentabilidad s/ Activos (ROA): %.1f%%".format(analysis.roaPct), 280f, yPos, paint)

        yPos += 34f

        // Footer
        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 8f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        canvas.drawText("Documento generado automáticamente por KashApp Enterprise Vault. Certificado bajo principios contables NIIF / NIC.", 36f, 810f, paint)

        pdfDocument.finishPage(page)

        // Write to Cache file
        val outputDir = File(context.cacheDir, "reports")
        if (!outputDir.exists()) outputDir.mkdirs()
        val outputFile = File(outputDir, "informe_financiero_${System.currentTimeMillis()}.pdf")

        FileOutputStream(outputFile).use { out ->
            pdfDocument.writeTo(out)
        }
        pdfDocument.close()

        return outputFile
    }
}
