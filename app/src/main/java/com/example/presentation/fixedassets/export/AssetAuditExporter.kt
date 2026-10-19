package com.example.presentation.fixedassets.export

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import com.example.data.local.entity.AssetAuditLogEntity
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AssetAuditExporter {

    fun exportToPdf(
        context: Context,
        auditLogs: List<AssetAuditLogEntity>,
        title: String = "Libro Oficial de Auditoría y Trazabilidad de Activos Fijos"
    ): File {
        val document = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 standard
        val page = document.startPage(pageInfo)
        val canvas: Canvas = page.canvas

        val paint = Paint().apply { isAntiAlias = true }
        var yPos = 40f

        // Header Background
        paint.color = Color.parseColor("#071221")
        canvas.drawRect(0f, 0f, 595f, 100f, paint)

        // Title
        paint.color = Color.WHITE
        paint.textSize = 15f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KASHAPP • SISTEMA DE GESTIÓN PATRIMONIAL", 30f, yPos, paint)

        yPos += 22f
        paint.textSize = 13f
        paint.color = Color.parseColor("#38BDF8")
        canvas.drawText(title.uppercase(), 30f, yPos, paint)

        yPos += 18f
        paint.textSize = 9.5f
        paint.color = Color.parseColor("#94A3B8")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val nowStr = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
        canvas.drawText("Generado el: $nowStr • Trazabilidad Contable NIIF / NIC 16 • Total Eventos: ${auditLogs.size}", 30f, yPos, paint)

        yPos = 120f

        // Table Header
        paint.color = Color.parseColor("#0E223D")
        canvas.drawRect(25f, yPos, 570f, yPos + 24f, paint)

        paint.color = Color.WHITE
        paint.textSize = 9f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("FECHA / HORA", 32f, yPos + 16f, paint)
        canvas.drawText("ACCIÓN", 130f, yPos + 16f, paint)
        canvas.drawText("ACTIVO", 210f, yPos + 16f, paint)
        canvas.drawText("DETALLE / IMPACTO", 310f, yPos + 16f, paint)
        canvas.drawText("AUDITOR", 500f, yPos + 16f, paint)

        yPos += 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)

        val itemsToDraw = auditLogs.take(28)
        for ((index, log) in itemsToDraw.withIndex()) {
            if (yPos > 740f) break

            if (index % 2 == 1) {
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRect(25f, yPos - 12f, 570f, yPos + 12f, paint)
            }

            paint.color = Color.parseColor("#1E293B")
            paint.textSize = 8.5f

            // Date
            canvas.drawText(log.formattedDate.take(16), 32f, yPos, paint)

            // Action
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            val actionColor = when (log.actionType) {
                "CREACION" -> Color.parseColor("#0284C7")
                "BAJA" -> Color.parseColor("#DC2626")
                "DEPRECIACION" -> Color.parseColor("#7C3AED")
                "MANTENIMIENTO" -> Color.parseColor("#D97706")
                else -> Color.parseColor("#2563EB")
            }
            paint.color = actionColor
            canvas.drawText(log.actionType.take(12), 130f, yPos, paint)

            // Asset
            paint.color = Color.parseColor("#0F172A")
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val assetName = (log.assetName ?: "Módulo General").take(18)
            canvas.drawText(assetName, 210f, yPos, paint)

            // Detail
            val detailPreview = log.details.take(36)
            canvas.drawText(detailPreview, 310f, yPos, paint)

            // Auditor
            val userStr = log.userRole.take(14)
            canvas.drawText(userStr, 500f, yPos, paint)

            yPos += 20f
        }

        // Bottom Certification Signature Section
        yPos = 780f
        paint.color = Color.parseColor("#CBD5E1")
        canvas.drawLine(40f, yPos, 220f, yPos, paint)
        canvas.drawLine(360f, yPos, 540f, yPos, paint)

        yPos += 14f
        paint.textSize = 8f
        paint.color = Color.parseColor("#64748B")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        canvas.drawText("Firma Auditor Interno / Revisor Fiscal", 45f, yPos, paint)
        canvas.drawText("Firma Contador General / Dirección Financiera", 365f, yPos, paint)

        document.finishPage(page)

        val fileName = "Auditoria_Activos_Fijos_${System.currentTimeMillis()}.pdf"
        val file = File(com.example.util.SharedExportSecurityUtil.getExportsDir(context), fileName)
        FileOutputStream(file).use { out ->
            document.writeTo(out)
        }
        document.close()
        return file
    }

    fun exportToExcel(
        context: Context,
        auditLogs: List<AssetAuditLogEntity>
    ): File {
        val fileName = "Auditoria_Activos_Fijos_${System.currentTimeMillis()}.csv"
        val file = File(com.example.util.SharedExportSecurityUtil.getExportsDir(context), fileName)

        file.bufferedWriter(Charsets.UTF_8).use { writer ->
            // UTF-8 BOM for Microsoft Excel compatibility
            writer.write("\uFEFF")
            // Header
            writer.write("ID;Fecha y Hora;Tipo de Acción;Título de Acción;Activo;Detalle de la Operación;Impacto Financiero;Usuario Auditor;Timestamp\n")

            for (log in auditLogs) {
                val cleanDetails = log.details.replace(";", ",").replace("\n", " ")
                val cleanTitle = log.actionTitle.replace(";", ",")
                val cleanAsset = (log.assetName ?: "").replace(";", ",")
                val cleanImpact = (log.financialImpact ?: "").replace(";", ",")
                val cleanRole = log.userRole.replace(";", ",")

                writer.write("${log.id};${log.formattedDate};${log.actionType};$cleanTitle;$cleanAsset;$cleanDetails;$cleanImpact;$cleanRole;${log.timestamp}\n")
            }
        }
        return file
    }
}
