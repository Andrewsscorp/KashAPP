package com.example.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.widget.Toast
import androidx.core.content.FileProvider
import com.example.domain.models.AccountMovementDisplay
import com.example.domain.models.CardSubPurchaseItem
import com.example.domain.models.FinancialRubroType
import com.example.domain.models.RubroItemDisplay
import java.io.File
import java.io.FileOutputStream
import java.text.DecimalFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

object FinancialReportExporter {

    data class ExportResult(
        val success: Boolean,
        val filePath: String?,
        val fileUri: Uri?,
        val summaryText: String,
        val message: String
    )

    /**
     * Generates a genuine formatted PDF file with headers, tables, and metrics.
     */
    fun exportToPdf(
        context: Context,
        rubroType: FinancialRubroType,
        totalAmountFormatted: String,
        percentageNetWorth: Double,
        items: List<RubroItemDisplay>,
        appliedFiltersDesc: String
    ): ExportResult {
        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 standard width in points
            val pageHeight = 842 // A4 standard height in points
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            // Paints
            val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val headerPaint = Paint().apply { color = Color.parseColor("#0F172A") }
            val accentPaint = Paint().apply { color = Color.parseColor("#0284C7") }
            val cardBgPaint = Paint().apply { color = Color.WHITE }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val titlePaint = Paint().apply {
                color = Color.WHITE
                textSize = 18f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val subtitlePaint = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 10f
                isAntiAlias = true
            }

            val sectionTitlePaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 13f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val boldTextPaint = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val bodyPaint = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 9.5f
                isAntiAlias = true
            }

            val subBodyPaint = Paint().apply {
                color = Color.parseColor("#64748B")
                textSize = 8.5f
                isAntiAlias = true
            }

