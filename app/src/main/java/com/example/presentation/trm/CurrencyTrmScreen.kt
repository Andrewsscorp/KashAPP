package com.example.presentation.trm

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.SwapVert
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.CurrencyRateCardItem
import com.example.domain.model.HistoryRange
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.account.components.WORLD_CURRENCIES_CATALOG
import com.example.presentation.trm.components.AddManualRateDialog
import com.example.presentation.trm.components.ExchangeRateTestSuiteModal
import com.example.presentation.trm.components.HistoricalRatesChart
import com.example.presentation.trm.components.ManualRateHistoryModal
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyTrmScreen(
    viewModel: CurrencyTrmViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    val timeFormat = remember { SimpleDateFormat("dd MMM yyyy · hh:mm a", Locale("es", "CO")) }
    val moneyFormatter = remember {
        val symbols = DecimalFormatSymbols(Locale.GERMAN).apply { groupingSeparator = '.' }
        DecimalFormat("#,##0.00", symbols)
    }

    var showCurrencyPickerForBase by remember { mutableStateOf(false) }
    var showCurrencyPickerForConverterFrom by remember { mutableStateOf(false) }
    var showCurrencyPickerForConverterTo by remember { mutableStateOf(false) }
    var showAddManualRateModal by remember { mutableStateOf(false) }
    var showManualHistoryModal by remember { mutableStateOf(false) }
    var showTestSuiteModal by remember { mutableStateOf(false) }
    var showPairDropdown by remember { mutableStateOf(false) }

    LaunchedEffect(uiState.statusMessage) {
        uiState.statusMessage?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC),
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .imePadding()
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ==========================================
            // HEADER BAR: Back + Title + Diagnostic Tool
            // ==========================================
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 12.dp, bottom = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White)
                            .border(1.dp, Color(0xFFE2E8F0), CircleShape)
                            .testTag("trm_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Regresar",
                            tint = KashNavy,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "Moneda y TRM",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Monedas, tasas y conversiones",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                // Diagnostics & Test Suite Button
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFEFF6FF),
                    border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                    modifier = Modifier
                        .clickable { showTestSuiteModal = true }
                        .testTag("open_test_suite_btn")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Science,
                            contentDescription = "Pruebas",
                            tint = KashBlue,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Pruebas",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashBlue
                        )
                    }
                }
            }

            // ==========================================
            // CARD 1: MONEDA PRINCIPAL
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AccountBalanceWallet,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Moneda principal",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Selecciona la moneda de tu cuenta principal",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Selectable Currency Pills Row
                    val primaryOptions = listOf(
                        Triple("COP", "🇨🇴", "Peso"),
                        Triple("USD", "🇺🇸", "Dólar"),
                        Triple("EUR", "🇪🇺", "Euro"),
                        Triple("MXN", "🇲🇽", "Peso Mex"),
                        Triple("GBP", "🇬🇧", "Libra")
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        primaryOptions.forEach { (code, flag, _) ->
                            val isSelected = uiState.baseCurrency.equals(code, ignoreCase = true)
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) KashBlue else Color(0xFFF8FAFC),
                                border = BorderStroke(
                                    1.dp,
                                    if (isSelected) KashBlue else Color(0xFFE2E8F0)
                                ),
                                modifier = Modifier
                                    .clickable { viewModel.setBaseCurrency(code) }
                                    .testTag("base_currency_pill_$code")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = flag, fontSize = 15.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = code,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else KashNavy
                                    )
                                    if (isSelected) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // "Ver todas · 200+ divisas" trigger
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCurrencyPickerForBase = true }
                            .testTag("view_all_currencies_btn")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Language,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Ver todas · +200 divisas disponibles",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashBlue
                                )
                            }
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            // ==========================================
            // CARD 2: TASAS DE CAMBIO & HISTÓRICO
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Top Status Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.TrendingUp,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Tasas de cambio",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Text(
                                    text = "Proveedor: Frankfurter (BCE)",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                                Text(
                                    text = "Última act: ${timeFormat.format(Date(uiState.lastUpdateTimestamp))}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF94A3B8)
                                )
                            }
                        }

                        Column(horizontalAlignment = Alignment.End) {
                            // Connection status indicator
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (uiState.isConnectionOnline) Color(0xFFECFDF5) else Color(0xFFFFFBEB)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .clip(CircleShape)
                                            .background(if (uiState.isConnectionOnline) Color(0xFF10B981) else Color(0xFFF59E0B))
                                    )
                                    Spacer(modifier = Modifier.width(5.dp))
                                    Text(
                                        text = if (uiState.isConnectionOnline) "Actualización automática" else "Modo Caché Offline",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (uiState.isConnectionOnline) Color(0xFF059669) else Color(0xFFD97706)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(6.dp))

                            // Actualizar ahora Button
                            Button(
                                onClick = { viewModel.refreshRates(forceNetwork = true) },
                                enabled = !uiState.isRefreshing,
                                shape = RoundedCornerShape(10.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                                modifier = Modifier.height(34.dp)
                            ) {
                                if (uiState.isRefreshing) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(14.dp),
                                        color = Color.White,
                                        strokeWidth = 2.dp
                                    )
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = null,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text("Actualizar ahora", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Rate Cards Row (USD/COP, EUR/COP, GBP/COP, MXN/COP)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        uiState.rateCards.forEach { card ->
                            RateCardMini(
                                card = card,
                                isSelected = uiState.selectedHistoryPair == card.pairSymbol,
                                onClick = { viewModel.setHistoryPair(card.pairSymbol) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // Time Range Filter Buttons
                    val ranges = listOf(
                        HistoryRange.HOY,
                        HistoryRange.DAYS_7,
                        HistoryRange.DAYS_30,
                        HistoryRange.MONTHS_3,
                        HistoryRange.YEAR_1,
                        HistoryRange.CUSTOM
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ranges.forEach { r ->
                            val isSel = uiState.selectedHistoryRange == r
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) KashBlue else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { viewModel.setHistoryRange(r) }
                            ) {
                                Text(
                                    text = r.label,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSel) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSel) Color.White else Color(0xFF475569),
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Histórico Header + Currency Pair Dropdown
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Histórico ${uiState.selectedHistoryPair}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )

                        Box {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable { showPairDropdown = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = uiState.selectedHistoryPair,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = showPairDropdown,
                                onDismissRequest = { showPairDropdown = false }
                            ) {
                                listOf("USD / ${uiState.baseCurrency}", "EUR / ${uiState.baseCurrency}", "GBP / ${uiState.baseCurrency}", "MXN / ${uiState.baseCurrency}").forEach { pair ->
                                    DropdownMenuItem(
                                        text = { Text(pair, fontSize = 12.sp, fontWeight = FontWeight.SemiBold) },
                                        onClick = {
                                            viewModel.setHistoryPair(pair)
                                            showPairDropdown = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Historical Chart
                    HistoricalRatesChart(
                        points = uiState.historicalPoints,
                        isLoading = uiState.isLoadingHistory,
                        lineColor = KashBlue,
                        chartHeight = 150
                    )
                }
            }

            // ==========================================
            // CARD 3 & 4 ROW: CONVERSOR & TASA MANUAL
            // ==========================================
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Conversor Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapHoriz,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Conversor",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = KashNavy
                                )
                                Text(
                                    text = "Convierte entre monedas al instante",
                                    fontSize = 11.sp,
                                    color = Color(0xFF64748B)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        Text(
                            text = "Importe",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF475569)
                        )
                        Spacer(modifier = Modifier.height(4.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            OutlinedTextField(
                                value = uiState.converterAmountText,
                                onValueChange = { viewModel.onConverterAmountChanged(it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                ),
                                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
                            )

                            // From Currency Trigger
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clickable { showCurrencyPickerForConverterFrom = true }
                                    .height(54.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = getFlagForCurrency(uiState.converterFromCurrency), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.converterFromCurrency,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "A",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF475569)
                            )

                            IconButton(
                                onClick = { viewModel.swapConverterCurrencies() },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.SwapVert,
                                    contentDescription = "Intercambiar",
                                    tint = KashBlue,
                                    modifier = Modifier.size(18.dp)
                                )
                            }

                            // To Currency Trigger
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .clickable { showCurrencyPickerForConverterTo = true }
                                    .height(44.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = getFlagForCurrency(uiState.converterToCurrency), fontSize = 16.sp)
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = uiState.converterToCurrency,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.KeyboardArrowDown,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Calculated Result Box
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF0F9FF),
                            border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "Resultado aproximado",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color(0xFF0284C7)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$${moneyFormatter.format(uiState.convertedAmount)} ${uiState.converterToCurrency}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Black,
                                    color = KashBlue
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val effective = uiState.converterEffectiveRate
                                if (effective != null) {
                                    Text(
                                        text = "Tasa usada: ${moneyFormatter.format(effective.rate)} • ${effective.providerName}",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }
                    }
                }

                // Tasa Manual Card
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Top
                        ) {
                            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(34.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFFEFF6FF)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Shield,
                                        contentDescription = null,
                                        tint = KashBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(
                                            text = "Tasa manual",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = Color(0xFF0284C7).copy(alpha = 0.15f)
                                        ) {
                                            Text(
                                                text = "Manual",
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0284C7),
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                            )
                                        }
                                    }
                                    Text(
                                        text = "¿Quieres usar una tasa propia?",
                                        fontSize = 11.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Puedes agregar una tasa manual para un par de monedas específico y reemplazar temporalmente la tasa automática.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B),
                            lineHeight = 15.sp
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        // "+ Agregar tasa manual" Button
                        OutlinedButton(
                            onClick = { showAddManualRateModal = true },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.5.dp, KashBlue),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Agregar tasa manual", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashBlue)
                        }

                        // Active Manual Rate Item (if any)
                        val activeRate = uiState.activeManualRate
                        if (activeRate != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "Tasa manual activa ℹ️",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = KashNavy
                                        )
                                        Switch(
                                            checked = activeRate.isActive,
                                            onCheckedChange = { viewModel.toggleManualRateActive(activeRate.id, it) },
                                            colors = SwitchDefaults.colors(
                                                checkedThumbColor = Color.White,
                                                checkedTrackColor = KashBlue
                                            )
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Par:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("${activeRate.fromCurrency} → ${activeRate.toCurrency}", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Tasa:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(moneyFormatter.format(activeRate.rate), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KashBlue)
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Vigencia:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text("${activeRate.validFromDate} – ${activeRate.validToDate}", fontSize = 11.sp, color = Color(0xFF475569))
                                    }
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                        Text("Fuente:", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(activeRate.note, fontSize = 11.sp, color = Color(0xFF64748B))
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // CARD 4: FUENTES Y CONFIGURACIÓN
            // ==========================================
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFFE0F2FE)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Fuentes y configuración",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Gestiona la fuente de tasas, la actualización y otras opciones.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 3 Items Grid/Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Proveedor automático
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(Icons.Default.Language, null, tint = KashBlue, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Proveedor", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("Frankfurter", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                            }
                        }

                        // Actualización automática
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(Icons.Default.Refresh, null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Frecuencia", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("Diaria / Instantánea", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                            }
                        }

                        // Historial de cambios
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { showManualHistoryModal = true }
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Icon(Icons.Default.History, null, tint = Color(0xFF6366F1), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.height(4.dp))
                                Text("Historial", fontSize = 11.sp, color = Color(0xFF64748B))
                                Text("Ver registros >", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF6366F1))
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    HorizontalDivider(color = Color(0xFFF1F5F9))
                    Spacer(modifier = Modifier.height(12.dp))

                    // Action rows
                    val activeId = uiState.activeManualRate?.id
                    if (activeId != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { viewModel.resetToAutomaticRate(activeId) }
                                .padding(vertical = 6.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Restore, null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text("Restablecer a tasa automática", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                                    Text("Quita la tasa manual activa", fontSize = 11.sp, color = Color(0xFF64748B))
                                }
                            }
                            Icon(Icons.AutoMirrored.Filled.KeyboardArrowRight, null, tint = Color(0xFF94A3B8), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    // Usar fuente oficial Switch
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.Shield, null, tint = KashBlue, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text("Usar fuente oficial (si aplica)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                                Text("Referencia TRM colombiana para el par USD/COP", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                        }
                        Switch(
                            checked = uiState.isOfficialTrmActive,
                            onCheckedChange = { viewModel.toggleOfficialTrm(it) },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Notice Banner
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFE0F2FE)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Icon(Icons.Default.Info, null, tint = KashBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Las tasas automáticas son de referencia del mercado global provistas por Frankfurter. La TRM oficial colombiana proviene de la Superintendencia Financiera.",
                                fontSize = 11.sp,
                                color = Color(0xFF0369A1),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }

    // ==========================================
    // MODALS
    // ==========================================

    if (showCurrencyPickerForBase) {
        CurrencyPickerModal(
            selectedCurrencyCode = uiState.baseCurrency,
            onCurrencySelected = { code ->
                viewModel.setBaseCurrency(code)
                showCurrencyPickerForBase = false
            },
            onDismiss = { showCurrencyPickerForBase = false }
        )
    }

    if (showCurrencyPickerForConverterFrom) {
        CurrencyPickerModal(
            selectedCurrencyCode = uiState.converterFromCurrency,
            onCurrencySelected = { code ->
                viewModel.setConverterFromCurrency(code)
                showCurrencyPickerForConverterFrom = false
            },
            onDismiss = { showCurrencyPickerForConverterFrom = false }
        )
    }

    if (showCurrencyPickerForConverterTo) {
        CurrencyPickerModal(
            selectedCurrencyCode = uiState.converterToCurrency,
            onCurrencySelected = { code ->
                viewModel.setConverterToCurrency(code)
                showCurrencyPickerForConverterTo = false
            },
            onDismiss = { showCurrencyPickerForConverterTo = false }
        )
    }

    if (showAddManualRateModal) {
        AddManualRateDialog(
            initialFrom = uiState.converterFromCurrency,
            initialTo = uiState.converterToCurrency,
            onDismiss = { showAddManualRateModal = false },
            onSave = { from, to, rate, isActive, validFrom, validTo, note ->
                viewModel.saveManualRate(from, to, rate, isActive, validFrom, validTo, note)
            }
        )
    }

    if (showManualHistoryModal) {
        ManualRateHistoryModal(
            logs = uiState.manualRateLogs,
            onDismiss = { showManualHistoryModal = false }
        )
    }

    if (showTestSuiteModal) {
        ExchangeRateTestSuiteModal(
            results = uiState.testSuiteResults,
            isRunning = uiState.isRunningTestSuite,
            onRunTests = { viewModel.runTestSuite() },
            onDismiss = { showTestSuiteModal = false }
        )
    }
}

@Composable
private fun RateCardMini(
    card: CurrencyRateCardItem,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
        border = BorderStroke(
            1.5.dp,
            if (isSelected) KashBlue else Color(0xFFE2E8F0)
        ),
        modifier = Modifier
            .width(145.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = getFlagForCurrency(card.fromCurrency), fontSize = 16.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = card.pairSymbol,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF64748B)
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = card.formattedRate,
                fontSize = 15.sp,
                fontWeight = FontWeight.Black,
                color = KashNavy
            )

            Spacer(modifier = Modifier.height(4.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "↑ +${card.variationPercent}%",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF10B981)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color(0xFF10B981).copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "+${String.format(Locale.US, "%.2f", card.variationDiff)}",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF059669),
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                    )
                }
            }
        }
    }
}

private fun getFlagForCurrency(currencyCode: String): String {
    return WORLD_CURRENCIES_CATALOG.find { it.code.equals(currencyCode, ignoreCase = true) }?.flag
        ?: when (currencyCode.uppercase()) {
            "USD" -> "🇺🇸"
            "EUR" -> "🇪🇺"
            "COP" -> "🇨🇴"
            "MXN" -> "🇲🇽"
            "GBP" -> "🇬🇧"
            "CAD" -> "🇨🇦"
            "BRL" -> "🇧🇷"
            "CLP" -> "🇨🇱"
            "PEN" -> "🇵🇪"
            "ARS" -> "🇦🇷"
            "JPY" -> "🇯🇵"
            "CHF" -> "🇨🇭"
            else -> "🌐"
        }
}
