package com.example.feature.impuestosdian.data.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.NumberFormat
import java.util.Locale

class DianPreRentaPdfExporter(private val context: Context) {

    fun exportPreRentaPdf(report: DianTaxRadarReport): File {
        val exportDir = File(context.cacheDir, "shared_exports").apply {
            if (!exists()) mkdirs()
        }
        val fileName = "KashApp_Borrador_PreRenta_${report.configProfile.jurisdiction.code}_${report.fiscalYear}.pdf"
        val outFile = File(exportDir, fileName)

        val currencyFormatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
        val cur = report.configProfile.currency
        val unitName = report.configProfile.taxUnitName

        val canonicalPayload = buildString {
            append("${report.userId}|${report.fiscalYear}|${report.configProfile.jurisdiction.code}|")
            report.thresholds.forEach { t ->
                append("${t.type}:${t.accumulatedCop.toLong()}|")
            }
            append("GMF:${report.totalMonthlyGmfTaxCop.toLong()}|")
            append("FE:${report.electronicInvoiceBenefit.appliedDeductionCop.toLong()}")
        }
        val sha256Signature = computeSha256Hex(canonicalPayload)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val titlePaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 15f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.rgb(37, 99, 235)
            textSize = 11f
            isFakeBoldText = true
            isAntiAlias = true
        }
        val bodyPaint = Paint().apply {
            color = Color.rgb(51, 65, 85)
            textSize = 10f
            isAntiAlias = true
        }
        val boldBodyPaint = Paint().apply {
            color = Color.rgb(15, 23, 42)
            textSize = 10.5f
            isFakeBoldText = true
            isAntiAlias = true
        }

        var y = 45f
        canvas.drawText(
            "KASHAPP • CERTIFICADO BORRADOR PRE-RENTA E IMPUESTOS (${report.fiscalYear})",
            36f,
            y,
            titlePaint
        )
        y += 18f
        canvas.drawText(
            "Jurisdicción: ${report.configProfile.jurisdiction.countryName} (${report.configProfile.jurisdiction.authorityAcronym}) • 1 $unitName = ${currencyFormatter.format(report.uvtValueCop)} $cur",
            36f,
            y,
            subtitlePaint
        )
        y += 16f
        canvas.drawText(report.estimatedFilingDeadlineDescription, 36f, y, bodyPaint)
        y += 24f

        canvas.drawText("1. EVALUACIÓN DE TOPES ANUALES (${report.fiscalYear})", 36f, y, boldBodyPaint)
        y += 16f
        report.thresholds.forEach { t ->
            val line = "• ${t.title}: ${currencyFormatter.format(t.accumulatedCop)} $cur (${"%.1f".format(t.accumulatedUvt)} / ${t.limitUvt.toInt()} $unitName) — [${t.alertLevel.name}]"
            canvas.drawText(line, 44f, y, bodyPaint)
            y += 15f
        }

        y += 12f
        canvas.drawText("2. CONTROL MENSUAL DE IMPUESTOS BANCARIOS (4x1000 / ITF — ${report.currentYearMonth})", 36f, y, boldBodyPaint)
        y += 16f
        canvas.drawText(
            "• Total Impuesto Bancario / 4x1000 del Mes: ${currencyFormatter.format(report.totalMonthlyGmfTaxCop)} $cur (50% Deducible Art. 115 E.T.: ${currencyFormatter.format(report.totalMonthlyGmfTaxCop * 0.5)} $cur)",
            44f,
            y,
            bodyPaint
        )
        y += 15f
        report.accountGmfStatuses.take(6).forEach { acc ->
            val exemptTag = when {
                acc.isMarkedTaxExempt350Uvt -> "Exenta 350 UVT"
                acc.isLowAmountDigitalWallet65Uvt -> "Billetera 65 UVT"
                else -> "Gravada"
            }
            canvas.drawText(
                "  - ${acc.accountName} (${acc.bankName}) [$exemptTag]: Salidas ${currencyFormatter.format(acc.monthlyOutflowsCop)} $cur | Impuesto: ${currencyFormatter.format(acc.estimatedGmfTaxPaidCop)} $cur",
                48f,
                y,
                bodyPaint
            )
            y += 14f
        }

        y += 12f
        val fe = report.electronicInvoiceBenefit
        canvas.drawText("3. DEDUCCIÓN POR FACTURA ELECTRÓNICA / GASTOS SOPORTADOS", 36f, y, boldBodyPaint)
        y += 16f
        canvas.drawText(
            "• Compras elegibles (${fe.eligibleTransactionsCount} mov.): ${currencyFormatter.format(fe.eligiblePurchasesTotalCop)} $cur | Deducción aplicable: ${currencyFormatter.format(fe.appliedDeductionCop)} $cur",
            44f,
            y,
            bodyPaint
        )

        report.internationalTaxSummary?.let { intl ->
            y += 22f
            canvas.drawText("4. RESUMEN DE IMPUESTOS GENERALES (${intl.jurisdiction.countryName})", 36f, y, boldBodyPaint)
            y += 16f
            canvas.drawText(
                "• IVA / IGV / Sales Tax estimado en compras (${"%.1f".format(intl.vatRateApplied * 100)}%): ${currencyFormatter.format(intl.estimatedVatOrSalesTaxPaid)} $cur",
                44f,
                y,
                bodyPaint
            )
            y += 15f
            canvas.drawText(
                "• Retención estimada s/ rendimientos de inversión: ${currencyFormatter.format(intl.estimatedInvestmentWithholdingTax)} $cur",
                44f,
                y,
                bodyPaint
            )
            y += 15f
            canvas.drawText(
                "• Renta Líquida Gravable Estimada: ${currencyFormatter.format(intl.estimatedTaxableNetIncome)} $cur | Impuesto Renta Estimado: ${currencyFormatter.format(intl.estimatedAnnualIncomeTax)} $cur",
                44f,
                y,
                bodyPaint
            )
            y += 15f
            canvas.drawText(
                "• Presión Fiscal Efectiva Total: ${"%.1f".format(intl.effectiveTaxPressurePercentage)}% (${currencyFormatter.format(intl.totalEstimatedTaxBurden)} $cur)",
                44f,
                y,
                bodyPaint
            )
        }

        y = 785f
        canvas.drawText("Sello de Integridad SHA-256: $sha256Signature", 36f, y, bodyPaint)
        y += 14f
        canvas.drawText(
            "Nota Legal (Ley 43 de 1990): Herramienta informativa de planeación tributaria (Andrés Suárez). No sustituye firma de Contador Público.",
            36f,
            y,
            bodyPaint
        )

        pdfDocument.finishPage(page)
        FileOutputStream(outFile).use { fos ->
            pdfDocument.writeTo(fos)
        }
        pdfDocument.close()
        return outFile
    }

    private fun computeSha256Hex(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