            val greenPaint = Paint().apply {
                color = Color.parseColor("#059669")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            val redPaint = Paint().apply {
                color = Color.parseColor("#DC2626")
                textSize = 10.5f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            // Fill page background
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)

            // Draw Header Banner (Matching exact image.png style)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)
            canvas.drawText("KashApp Financial Suite - ${rubroType.title}", 24f, 38f, titlePaint.apply { textSize = 17f })
            val dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd", Locale("es", "CO")))
            canvas.drawText("Rubro: ${rubroType.title} • Fecha: $dateStr", 24f, 58f, subtitlePaint)
            canvas.drawText("Filtros: $appliedFiltersDesc • Moneda: COP", 24f, 76f, subtitlePaint)

            // Balance Summary Card (Matching exact image.png white rounded card)
            var currentY = 120f
            canvas.drawRoundRect(24f, currentY, (pageWidth - 24).toFloat(), currentY + 60f, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(24f, currentY, (pageWidth - 24).toFloat(), currentY + 60f, 8f, 8f, borderPaint)

            val isDebtRubro = rubroType == FinancialRubroType.DEBTS
            val totalLabel = if (isDebtRubro) "SALDO / DEUDA TOTAL DEL RUBRO:" else "SALDO / TOTAL DEL RUBRO:"
            canvas.drawText(totalLabel, 36f, currentY + 22f, subBodyPaint.apply { color = Color.parseColor("#64748B") })
            canvas.drawText(
                "$totalAmountFormatted COP",
                36f,
                currentY + 46f,
                if (isDebtRubro) redPaint.apply { textSize = 16f } else greenPaint.apply { textSize = 16f }
            )

            currentY += 75f

            // Table Header Bar (Exact 4 columns from image.png: Concepto/Movimiento, Categoría/Fecha, Monto, % Total)
            val tableHeaderBg = Paint().apply { color = Color.parseColor("#E2E8F0") }
            canvas.drawRect(24f, currentY, (pageWidth - 24).toFloat(), currentY + 25f, tableHeaderBg)
            canvas.drawText("Concepto / Movimiento", 32f, currentY + 17f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0F172A") })
            canvas.drawText("Categoría / Fecha", 240f, currentY + 17f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0F172A") })
            canvas.drawText("Monto", 380f, currentY + 17f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0F172A") })
            canvas.drawText("% Total", 490f, currentY + 17f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0F172A") })
            currentY += 30f

            // Render Accounts and movements in 4-column formatted rows
            var rowCount = 0
            items.forEach { item ->
                if (currentY > pageHeight - 90f) return@forEach

                // Account Sub-Header Band
                canvas.drawRect(24f, currentY - 6f, (pageWidth - 24).toFloat(), currentY + 18f, Paint().apply { color = Color.parseColor("#E0F2FE") })
                val accRateStr = if (item.rateEa != null) " • ${item.rateEa}% EA" else ""
                canvas.drawText("CUENTA: ${item.name} (${item.institution} • ${item.category})$accRateStr", 32f, currentY + 10f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0284C7") })
                val subtotalStr = "Subtotal: $ ${DecimalFormat("#,##0").format(item.amount.toDouble())}"
                canvas.drawText(subtotalStr, 380f, currentY + 10f, boldTextPaint.apply { textSize = 9.5f; color = Color.parseColor("#0F172A") })
                currentY += 26f

                // Account Movements
                if (item.accountMovements.isNotEmpty()) {
                    val accTotal = item.amount.toDouble().coerceAtLeast(1.0)
                    item.accountMovements.forEach { mov ->
                        if (currentY > pageHeight - 65f) return@forEach
                        val rowBg = if (rowCount % 2 == 0) Color.WHITE else Color.parseColor("#F1F5F9")
                        canvas.drawRect(24f, currentY - 6f, (pageWidth - 24).toFloat(), currentY + 18f, Paint().apply { color = rowBg })

                        val movTitle = if (mov.title.length > 28) mov.title.take(26) + ".." else mov.title
                        val titleColor = if (mov.title.contains("Estrategia", ignoreCase = true) || mov.title.contains("Sueldo", ignoreCase = true) || mov.title.contains("Nómina", ignoreCase = true)) {
                            Color.parseColor("#0284C7")
                        } else Color.parseColor("#0F172A")

                        canvas.drawText(movTitle, 32f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = titleColor })
                        val catDate = "${mov.category} • ${mov.dateFormatted}".take(26)
                        canvas.drawText(catDate, 240f, currentY + 10f, bodyPaint.apply { textSize = 9f })
                        val amtStr = "$ ${DecimalFormat("#,##0").format(mov.amount)}"
                        canvas.drawText(amtStr, 380f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = if (isDebtRubro) Color.parseColor("#DC2626") else Color.parseColor("#0F172A") })
                        val pct = if (mov.percentageOfTotal > 0.0) mov.percentageOfTotal else ((mov.amount / accTotal) * 100.0)
                        canvas.drawText("${"%.1f".format(pct)}%", 490f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = Color.parseColor("#0284C7") })

                        rowCount++
                        currentY += 24f
                    }
                }

                // Sub-purchases (Credit Cards)
                if (item.subPurchases.isNotEmpty()) {
                    val cardTotal = item.amount.toDouble().coerceAtLeast(1.0)
                    item.subPurchases.forEach { sub ->
                        if (currentY > pageHeight - 65f) return@forEach
                        val rowBg = if (rowCount % 2 == 0) Color.WHITE else Color.parseColor("#F1F5F9")
                        canvas.drawRect(24f, currentY - 6f, (pageWidth - 24).toFloat(), currentY + 18f, Paint().apply { color = rowBg })

                        val subConcept = "${sub.merchant} (${sub.concept})".take(28)
                        canvas.drawText(subConcept, 32f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = Color.parseColor("#0F172A") })
                        val subCatCuota = "${sub.category} • Cuota ${sub.paidInstallments}/${sub.totalInstallments}".take(26)
                        canvas.drawText(subCatCuota, 240f, currentY + 10f, bodyPaint.apply { textSize = 9f })
                        val amtStr = "$ ${DecimalFormat("#,##0").format(sub.currentBalance)}"
                        canvas.drawText(amtStr, 380f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = Color.parseColor("#DC2626") })
                        val pct = (sub.currentBalance / cardTotal) * 100.0
                        canvas.drawText("${"%.1f".format(pct)}%", 490f, currentY + 10f, boldTextPaint.apply { textSize = 9f; color = Color.parseColor("#0284C7") })

                        rowCount++
                        currentY += 24f
                    }
                }
            }

            // Footer (Matching image.png footer exactly)
            canvas.drawText("Generado automáticamente por KashApp Financial Management • Página 1 de 1", 24f, (pageHeight - 20).toFloat(), subBodyPaint.apply { color = Color.parseColor("#94A3B8"); textSize = 8.5f })

            pdfDocument.finishPage(page)

            // Save PDF File
            val fileName = "KashApp_Reporte_${rubroType.name}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                pdfDocument.writeTo(out)
            }

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Throwable) {
                Uri.fromFile(file)
            }

