package com.example.presentation.fixedassets.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.util.FinancialReportExporter.ExportResult
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Enterprise PDF Exporter for Fixed Assets compliant with NIIF / NIC 16.
 * Generates:
 * 1. Single-asset executive "Ficha Técnica y Hoja de Vida de Activo Fijo" with identification,
 *    financial valuation, depreciation policies, and legal sign-off boxes.
 * 2. Multi-asset paginated inventory report with KPI summary cards, alternating shaded rows,
 *    right-aligned currency columns, status pills, and summary totals row.
 */
object FixedAssetPdfExporter {

    private val moneyFmt = DecimalFormat("$ #,##0")
    private val dateFmt = DateTimeFormatter.ofPattern("dd/MM/yyyy", Locale("es", "CO"))

    fun exportToPdf(
        context: Context,
        assets: List<FixedAssetEntity>,
        userName: String = "Usuario KashApp",
        filterDesc: String = "Todos los Activos Fijos"
    ): ExportResult {
        if (assets.isEmpty()) {
            return ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "No hay activos seleccionados para exportar"
            )
        }

        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points

            if (assets.size == 1) {
                renderSingleAssetDossier(pdfDocument, assets.first(), pageWidth, pageHeight)
            } else {
                renderMultiAssetReport(pdfDocument, assets, filterDesc, pageWidth, pageHeight)
            }

