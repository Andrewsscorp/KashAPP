package com.example.presentation.account.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cached
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.KashAppApplication
import com.example.domain.model.EffectiveRate
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * Enterprise Live Currency Conversion Preview with bidirectional calculation ("viceversa"),
 * customizable target currency chips, live Frankfurter & Room rate sync, and clear rate formulas.
 */
@Composable
fun LiveCurrencyConversionPreview(
    amount: Double,
    inputCurrency: String,
    modifier: Modifier = Modifier,
    isDarkTheme: Boolean = false,
    userId: String = "kash_user_01"
) {
    val context = LocalContext.current
    val appContainer = remember(context) { (context.applicationContext as KashAppApplication).appContainer }

    var baseCurrency by remember { mutableStateOf("COP") }
    var effectiveRate by remember { mutableStateOf<EffectiveRate?>(null) }
    var selectedTargetCurrency by remember { mutableStateOf<String?>(null) }
    var isSwappedDirection by remember { mutableStateOf(false) }

    LaunchedEffect(userId) {
        appContainer.settingsRepository.observePreferences(userId).collect { prefs ->
            baseCurrency = prefs.baseCurrency.ifBlank { "COP" }
        }
    }

    val fromCurrencyUpper = inputCurrency.trim().uppercase()
    val baseUpper = baseCurrency.trim().uppercase()

    // Determine target currency:
    val defaultTarget = if (fromCurrencyUpper != baseUpper) {
        baseUpper
    } else if (baseUpper == "COP") {
        "USD"
    } else {
        "COP"
    }

    val targetCurrency = (selectedTargetCurrency ?: defaultTarget).trim().uppercase()

    LaunchedEffect(fromCurrencyUpper, targetCurrency) {
        effectiveRate = appContainer.exchangeRateRepository.getInstantEffectiveRate(fromCurrencyUpper, targetCurrency)
        launch {
            appContainer.exchangeRateRepository.refreshRates(fromCurrencyUpper, forceNetwork = false)
        }
        appContainer.exchangeRateRepository.observeEffectiveRate(fromCurrencyUpper, targetCurrency).collect {
            effectiveRate = it
        }
    }

    val directRate = effectiveRate?.rate ?: 1.0

    val inverseRate = if (directRate > 0) 1.0 / directRate else 1.0

    // Source & Destination based on swap state:
    val activeFrom = if (isSwappedDirection) targetCurrency else fromCurrencyUpper
    val activeTo = if (isSwappedDirection) fromCurrencyUpper else targetCurrency
    val activeMultiplier = if (isSwappedDirection) inverseRate else directRate

    val convertedValue = amount * activeMultiplier

    val moneyFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply { groupingSeparator = '.' }
        DecimalFormat("#,##0.00", symbols)
    }

    val rateFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply { groupingSeparator = '.' }
        DecimalFormat("#,##0.00##", symbols)
    }

    val activeToFlag = remember(activeTo) {
        WORLD_CURRENCIES_CATALOG.find { it.code.equals(activeTo, ignoreCase = true) }?.flag
            ?: when (activeTo) {
                "COP" -> "🇨🇴"
                "USD" -> "🇺🇸"
                "EUR" -> "🇪🇺"
                "MXN" -> "🇲🇽"
                "GBP" -> "🇬🇧"
                else -> "🌐"
            }
    }

    val activeFromFlag = remember(activeFrom) {
        WORLD_CURRENCIES_CATALOG.find { it.code.equals(activeFrom, ignoreCase = true) }?.flag
            ?: when (activeFrom) {
                "COP" -> "🇨🇴"
                "USD" -> "🇺🇸"
                "EUR" -> "🇪🇺"
                "MXN" -> "🇲🇽"
                "GBP" -> "🇬🇧"
                else -> "🌐"
            }
    }

    val containerBg = if (isDarkTheme) Color(0xFF1E293B) else Color(0xFFF0F9FF)
    val borderCol = if (isDarkTheme) Color(0xFF38BDF8).copy(alpha = 0.35f) else Color(0xFFBAE6FD)
    val titleCol = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val valueCol = if (isDarkTheme) Color.White else KashNavy
    val subCol = if (isDarkTheme) Color(0xFF94A3B8) else Color(0xFF64748B)

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = containerBg,
        border = BorderStroke(1.dp, borderCol),
        modifier = modifier
            .fillMaxWidth()
            .testTag("live_currency_conversion_preview")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp)
        ) {
            // Header: Icon + Title + Swap (Viceversa) Button + Manual Rate Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(titleCol.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = "Conversión en vivo",
                            tint = titleCol,
                            modifier = Modifier.size(17.dp)
                        )
                    }

                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Equivalente en $activeTo $activeToFlag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = titleCol
                            )
                            if (effectiveRate?.isManual == true) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = Color(0xFF0284C7).copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = "Tasa manual",
                                        fontSize = 9.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF0284C7),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                        Text(
                            text = if (isSwappedDirection) "Vista invertida ($activeFrom ➔ $activeTo)" else "De $activeFrom ➔ $activeTo",
                            fontSize = 10.sp,
                            color = subCol
                        )
                    }
                }

                // Interactive Swap (Viceversa) Button
                Surface(
                    onClick = { isSwappedDirection = !isSwappedDirection },
                    shape = RoundedCornerShape(10.dp),
                    color = if (isDarkTheme) Color(0xFF334155) else Color.White,
                    border = BorderStroke(1.dp, if (isDarkTheme) Color(0xFF475569) else Color(0xFFBAE6FD)),
                    modifier = Modifier.testTag("swap_currency_conversion_direction_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.SwapHoriz,
                            contentDescription = "Invertir dirección viceversa",
                            tint = titleCol,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Invertir ⇄",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = titleCol
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Main Amount Conversion Display
            if (amount > 0.0) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    Column {
                        Text(
                            text = "${com.example.domain.currency.CurrencyConversionEngine.formatLocalized(amount, activeFrom)} $activeFromFlag equivale a:",
                            fontSize = 11.sp,
                            color = subCol
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "≈ ${com.example.domain.currency.CurrencyConversionEngine.formatLocalized(convertedValue, activeTo)} $activeToFlag",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = valueCol
                        )
                    }
                }
            } else {
                Text(
                    text = "Ingresa el saldo para calcular la equivalencia exacta en tiempo real.",
                    fontSize = 11.sp,
                    color = subCol,
                    fontStyle = androidx.compose.ui.text.font.FontStyle.Italic
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Dual Exchange Rate Display (Forward & Reciprocal)
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = if (isDarkTheme) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.7f),
                border = BorderStroke(0.5.dp, borderCol.copy(alpha = 0.5f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "1 $fromCurrencyUpper = ${com.example.domain.currency.CurrencyConversionEngine.formatRateValue(directRate)} $targetCurrency",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = valueCol
                        )
                        Text(
                            text = "1 $targetCurrency = ${com.example.domain.currency.CurrencyConversionEngine.formatRateValue(inverseRate)} $fromCurrencyUpper",
                            fontSize = 10.sp,
                            color = subCol
                        )
                    }
                    Text(
                        text = "Proveedor: ${effectiveRate?.providerName ?: "Frankfurter API (BCE)"}",
                        fontSize = 9.sp,
                        color = subCol
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Currency Comparator Quick Chips
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = "Comparar con:",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = subCol
                )

                val quickCurrencies = listOf(baseUpper, "USD", "COP", "EUR").distinct().filter { it != fromCurrencyUpper }
                quickCurrencies.forEach { cur ->
                    val isCurSelected = (cur == targetCurrency)
                    val flag = WORLD_CURRENCIES_CATALOG.find { it.code.equals(cur, ignoreCase = true) }?.flag ?: ""
                    Surface(
                        onClick = {
                            selectedTargetCurrency = cur
                            isSwappedDirection = false
                        },
                        shape = RoundedCornerShape(8.dp),
                        color = if (isCurSelected) titleCol else if (isDarkTheme) Color(0xFF334155) else Color.White,
                        border = BorderStroke(1.dp, if (isCurSelected) titleCol else borderCol),
                        modifier = Modifier.testTag("compare_chip_$cur")
                    ) {
                        Text(
                            text = "$flag $cur",
                            fontSize = 10.sp,
                            fontWeight = if (isCurSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurSelected) Color.White else valueCol,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
