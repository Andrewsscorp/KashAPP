package com.example.feature.informes.ui.components

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.abs

object InformesFormatters {
    private val copFormat = NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    fun formatMoney(amount: Double, currency: String = "COP"): String {
        val bd = BigDecimal.valueOf(amount).setScale(if (currency == "USD") 2 else 0, RoundingMode.HALF_UP)
        return if (currency == "USD") {
            "US$ %,.2f".format(bd.toDouble())
        } else {
            copFormat.format(bd.toDouble())
        }
    }

    fun formatCompactMoney(amount: Double): String {
        val bd = BigDecimal.valueOf(amount)
        val absVal = abs(bd.toDouble())
        return when {
            absVal >= 1_000_000_000 -> "$%.1fB".format(amount / 1_000_000_000)
            absVal >= 1_000_000 -> "$%.1fM".format(amount / 1_000_000)
            absVal >= 1_000 -> "$%.0fK".format(amount / 1_000)
            else -> "$%.0f".format(amount)
        }
    }

    fun formatVariation(pct: Double): String {
        val bd = BigDecimal.valueOf(pct).setScale(1, RoundingMode.HALF_UP)
        val sign = if (bd.toDouble() > 0) "+" else ""
        return "$sign%.1f%%".format(bd.toDouble())
    }
}
