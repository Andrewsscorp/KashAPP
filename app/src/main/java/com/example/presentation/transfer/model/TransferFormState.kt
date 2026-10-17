package com.example.presentation.transfer.model

import android.net.Uri
import com.example.data.local.entity.AccountEntity
import java.text.NumberFormat
import java.time.LocalDate
import java.util.Locale

/**
 * Predefined Quick Transfer Routes (frequent routes).
 */
data class QuickTransferRoute(
    val title: String,
    val sourceName: String,
    val destinationName: String,
    val defaultConcept: String
)

/**
 * Summary data for the iPhone-style transaction confirmation popup.
 */
data class TransferSuccessSummary(
    val transactionId: String,
    val sourceAccountName: String,
    val sourceAccountBalanceAfter: Double,
    val destinationAccountName: String,
    val destinationAccountBalanceAfter: Double,
    val amount: Double,
    val currency: String,
    val dateFormatted: String,
    val timeFormatted: String,
    val concept: String?,
    val gmfAmount: Double = 0.0
)

/**
 * Immutable State holding the transfer form fields, calculations, and GMF rules.
 */
data class TransferFormState(
    val rawDigits: String = "",
    val selectedCurrency: String = "COP",
    val selectedSourceAccount: AccountEntity? = null,
    val selectedDestinationAccount: AccountEntity? = null,
    val selectedDate: LocalDate = LocalDate.now(),
    val conceptText: String = "",
    val noteText: String = "",
    val isNumpadVisible: Boolean = true,
    val saveAsTemplate: Boolean = false,
    val isMoreDetailsExpanded: Boolean = false,
    val tagsList: List<String> = emptyList(),
    val selectedReceiptUri: Uri? = null,
    val isSubmitting: Boolean = false,
    val errorMessage: String? = null,
    val applyGmf: Boolean = false,
    val gmfRate: Double = 0.004 // 4x1000 = 0.4%
) {
    val numericAmount: Double
        get() = if (rawDigits.isEmpty()) 0.0 else rawDigits.toDoubleOrNull() ?: 0.0

    val gmfAmount: Double
        get() = if (applyGmf) (numericAmount * gmfRate) else 0.0

    val totalDeduction: Double
        get() = numericAmount + gmfAmount

    val sourceBalance: Double
        get() = selectedSourceAccount?.balance ?: 0.0

    val destBalance: Double
        get() = selectedDestinationAccount?.balance ?: 0.0

    val newSourceBalance: Double
        get() = sourceBalance - totalDeduction

    val newDestBalance: Double
        get() = destBalance + numericAmount

    val isOverdraft: Boolean
        get() = totalDeduction > sourceBalance && sourceBalance > 0

    val isFormValid: Boolean
        get() = numericAmount > 0 &&
                selectedSourceAccount != null &&
                selectedDestinationAccount != null &&
                selectedSourceAccount.id != selectedDestinationAccount.id &&
                !isSubmitting
}

/**
 * Formats a monetary amount with Spanish grouping symbols.
 */
fun formatTransferMoney(amount: Double, currency: String = "COP"): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    return "$ ${formatter.format(amount)} $currency"
}

/**
 * Formats a raw number without currency suffix.
 */
fun formatTransferNumberOnly(amount: Double): String {
    val formatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 2
        minimumFractionDigits = if (amount % 1.0 == 0.0) 0 else 2
    }
    return formatter.format(amount)
}
