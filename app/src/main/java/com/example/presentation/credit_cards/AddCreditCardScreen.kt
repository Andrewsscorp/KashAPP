package com.example.presentation.credit_cards

import android.content.Context
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.repository.FinancialRepository
import com.example.presentation.account.components.CurrencyItemInfo
import com.example.presentation.account.components.CurrencyPickerModal
import com.example.presentation.account.components.LiveCurrencyConversionPreview
import com.example.presentation.account.components.WORLD_CURRENCIES_CATALOG
import com.example.presentation.category.CategoryCreationDialog
import com.example.presentation.category.resolveCategoryIcon
import com.example.presentation.credit_cards.state.CreditCardFormViewModel
import com.example.presentation.expense.ExpenseCategory
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.launch
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.util.Locale

/**
 * VisualTransformation that live-formats user typed digits into dot thousand separators (e.g. 5000000 -> 5.000.000)
 */
private class CardCurrencyDotVisualTransformation : VisualTransformation {
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
                if (offset <= 0) return 0
                val sub = original.take(offset)
                val subParts = sub.split(".")
                val subInt = subParts[0]
                val subDec = if (subParts.size > 1) subParts[1] else null
                val formattedSubInt = if (subInt.isNotEmpty()) {
                    val n = subInt.toLongOrNull() ?: 0L
                    formatter.format(n)
                } else ""
                val res = when {
                    subDec != null -> "$formattedSubInt,$subDec"
                    sub.contains(".") -> "$formattedSubInt,"
                    else -> formattedSubInt
                }
                return res.length.coerceAtMost(transformed.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = transformed.take(offset)
                val digitsBefore = sub.count { it.isDigit() || it == ',' || it == '.' }
                return digitsBefore.coerceAtMost(original.length)
            }
        }

        return TransformedText(AnnotatedString(transformed), offsetMapping)
    }
}

private fun formatMoneyDisplay(amount: Double, currency: String = "COP"): String {
    val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
        groupingSeparator = '.'
        decimalSeparator = ','
    }
    val formatter = DecimalFormat("#,##0", symbols)
    return "$ ${formatter.format(amount)} $currency"
}

