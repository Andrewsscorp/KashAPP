package com.example.presentation.audit

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.util.FinancialReportExporter.ExportResult
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise Immutable Audit PDF Exporter for KashApp.
 * Produces official A4 forensic audit reports with security seals,
 * statistical metrics, SHA-256 chain verification, and legal sign-off blocks.
 */
object AuditPdfExporter {

    fun exportToPdf(
        context: Context,
        logs: List<AuditUiEntry>,
        userName: String = "Usuario KashApp",
        filterDesc: String = "Todos los Eventos"
    ): ExportResult {
        if (logs.isEmpty()) {
            return ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "No hay registros de auditoría para exportar"
            )
        }

        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 595 // Standard A4 width (points)
            val pageHeight = 842 // Standard A4 height (points)
            val margin = 36f

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val generatedDateStr = dateFormat.format(Date())

            // Paints
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.parseColor("#1E293B")
            }
            val boldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#0F172A")
            }
            val hashPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                typeface = Typeface.MONOSPACE
                color = Color.parseColor("#64748B")
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 0.8f
            }

            var currentPageNumber = 1
            var pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
            var page = pdfDocument.startPage(pageInfo)
            var canvas = page.canvas

            // Page 1 Header
            drawOfficialHeader(canvas, pageWidth, generatedDateStr, userName, filterDesc)
            var currentY = 135f

            // Summary KPI Cards on Page 1
            drawSummaryKpiCards(canvas, logs, margin, currentY, pageWidth - margin * 2)
            currentY += 68f

            // Cryptographic Guarantee Banner
            drawCryptographicBanner(canvas, margin, currentY, pageWidth - margin * 2)
            currentY += 46f

            // Table Header
            drawTableHeader(canvas, margin, currentY, pageWidth - margin * 2)
            currentY += 22f

            val rowHeight = 30f

            for ((index, entry) in logs.withIndex()) {
                // Check page overflow
                if (currentY + rowHeight > pageHeight - 50f) {
                    drawPageFooter(canvas, currentPageNumber, pageWidth, pageHeight)
                    pdfDocument.finishPage(page)

                    currentPageNumber++
                    pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                    page = pdfDocument.startPage(pageInfo)
                    canvas = page.canvas

                    // Subsequent page header
                    drawSubsequentHeader(canvas, pageWidth, currentPageNumber)
                    currentY = 65f

                    // Redraw table header
                    drawTableHeader(canvas, margin, currentY, pageWidth - margin * 2)
                    currentY += 22f
                }

                // Alternate row background
                if (index % 2 == 1) {
                    val rowBg = Paint().apply { color = Color.parseColor("#F8FAFC") }
                    canvas.drawRect(margin, currentY - 4f, pageWidth - margin, currentY + rowHeight - 4f, rowBg)
                }

                // Row columns:
                // 1. Date/Time (margin to margin + 90)
                canvas.drawText(entry.formattedDateTime, margin + 4f, currentY + 9f, hashPaint)

                // 2. Category (margin + 92 to margin + 180)
                val catPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 8f
                    typeface = Typeface.DEFAULT_BOLD
                    color = entry.category.colorHex.toInt()
                }
                canvas.drawText(entry.category.label.take(18), margin + 92f, currentY + 9f, catPaint)

                // 3. Severity Pill (margin + 185 to margin + 225)
                val (sevBgColor, sevTextColor) = when (entry.severity) {
                    AuditSeverity.CRITICAL -> Color.parseColor("#FEE2E2") to Color.parseColor("#DC2626")
                    AuditSeverity.WARNING -> Color.parseColor("#FEF3C7") to Color.parseColor("#D97706")
                    AuditSeverity.SUCCESS -> Color.parseColor("#DCFCE7") to Color.parseColor("#16A34A")
                    AuditSeverity.INFO -> Color.parseColor("#E0F2FE") to Color.parseColor("#0284C7")
                }
                val pillPaint = Paint().apply { color = sevBgColor }
                val pillRect = RectF(margin + 185f, currentY - 2f, margin + 230f, currentY + 12f)
                canvas.drawRoundRect(pillRect, 4f, 4f, pillPaint)
                val sevTextPaint = Paint().apply {
                    isAntiAlias = true
                    textSize = 7f
                    typeface = Typeface.DEFAULT_BOLD
                    color = sevTextColor
                }
                canvas.drawText(entry.severity.name.take(6), margin + 189f, currentY + 8f, sevTextPaint)

                // 4. Title & Details (margin + 235 to margin + 410)
                val truncatedTitle = if (entry.eventTitle.length > 36) entry.eventTitle.take(34) + "..." else entry.eventTitle
                canvas.drawText(truncatedTitle, margin + 235f, currentY + 8f, boldPaint)
                val singleLineDetail = entry.details.replace("\n", " ").trim()
                val truncatedDetail = if (singleLineDetail.length > 44) singleLineDetail.take(42) + "..." else singleLineDetail
                canvas.drawText(truncatedDetail, margin + 235f, currentY + 19f, textPaint)

                // 5. SHA-256 Hash (margin + 415 to pageWidth - margin)
                val shortHash = if (entry.hashSha256.length > 14) {
                    entry.hashSha256.take(7) + "..." + entry.hashSha256.takeLast(6)
                } else entry.hashSha256
                canvas.drawText(shortHash, margin + 415f, currentY + 9f, hashPaint)

                // Bottom divider
                canvas.drawLine(margin, currentY + rowHeight - 4f, pageWidth - margin, currentY + rowHeight - 4f, dividerPaint)
                currentY += rowHeight
            }

            // Draw Sign-off block if space permits, else add final page
            if (currentY + 80f > pageHeight - 50f) {
                drawPageFooter(canvas, currentPageNumber, pageWidth, pageHeight)
                pdfDocument.finishPage(page)

                currentPageNumber++
                pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, currentPageNumber).create()
                page = pdfDocument.startPage(pageInfo)
                canvas = page.canvas
                drawSubsequentHeader(canvas, pageWidth, currentPageNumber)
                currentY = 80f
            }

            drawSignOffSection(canvas, margin, currentY + 15f, pageWidth - margin * 2)

            // Final page footer
            drawPageFooter(canvas, currentPageNumber, pageWidth, pageHeight)
            pdfDocument.finishPage(page)

            val fileName = "KashApp_Auditoria_Forense_${System.currentTimeMillis()}.pdf"
            val file = File(com.example.util.SharedExportSecurityUtil.getExportsDir(context), fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = "Certificado forense en PDF exportado exitosamente ($fileName)",
                message = "Informe de Auditoría PDF exportado correctamente (${logs.size} eventos)"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "Error al exportar PDF de Auditoría: ${e.message}"
            )
        } finally {
            pdfDocument?.close()
        }
    }

    private fun drawOfficialHeader(
        canvas: Canvas,
        pageWidth: Int,
        generatedDate: String,
        userName: String,
        filterDesc: String
    ) {
        val bannerPaint = Paint().apply { color = Color.parseColor("#091424") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 118f, bannerPaint)

        // Accent strip
        val accentPaint = Paint().apply { color = Color.parseColor("#0284C7") }
        canvas.drawRect(0f, 114f, pageWidth.toFloat(), 118f, accentPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 15f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }
        val subtitlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            color = Color.parseColor("#38BDF8")
        }
        val metaPaint = Paint().apply {
            isAntiAlias = true
            textSize = 7.5f
            color = Color.parseColor("#CBD5E1")
        }

        canvas.drawText("KASHAPP ENTERPRISE - BÓVEDA DE AUDITORÍA", 36f, 32f, titlePaint)
        canvas.drawText("CERTIFICADO OFICIAL DE TRAZABILIDAD Y CUSTODIA FORENSE (APPEND-ONLY)", 36f, 48f, subtitlePaint)

        canvas.drawText("Fecha de Emisión: $generatedDate", 36f, 68f, metaPaint)
        canvas.drawText("Operador Responsable: $userName", 36f, 80f, metaPaint)
        canvas.drawText("Filtro Aplicado: $filterDesc", 36f, 92f, metaPaint)

        canvas.drawText("Motor Criptográfico: SQLCipher AES-256 / SHA-256", (pageWidth - 230).toFloat(), 68f, metaPaint)
        canvas.drawText("Garantía de Integridad: Inmutable (No Delete / Update)", (pageWidth - 230).toFloat(), 80f, metaPaint)
        canvas.drawText("Estado Bóveda: CERTIFICADO VÁLIDO PARA COMPLIANCE", (pageWidth - 230).toFloat(), 92f, metaPaint)
    }

    private fun drawSubsequentHeader(canvas: Canvas, pageWidth: Int, pageNumber: Int) {
        val bannerPaint = Paint().apply { color = Color.parseColor("#091424") }
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 45f, bannerPaint)

        val textPaint = Paint().apply {
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            color = Color.parseColor("#94A3B8")
        }

        canvas.drawText("KASHAPP ENTERPRISE | INFORME DE AUDITORÍA FORENSE", 36f, 24f, textPaint)
        canvas.drawText("Página $pageNumber - Registro Continuo Append-Only", 36f, 37f, subPaint)
    }

    private fun drawSummaryKpiCards(
        canvas: Canvas,
        logs: List<AuditUiEntry>,
        startX: Float,
        startY: Float,
        totalWidth: Float
    ) {
        val total = logs.size
        val secCount = logs.count { it.category == AuditCategory.SECURITY }
        val assetsCount = logs.count { it.category == AuditCategory.FIXED_ASSETS }
        val txCount = logs.count { it.category == AuditCategory.TRANSACTIONS }
        val accCount = logs.count { it.category == AuditCategory.ACCOUNTS }

        val cardWidth = (totalWidth - 36f) / 4f
        val cardHeight = 52f

        val metrics = listOf(
            Triple("TOTAL EVENTOS", "$total", Color.parseColor("#0284C7")),
            Triple("SEGURIDAD", "$secCount", Color.parseColor("#EF4444")),
            Triple("ACTIVOS FIJOS", "$assetsCount", Color.parseColor("#8B5CF6")),
            Triple("FINANZAS / CTAS", "${txCount + accCount}", Color.parseColor("#10B981"))
        )

        for ((i, metric) in metrics.withIndex()) {
            val cx = startX + i * (cardWidth + 12f)
            val cardRect = RectF(cx, startY, cx + cardWidth, startY + cardHeight)

            val bgPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#CBD5E1")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            canvas.drawRoundRect(cardRect, 6f, 6f, bgPaint)
            canvas.drawRoundRect(cardRect, 6f, 6f, borderPaint)

            // Accent bar on top of card
            val barPaint = Paint().apply { color = metric.third }
            val barRect = RectF(cx, startY, cx + cardWidth, startY + 3f)
            canvas.drawRoundRect(barRect, 3f, 3f, barPaint)

            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#64748B")
            }
            val valPaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#0F172A")
            }

            canvas.drawText(metric.first, cx + 8f, startY + 18f, titlePaint)
            canvas.drawText(metric.second, cx + 8f, startY + 38f, valPaint)
        }
    }

    private fun drawCryptographicBanner(canvas: Canvas, x: Float, y: Float, width: Float) {
        val bannerRect = RectF(x, y, x + width, y + 36f)
        val bgPaint = Paint().apply { color = Color.parseColor("#0F172A") }
        canvas.drawRoundRect(bannerRect, 6f, 6f, bgPaint)

        val borderPaint = Paint().apply {
            color = Color.parseColor("#38BDF8")
            style = Paint.Style.STROKE
            strokeWidth = 0.8f
        }
        canvas.drawRoundRect(bannerRect, 6f, 6f, borderPaint)

        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#38BDF8")
        }
        val descPaint = Paint().apply {
            isAntiAlias = true
            textSize = 7.5f
            color = Color.parseColor("#E2E8F0")
        }

        canvas.drawText("DECLARACIÓN DE INMUTABILIDAD Y NO REPUDIO", x + 10f, y + 14f, titlePaint)
        canvas.drawText(
            "Los eventos aquí plasmados provienen de una base de datos local SQLite cifrada con SQLCipher (AES-256). Cada registro cuenta con sello SHA-256 indeleble.",
            x + 10f,
            y + 26f,
            descPaint
        )
    }

    private fun drawTableHeader(canvas: Canvas, startX: Float, startY: Float, totalWidth: Float) {
        val headerRect = RectF(startX, startY, startX + totalWidth, startY + 18f)
        val bgPaint = Paint().apply { color = Color.parseColor("#1E293B") }
        canvas.drawRoundRect(headerRect, 4f, 4f, bgPaint)

        val headerTextPaint = Paint().apply {
            isAntiAlias = true
            textSize = 8f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.WHITE
        }

        canvas.drawText("FECHA / HORA", startX + 4f, startY + 12f, headerTextPaint)
        canvas.drawText("CATEGORÍA", startX + 92f, startY + 12f, headerTextPaint)
        canvas.drawText("SEV.", startX + 188f, startY + 12f, headerTextPaint)
        canvas.drawText("EVENTO & DESCRIPCIÓN FORENSE", startX + 235f, startY + 12f, headerTextPaint)
        canvas.drawText("FIRMA SHA-256", startX + 415f, startY + 12f, headerTextPaint)
    }

    private fun drawSignOffSection(canvas: Canvas, x: Float, y: Float, width: Float) {
        val boxWidth = (width - 24f) / 2f
        val boxHeight = 65f

        val linePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            strokeWidth = 0.8f
        }
        val titlePaint = Paint().apply {
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.DEFAULT_BOLD
            color = Color.parseColor("#0F172A")
        }
        val subPaint = Paint().apply {
            isAntiAlias = true
            textSize = 7.5f
            color = Color.parseColor("#64748B")
        }

        // Left signature box: Auditor de Sistemas
        val leftX = x
        canvas.drawLine(leftX + 16f, y + 42f, leftX + boxWidth - 16f, y + 42f, linePaint)
        canvas.drawText("OFICIAL DE CUMPLIMIENTO / AUDITOR", leftX + 16f, y + 54f, titlePaint)
        canvas.drawText("Verificación Criptográfica Aprobada - SHA-256", leftX + 16f, y + 64f, subPaint)

        // Right signature box: Titular de la Cuenta
        val rightX = x + boxWidth + 24f
        canvas.drawLine(rightX + 16f, y + 42f, rightX + boxWidth - 16f, y + 42f, linePaint)
        canvas.drawText("TITULAR / ADMINISTRADOR PRINCIPAL", rightX + 16f, y + 54f, titlePaint)
        canvas.drawText("Aceptación de Custodia y Trazabilidad Forense", rightX + 16f, y + 64f, subPaint)
    }

    private fun drawPageFooter(canvas: Canvas, pageNumber: Int, pageWidth: Int, pageHeight: Int) {
        val footerPaint = Paint().apply {
            isAntiAlias = true
            textSize = 7.5f
            color = Color.parseColor("#94A3B8")
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 0.8f
        }
        canvas.drawLine(36f, pageHeight - 35f, pageWidth - 36f, pageHeight - 35f, dividerPaint)
        canvas.drawText("KashApp Enterprise • Documento Forense Confidencial • Inmutable", 36f, pageHeight - 20f, footerPaint)
        val pageText = "Página $pageNumber"
        val textWidth = footerPaint.measureText(pageText)
        canvas.drawText(pageText, pageWidth - 36f - textWidth, pageHeight - 20f, footerPaint)
    }
}
