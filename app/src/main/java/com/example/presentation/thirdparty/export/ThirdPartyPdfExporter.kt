package com.example.presentation.thirdparty.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.data.local.entity.ThirdPartyEntity
import com.example.util.FinancialReportExporter.ExportResult
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise PDF Exporter for Third Parties (Terceros, Proveedores, Concesionarios, Comercios).
 * Generates official PDF documents with forensic integrity hashes, banking specs, and contact details.
 */
object ThirdPartyPdfExporter {

    fun exportThirdPartyToPdf(
        context: Context,
        thirdParty: ThirdPartyEntity,
        linkedAssetsCount: Int = 0,
        linkedExpensesCount: Int = 0,
        totalPurchasesAmount: Double = 0.0
    ): ExportResult {
        var pdfDocument: PdfDocument? = null
        return try {
            pdfDocument = PdfDocument()
            val pageWidth = 595 // A4 width
            val pageHeight = 842 // A4 height
            val margin = 36f

            val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault())
            val generatedDateStr = dateFormat.format(Date())

            // Paints
            val textPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9.5f
                color = Color.parseColor("#1E293B")
            }
            val boldPaint = Paint().apply {
                isAntiAlias = true
                textSize = 10f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#0F172A")
            }
            val titlePaint = Paint().apply {
                isAntiAlias = true
                textSize = 16f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#0284C7")
            }
            val subPaint = Paint().apply {
                isAntiAlias = true
                textSize = 8.5f
                color = Color.parseColor("#64748B")
            }
            val dividerPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                strokeWidth = 1f
            }
            val sectionPaint = Paint().apply {
                isAntiAlias = true
                textSize = 11f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.parseColor("#0369A1")
            }
            val cardBgPaint = Paint().apply {
                color = Color.parseColor("#F8FAFC")
                style = Paint.Style.FILL
            }
            val cardBorderPaint = Paint().apply {
                color = Color.parseColor("#E2E8F0")
                style = Paint.Style.STROKE
                strokeWidth = 1f
            }

            val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
            val page = pdfDocument.startPage(pageInfo)
            val canvas = page.canvas

            var y = margin + 15f

            // Top Header Bar
            val headerRect = RectF(margin, y, pageWidth - margin, y + 42f)
            val headerBg = Paint().apply {
                color = Color.parseColor("#0284C7")
                style = Paint.Style.FILL
            }
            canvas.drawRoundRect(headerRect, 8f, 8f, headerBg)

            val headerTextPaint = Paint().apply {
                isAntiAlias = true
                textSize = 14f
                typeface = Typeface.DEFAULT_BOLD
                color = Color.WHITE
            }
            canvas.drawText("KASHAPP ENTERPRISE", margin + 14f, y + 25f, headerTextPaint)

            val headerRightPaint = Paint().apply {
                isAntiAlias = true
                textSize = 9f
                color = Color.parseColor("#E0F2FE")
                textAlign = Paint.Align.RIGHT
            }
            canvas.drawText("FICHA TÉCNICA DE TERCERO", pageWidth - margin - 14f, y + 25f, headerRightPaint)

            y += 56f

            // Entity Title & Code
            canvas.drawText(thirdParty.displayName, margin, y, titlePaint)
            y += 14f
            canvas.drawText("Código Oficial: ${thirdParty.displayCode}  |  Documento: ${thirdParty.fullDocument}  |  Categoría: ${thirdParty.category}", margin, y, subPaint)
            y += 8f
            canvas.drawLine(margin, y, pageWidth - margin, y, dividerPaint)
            y += 18f

            // Section 1: Información General
            canvas.drawText("1. INFORMACIÓN GENERAL Y FISCAL", margin, y, sectionPaint)
            y += 12f

            val infoCard = RectF(margin, y, pageWidth - margin, y + 74f)
            canvas.drawRoundRect(infoCard, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(infoCard, 8f, 8f, cardBorderPaint)

            val leftX = margin + 14f
            val midX = margin + 260f
            var cardY = y + 20f

            canvas.drawText("Nombre / Razón Social:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.name, leftX + 130f, cardY, textPaint)

            canvas.drawText("Tipo de Persona:", midX, cardY, boldPaint)
            canvas.drawText(if (thirdParty.personType == "JURIDICA") "Persona Jurídica" else "Persona Natural", midX + 90f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Tipo y Documento:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.fullDocument, leftX + 130f, cardY, textPaint)

            canvas.drawText("Nombre Comercial:", midX, cardY, boldPaint)
            canvas.drawText(thirdParty.commercialName ?: "Mismo nombre", midX + 90f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Categoría / Rol:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.category, leftX + 130f, cardY, textPaint)

            canvas.drawText("Estado en Sistema:", midX, cardY, boldPaint)
            canvas.drawText(if (thirdParty.isActive) "ACTIVO" else "INACTIVO", midX + 90f, cardY, textPaint)

            y += 90f

            // Section 2: Canales de Contacto
            canvas.drawText("2. CANALES DE CONTACTO Y UBICACIÓN", margin, y, sectionPaint)
            y += 12f

            val contactCard = RectF(margin, y, pageWidth - margin, y + 74f)
            canvas.drawRoundRect(contactCard, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(contactCard, 8f, 8f, cardBorderPaint)

            cardY = y + 20f
            canvas.drawText("Teléfono Principal:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.primaryPhone ?: "No registrado", leftX + 130f, cardY, textPaint)

            canvas.drawText("Teléfono Secundario:", midX, cardY, boldPaint)
            canvas.drawText(thirdParty.secondaryPhone ?: "No registrado", midX + 110f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Correo Electrónico:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.email ?: "No registrado", leftX + 130f, cardY, textPaint)

            canvas.drawText("Ciudad / Municipio:", midX, cardY, boldPaint)
            canvas.drawText(thirdParty.city ?: "No registrada", midX + 110f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Dirección Física:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.address ?: "No registrada", leftX + 130f, cardY, textPaint)

            y += 90f

            // Section 3: Datos Financieros y Comerciales
            canvas.drawText("3. CONDICIONES COMERCIALES Y BANCARIAS", margin, y, sectionPaint)
            y += 12f

            val bankCard = RectF(margin, y, pageWidth - margin, y + 74f)
            canvas.drawRoundRect(bankCard, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(bankCard, 8f, 8f, cardBorderPaint)

            cardY = y + 20f
            canvas.drawText("Plazo de Pago:", leftX, cardY, boldPaint)
            canvas.drawText("${thirdParty.paymentTermsDays} días ${if (thirdParty.paymentTermsDays == 0) "(Contado)" else "(Crédito)"}", leftX + 130f, cardY, textPaint)

            canvas.drawText("Entidad Bancaria:", midX, cardY, boldPaint)
            canvas.drawText(thirdParty.defaultBank ?: "No configurado", midX + 110f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Tipo de Cuenta:", leftX, cardY, boldPaint)
            canvas.drawText(thirdParty.bankAccountType ?: "No especificado", leftX + 130f, cardY, textPaint)

            canvas.drawText("Número de Cuenta:", midX, cardY, boldPaint)
            canvas.drawText(thirdParty.bankAccountNumber ?: "No registrado", midX + 110f, cardY, textPaint)

            cardY += 18f
            canvas.drawText("Favorito / Frecuente:", leftX, cardY, boldPaint)
            canvas.drawText(if (thirdParty.isFavorite) "SÍ (Prioritario)" else "NO", leftX + 130f, cardY, textPaint)

            y += 90f

            // Section 4: Resumen de Operaciones
            canvas.drawText("4. OPERACIONES VINCULADAS EN KASHAPP", margin, y, sectionPaint)
            y += 12f

            val opsCard = RectF(margin, y, pageWidth - margin, y + 60f)
            canvas.drawRoundRect(opsCard, 8f, 8f, cardBgPaint)
            canvas.drawRoundRect(opsCard, 8f, 8f, cardBorderPaint)

            cardY = y + 22f
            canvas.drawText("Activos Fijos Suministrados: $linkedAssetsCount", leftX, cardY, boldPaint)
            canvas.drawText("Gastos / Facturas Registradas: $linkedExpensesCount", midX, cardY, boldPaint)

            cardY += 20f
            val formattedPurchases = String.format(Locale.GERMANY, "%,.2f", totalPurchasesAmount)
            canvas.drawText("Total Estimado Acumulado: $formattedPurchases COP", leftX, cardY, boldPaint)

            y += 85f

            // Notes if any
            val notesContent = thirdParty.notes
            if (!notesContent.isNullOrBlank()) {
                canvas.drawText("5. OBSERVACIONES Y NOTAS INTERNAS", margin, y, sectionPaint)
                y += 12f
                val notesCard = RectF(margin, y, pageWidth - margin, y + 40f)
                canvas.drawRoundRect(notesCard, 8f, 8f, cardBgPaint)
                canvas.drawRoundRect(notesCard, 8f, 8f, cardBorderPaint)
                canvas.drawText(notesContent.take(90), margin + 14f, y + 24f, textPaint)
                y += 55f
            }

            // Forensic Integrity Signature & Footer
            val payloadToHash = "${thirdParty.id}|${thirdParty.name}|${thirdParty.idNumber}|${thirdParty.category}|$generatedDateStr"
            val digest = MessageDigest.getInstance("SHA-256")
            val sha256Hex = digest.digest(payloadToHash.toByteArray()).joinToString("") { "%02x".format(it) }

            val footerY = pageHeight - margin - 35f
            canvas.drawLine(margin, footerY, pageWidth - margin, footerY, dividerPaint)

            val hashPaint = Paint().apply {
                isAntiAlias = true
                textSize = 7f
                typeface = Typeface.MONOSPACE
                color = Color.parseColor("#64748B")
            }
            canvas.drawText("SELLO FORENSE SHA-256: $sha256Hex", margin, footerY + 12f, hashPaint)
            canvas.drawText("Generado el: $generatedDateStr por KashApp Enterprise Audit Security Engine.", margin, footerY + 24f, subPaint)
            canvas.drawText("Documento inmutable generado con validez legal interna.", margin, footerY + 34f, subPaint)

            pdfDocument.finishPage(page)

            // Save PDF file in restricted shared_exports directory
            val exportDir = com.example.util.SharedExportSecurityUtil.getExportsDir(context)
            val sanitizedName = thirdParty.name.replace(Regex("[^a-zA-Z0-9]"), "_").take(20)
            val outputFile = File(exportDir, "Tercero_${sanitizedName}_${System.currentTimeMillis()}.pdf")

            FileOutputStream(outputFile).use { out ->
                pdfDocument.writeTo(out)
            }

            val contentUri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                outputFile
            )

            ExportResult(
                success = true,
                filePath = outputFile.absolutePath,
                fileUri = contentUri,
                summaryText = "Ficha de tercero exportada exitosamente.",
                message = "Ficha generada: ${outputFile.name}"
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "Error al exportar ficha técnica: ${e.message}"
            )
        } finally {
            pdfDocument?.close()
        }
    }
}
