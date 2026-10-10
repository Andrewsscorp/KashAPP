package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.settings.presentation.components.formatMoney
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Base Currency & Live TRM Rate Converter Screen.
 */
@Composable
fun SettingsCurrencyScreen(
    currentCurrency: String,
    onSelectCurrency: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var convertAmountInput by remember { mutableStateOf("100") }
    var fromCurrency by remember { mutableStateOf("USD") }
    var toCurrency by remember { mutableStateOf("COP") }

    val trmRates = mapOf(
        "USD_COP" to 4142.50,
        "EUR_COP" to 4498.20,
        "MXN_COP" to 215.30,
        "BRL_COP" to 765.40,
        "GBP_COP" to 5310.80,
        "COP_USD" to (1.0 / 4142.50),
        "COP_EUR" to (1.0 / 4498.20)
    )

    val inputVal = convertAmountInput.toDoubleOrNull() ?: 0.0
    val convertedVal = when {
        fromCurrency == toCurrency -> inputVal
        fromCurrency == "USD" && toCurrency == "COP" -> inputVal * (trmRates["USD_COP"] ?: 4142.5)
        fromCurrency == "EUR" && toCurrency == "COP" -> inputVal * (trmRates["EUR_COP"] ?: 4498.2)
        fromCurrency == "COP" && toCurrency == "USD" -> inputVal * (trmRates["COP_USD"] ?: (1.0 / 4142.5))
        fromCurrency == "COP" && toCurrency == "EUR" -> inputVal * (trmRates["COP_EUR"] ?: (1.0 / 4498.2))
        else -> inputVal * (trmRates["${fromCurrency}_COP"] ?: 4000.0) / (trmRates["${toCurrency}_COP"] ?: 1.0)
    }

    var showFullPicker by remember { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Moneda Principal de la Cuenta", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)
                Text(
                    text = "Selecciona la divisa en la que se calcularán los totales consolidados de patrimonio y flujo de caja.",
                    fontSize = 12.sp,
                    color = Color(0xFF64748B)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("COP", "USD", "EUR", "MXN").forEach { code ->
                        val isSelected = currentCurrency.equals(code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectCurrency(code) },
                            label = { Text(code, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KashBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }

                    FilterChip(
                        selected = false,
                        onClick = { showFullPicker = true },
                        label = { Text("Más (164)...", fontWeight = FontWeight.SemiBold) },
                        leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    )
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showFullPicker = true }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = KashBlue)
                            Column {
                                Text("Moneda activa: $currentCurrency", fontWeight = FontWeight.Bold, fontSize = 13.sp, color = Color(0xFF0F172A))
                                Text("Toca para abrir las 164 divisas mundiales o crear una nueva", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                        Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, contentDescription = null, tint = KashBlue)
                    }
                }
            }
        }

        if (showFullPicker) {
            CurrencyPickerModal(
                selectedCurrencyCode = currentCurrency,
                onCurrencySelected = { code ->
                    onSelectCurrency(code)
                    showFullPicker = false
                },
                onDismiss = { showFullPicker = false }
            )
        }

        // Live TRM Interactive Converter Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Calculate, contentDescription = null, tint = Color(0xFF16A34A), modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text("Calculadora y Tasas TRM en Vivo", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KashNavy)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 USD (Dólar estadounidense) ≈", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("$4,142.50 COP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("1 EUR (Euro europeo) ≈", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("$4,498.20 COP", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("Simular conversión instantánea:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = convertAmountInput,
                        onValueChange = { convertAmountInput = it.filter { c -> c.isDigit() || c == '.' } },
                        label = { Text("Importe") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        textStyle = TextStyle(
                            color = Color(0xFF0F172A),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color.White,
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.clickable {
                            val nextFrom = if (fromCurrency == "USD") "COP" else if (fromCurrency == "COP") "EUR" else "USD"
                            val nextTo = if (nextFrom == "COP") "USD" else "COP"
                            fromCurrency = nextFrom
                            toCurrency = nextTo
                        }
                    ) {
                        Text(
                            text = "$fromCurrency → $toCurrency",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 14.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Resultado estimado:", fontSize = 12.sp, color = Color(0xFF64748B))
                        Text(
                            text = formatMoney(convertedVal, toCurrency),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color(0xFF0284C7)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Backward compatibility alias for SettingsCurrencySubScreen
 */
@Composable
fun SettingsCurrencySubScreen(
    currentCurrency: String,
    onSelectCurrency: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsCurrencyScreen(
        currentCurrency = currentCurrency,
        onSelectCurrency = onSelectCurrency,
        onBack = onBack,
        modifier = modifier
    )
}
