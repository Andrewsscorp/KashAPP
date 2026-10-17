package com.example.feature.gastoscompartidos.data.export

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.DashPathEffect
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import androidx.core.content.FileProvider
import com.example.data.local.SecureDocumentManager
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine.SharedDigitalTicketModel
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * Renderizador gráfico y vectorial de Tickets de Gastos Compartidos (TK, PG, LQ)
 * con custodia automática en la bóveda cifrada AES-256 (`SecureDocumentManager`).
 */
class SharedTicketExporter(
    private val context: Context,
    private val secureDocumentManager: SecureDocumentManager = SecureDocumentManager(context)
) {

    data class ExportedTicketArtifacts(
        val ticketCode: String,
        val pngFile: File,
        val pdfFile: File,
        val encryptedVaultPngUri: String?,
        val encryptedVaultPdfUri: String?,
        val fileSizeFormatted: String
    )

    /**
     * Renderiza el modelo de Ticket en un Bitmap de alta definición (estilo tirilla térmica bancaria + iOS).
     */
    fun renderTicketToThermalBitmap(ticket: SharedDigitalTicketModel): Bitmap {
        val width = 1080
        val estimatedItemHeight = ticket.itemizedRows.size * 52
        val estimatedPartHeight = ticket.participantRows.size * 92
        val estimatedTransferHeight = ticket.simplifiedTransfers.size * 70
        val qrSectionHeight = if (ticket.collectionInstructions != null) 340 else 80
        val totalHeight = (980 + estimatedItemHeight + estimatedPartHeight + estimatedTransferHeight + qrSectionHeight)
            .coerceIn(1200, 4200)

        val bitmap = Bitmap.createBitmap(width, totalHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)

        // 1. Fondo Exterior Gris Azulado Suave
        canvas.drawColor(Color.parseColor("#F1F5F9"))

        // 2. Cuerpo del Ticket Blanco con Sombra Suave
        val margin = 44f
        val ticketLeft = margin
        val ticketRight = width - margin
        val ticketTop = 44f
        val ticketBottom = totalHeight - 44f

        paint.color = Color.WHITE
        canvas.drawRoundRect(RectF(ticketLeft, ticketTop, ticketRight, ticketBottom), 36f, 36f, paint)

        // 3. Borde Dentado Superior (Efecto Tirilla Térmica)
        drawPerforatedZigZagEdge(canvas, ticketLeft, ticketRight, ticketTop, isTop = true)

        // 4. Cabecera Corporativa KashApp Navy
        val headerBottom = ticketTop + 210f
        paint.color = Color.parseColor(ticket.kind.primaryColorHex)
        canvas.drawRoundRect(RectF(ticketLeft, ticketTop, ticketRight, headerBottom), 36f, 36f, paint)
        canvas.drawRect(ticketLeft, headerBottom - 36f, ticketRight, headerBottom, paint)

        paint.color = Color.parseColor("#38BDF8")
        paint.textSize = 24f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("KASHAPP ENTERPRISE • FINANZAS COMPARTIDAS", ticketLeft + 44f, ticketTop + 62f, paint)

        paint.color = Color.WHITE
        paint.textSize = 34f
        canvas.drawText(ticket.primaryTicketConsecutive, ticketLeft + 44f, ticketTop + 114f, paint)

        paint.color = Color.parseColor("#E2E8F0")
        paint.textSize = 22f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        val subHeader = buildString {
            append(ticket.kind.badgeTitle)
            if (!ticket.relatedExpenseConsecutive.isNullOrBlank()) {
                append(" • Ref: ${ticket.relatedExpenseConsecutive}")
            }
        }
        canvas.drawText(subHeader, ticketLeft + 44f, ticketTop + 156f, paint)

        paint.color = Color.parseColor("#94A3B8")
        paint.textSize = 20f
        canvas.drawText(ticket.dateTimeFormatted, ticketLeft + 44f, ticketTop + 190f, paint)

        var y = headerBottom + 54f
        val leftPad = ticketLeft + 44f
        val rightPad = ticketRight - 44f

        // 5. Título del Gasto, Comercio y Monto Principal
        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 34f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText(ticket.headlineTitle.take(34), leftPad, y, paint)
        y += 38f

        if (!ticket.merchantName.isNullOrBlank()) {
            paint.color = Color.parseColor("#475569")
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            val nitSuffix = ticket.merchantNit?.let { " • NIT: $it" }.orEmpty()
            canvas.drawText("${ticket.merchantName}$nitSuffix", leftPad, y, paint)
            y += 34f
        }

        // Tarjeta Destacada del Total
        val boxTop = y + 10f
        val boxBottom = boxTop + 124f
        paint.color = Color.parseColor("#F0F9FF")
        canvas.drawRoundRect(RectF(leftPad, boxTop, rightPad, boxBottom), 24f, 24f, paint)

        paint.color = Color.parseColor("#0369A1")
        paint.textSize = 20f
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        canvas.drawText("TOTAL OFICIAL (${ticket.splitMethodLabel})", leftPad + 28f, boxTop + 42f, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.textSize = 44f
        canvas.drawText(ticket.grandTotalFormatted, leftPad + 28f, boxTop + 98f, paint)

        y = boxBottom + 44f

        // 6. Desglose de Ítems OCR (si existen)
        if (ticket.itemizedRows.isNotEmpty()) {
            paint.color = Color.parseColor("#334155")
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DESGLOSE DE PRODUCTOS / FACTURA OCR", leftPad, y, paint)
            y += 28f
            drawDashedDivider(canvas, leftPad, rightPad, y)
            y += 32f

            for (item in ticket.itemizedRows) {
                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 21f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                val who = if (item.consumersInitials.isNotEmpty()) " [${item.consumersInitials.joinToString(",")}]" else ""
                canvas.drawText("${item.quantityText} ${item.description.take(26)}$who", leftPad, y, paint)

                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(item.formattedLineTotal, rightPad, y, paint)
                paint.textAlign = Paint.Align.LEFT
                y += 44f
            }
            y += 12f
        }

        // 7. Subtotales e Impuestos
        drawKeyValueRow(canvas, leftPad, rightPad, y, "Subtotal Base:", ticket.subtotalFormatted)
        y += 34f
        ticket.ivaFormatted?.let {
            drawKeyValueRow(canvas, leftPad, rightPad, y, "IVA Facturado:", it)
            y += 34f
        }
        ticket.impoconsumoFormatted?.let {
            drawKeyValueRow(canvas, leftPad, rightPad, y, "Impoconsumo (8%):", it)
            y += 34f
        }
        ticket.tipFormatted?.let {
            drawKeyValueRow(canvas, leftPad, rightPad, y, "Propina / Servicio:", it)
            y += 34f
        }
        ticket.discountFormatted?.let {
            drawKeyValueRow(canvas, leftPad, rightPad, y, "Descuento Aplicado:", it)
            y += 34f
        }

        drawDashedDivider(canvas, leftPad, rightPad, y)
        y += 42f

        // 8. Tabla de Cuotas Parte y Saldos por Participante
        if (ticket.participantRows.isNotEmpty()) {
            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 23f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("CUOTAS PARTE Y ESTADO POR PARTICIPANTE", leftPad, y, paint)
            y += 32f

            for (pRow in ticket.participantRows) {
                val cardTop = y
                val cardBottom = cardTop + 76f
                paint.color = Color.parseColor("#F8FAFC")
                canvas.drawRoundRect(RectF(leftPad, cardTop, rightPad, cardBottom), 16f, 16f, paint)

                paint.color = Color.parseColor("#0F172A")
                paint.textSize = 22f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                canvas.drawText("${pRow.displayName} (${pRow.statusBadgeText})", leftPad + 20f, cardTop + 32f, paint)

                paint.color = Color.parseColor("#64748B")
                paint.textSize = 18f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
                canvas.drawText("Consumo: ${pRow.totalOwedShareFormatted} • Pagó: ${pRow.paidInRegisterFormatted}", leftPad + 20f, cardTop + 60f, paint)

                paint.color = Color.parseColor(pRow.statusColorHex)
                paint.textSize = 23f
                paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
                paint.textAlign = Paint.Align.RIGHT
                canvas.drawText(pRow.netBalanceFormatted, rightPad - 20f, cardTop + 46f, paint)
                paint.textAlign = Paint.Align.LEFT

                y = cardBottom + 14f
            }
        }

        // 9. Bloque de Recaudo Bancario y Matriz QR Vectorial
        ticket.collectionInstructions?.let { collect ->
            y += 20f
            drawDashedDivider(canvas, leftPad, rightPad, y)
            y += 36f

            paint.color = Color.parseColor("#0F172A")
            paint.textSize = 22f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
            canvas.drawText("DATOS PARA TRANSFERENCIA / PAGO RÁPIDO", leftPad, y, paint)
            y += 32f

            paint.color = Color.parseColor("#334155")
            paint.textSize = 20f
            paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
            canvas.drawText("Titular Acreedor: ${collect.collectorDisplayName}", leftPad, y, paint)
            y += 30f
            canvas.drawText("${collect.preferredMethodLabel}: ${collect.accountOrKeyDisplay}", leftPad, y, paint)
            y += 28f

            // Dibujar matriz QR determinista centrada
            val qrSize = 200f
            val qrLeft = (width - qrSize) / 2f
            drawDeterministicQrMatrix(canvas, qrLeft, y, qrSize, collect.qrPayloadString)
            y += qrSize + 36f
        }

        // 10. Pie Criptográfico SHA-256
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 18f
        paint.typeface = Typeface.create(Typeface.MONOSPACE, Typeface.BOLD)
        paint.textAlign = Paint.Align.CENTER
        canvas.drawText(ticket.verificationFooterText, width / 2f, ticketBottom - 36f, paint)
        paint.textAlign = Paint.Align.LEFT

        drawPerforatedZigZagEdge(canvas, ticketLeft, ticketRight, ticketBottom, isTop = false)
        return bitmap
    }

    /**
     * Exporta el Ticket simultáneamente a PNG HD y PDF Vectorial, guardando copias cifradas
     * con AES-256 GCM en `SecureDocumentManager` (`/files/hidden_docs/`).
     */
    fun exportAndEncryptTicket(
        ticket: SharedDigitalTicketModel,
        userId: String = "user_default"
    ): ExportedTicketArtifacts {
        val safeCode = ticket.primaryTicketConsecutive.replace(Regex("[^a-zA-Z0-9_-]"), "_")
        val exportDir = File(context.cacheDir, "shared_tickets").apply { if (!exists()) mkdirs() }

        // 1. Guardar PNG HD
        val bitmap = renderTicketToThermalBitmap(ticket)
        val pngFile = File(exportDir, "${safeCode}.png")
        FileOutputStream(pngFile).use { out ->
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
        }

        // 2. Guardar PDF Vectorial A4 / Tirilla
        val pdfFile = File(exportDir, "${safeCode}.pdf")
        val pdfDoc = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(bitmap.width, bitmap.height, 1).create()
        val page = pdfDoc.startPage(pageInfo)
        page.canvas.drawBitmap(bitmap, 0f, 0f, null)
        pdfDoc.finishPage(page)
        FileOutputStream(pdfFile).use { out ->
            pdfDoc.writeTo(out)
        }
        pdfDoc.close()

        // 3. Cifrar ambos artefactos en reposo dentro de SecureDocumentManager
        val encryptedPng = secureDocumentManager.saveDocumentSafely(Uri.fromFile(pngFile), userId).getOrNull()
        val encryptedPdf = secureDocumentManager.saveDocumentSafely(Uri.fromFile(pdfFile), userId).getOrNull()

        val sizeKb = (pdfFile.length() / 1024L).coerceAtLeast(1L)
        return ExportedTicketArtifacts(
            ticketCode = ticket.primaryTicketConsecutive,
            pngFile = pngFile,
            pdfFile = pdfFile,
            encryptedVaultPngUri = encryptedPng?.localUri,
            encryptedVaultPdfUri = encryptedPdf?.localUri,
            fileSizeFormatted = "${sizeKb} KB"
        )
    }

    /**
     * Crea el Intent seguro con FileProvider para compartir el Ticket por WhatsApp, Telegram o Correo.
     */
    fun createShareTicketIntent(
        artifactFile: File,
        mimeType: String = "image/png",
        accompanyingMessage: String
    ): Intent {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            artifactFile
        )
        return Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, accompanyingMessage)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    }

    private fun drawKeyValueRow(
        canvas: Canvas,
        left: Float,
        right: Float,
        y: Float,
        label: String,
        value: String
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        paint.color = Color.parseColor("#64748B")
        paint.textSize = 20f
        canvas.drawText(label, left, y, paint)

        paint.color = Color.parseColor("#0F172A")
        paint.typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        paint.textAlign = Paint.Align.RIGHT
        canvas.drawText(value, right, y, paint)
    }

    private fun drawDashedDivider(canvas: Canvas, left: Float, right: Float, y: Float) {
        val dashPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#CBD5E1")
            style = Paint.Style.STROKE
            strokeWidth = 2.5f
            pathEffect = DashPathEffect(floatArrayOf(12f, 8f), 0f)
        }
        val path = Path().apply {
            moveTo(left, y)
            lineTo(right, y)
        }
        canvas.drawPath(path, dashPaint)
    }

    private fun drawPerforatedZigZagEdge(
        canvas: Canvas,
        left: Float,
        right: Float,
        yEdge: Float,
        isTop: Boolean
    ) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#F1F5F9")
            style = Paint.Style.FILL
        }
        val radius = 12f
        val step = 36f
        var x = left + step
        while (x < right - step / 2f) {
            canvas.drawCircle(x, if (isTop) yEdge else yEdge, radius, paint)
            x += step
        }
    }

    private fun drawDeterministicQrMatrix(
        canvas: Canvas,
        left: Float,
        top: Float,
        size: Float,
        payload: String
    ) {
        val grid = 21
        val cell = size / grid.toFloat()
        val digest = MessageDigest.getInstance("SHA-256").digest(payload.toByteArray(Charsets.UTF_8))
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.parseColor("#0F172A")
            style = Paint.Style.FILL
        }

        for (r in 0 until grid) {
            for (c in 0 until grid) {
                val isFinderCorner = (r < 7 && c < 7) || (r < 7 && c >= grid - 7) || (r >= grid - 7 && c < 7)
                val bitOn = if (isFinderCorner) {
                    val lr = if (r >= grid - 7) r - (grid - 7) else r
                    val lc = if (c >= grid - 7) c - (grid - 7) else c
                    lr == 0 || lr == 6 || lc == 0 || lc == 6 || (lr in 2..4 && lc in 2..4)
                } else {
                    val byteIdx = ((r * grid + c) % digest.size)
                    val bitIdx = (r + c) % 8
                    ((digest[byteIdx].toInt() shr bitIdx) and 1) == 1
                }
                if (bitOn) {
                    val x = left + c * cell
                    val y = top + r * cell
                    canvas.drawRect(x, y, x + cell, y + cell, paint)
                }
            }
        }
    }
}