            val summaryText = buildString {
                append("KashApp Reporte: ${rubroType.title}\n")
                append("Total: $totalAmountFormatted\n")
                append("Registros: ${items.size}\n")
                append("Generado: $dateStr")
            }

            // Trigger Intent Share
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_SUBJECT, "Reporte PDF KashApp - ${rubroType.title}")
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            val chooser = Intent.createChooser(shareIntent, "Abrir / Compartir Reporte PDF").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            try {
                context.startActivity(chooser)
            } catch (_: Throwable) {}

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = summaryText,
                message = "PDF generado y compartido exitosamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            // Fallback clipboard
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val fallbackSummary = "Reporte ${rubroType.title}\nTotal: $totalAmountFormatted\nRegistros: ${items.size}"
            clipboard.setPrimaryClip(ClipData.newPlainText("KashApp Reporte", fallbackSummary))

            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = fallbackSummary,
                message = "Reporte copiado al portapapeles: ${e.message}"
            )
        } finally {
            pdfDocument?.close()
        }
    }

    /**
     * Generates a formatted Excel-compatible CSV file with UTF-8 BOM.
     */
    fun exportToExcelCsv(
        context: Context,
        rubroType: FinancialRubroType,
        totalAmountFormatted: String,
        items: List<RubroItemDisplay>,
        appliedFiltersDesc: String
    ): ExportResult {
        return try {
            val sb = StringBuilder()
            // UTF-8 BOM for Excel compatibility with accents and symbols
            sb.append("\uFEFF")
            sb.append("KashApp Financial Suite - Planilla Detallada con Movimientos\n")
            sb.append("Rubro:;\"${rubroType.title}\"\n")
            sb.append("Fecha de Generación:;${LocalDate.now()}\n")
            sb.append("Filtros Aplicados:;\"$appliedFiltersDesc\"\n")
            sb.append("Total Consolidado:;\"$totalAmountFormatted\"\n\n")

            sb.append("ID;Cuenta / Instrumento;Entidad Bancaria;Categoría / Tipo;Moneda;Saldo / Subtotal;Tasa E.A.;Cupo / Detalle;Estado\n")

            items.forEach { item ->
                val rateStr = if (item.rateEa != null) "${item.rateEa}%" else "N/A"
                val quotaLimitStr = if (item.quotaLimit != null) DecimalFormat("#,##0").format(item.quotaLimit) else "N/A"
                val quotaAvailStr = if (item.availableQuota != null) DecimalFormat("#,##0").format(item.availableQuota) else "N/A"
                val amountFormatted = DecimalFormat("#,##0").format(item.amount.toDouble())

                sb.append("${item.id};\"${item.name}\";\"${item.institution}\";\"${item.category}\";${item.currency};$amountFormatted;$rateStr;$quotaLimitStr;$quotaAvailStr;${item.status}\n")

                // Sub-purchases (Credit Cards)
                item.subPurchases.forEach { sub ->
                    val subOrig = DecimalFormat("#,##0").format(sub.originalAmount)
                    val subBal = DecimalFormat("#,##0").format(sub.currentBalance)
                    sb.append(";;↳ Compra TC: \"${sub.merchant} (${sub.concept})\";${sub.category};COP;Saldo: $subBal (Orig: $subOrig);${sub.rateEa}%;Cuotas: ${sub.paidInstallments}/${sub.totalInstallments};Vence: ${sub.nextPaymentDueDate};En Amortización\n")
                }

                // Account movements (Cash / Accounts)
                item.accountMovements.forEach { mov ->
                    val movAmt = DecimalFormat("#,##0").format(mov.amount)
                    sb.append(";;↳ Movimiento: \"${mov.title}\";${mov.category};COP;Monto: $movAmt (${mov.percentageOfTotal}% subtotal);;Fecha: ${mov.dateFormatted};${mov.details};Registrado\n")
                }
            }

            val fileName = "KashApp_Excel_${rubroType.name}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Throwable) {
                Uri.fromFile(file)
            }

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = sb.toString(),
                message = "Planilla Excel generada exitosamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val fallback = "Planilla ${rubroType.title} - Total: $totalAmountFormatted"
            clipboard.setPrimaryClip(ClipData.newPlainText("KashApp Excel", fallback))

            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = fallback,
                message = "Copiado al portapapeles: ${e.message}"
            )
        }
    }

    /**
     * Export a single account / card with all detailed movements / sub-purchases and % of total
     */
    fun exportAccountToPdf(
        context: Context,
        account: RubroItemDisplay,
        subItems: List<AccountMovementDisplay>
    ): ExportResult {
        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 595
            val pageHeight = 842
            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas: Canvas = page.canvas

            val bgPaint = Paint().apply { color = Color.parseColor("#F8FAFC") }
            val headerPaint = Paint().apply { color = Color.parseColor("#0F172A") }
            val borderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }
            val whiteText = Paint().apply {
                color = Color.WHITE
                textSize = 17f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val subWhite = Paint().apply {
                color = Color.parseColor("#94A3B8")
                textSize = 10f
                isAntiAlias = true
            }
            val boldText = Paint().apply {
                color = Color.parseColor("#0F172A")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val bodyPaint = Paint().apply {
                color = Color.parseColor("#334155")
                textSize = 9.5f
                isAntiAlias = true
            }
            val greenPaint = Paint().apply {
                color = Color.parseColor("#16A34A")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }
            val redPaint = Paint().apply {
                color = Color.parseColor("#DC2626")
                textSize = 11f
                typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                isAntiAlias = true
            }

            canvas.drawRect(0f, 0f, pageWidth.toFloat(), pageHeight.toFloat(), bgPaint)
            canvas.drawRect(0f, 0f, pageWidth.toFloat(), 110f, headerPaint)

            canvas.drawText("KashApp Financial Suite - Detalle de Cuenta", 24f, 38f, whiteText)
            canvas.drawText("Instrumento: ${account.name} (${account.institution}) • Fecha: ${LocalDate.now()}", 24f, 58f, subWhite)
            canvas.drawText("Categoría: ${account.category} • Moneda: ${account.currency}", 24f, 76f, subWhite)

            // Balance Summary Card
            val summaryPaint = Paint().apply { color = Color.WHITE }
            canvas.drawRoundRect(24f, 120f, (pageWidth - 24).toFloat(), 180f, 8f, 8f, summaryPaint)
            canvas.drawRoundRect(24f, 120f, (pageWidth - 24).toFloat(), 180f, 8f, 8f, borderPaint)

            canvas.drawText("SALDO / DEUDA TOTAL DE LA CUENTA:", 36f, 142f, subWhite.apply { color = Color.parseColor("#64748B") })
            val isDebt = account.category.contains("Tarjeta", ignoreCase = true) || account.category.contains("Crédito", ignoreCase = true)
            val amtStr = "$ ${DecimalFormat("#,##0").format(account.amount.toDouble())} ${account.currency}"
            canvas.drawText(amtStr, 36f, 166f, if (isDebt) redPaint.apply { textSize = 16f } else greenPaint.apply { textSize = 16f })

            // Table Header
            canvas.drawRect(24f, 195f, (pageWidth - 24).toFloat(), 220f, Paint().apply { color = Color.parseColor("#E2E8F0") })
            canvas.drawText("Concepto / Movimiento", 32f, 212f, boldText.apply { textSize = 9.5f })
            canvas.drawText("Categoría / Fecha", 240f, 212f, boldText)
            canvas.drawText("Monto", 380f, 212f, boldText)
            canvas.drawText("% Total", 490f, 212f, boldText)

            var yPos = 240f
            subItems.take(22).forEachIndexed { idx, mov ->
                val rowBg = if (idx % 2 == 0) Color.WHITE else Color.parseColor("#F1F5F9")
                canvas.drawRect(24f, yPos - 14f, (pageWidth - 24).toFloat(), yPos + 16f, Paint().apply { color = rowBg })

                canvas.drawText(mov.title.take(30), 32f, yPos + 2f, boldText.apply { textSize = 9f })
                canvas.drawText("${mov.category} • ${mov.dateFormatted}".take(24), 240f, yPos + 2f, bodyPaint)
                val mVal = "$ ${DecimalFormat("#,##0").format(mov.amount)}"
                canvas.drawText(mVal, 380f, yPos + 2f, if (isDebt) redPaint.apply { textSize = 9.5f } else boldText.apply { textSize = 9.5f })
                canvas.drawText("${"%.1f".format(mov.percentageOfTotal)}%", 490f, yPos + 2f, boldText.apply { textSize = 9.5f; color = Color.parseColor("#0284C7") })

                yPos += 26f
            }

            // Footer
            canvas.drawText("Generado automáticamente por KashApp Financial Management • Página 1 de 1", 24f, (pageHeight - 20).toFloat(), subWhite.apply { color = Color.parseColor("#94A3B8"); textSize = 8.5f })

            pdfDocument.finishPage(page)

            val fileName = "KashApp_Cuenta_${account.name.replace(" ", "_")}_${System.currentTimeMillis()}.pdf"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out -> pdfDocument.writeTo(out) }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = "Reporte PDF de ${account.name}",
                message = "Reporte PDF de la cuenta generado exitosamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, null, "", "Error al generar PDF: ${e.message}")
        } finally {
            pdfDocument?.close()
        }
    }

    /**
     * Export a single account / card with all detailed movements to Excel CSV
     */
    fun exportAccountToExcelCsv(
        context: Context,
        account: RubroItemDisplay,
        subItems: List<AccountMovementDisplay>
    ): ExportResult {
        return try {
            val sb = StringBuilder()
            sb.append("\uFEFF")
            sb.append("KashApp Financial Suite - Movimientos de Cuenta\n")
            sb.append("Cuenta / Tarjeta:;\"${account.name}\"\n")
            sb.append("Entidad:;\"${account.institution}\"\n")
            sb.append("Fecha Generación:;${LocalDate.now()}\n")
            sb.append("Saldo Total:;\"$ ${DecimalFormat("#,##0").format(account.amount.toDouble())} ${account.currency}\"\n\n")

            sb.append("ID;Concepto / Comercio;Categoría;Fecha;Monto;Porcentaje del Total (%)\n")
            subItems.forEachIndexed { index, item ->
                val amt = DecimalFormat("#,##0").format(item.amount)
                val pct = "${"%.2f".format(item.percentageOfTotal)}%"
                sb.append("${index + 1};\"${item.title}\";\"${item.category}\";\"${item.dateFormatted}\";$amt;$pct\n")
            }

            val fileName = "KashApp_Excel_Cuenta_${account.name.replace(" ", "_")}_${System.currentTimeMillis()}.csv"
            val file = File(context.cacheDir, fileName)
            FileOutputStream(file).use { out ->
                out.write(sb.toString().toByteArray(Charsets.UTF_8))
            }

            val uri = FileProvider.getUriForFile(context, "${context.packageName}.provider", file)

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = sb.toString(),
                message = "Planilla Excel de la cuenta generada exitosamente"
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ExportResult(false, null, null, "", "Error al exportar Excel: ${e.message}")
        }
    }

    /**
     * Opens the exported PDF or Excel file immediately using the system's default viewer.
     */
    fun openExportedFile(context: Context, uri: Uri, mimeType: String) {
        try {
            val viewIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mimeType)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(viewIntent)
        } catch (e: Exception) {
            Toast.makeText(context, "Abriendo archivo...", Toast.LENGTH_SHORT).show()
            shareExportedFile(context, uri, mimeType, "Archivo Financiero KashApp")
        }
    }

    /**
     * Opens the native Android share sheet for the exported file.
     */
    fun shareExportedFile(context: Context, uri: Uri, mimeType: String, title: String) {
        try {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = mimeType
                putExtra(Intent.EXTRA_SUBJECT, title)
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            val chooser = Intent.createChooser(shareIntent, "Abrir / Compartir $title").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(chooser)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
