package com.example.presentation.expense.model

import android.net.Uri
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FitnessCenter
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalCafe
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.Store
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Work
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.time.LocalDate
import java.util.Locale

/**
 * Currency information model
 */
data class CurrencyInfo(
    val code: String,
    val name: String,
    val symbol: String,
    val flagEmoji: String
)

val SupportedCurrencies = listOf(
    CurrencyInfo("COP", "Peso colombiano", "$", "🇨🇴"),
    CurrencyInfo("USD", "Dólar estadounidense", "$", "🇺🇸"),
    CurrencyInfo("EUR", "Euro", "€", "🇪🇺"),
    CurrencyInfo("MXN", "Peso mexicano", "$", "🇲🇽"),
    CurrencyInfo("CLP", "Peso chileno", "$", "🇨🇱"),
    CurrencyInfo("PEN", "Sol peruano", "S/", "🇵🇪"),
    CurrencyInfo("BRL", "Real brasileño", "R$", "🇧🇷"),
    CurrencyInfo("GBP", "Libra esterlina", "£", "🇬🇧")
)

/**
 * Receipt Voucher for Credit Card Payments / Advances
 */
data class CardPaymentVoucher(
    val transactionId: String,
    val authCode: String,
    val cardName: String,
    val cardMask: String,
    val sourceAccountName: String,
    val amount: Double,
    val currency: String,
    val strategyName: String,
    val previousDebt: Double,
    val newDebt: Double,
    val quotaRestored: Double,
    val dateFormatted: String,
    val timeFormatted: String,
    val principalAmortized: Double = 0.0,
    val currentInterestPaid: Double = 0.0,
    val handlingFeePaid: Double = 0.0,
    val lateInterestPaid: Double = 0.0,
    val rateEa: Double = 24.5,
    val rateEm: Double = 1.84,
    val projectedImpact: String = ""
)

/**
 * Data structures for form selection
 */
data class ExpenseCategory(
    val id: String,
    val name: String,
    val icon: ImageVector,
    val color: Color,
    val bgColor: Color
)

data class ExpenseAccount(
    val id: String,
    val name: String,
    val balance: Double,
    val icon: ImageVector,
    val currency: String = "COP",
    val mask: String = "•••• 4208"
)

/**
 * Visual Vector Symbol helper catalog for custom category creation
 */
data class CategoryIconOption(
    val id: String,
    val name: String,
    val icon: ImageVector
)

val CategoryIconCatalog = listOf(
    CategoryIconOption("restaurant", "Comida", Icons.Default.Restaurant),
    CategoryIconOption("cafe", "Café", Icons.Default.LocalCafe),
    CategoryIconOption("car", "Transporte", Icons.Default.DirectionsCar),
    CategoryIconOption("home", "Vivienda", Icons.Default.Home),
    CategoryIconOption("shopping", "Compras", Icons.Default.ShoppingBag),
    CategoryIconOption("movie", "Cine", Icons.Default.Movie),
    CategoryIconOption("health", "Salud", Icons.Default.Favorite),
    CategoryIconOption("gym", "Gimnasio", Icons.Default.FitnessCenter),
    CategoryIconOption("travel", "Viajes", Icons.Default.Flight),
    CategoryIconOption("pet", "Mascota", Icons.Default.Pets),
    CategoryIconOption("school", "Estudio", Icons.Default.School),
    CategoryIconOption("bill", "Servicios", Icons.Default.ReceiptLong),
    CategoryIconOption("work", "Trabajo", Icons.Default.Work),
    CategoryIconOption("gift", "Regalo", Icons.Default.CardGiftcard),
    CategoryIconOption("offer", "Descuento", Icons.Default.LocalOffer),
    CategoryIconOption("card", "Tarjeta", Icons.Default.CreditCard)
)

/**
 * Form helper to format numbers in Colombian / Latin currency style ($ 1.250.000)
 */
fun formatMoneyDisplay(amount: Double, currencyCode: String): String {
    val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0.##", symbols)
    return "$ ${formatter.format(amount)} $currencyCode"
}

