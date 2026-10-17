package com.example.feature.ocr.parser

import com.example.feature.ocr.model.ReceiptItem
import com.example.feature.ocr.model.ReceiptScanResult
import java.text.DecimalFormat
import java.util.Locale

/**
 * Intelligent receipt and invoice parser that extracts financial metadata
 * (Total, Subtotal, Taxes, Merchant, Date, Items) from raw OCR text.
 */
object ReceiptTextParser {

    private val TOTAL_PATTERNS = listOf(
        Regex("""(?i)\b(total|valor\s+total|monto\s+total|pagar|importe\s+total|a\s+pagar)\s*[:=]?\s*[$]?\s*([0-9.,]+)"""),
        Regex("""(?i)\b(total)\b.*?([0-9]{1,3}(?:[.,][0-9]{3})*(?:[.,][0-9]{2})?)""")
    )

    private val SUBTOTAL_PATTERNS = listOf(
        Regex("""(?i)\b(subtotal|sub-total|sub\s+total|base\s+gravable)\s*[:=]?\s*[$]?\s*([0-9.,]+)""")
    )

    private val TAX_PATTERNS = listOf(
        Regex("""(?i)\b(iva(?:\s*\d+%)?|impuesto|tax)\s*[:=]?\s*[$]?\s*([0-9.,]+)""")
    )

    private val DATE_PATTERNS = listOf(
        Regex("""\b([0-3]?[0-9][/-][0-1]?[0-9][/-]20[2-3][0-9])\b"""),
        Regex("""\b(20[2-3][0-9][/-][0-1]?[0-9][/-][0-3]?[0-9])\b""")
    )

    fun parse(rawText: String): ReceiptScanResult {
        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        if (lines.isEmpty()) {
            return ReceiptScanResult(rawText = rawText)
        }

        val merchantName = lines.firstOrNull { line ->
            line.length > 2 && !line.any { it.isDigit() } && line.length < 50
        } ?: lines.firstOrNull() ?: "Comercio no identificado"

        var total = 0.0
        for (pattern in TOTAL_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                val numStr = match.groupValues.last()
                total = parseMoneyNumber(numStr)
                if (total > 0) break
            }
        }

        var subtotal = 0.0
        for (pattern in SUBTOTAL_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                val numStr = match.groupValues.last()
                subtotal = parseMoneyNumber(numStr)
                if (subtotal > 0) break
            }
        }

        var tax = 0.0
        for (pattern in TAX_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                val numStr = match.groupValues.last()
                tax = parseMoneyNumber(numStr)
                if (tax > 0) break
            }
        }

        var dateStr = ""
        for (pattern in DATE_PATTERNS) {
            val match = pattern.find(rawText)
            if (match != null) {
                dateStr = match.value
                break
            }
        }

        // Extracted line items
        val items = mutableListOf<ReceiptItem>()
        val itemRegex = Regex("""^([A-Za-zÀ-ÿ0-9\s]{3,30})\s+([0-9.,]+)$""")
        for (line in lines) {
            val itemMatch = itemRegex.find(line)
            if (itemMatch != null) {
                val desc = itemMatch.groupValues[1].trim()
                val price = parseMoneyNumber(itemMatch.groupValues[2])
                if (price > 0 && !desc.lowercase(Locale.getDefault()).contains("total")) {
                    items.add(ReceiptItem(description = desc, totalPrice = price))
                }
            }
        }

        if (total == 0.0 && subtotal > 0) {
            total = subtotal + tax
        }

        val suggestedCategory = guessCategory(merchantName, rawText)

        return ReceiptScanResult(
            merchantName = merchantName,
            totalAmount = total,
            subtotal = if (subtotal > 0) subtotal else (total - tax).coerceAtLeast(0.0),
            taxAmount = tax,
            dateString = dateStr,
            items = items,
            rawText = rawText,
            confidence = if (total > 0) 0.96f else 0.70f,
            suggestedCategory = suggestedCategory
        )
    }

    private fun parseMoneyNumber(str: String): Double {
        val cleaned = str.replace("$", "").replace(" ", "").trim()
        if (cleaned.isEmpty()) return 0.0

        val standardized = when {
            // e.g. 1.250.000 or 100.555 or 18.500 (Colombian/Latin thousands separators)
            cleaned.matches(Regex("""^\d{1,3}(\.\d{3})+(,\d{1,2})?$""")) -> {
                cleaned.replace(".", "").replace(",", ".")
            }
            // e.g. 1,250,000.00 or 100,555 (US thousands separators)
            cleaned.matches(Regex("""^\d{1,3}(,\d{3})+(\.\d{1,2})?$""")) -> {
                cleaned.replace(",", "")
            }
            cleaned.matches(Regex("""^\d{1,3}\.\d{3}$""")) -> {
                cleaned.replace(".", "")
            }
            cleaned.count { it == '.' } > 1 -> {
                cleaned.replace(".", "")
            }
            cleaned.contains(",") && cleaned.contains(".") -> {
                if (cleaned.indexOf('.') < cleaned.indexOf(',')) {
                    cleaned.replace(".", "").replace(",", ".")
                } else {
                    cleaned.replace(",", "")
                }
            }
            cleaned.contains(",") -> {
                cleaned.replace(",", ".")
            }
            else -> cleaned
        }
        return standardized.toDoubleOrNull() ?: 0.0
    }

    private fun guessCategory(merchant: String, fullText: String): String {
        val text = "${merchant.lowercase()} ${fullText.lowercase()}"
        return when {
            listOf("exito", "carulla", "jumbo", "d1", "ara", "supermercado", "olimpica", "alimentos", "restaurante", "cafe", "comida").any { text.contains(it) } -> "Alimentación"
            listOf("gasolina", "terpel", "esso", "mobil", "uber", "taxi", "peaje", "transporte").any { text.contains(it) } -> "Transporte"
            listOf("farmacia", "drogueria", "cruz verde", "clinica", "eps", "salud").any { text.contains(it) } -> "Salud"
            listOf("claro", "tigo", "movistar", "epm", "enel", "acueducto", "servicios").any { text.contains(it) } -> "Servicios"
            listOf("computador", "tecnologia", "apple", "samsung", "electronica", "software").any { text.contains(it) } -> "Tecnología"
            else -> "Gastos Generales"
        }
    }
}