            val fileName = if (assets.size == 1) {
                "FichaTecnica_${assets.first().assetCode.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            } else {
                "KashApp_Inventario_ActivosFijos_${System.currentTimeMillis()}.pdf"
            }

            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = "Reporte PDF oficial generado exitosamente ($fileName)",
                message = "PDF de Activos Fijos exportado correctamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "Error al exportar PDF de Activos Fijos: ${e.message}"
            )
        } finally {
            pdfDocument?.close()
        }
    }

    /**
     * Executive Technical Dossier (Ficha Técnica / Hoja de Vida) for an individual asset.
     */
    private fun renderSingleAssetDossier(
        doc: PdfDocument,
        asset: FixedAssetEntity,
        pageWidth: Int,
        pageHeight: Int
    ) {
        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = doc.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        // Palette
        val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
        val headerPaint = Paint().apply { color = Color.parseColor("#091424") }
        val cardPaint = Paint().apply { color = Color.WHITE }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        // Typography
        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            isAntiAlias = true
        }
        val sectionHeaderPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val labelPaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8.5f
            isAntiAlias = true
        }
        val valPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val highlightPaint = Paint().apply {
            color = Color.parseColor("#0284C7")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val greenValPaint = Paint().apply {
            color = Color.parseColor("#059669")
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }

        // Draw page bg
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

        // 1. Executive Top Banner
        canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, headerPaint)
        val accentBar = Paint().apply { color = Color.parseColor("#0284C7") }
        canvas.drawRect(0f, 92f, pageWidth.toFloat(), 95f, accentBar)

        canvas.drawText("KASHAPP • SISTEMA DE GESTIÓN PATRIMONIAL Y NIIF", 36f, 32f, subtitlePaint)
        canvas.drawText("FICHA TÉCNICA Y HOJA DE VIDA DE ACTIVO FIJO", 36f, 54f, titlePaint)
        val todayStr = LocalDate.now().format(dateFmt)
        canvas.drawText("Documento Oficial • Generado: $todayStr • Norma NIC 16 / NIIF para Pymes", 36f, 74f, subtitlePaint)

        // Asset State Badge floating in header with state-specific color
        val state = AssetStateMachine.resolveState(asset.status)
        val stateColorHex = try {
            when (state.code) {
                "EN_USO", "ACTIVE" -> "#059669"
                "EN_MANTENIMIENTO", "MAINTENANCE" -> "#D97706"
                "DADO_DE_BAJA", "WRITTEN_OFF", "DISPOSED" -> "#DC2626"
                "MANTENIDO_PARA_LA_VENTA" -> "#0284C7"
                "DISPONIBLE" -> "#4F46E5"
                else -> "#0284C7"
            }
        } catch (_: Exception) { "#0284C7" }

        val badgeBg = Paint().apply { color = Color.parseColor("#0F2942") }
        val badgeBorder = Paint().apply {
            color = Color.parseColor(stateColorHex)
            style = Paint.Style.STROKE
            strokeWidth = 1.5f
        }
        val badgeText = Paint().apply {
            color = Color.parseColor(stateColorHex)
            textSize = 9.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val badgeRect = RectF(pageWidth - 175f, 36f, pageWidth - 36f, 66f)
        canvas.drawRoundRect(badgeRect, 6f, 6f, badgeBg)
        canvas.drawRoundRect(badgeRect, 6f, 6f, badgeBorder)
        canvas.drawText("ESTADO: ${state.shortBadgeLabel.uppercase()}", pageWidth - 165f, 54f, badgeText)

        var curY = 110f

        // 2. BLOCK 1: IDENTIFICACIÓN Y ESPECIFICACIONES GENERALES CON FOTO / EVIDENCIA
        val b1Height = 158f
        val b1Rect = RectF(36f, curY, pageWidth - 36f, curY + b1Height)
        canvas.drawRoundRect(b1Rect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(b1Rect, 8f, 8f, borderPaint)

        canvas.drawText("1. IDENTIFICACIÓN Y REGISTRO PATRIMONIAL", 50f, curY + 22f, sectionHeaderPaint)
        canvas.drawLine(50f, curY + 30f, pageWidth - 50f, curY + 30f, dividerPaint)

        // Try to draw asset photo thumbnail on right
        var hasDrawnPhoto = false
        if (!asset.imageUrl.isNullOrBlank()) {
            try {
                val photoPath = asset.imageUrl
                val file = File(photoPath)
                if (file.exists()) {
                    val opts = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val bmp = BitmapFactory.decodeFile(file.absolutePath, opts)
                    if (bmp != null) {
                        val photoTarget = RectF(pageWidth - 145f, curY + 42f, pageWidth - 50f, curY + 138f)
                        canvas.drawBitmap(bmp, null, photoTarget, null)
                        val photoBorder = Paint().apply {
                            color = Color.parseColor("#94A3B8")
                            style = Paint.Style.STROKE
                            strokeWidth = 1f
                        }
                        canvas.drawRoundRect(photoTarget, 6f, 6f, photoBorder)
                        hasDrawnPhoto = true
                    }
                }
            } catch (_: Exception) {}
        }

        if (!hasDrawnPhoto) {
            // Draw official Asset Identifier Box
            val idBox = RectF(pageWidth - 145f, curY + 45f, pageWidth - 50f, curY + 135f)
            val idBoxBg = Paint().apply { color = Color.parseColor("#F1F5F9") }
            canvas.drawRoundRect(idBox, 6f, 6f, idBoxBg)
            canvas.drawRoundRect(idBox, 6f, 6f, borderPaint)
            val idTitlePaint = Paint().apply {
                color = Color.parseColor("#475569")
                textSize = 8f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            val idCodePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                isAntiAlias = true
            }
            canvas.drawText("PLACA AUDITORÍA", idBox.centerX(), idBox.top + 30f, idTitlePaint)
            canvas.drawText(asset.assetCode, idBox.centerX(), idBox.top + 50f, idCodePaint)
            canvas.drawText("NIIF NIC 16", idBox.centerX(), idBox.top + 68f, subtitlePaint.apply { textAlign = Paint.Align.CENTER })
            subtitlePaint.textAlign = Paint.Align.LEFT
        }

        // Left 2 columns
        val col2X = if (hasDrawnPhoto) 250f else 280f
        drawField(canvas, "Nombre del Activo:", asset.name, 50f, curY + 48f, labelPaint, valPaint)
        drawField(canvas, "Código Patrimonial:", asset.assetCode, col2X, curY + 48f, labelPaint, highlightPaint)

        drawField(canvas, "Categoría NIIF:", asset.category, 50f, curY + 74f, labelPaint, valPaint)
        drawField(canvas, "Tipo de Activo:", if (asset.type == "TANGIBLE") "Activo Tangible (Propiedad, Planta)" else "Activo Intangible", col2X, curY + 74f, labelPaint, valPaint)

        drawField(canvas, "Marca y Modelo:", "${asset.brand ?: "Genérico"} / ${asset.model ?: "Estándar"}", 50f, curY + 100f, labelPaint, valPaint)
        val idExtra = if (!asset.licensePlate.isNullOrBlank()) "Placa: ${asset.licensePlate}" else "Serial: ${asset.serialNumber ?: "N/A"}"
        drawField(canvas, "Serial / Placa:", idExtra, col2X, curY + 100f, labelPaint, valPaint)

        drawField(canvas, "Ubicación Física:", asset.location ?: "Sede Principal", 50f, curY + 126f, labelPaint, valPaint)
        drawField(canvas, "Fecha de Adquisición:", asset.acquisitionDate, col2X, curY + 126f, labelPaint, valPaint)

        curY += b1Height + 14f

        // 3. BLOCK 2: VALORACIÓN Y SALDOS FINANCIEROS (COP)
        val b2Height = 160f
        val b2Rect = RectF(36f, curY, pageWidth - 36f, curY + b2Height)
        canvas.drawRoundRect(b2Rect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(b2Rect, 8f, 8f, borderPaint)

        canvas.drawText("2. VALORACIÓN CONTABLE Y SALDOS NIIF (COP)", 50f, curY + 22f, sectionHeaderPaint)
        canvas.drawLine(50f, curY + 30f, pageWidth - 50f, curY + 30f, dividerPaint)

        val bookValue = AssetFinancialCalculations.calculateCurrentBookValue(
            asset.acquisitionValue, asset.accumulatedDepreciation, asset.residualValue
        )
        val depreciableBase = kotlin.math.max(0.0, asset.acquisitionValue - asset.residualValue)
        val amortizedPct = if (asset.acquisitionValue > 0) ((asset.accumulatedDepreciation / asset.acquisitionValue) * 100).toInt() else 0

        // Row 1
        drawField(canvas, "Costo de Adquisición Inicial:", moneyFmt.format(asset.acquisitionValue), 50f, curY + 50f, labelPaint, valPaint)
        drawField(canvas, "Depreciación Acumulada:", "${moneyFmt.format(asset.accumulatedDepreciation)} ($amortizedPct%)", 320f, curY + 50f, labelPaint, valPaint)

        // Row 2
        drawField(canvas, "Valor Neto en Libros:", moneyFmt.format(bookValue), 50f, curY + 78f, labelPaint, greenValPaint)
        drawField(canvas, "Valor Comercial / Avalúo:", moneyFmt.format(asset.currentMarketValue), 320f, curY + 78f, labelPaint, valPaint)

        // Row 3
        drawField(canvas, "Valor Residual / Salvamento:", moneyFmt.format(asset.residualValue), 50f, curY + 106f, labelPaint, valPaint)
        drawField(canvas, "Base Depreciable NIIF:", moneyFmt.format(depreciableBase), 320f, curY + 106f, labelPaint, highlightPaint)

        // Row 4
        val monthlyDep = AssetFinancialCalculations.calculateMonthlyDepreciation(
            acquisitionValue = asset.acquisitionValue,
            residualValue = asset.residualValue,
            usefulLifeMonths = asset.usefulLifeMonths,
            method = com.example.domain.model.fixedassets.DepreciationMethod.fromString(asset.depreciationMethod)
        )
        drawField(canvas, "Cuota de Depreciación Mensual:", moneyFmt.format(monthlyDep), 50f, curY + 134f, labelPaint, valPaint)
        drawField(canvas, "Última Depreciación Aplicada:", asset.lastDepreciationDate ?: "Sin registro", 320f, curY + 134f, labelPaint, valPaint)

        curY += b2Height + 14f

        // 4. BLOCK 3: POLÍTICAS Y PARÁMETROS CONTABLES DE DEPRECIACIÓN
        val b3Height = 145f
        val b3Rect = RectF(36f, curY, pageWidth - 36f, curY + b3Height)
        canvas.drawRoundRect(b3Rect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(b3Rect, 8f, 8f, borderPaint)

        canvas.drawText("3. POLÍTICA CONTABLE Y OBSERVACIONES TÉCNICAS", 50f, curY + 22f, sectionHeaderPaint)
        canvas.drawLine(50f, curY + 30f, pageWidth - 50f, curY + 30f, dividerPaint)

        // Row 1
        val methodName = when (asset.depreciationMethod) {
            "DIGITS_SUM" -> "Suma de Dígitos (Acelerada)"
            "DECLINING_BALANCE" -> "Saldos Decrecientes"
            else -> "Línea Recta (Constante)"
        }
        drawField(canvas, "Método de Depreciación:", methodName, 50f, curY + 48f, labelPaint, valPaint)
        val lifeDesc = "${asset.usefulLifeMonths} meses (${"%.1f".format(asset.usefulLifeMonths / 12.0)} años)"
        drawField(canvas, "Vida Útil Estimada:", lifeDesc, 320f, curY + 48f, labelPaint, valPaint)

        // Row 2
        drawField(canvas, "Frecuencia de Cálculo:", asset.frequency, 50f, curY + 74f, labelPaint, valPaint)
        drawField(canvas, "Convención de Inicio:", asset.startConvention, 320f, curY + 74f, labelPaint, valPaint)

        // Row 3
        val impairmentLabel = if (asset.impairmentTracking) "Activo (Revisión anual NIC 36)" else "Inactivo"
        drawField(canvas, "Seguimiento Deterioro:", impairmentLabel, 50f, curY + 100f, labelPaint, valPaint)
        val revalLabel = if (asset.revaluationEnabled) "Modelo de Revaluación Habilitado" else "Modelo del Costo Histórico"
        drawField(canvas, "Modelo de Medición Posterior:", revalLabel, 320f, curY + 100f, labelPaint, valPaint)

        // Row 4 - Notas y bitácora
        val notesDisplay = asset.stateNotes ?: asset.notes ?: "Sin novedades registradas al cierre del periodo."
        val notesTrunc = if (notesDisplay.length > 70) notesDisplay.take(68) + ".." else notesDisplay
        drawField(canvas, "Observaciones / Estado:", notesTrunc, 50f, curY + 126f, labelPaint, valPaint)

        curY += b3Height + 14f

        // 5. BLOCK 4: FIRMAS AUTORIZADAS Y CONTROL DE INVENTARIO
        val b4Height = 100f
        val b4Rect = RectF(36f, curY, pageWidth - 36f, curY + b4Height)
        canvas.drawRoundRect(b4Rect, 8f, 8f, cardPaint)
        canvas.drawRoundRect(b4Rect, 8f, 8f, borderPaint)

        canvas.drawText("4. CONTROL LEGAL Y FIRMAS DE RESPONSABILIDAD", 50f, curY + 22f, sectionHeaderPaint)
        canvas.drawLine(50f, curY + 30f, pageWidth - 50f, curY + 30f, dividerPaint)

        // Signature box 1: Custodio
        val sigLinePaint = Paint().apply { color = Color.parseColor("#94A3B8"); strokeWidth = 1f }
        canvas.drawLine(60f, curY + 72f, 240f, curY + 72f, sigLinePaint)
        canvas.drawText("Responsable / Custodio del Activo", 60f, curY + 84f, labelPaint)
        canvas.drawText("C.C. ________________________", 60f, curY + 94f, subtitlePaint)

        // Signature box 2: Contador / Revisor Fiscal
        canvas.drawLine(330f, curY + 72f, 510f, curY + 72f, sigLinePaint)
        canvas.drawText("Contador General / Revisor Fiscal", 330f, curY + 84f, labelPaint)
        canvas.drawText("T.P. ________________________", 330f, curY + 94f, subtitlePaint)

        // Footer
        val footerY = pageHeight - 32f
        canvas.drawText("Documento emitido conforme a las Normas Internacionales de Información Financiera (NIIF / NIC 16)", 36f, footerY, subtitlePaint)
        canvas.drawText("Página 1 de 1", pageWidth - 100f, footerY, subtitlePaint)

        doc.finishPage(page)
    }

    private fun drawField(canvas: Canvas, label: String, value: String, x: Float, y: Float, labelPaint: Paint, valPaint: Paint) {
        canvas.drawText(label, x, y, labelPaint)
        canvas.drawText(value, x, y + 12f, valPaint)
    }

    /**
     * Multi-Asset Paginated Inventory and Depreciation Report.
     */
    private fun renderMultiAssetReport(
        doc: PdfDocument,
        assets: List<FixedAssetEntity>,
        filterDesc: String,
        pageWidth: Int,
        pageHeight: Int
    ) {
        val rowsPerPage = 18
        val totalPages = kotlin.math.max(1, ((assets.size - 1) / rowsPerPage) + 1)

        val totalAcq = assets.sumOf { it.acquisitionValue }
        val totalAccumDep = assets.sumOf { it.accumulatedDepreciation }
        val totalBook = assets.sumOf {
            AssetFinancialCalculations.calculateCurrentBookValue(it.acquisitionValue, it.accumulatedDepreciation, it.residualValue)
        }
        val totalMarket = assets.sumOf { it.currentMarketValue }

        // Common Paints
        val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
        val headerPaint = Paint().apply { color = Color.parseColor("#091424") }
        val cardPaint = Paint().apply { color = Color.WHITE }
        val altRowPaint = Paint().apply { color = Color.parseColor("#F1F5F9") }
        val borderPaint = Paint().apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 1f
        }
        val dividerPaint = Paint().apply {
            color = Color.parseColor("#E2E8F0")
            strokeWidth = 1f
        }

        val titlePaint = Paint().apply {
            color = Color.WHITE
            textSize = 15f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val subtitlePaint = Paint().apply {
            color = Color.parseColor("#94A3B8")
            textSize = 9f
            isAntiAlias = true
        }
        val cardTitlePaint = Paint().apply {
            color = Color.parseColor("#64748B")
            textSize = 8f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cardValuePaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 11.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val tableHeaderPaint = Paint().apply {
            color = Color.parseColor("#475569")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val cellTextPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 8.5f
            isAntiAlias = true
        }
        val cellBoldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            isAntiAlias = true
        }
        val rightAlignTextPaint = Paint().apply {
            color = Color.parseColor("#1E293B")
            textSize = 8.5f
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }
        val rightAlignBoldPaint = Paint().apply {
            color = Color.parseColor("#0F172A")
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            textAlign = Paint.Align.RIGHT
            isAntiAlias = true
        }

        var assetIndex = 0

        for (pageIndex in 1..totalPages) {
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, pageIndex).create()
            val page = doc.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Page BG
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            var curY = 0f

            if (pageIndex == 1) {
                // Page 1 Header Banner
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 95f, headerPaint)
                val accentBar = Paint().apply { color = Color.parseColor("#0284C7") }
                canvas.drawRect(0f, 92f, pageWidth.toFloat(), 95f, accentBar)

                canvas.drawText("KASHAPP • SISTEMA DE GESTIÓN PATRIMONIAL Y NIIF", 36f, 32f, subtitlePaint)
                canvas.drawText("INVENTARIO CONSOLIDADO DE ACTIVOS FIJOS", 36f, 54f, titlePaint)
                val todayStr = LocalDate.now().format(dateFmt)
                canvas.drawText("Filtro: $filterDesc • Generado: $todayStr • Total Ítems: ${assets.size}", 36f, 74f, subtitlePaint)

                // KPI Mini Cards
                val cardWidth = 125f
                val cardHeight = 46f
                val cardY = 110f
                val gap = 6f
                val kpis = listOf(
                    Pair("VALOR ADQUISICIÓN", moneyFmt.format(totalAcq)),
                    Pair("DEP. ACUMULADA", moneyFmt.format(totalAccumDep)),
                    Pair("VALOR EN LIBROS", moneyFmt.format(totalBook)),
                    Pair("VALOR COMERCIAL", moneyFmt.format(totalMarket))
                )

                kpis.forEachIndexed { i, kpi ->
                    val x = 36f + (i * (cardWidth + gap))
                    val kRect = RectF(x, cardY, x + cardWidth, cardY + cardHeight)
                    canvas.drawRoundRect(kRect, 6f, 6f, cardPaint)
                    canvas.drawRoundRect(kRect, 6f, 6f, borderPaint)
                    canvas.drawText(kpi.first, x + 8f, cardY + 17f, cardTitlePaint)
                    canvas.drawText(kpi.second, x + 8f, cardY + 35f, cardValuePaint)
                }

                curY = 175f
            } else {
                // Continuation Header
                canvas.drawRect(0f, 0f, pageWidth.toFloat(), 50f, headerPaint)
                canvas.drawText("KASHAPP • INVENTARIO DE ACTIVOS FIJOS (CONTINUACIÓN)", 36f, 26f, titlePaint)
                canvas.drawText("Filtro: $filterDesc", 36f, 40f, subtitlePaint)
                curY = 65f
            }

            // Table Container
            val tableTop = curY
            val tableHeight = pageHeight - tableTop - 50f
            val tableRect = RectF(36f, tableTop, pageWidth - 36f, tableTop + tableHeight)
            canvas.drawRoundRect(tableRect, 8f, 8f, cardPaint)
            canvas.drawRoundRect(tableRect, 8f, 8f, borderPaint)

            // Table Header Bar
            val thY = tableTop + 20f
            canvas.drawText("CÓDIGO / ACTIVO", 46f, thY, tableHeaderPaint)
            canvas.drawText("CATEGORÍA", 185f, thY, tableHeaderPaint)
            canvas.drawText("ESTADO", 265f, thY, tableHeaderPaint)
            canvas.drawText("V. INICIAL", 375f, thY, rightAlignBoldPaint)
            canvas.drawText("DEP. ACUM.", 465f, thY, rightAlignBoldPaint)
            canvas.drawText("EN LIBROS", 545f, thY, rightAlignBoldPaint)

            canvas.drawLine(36f, tableTop + 28f, pageWidth - 36f, tableTop + 28f, dividerPaint)

            var rowY = tableTop + 44f
            val rowHeight = 26f
            val pageEndIndex = kotlin.math.min(assets.size, assetIndex + rowsPerPage)

            for (i in assetIndex until pageEndIndex) {
                val asset = assets[i]
                val bookVal = AssetFinancialCalculations.calculateCurrentBookValue(
                    asset.acquisitionValue, asset.accumulatedDepreciation, asset.residualValue
                )

                // Alternate shading
                if (i % 2 == 1) {
                    val rowShade = RectF(37f, rowY - 14f, pageWidth - 37f, rowY + rowHeight - 14f)
                    canvas.drawRect(rowShade, altRowPaint)
                }

                // Code + Name
                val nameDisplay = "${asset.assetCode} • ${if (asset.name.length > 20) asset.name.take(18) + ".." else asset.name}"
                canvas.drawText(nameDisplay, 46f, rowY, cellBoldPaint)

                // Category
                val catDisplay = if (asset.category.length > 13) asset.category.take(12) + ".." else asset.category
                canvas.drawText(catDisplay, 185f, rowY, cellTextPaint)

                // State Badge
                val st = AssetStateMachine.resolveState(asset.status)
                val stText = if (st.shortBadgeLabel.length > 11) st.shortBadgeLabel.take(10) + ".." else st.shortBadgeLabel
                canvas.drawText(stText, 265f, rowY, cellTextPaint)

                // Right aligned financial figures
                canvas.drawText(moneyFmt.format(asset.acquisitionValue), 375f, rowY, rightAlignTextPaint)
                canvas.drawText(moneyFmt.format(asset.accumulatedDepreciation), 465f, rowY, rightAlignTextPaint)
                canvas.drawText(moneyFmt.format(bookVal), 545f, rowY, rightAlignBoldPaint)

                canvas.drawLine(46f, rowY + 9f, pageWidth - 46f, rowY + 9f, dividerPaint)
                rowY += rowHeight
            }

            assetIndex = pageEndIndex

            // If last page, draw grand totals row
            if (pageIndex == totalPages) {
                val totalsY = rowY + 4f
                val totalRect = RectF(37f, totalsY - 14f, pageWidth - 37f, totalsY + 18f)
                val totalBg = Paint().apply { color = Color.parseColor("#E2E8F0") }
                canvas.drawRect(totalRect, totalBg)

                canvas.drawText("TOTALES CONSOLIDADOS (${assets.size} ACTIVOS)", 46f, totalsY, cellBoldPaint)
                canvas.drawText(moneyFmt.format(totalAcq), 375f, totalsY, rightAlignBoldPaint)
                canvas.drawText(moneyFmt.format(totalAccumDep), 465f, totalsY, rightAlignBoldPaint)
                canvas.drawText(moneyFmt.format(totalBook), 545f, totalsY, rightAlignBoldPaint)
            }

            // Footer
            val footerY = pageHeight - 25f
            canvas.drawText("Reporte oficial generado por KashApp • Cumple con normativa contable NIIF para Pymes", 36f, footerY, subtitlePaint)
            canvas.drawText("Página $pageIndex de $totalPages", pageWidth - 100f, footerY, subtitlePaint)

            doc.finishPage(page)
        }
    }
}