fun formatLiveNumberString(rawDigits: String): String {
    if (rawDigits.isBlank()) return "0"
    val clean = rawDigits.filter { it.isDigit() || it == '.' }
    if (clean.isBlank()) return "0"
    val parts = clean.split(".")
    val integerPart = parts[0].toLongOrNull() ?: 0L
    val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
    }
    val formatter = DecimalFormat("#,###", symbols)
    val formattedInt = formatter.format(integerPart)
    return if (parts.size > 1) {
        "$formattedInt.${parts[1]}"
    } else if (clean.endsWith(".")) {
        "$formattedInt."
    } else {
        formattedInt
    }
}

/**
 * VisualTransformation that live-formats user typed digits into dot thousand separators (e.g. 2222222 -> 22.222.222)
 */
class CurrencyDotVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val hasDot = original.contains(".")
        val parts = original.split(".")
        val intStr = parts[0]
        val decStr = if (parts.size > 1) parts[1] else null

        val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedInt = if (intStr.isNotEmpty()) {
            val num = intStr.toLongOrNull() ?: 0L
            formatter.format(num)
        } else {
            ""
        }

        val transformed = when {
            decStr != null -> "$formattedInt,$decStr"
            hasDot -> "$formattedInt,"
            else -> formattedInt
        }

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                return transformed.length.coerceAtMost(transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                return original.length.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

fun mapIconStringToVector(iconKey: String, name: String): ImageVector {
    val key = iconKey.lowercase()
    return when {
        key.contains("restaurant") || key.contains("food") || key.contains("comida") -> Icons.Default.Restaurant
        key.contains("car") || key.contains("auto") || key.contains("transporte") -> Icons.Default.DirectionsCar
        key.contains("home") || key.contains("hogar") || key.contains("casa") -> Icons.Default.Home
        key.contains("shopping") || key.contains("compras") || key.contains("bag") -> Icons.Default.ShoppingBag
        key.contains("movie") || key.contains("cine") || key.contains("entretenimiento") -> Icons.Default.Movie
        key.contains("health") || key.contains("salud") || key.contains("medico") -> Icons.Default.Favorite
        key.contains("school") || key.contains("educacion") || key.contains("estudio") -> Icons.Default.School
        key.contains("bill") || key.contains("servicio") || key.contains("recibo") -> Icons.Default.ReceiptLong
        key.contains("cafe") || key.contains("coffee") -> Icons.Default.LocalCafe
        key.contains("gym") || key.contains("fitness") -> Icons.Default.FitnessCenter
        key.contains("travel") || key.contains("viaje") || key.contains("vuelo") -> Icons.Default.Flight
        key.contains("pet") || key.contains("mascota") || key.contains("veterinaria") -> Icons.Default.Pets
        key.contains("work") || key.contains("trabajo") || key.contains("nomina") || key.contains("salario") -> Icons.Default.Work
        key.contains("gift") || key.contains("regalo") -> Icons.Default.CardGiftcard
        key.contains("card") || key.contains("tarjeta") || key.contains("credito") -> Icons.Default.CreditCard
        key.contains("business") || key.contains("empresa") -> Icons.Default.BusinessCenter
        key.contains("rendimiento") || key.contains("inversion") -> Icons.Default.TrendingUp
        name.contains("nomina", ignoreCase = true) || name.contains("sueldo", ignoreCase = true) || name.contains("salario", ignoreCase = true) -> Icons.Default.Work
        name.contains("honorario", ignoreCase = true) || name.contains("servicio", ignoreCase = true) -> Icons.Default.BusinessCenter
        name.contains("venta", ignoreCase = true) || name.contains("comercio", ignoreCase = true) || name.contains("negocio", ignoreCase = true) -> Icons.Default.Store
        else -> Icons.Default.ReceiptLong
    }
}

/**
 * Immutable Expense Form State with input fields and validation
 */
data class ExpenseFormState(
    val rawAmountInput: String = "",
    val currencyCode: String = "COP",
    val isCreditCardPaymentMode: Boolean = false,
    val selectedAccountId: String? = null,
    val selectedCreditCardId: String? = null,
    val selectedCategoryId: String = "",
    val selectedDate: LocalDate = LocalDate.now(),
    val isRecurring: Boolean = false,
    val recurrenceFrequency: String = "Mensual",
    val conceptText: String = "",
    val additionalNotes: String = "",
    val isSavedAsFrequent: Boolean = false,
    val tagsList: List<String> = emptyList(),
    val receiptUri: Uri? = null,
    val receiptFileName: String? = null,
    val isPdfDocument: Boolean = false,
    val cardPaymentStrategy: String = "AVALANCHE",
    val strategyConfirmed: Boolean = false,
    val isSubmitting: Boolean = false
) {
    val amountDouble: Double
        get() = rawAmountInput.toDoubleOrNull() ?: 0.0

    val isAmountValid: Boolean
        get() = amountDouble > 0.0

    val isFormSubmittable: Boolean
        get() = isAmountValid && !isSubmitting
}

val DefaultExpenseCategories = listOf(
    ExpenseCategory("cat_alimentacion", "Alimentación", Icons.Default.Restaurant, Color(0xFF8B5CF6), Color(0xFFF3E8FF)),
    ExpenseCategory("cat_transporte", "Transporte", Icons.Default.DirectionsCar, Color(0xFF0284C7), Color(0xFFE0F2FE)),
    ExpenseCategory("cat_hogar", "Vivienda y Hogar", Icons.Default.Home, Color(0xFF06B6D4), Color(0xFFCFFAFE)),
    ExpenseCategory("cat_servicios", "Servicios públicos", Icons.Default.ReceiptLong, Color(0xFFF59E0B), Color(0xFFFEF3C7)),
    ExpenseCategory("cat_salud", "Salud y Cuidado", Icons.Default.Favorite, Color(0xFFEC4899), Color(0xFFFCE7F3)),
    ExpenseCategory("cat_entretenimiento", "Entretenimiento", Icons.Default.Movie, Color(0xFF8B5CF6), Color(0xFFF3E8FF)),
    ExpenseCategory("cat_educacion", "Educación", Icons.Default.School, Color(0xFF10B981), Color(0xFFDCFCE7)),
    ExpenseCategory("cat_compras", "Compras y Ropa", Icons.Default.ShoppingBag, Color(0xFFF97316), Color(0xFFFFEDD5)),
    ExpenseCategory("cat_otros", "Otros gastos", Icons.Default.ReceiptLong, Color(0xFF64748B), Color(0xFFE2E8F0))
)

fun getDefaultCreditCardAccounts(userId: String) = listOf(
    com.example.data.local.entity.AccountEntity(
        id = "cc_bancolombia_default",
        userId = userId,
        name = "Bancolombia Visa Signature",
        type = "CREDIT_CARD",
        currency = "COP",
        balance = -3150000.0,
        availableBalance = 4850000.0,
        bankName = "Bancolombia",
        overdraftLimit = 8000000.0,
        accountNumberMasked = "•••• 8912"
    )
)

fun buildDisplayCategories(roomCategories: List<com.example.data.local.entity.CategoryEntity>): List<ExpenseCategory> {
    val validRoomCats = roomCategories.filter { (it.type == "EXPENSE" || it.type == "BOTH") && it.name.trim().isNotBlank() }
    val customCats = validRoomCats.map { entity ->
        val icon = com.example.presentation.category.resolveCategoryIcon(entity.icon, entity.name)
        val color = try {
            if (entity.colorHex.isNotBlank() && entity.colorHex != "#FFFFFF") {
                Color(android.graphics.Color.parseColor(entity.colorHex))
            } else Color(0xFF0284C7)
        } catch (_: Exception) {
            Color(0xFF0284C7)
        }
        ExpenseCategory(entity.id, entity.name.trim(), icon, color, color.copy(alpha = 0.15f))
    }
    val customNames = customCats.map { it.name.lowercase().trim() }.toSet()
    val filteredDefaults = DefaultExpenseCategories.filter { it.name.lowercase().trim() !in customNames }
    return filteredDefaults + customCats
}
