package com.example.util

import java.math.BigDecimal
import java.util.Locale

/**
 * Enterprise financial currency formatter with BigDecimal precision, multi-currency support,
 * and Privacy masking.
 */
object MoneyFormatter {

    private val ZERO_DECIMAL_CURRENCIES = setOf(
        "COP", "CLP", "PYG", "KRW", "JPY", "VND", "HUF", "IDR"
    )

    fun format(amount: BigDecimal?, isPrivacyMode: Boolean = false, currencyCode: String = "COP"): String {
        if (isPrivacyMode) {
            return "••••••"
        }
        val safeAmount = amount ?: BigDecimal.ZERO
        val currUpper = currencyCode.trim().uppercase()
        val isZeroDecimal = ZERO_DECIMAL_CURRENCIES.contains(currUpper)

        val symbols = java.text.DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'
            decimalSeparator = ','
        }

        val pattern = if (isZeroDecimal) "#,##0" else "#,##0.00"
        val df = java.text.DecimalFormat(pattern, symbols)
        val formattedNum = df.format(safeAmount.toDouble())

        return when (currUpper) {
            "COP" -> "$ $formattedNum"
            "USD" -> "$ $formattedNum USD"
            "EUR" -> "€ $formattedNum EUR"
            "GBP" -> "£ $formattedNum GBP"
            "MXN" -> "$ $formattedNum MXN"
            "CAD" -> "CA$ $formattedNum"
            "BRL" -> "R$ $formattedNum"
            "PEN" -> "S/ $formattedNum"
            "CLP" -> "$ $formattedNum CLP"
            "JPY" -> "¥ $formattedNum JPY"
            "CHF" -> "CHF $formattedNum"
            else -> "$formattedNum $currUpper"
        }
    }

    fun formatCompact(amount: BigDecimal?, isPrivacyMode: Boolean = false, currencyCode: String = "COP"): String {
        if (isPrivacyMode) return "••••"
        val safeAmount = amount ?: BigDecimal.ZERO
        val value = safeAmount.toDouble()
        val currUpper = currencyCode.trim().uppercase()
        val absVal = kotlin.math.abs(value)
        val symbol = when (currUpper) {
            "EUR" -> "€"
            "GBP" -> "£"
            "JPY" -> "¥"
            else -> "$"
        }
        val suffix = if (currUpper != "COP") " $currUpper" else ""

        return when {
            absVal >= 1_000_000_000 -> "$symbol%.1fB%s".format(Locale.ROOT, value / 1_000_000_000, suffix)
            absVal >= 1_000_000 -> "$symbol%.1fM%s".format(Locale.ROOT, value / 1_000_000, suffix)
            absVal >= 1_000 -> "$symbol%.1fk%s".format(Locale.ROOT, value / 1_000, suffix)
            else -> format(amount, isPrivacyMode, currencyCode)
        }
    }
}
