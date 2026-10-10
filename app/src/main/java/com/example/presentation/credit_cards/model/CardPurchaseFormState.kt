package com.example.presentation.credit_cards.model

import android.net.Uri
import com.example.presentation.expense.ExpenseCategory
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

/**
 * Data Model for Credit Card Presentation in UI and calculations.
 */
data class CreditCardDisplayItem(
    val id: String,
    val name: String,
    val bankName: String,
    val mask: String,
    val quotaLimit: Double,
    val availableQuota: Double,
    val cutoffDay: Int = 15,
    val paymentDueDay: Int = 2,
    val interestRateEa: Double = 24.8,
    val cardBrand: String = "Visa",
    val colorHex: String = "#0284C7",
    val allowsOverdraft: Boolean = false
) {
    val usedQuota: Double get() = (quotaLimit - availableQuota).coerceAtLeast(0.0)
    val usagePercentage: Float get() = if (quotaLimit > 0) (usedQuota / quotaLimit).toFloat().coerceIn(0f, 1f) else 0f
}

/**
 * Data Model for Card Purchase Receipt Voucher (iPhone-style).
 */
data class CardPurchaseVoucher(
    val transactionId: String,
    val authCode: String,
    val cardName: String,
    val cardMask: String,
    val merchant: String,
    val amount: Double,
    val currency: String,
    val installments: Int,
    val monthlyPayment: Double,
    val dateFormatted: String,
    val timeFormatted: String,
    val remainingQuota: Double,
    val category: String
)

/**
 * Immutable State for Card Purchase Form including financial parameters and validation.
 */
data class CardPurchaseFormState(
    val rawAmountDigits: String = "",
    val selectedCurrency: String = "COP",
    val selectedCard: CreditCardDisplayItem? = null,
    val installmentsCount: Int = 1,
    val merchantNameInput: String = "",
    val selectedCategory: ExpenseCategory? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val notesText: String = "",
    val tagsList: List<String> = listOf("Tarjeta"),
    val selectedReceiptUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null
) {
    val numericAmount: Double
        get() = rawAmountDigits.toDoubleOrNull() ?: 0.0

    val rateEa: Double
        get() = selectedCard?.interestRateEa ?: 24.5

    val monthlyPaymentEstimated: Double
        get() = calculateMonthlyPayment(numericAmount, installmentsCount, rateEa)

    val totalInterestCalculated: Double
        get() = calculateTotalInterest(numericAmount, installmentsCount, monthlyPaymentEstimated)

    val totalDebtToPayCalculated: Double
        get() = calculateTotalDebt(numericAmount, totalInterestCalculated)

    val isQuotaExceeded: Boolean
        get() = selectedCard != null && !selectedCard.allowsOverdraft && numericAmount > selectedCard.availableQuota

    val remainingQuotaAfterPurchase: Double
        get() = ((selectedCard?.availableQuota ?: 0.0) - numericAmount).coerceAtLeast(0.0)

    val isFormValid: Boolean
        get() = numericAmount > 0 && selectedCard != null && !isQuotaExceeded && !isSubmitting
}

/**
 * Standard French amortization formula to estimate monthly card fee given an E.A. interest rate.
 */
fun calculateMonthlyPayment(principal: Double, installments: Int, rateEa: Double): Double {
    if (installments <= 1 || principal <= 0.0) {
        return principal
    }
    val rateMv = Math.pow(1.0 + rateEa / 100.0, 1.0 / 12.0) - 1.0
    val factor = Math.pow(1.0 + rateMv, installments.toDouble())
    return principal * (rateMv * factor) / (factor - 1.0)
}

/**
 * Total interest charged over the full term of the purchase.
 */
fun calculateTotalInterest(principal: Double, installments: Int, monthlyPayment: Double): Double {
    if (installments <= 1 || principal <= 0.0) {
        return 0.0
    }
    return ((monthlyPayment * installments) - principal).coerceAtLeast(0.0)
}

/**
 * Total debt to pay (Principal + Interest).
 */
fun calculateTotalDebt(principal: Double, totalInterest: Double): Double {
    return principal + totalInterest
}

/**
 * Format currency with locale grouping (e.g. $ 150.000 COP).
 */
fun formatCardMoney(amount: Double, currency: String = "COP"): String {
    val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "$ ${formatter.format(amount)} $currency"
}