/**
 * Premium Apple Wallet / iPhone Style "Tarjeta de Crédito / Deuda" Screen.
 * - Primary Values area at the TOP with oversized typography and quick preset chips.
 * - Interactive Apple Wallet virtual card live preview (bank, brand, quota progress, mask).
 * - Full Currency format with dot separators for all monetary inputs ($ 5.000.000).
 * - Smooth interest rate & billing date typing without snap-back bugs.
 * - Resets form state cleanly on every launch to allow creating multiple credit cards seamlessly.
 * - Categorization system synced with Room DB and instant inline creation.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddCreditCardScreen(
    viewModel: CreditCardFormViewModel,
    onNavigateBack: () -> Unit,
    onCreditCardCreated: () -> Unit,
    userId: String = "user_default",
    financialRepository: FinancialRepository? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val context = LocalContext.current

    // Reset ViewModel state on entrance so opening the screen fresh never auto-dismisses
    LaunchedEffect(Unit) {
        viewModel.resetForm()
    }

    BackHandler {
        viewModel.resetForm()
        onNavigateBack()
    }

    // Input States
    var rawTotalQuotaInput by remember { mutableStateOf("") }
    var rawUsedQuotaInput by remember { mutableStateOf("") }
    var rawRateEaInput by remember { mutableStateOf("24.5") }
    var rawCutOffInput by remember { mutableStateOf("15") }
    var rawDueDayInput by remember { mutableStateOf("2") }

    // Category Selector & Creation
    var showCategoryPickerSheet by remember { mutableStateOf(false) }
    var showCategoryCreationDialog by remember { mutableStateOf(false) }
    var categorySearchQuery by remember { mutableStateOf("") }

    // Currency Selector Modal (164 World Currencies + Custom Currencies)
    var showCurrencyPickerModal by remember { mutableStateOf(false) }
    val selectedCurrencyInfo = remember(state.currencyCode) {
        WORLD_CURRENCIES_CATALOG.find { it.code.equals(state.currencyCode, ignoreCase = true) }
            ?: CurrencyItemInfo(state.currencyCode, "Divisa Seleccionada", "$", "🌐")
    }

    val roomCategoriesState = financialRepository?.getCategories()?.collectAsState(initial = emptyList())
    val roomCategories = roomCategoriesState?.value ?: emptyList()

    val displayCategories = remember(roomCategories) {
        val mapped = roomCategories.map { entity ->
            val icon = resolveCategoryIcon(entity.icon, entity.name)
            val color = try {
                if (entity.colorHex.isNotBlank() && entity.colorHex != "#FFFFFF") {
                    Color(android.graphics.Color.parseColor(entity.colorHex))
                } else Color(0xFF0284C7)
            } catch (_: Exception) {
                Color(0xFF0284C7)
            }
            ExpenseCategory(entity.id, entity.name.trim(), icon, color, color.copy(alpha = 0.15f))
        }
        if (mapped.isNotEmpty()) mapped else listOf(
            ExpenseCategory("cat_general", "Compras & Gastos", Icons.Default.CreditCard, Color(0xFF0284C7), Color(0xFFE0F2FE)),
            ExpenseCategory("cat_tarjeta", "Tarjeta de Crédito", Icons.Default.CreditCard, Color(0xFF8B5CF6), Color(0xFFEDE9FE))
        )
    }

    LaunchedEffect(state.saveSuccess) {
        if (state.saveSuccess) {
            viewModel.resetForm()
            onCreditCardCreated()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let { error ->
            snackbarHostState.showSnackbar(error)
        }
    }

    val adaptive = rememberKashAdaptiveInfo()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("add_credit_card_screen"),
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF0284C7)
            ) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = adaptive.maxContentWidth)
                            .background(
                                Brush.verticalGradient(
                                    colors = listOf(
                                        Color(0xFF0369A1),
                                        Color(0xFF0284C7),
                                        Color(0xFF1E40AF)
                                    )
                                )
                            )
                            .statusBarsPadding()
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            // Back Button
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable {
                                        viewModel.resetForm()
                                        onNavigateBack()
                                    }
                                    .padding(4.dp)
                                    .testTag("back_button")
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.2f),
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Volver",
                                    color = Color.White,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Center Title
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = "Tarjeta / Pasivo",
                                    color = Color.White,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = (-0.2).sp
                                )
                                Text(
                                    text = "Línea de crédito & deuda",
                                    color = Color.White.copy(alpha = 0.85f),
                                    fontSize = 11.sp
                                )
                            }

                            // TOP ACTION PILL: GUARDAR
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (state.totalQuota > 0 || state.debtName.isNotBlank()) Color.White else Color.White.copy(alpha = 0.3f),
                                shadowElevation = if (state.totalQuota > 0) 3.dp else 0.dp,
                                modifier = Modifier
                                    .defaultMinSize(minHeight = 44.dp)
                                    .clickable(enabled = !state.isSaving) {
                                        viewModel.saveCreditCard(userId = userId, context = context)
                                    }
                                    .testTag("top_bar_save_card_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    if (state.isSaving) {
                                        CircularProgressIndicator(
                                            color = KashNavy,
                                            strokeWidth = 2.dp,
                                            modifier = Modifier.size(13.dp)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = null,
                                            tint = if (state.totalQuota > 0 || state.debtName.isNotBlank()) KashNavy else Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Text(
                                        text = "Guardar",
                                        color = if (state.totalQuota > 0 || state.debtName.isNotBlank()) KashNavy else Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(8.dp),
                color = Color.White
            ) {
                val bottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = adaptive.maxContentWidth)
                            .padding(horizontal = 16.dp, vertical = 12.dp)
                            .padding(bottom = bottomPadding)
                    ) {
                        Button(
                            onClick = { viewModel.saveCreditCard(userId = userId, context = context) },
                            enabled = !state.isSaving,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp)
                                .defaultMinSize(minHeight = 44.dp)
                                .testTag("save_credit_card_button"),
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A))
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(20.dp),
                                    color = Color.White,
                                    strokeWidth = 2.dp
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Guardando tarjeta...", color = Color.White, fontWeight = FontWeight.Bold)
                            } else {
                                Icon(
                                    imageVector = Icons.Default.Save,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Guardar Tarjeta de Crédito",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = Color(0xFFF8FAFC)
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .imePadding(),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = adaptive.maxContentWidth)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 28.dp)
            ) {
            // ==========================================
            // 1. APPLE WALLET LIVE CARD PREVIEW (VIRTUAL CARD HERO)
            // ==========================================
            item {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E3A8A),
                                        Color(0xFF0284C7)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            // Top Row: Bank Name + Franchise Badge
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.2f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = Color.White,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = state.creditorBank.ifBlank { "Bancolombia" },
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = state.franchise.ifBlank { "VISA" }.uppercase(),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFF38BDF8),
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // Card Name & Chip
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = state.debtName.ifBlank { "Tarjeta de Crédito Principal" },
                                        fontSize = 17.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color.White
                                    )
                                    Text(
                                        text = "•••• •••• •••• 4428",
                                        fontSize = 13.sp,
                                        letterSpacing = 1.5.sp,
                                        color = Color.White.copy(alpha = 0.75f)
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = Color(0xFFFBBF24),
                                    modifier = Modifier.size(width = 36.dp, height = 26.dp)
                                ) {
                                    Box(
                                        modifier = Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text("EMV", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF78350F))
                                    }
                                }
                            }

                            // Quota Details & Live Bar
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(end = 6.dp)
                                    ) {
                                        Text("Cupo Disponible", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                                        Text(
                                            text = formatMoneyDisplay(state.availableQuota, state.currencyCode),
                                            fontSize = if (state.availableQuota > 99_999_999) 12.5.sp else 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF34D399),
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }

                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(start = 6.dp),
                                        horizontalAlignment = Alignment.End
                                    ) {
                                        Text("Cupo Total", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f), maxLines = 1)
                                        Text(
                                            text = formatMoneyDisplay(state.totalQuota, state.currencyCode),
                                            fontSize = if (state.totalQuota > 99_999_999) 12.sp else 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = Color.White,
                                            softWrap = false,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }

                                val usage = if (state.totalQuota > 0) (state.usedQuota / state.totalQuota).toFloat().coerceIn(0f, 1f) else 0f
                                LinearProgressIndicator(
                                    progress = { usage },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(6.dp)
                                        .clip(CircleShape),
                                    color = Color(0xFF38BDF8),
                                    trackColor = Color.White.copy(alpha = 0.2f),
                                    strokeCap = StrokeCap.Round
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "Deuda actual: ${formatMoneyDisplay(state.usedQuota, state.currencyCode)}",
                                        fontSize = 11.sp,
                                        color = if (state.usedQuota > 0) Color(0xFFFCA5A5) else Color.White.copy(alpha = 0.7f),
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Uso: ${(usage * 100).toInt()}%",
                                        fontSize = 11.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 2. ÁREA DE VALORES (CUPO TOTAL & DEUDA CON SEPARADORES DE MILES)
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "CUPO ASIGNADO Y SALDOS EN FORMATO MONEDA",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 0.8.sp,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 6.dp),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )

                            // Currency pill selector (164 currencies + create)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFEFF6FF),
                                border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                modifier = Modifier
                                    .clickable { showCurrencyPickerModal = true }
                                    .testTag("credit_card_currency_picker_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(selectedCurrencyInfo.flag, fontSize = 13.sp)
                                    Text(
                                        text = state.currencyCode,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue,
                                        softWrap = false
                                    )
                                    Text("▾", fontSize = 11.sp, color = KashBlue, fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // 1. Cupo Total Field con VisualTransformation de miles y tamaño adaptativo
                        val quotaFontSize = when {
                            rawTotalQuotaInput.length > 14 -> 13.5.sp
                            rawTotalQuotaInput.length > 10 -> 15.sp
                            else -> 18.sp
                        }
                        OutlinedTextField(
                            value = rawTotalQuotaInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                rawTotalQuotaInput = clean
                                val dbl = clean.toDoubleOrNull() ?: 0.0
                                viewModel.onTotalQuotaChanged(dbl)
                            },
                            visualTransformation = CardCurrencyDotVisualTransformation(),
                            label = { Text("Cupo Total Aprobado *") },
                            placeholder = { Text("0", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Text("$", fontWeight = FontWeight.Bold, color = KashBlue, fontSize = quotaFontSize, modifier = Modifier.padding(start = 12.dp)) },
                            trailingIcon = { Text(state.currencyCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(end = 12.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = quotaFontSize, fontWeight = FontWeight.Bold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = KashBlue
                            )
                        )

                        // Quick Chips for Quota
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(
                                1_000_000L to "$ 1M",
                                3_000_000L to "$ 3M",
                                5_000_000L to "$ 5M",
                                10_000_000L to "$ 10M",
                                20_000_000L to "$ 20M"
                            ).forEach { (amount, label) ->
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFEFF6FF),
                                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                                    modifier = Modifier.clickable {
                                        rawTotalQuotaInput = amount.toString()
                                        viewModel.onTotalQuotaChanged(amount.toDouble())
                                    }
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    )
                                }
                            }
                        }

                        // Live Conversion Preview (e.g. USD -> COP or COP -> USD)
                        val quotaVal = rawTotalQuotaInput.toDoubleOrNull() ?: state.totalQuota
                        if (quotaVal > 0.0) {
                            Spacer(modifier = Modifier.height(10.dp))
                            LiveCurrencyConversionPreview(
                                amount = quotaVal,
                                inputCurrency = state.currencyCode,
                                userId = userId
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // 2. Cupo Utilizado (Deuda Actual) Field con VisualTransformation de miles
                        OutlinedTextField(
                            value = rawUsedQuotaInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() }
                                rawUsedQuotaInput = clean
                                val dbl = clean.toDoubleOrNull() ?: 0.0
                                viewModel.onUsedQuotaChanged(dbl)
                            },
                            visualTransformation = CardCurrencyDotVisualTransformation(),
                            label = { Text("Saldo Deuda Actual (Opcional - ¿Cuánto debes hoy?)") },
                            placeholder = { Text("0", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFFEF4444)) },
                            trailingIcon = { Text(state.currencyCode, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF64748B), modifier = Modifier.padding(end = 12.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = KashBlue
                            )
                        )
                    }
                }
            }

            // ==========================================
            // 2.5 TARJETA DE GUARDADO INMEDIATO SUPERIOR
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFEFF6FF)),
                    border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Listo para registrar",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF0F172A)
                            )
                            Text(
                                text = "Cupo y datos listos. Puedes guardar de inmediato.",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Button(
                            onClick = { viewModel.saveCreditCard(userId = userId, context = context) },
                            enabled = !state.isSaving,
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = KashBlue,
                                disabledContainerColor = Color(0xFFE2E8F0)
                            )
                        ) {
                            if (state.isSaving) {
                                CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.CheckCircle, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Guardar ahora", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 3. INFORMACIÓN BÁSICA DEL BANCO, FRANQUICIA Y CATEGORÍA
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "DETALLES DE LA TARJETA Y CATEGORÍA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        // Card Name
                        OutlinedTextField(
                            value = state.debtName,
                            onValueChange = { viewModel.onDebtNameChanged(it) },
                            label = { Text("Nombre o Alias de la Tarjeta *") },
                            placeholder = { Text("Ej: Visa Signature Bancolombia", color = Color(0xFF94A3B8)) },
                            leadingIcon = { Icon(Icons.Default.CreditCard, contentDescription = null, tint = KashBlue) },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                focusedContainerColor = Color.White,
                                unfocusedContainerColor = Color(0xFFF8FAFC),
                                cursorColor = KashBlue
                            )
                        )

                        // Category Picker Row
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showCategoryPickerSheet = true }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFE0F2FE)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Category, contentDescription = null, tint = KashBlue, modifier = Modifier.size(18.dp))
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text("Categoría asociada", fontSize = 11.sp, color = Color(0xFF64748B))
                                        Text(state.category.ifBlank { "Compras & Gastos Generales" }, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color(0xFFEFF6FF)
                                ) {
                                    Text("Cambiar", color = KashBlue, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp))
                                }
                            }
                        }

                        // Bank Selector Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Banco Emisor:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                listOf("Bancolombia", "Nu Colombia", "Davivienda", "BBVA", "Banco de Bogotá", "Falabella", "Scotiabank").forEach { bank ->
                                    val isSelected = state.creditorBank.equals(bank, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                        modifier = Modifier.clickable { viewModel.onCreditorBankChanged(bank) }
                                    ) {
                                        Text(
                                            text = bank,
                                            fontSize = 11.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Franchise Chips
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Franquicia:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF475569))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf("Visa", "Mastercard", "American Express").forEach { fran ->
                                    val isSelected = state.franchise.equals(fran, ignoreCase = true)
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) KashBlue else Color(0xFFF1F5F9),
                                        modifier = Modifier.clickable { viewModel.onFranchiseChanged(fran) }
                                    ) {
                                        Text(
                                            text = fran,
                                            fontSize = 12.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = if (isSelected) Color.White else Color(0xFF334155),
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // ==========================================
            // 4. FACTURACIÓN, CORTES E INTERESES (EDICIÓN FLUIDA SIN REAJUSTE FORZADO)
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "CICLO DE FACTURACIÓN Y TASA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Día de Corte
                            OutlinedTextField(
                                value = rawCutOffInput,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }
                                    rawCutOffInput = clean
                                    val day = clean.toIntOrNull()
                                    if (day != null) {
                                        viewModel.onCutOffDayChanged(day)
                                    }
                                },
                                label = { Text("Día de Corte") },
                                placeholder = { Text("15") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = KashBlue, modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    cursorColor = KashBlue
                                )
                            )

                            // Día de Pago
                            OutlinedTextField(
                                value = rawDueDayInput,
                                onValueChange = { input ->
                                    val clean = input.filter { it.isDigit() }
                                    rawDueDayInput = clean
                                    val day = clean.toIntOrNull()
                                    if (day != null) {
                                        viewModel.onPaymentDueDayChanged(day)
                                    }
                                },
                                label = { Text("Día de Pago") },
                                placeholder = { Text("2") },
                                leadingIcon = { Icon(Icons.Default.CalendarMonth, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(18.dp)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f),
                                textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Bold),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = Color(0xFF0F172A),
                                    unfocusedTextColor = Color(0xFF0F172A),
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFE2E8F0),
                                    cursorColor = KashBlue
                                )
                            )
                        }

                        // Tasa de Interés E.A. (Permite borrar sin forzar reversiones molestas)
                        OutlinedTextField(
                            value = rawRateEaInput,
                            onValueChange = { input ->
                                val clean = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
                                rawRateEaInput = clean
                                val dbl = clean.toDoubleOrNull()
                                if (dbl != null) {
                                    viewModel.onPurchasesRateEaChanged(dbl)
                                }
                            },
                            label = { Text("Tasa de Interés Efectiva Anual (% E.A.)") },
                            placeholder = { Text("Ej: 24.5") },
                            leadingIcon = { Icon(Icons.Default.Percent, contentDescription = null, tint = Color(0xFFF59E0B), modifier = Modifier.size(18.dp)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth(),
                            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.SemiBold),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color(0xFF0F172A),
                                unfocusedTextColor = Color(0xFF0F172A),
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFE2E8F0),
                                cursorColor = KashBlue
                            )
                        )
                    }
                }
            }

            // ==========================================
            // 5. CONFIGURACIÓN, DEUDAS Y CONTROL DE SOBREGIRO (IOS SWITCHES)
            // ==========================================
            item {
                Card(
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Text(
                            text = "CONTROL DE DEUDAS Y SOBREGIRO",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF64748B),
                            letterSpacing = 0.8.sp
                        )

                        // Switch: Impactar Pasivos / Dashboard
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Alimentar Tarjeta de Deudas en Dashboard", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Refleja el cupo usado como pasivo en tu patrimonio neto.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = state.includeInNetWorth,
                                onCheckedChange = { viewModel.onIncludeInNetWorthChanged(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                            )
                        }

                        HorizontalDivider(color = Color(0xFFF1F5F9))

                        // Switch: Recordatorios de Fecha de Pago
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Recordatorios de fecha de pago", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                                Text("Notificaciones automáticas antes del día límite de pago.", fontSize = 11.sp, color = Color(0xFF64748B))
                            }
                            Switch(
                                checked = state.remindersEnabled,
                                onCheckedChange = { viewModel.onRemindersEnabledChanged(it) },
                                colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                            )
                        }
                    }
                }
            }
        }
    }
}

    // ==========================================
    // MODAL: CATEGORY SELECTOR SHEET
    // ==========================================
    if (showCategoryPickerSheet) {
        val filteredCats = remember(displayCategories, categorySearchQuery) {
            if (categorySearchQuery.isBlank()) displayCategories
            else displayCategories.filter { it.name.contains(categorySearchQuery, ignoreCase = true) }
        }

        ModalBottomSheet(
            onDismissRequest = {
                categorySearchQuery = ""
                showCategoryPickerSheet = false
            },
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            containerColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Categoría de la Tarjeta", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text("Asocia esta tarjeta o pasivo a una categoría.", fontSize = 12.sp, color = Color(0xFF64748B))
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEFF6FF),
                        border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
                        modifier = Modifier.clickable { showCategoryCreationDialog = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Nueva", color = KashBlue, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                OutlinedTextField(
                    value = categorySearchQuery,
                    onValueChange = { categorySearchQuery = it },
                    placeholder = { Text("Buscar categoría...", color = Color(0xFF94A3B8)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = KashBlue) },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        cursorColor = KashBlue
                    )
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    filteredCats.forEach { cat ->
                        val isSelected = state.category == cat.name
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = if (isSelected) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.onCategoryChanged(cat.name)
                                    categorySearchQuery = ""
                                    showCategoryPickerSheet = false
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .background(cat.bgColor),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(cat.icon, contentDescription = null, tint = cat.color, modifier = Modifier.size(20.dp))
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(cat.name, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF0F172A), modifier = Modifier.weight(1f))
                                if (isSelected) {
                                    Icon(Icons.Default.CheckCircle, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // ==========================================
    // MODAL: CATEGORY CREATION DIALOG
    // ==========================================
    if (showCategoryCreationDialog) {
        CategoryCreationDialog(
            categoryType = "EXPENSE",
            onDismiss = { showCategoryCreationDialog = false },
            onCategoryCreated = { newName, iconKey, colorHex ->
                coroutineScope.launch {
                    financialRepository?.createCategory(
                        name = newName,
                        icon = iconKey,
                        colorHex = colorHex,
                        type = "EXPENSE"
                    )
                    viewModel.onCategoryChanged(newName)
                    showCategoryCreationDialog = false
                    showCategoryPickerSheet = false
                }
            }
        )
    }

    // ==========================================
    // MODAL: CURRENCY SELECTOR (164 WORLD CURRENCIES + CREATE)
    // ==========================================
    if (showCurrencyPickerModal) {
        CurrencyPickerModal(
            selectedCurrencyCode = state.currencyCode,
            onCurrencySelected = { code ->
                val found = WORLD_CURRENCIES_CATALOG.find { it.code.equals(code, ignoreCase = true) }
                viewModel.onCurrencyChanged(code, found?.name ?: code)
                showCurrencyPickerModal = false
            },
            onDismiss = { showCurrencyPickerModal = false },
            financialRepository = financialRepository
        )
    }
}
