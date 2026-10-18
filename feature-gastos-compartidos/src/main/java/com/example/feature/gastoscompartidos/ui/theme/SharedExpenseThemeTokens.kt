package com.example.feature.gastoscompartidos.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.text.NumberFormat
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToLong

object SharedExpenseThemeTokens {
    // Paleta Azul Oficial de KashApp
    val KashNavy = Color(0xFF0F172A)
    val KashDarkSlate = Color(0xFF032B43)
    val KashDeepBlue = Color(0xFF021727)
    val KashBlue = Color(0xFF0284C7)
    val KashBlueDark = Color(0xFF0369A1)
    val KashCyan = Color(0xFF0EA5E9)
    val KashCyanLight = Color(0xFF38BDF8)
    val KashSurfaceTint = Color(0xFFF0F9FF)
    val KashBlueBorder = Color(0xFFBAE6FD)
    val KashLightBg = Color(0xFFF1F5F9)
    val KashCardBg = Color(0xFFFFFFFF)
    val KashSurfaceMuted = Color(0xFFF8FAFC)
    val KashBorder = Color(0xFFCBD5E1)
    val KashTextPrimary = Color(0xFF0F172A)
    val KashTextSecondary = Color(0xFF475569)
    val KashTextMuted = Color(0xFF64748B)

    // Colores Semánticos Contables NIIF y Mental Accounting
    val ReceivableTeal = Color(0xFF0D9488)        // Activo Corriente: Te deben (Cuenta por Cobrar)
    val ReceivableTealBg = Color(0xFFF0FDFA)
    val ReceivableTealBorder = Color(0xFF99F6E4)

    val PayableRose = Color(0xFFE11D48)           // Pasivo Corriente: Debes (Cuenta por Pagar)
    val PayableRoseBg = Color(0xFFFFF1F2)
    val PayableRoseBorder = Color(0xFFFECDD3)

    val MentalHoldViolet = Color(0xFF7C3AED)      // Reserva en holdBalance (Mental Accounting)
    val MentalHoldVioletBg = Color(0xFFF5F3FF)
    val MentalHoldVioletBorder = Color(0xFFDDD6FE)

    val SettledEmerald = Color(0xFF10B981)        // Paz y Salvo / Liquidado
    val SettledEmeraldBg = Color(0xFFECFDF5)

    val PendingAmber = Color(0xFFD97706)          // Pendiente de cobro / Abono parcial
    val PendingAmberBg = Color(0xFFFFFBEB)

    val HeaderBlueGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF032B43),
                Color(0xFF0284C7)
            )
        )

    val HeroBlueGradient: Brush
        get() = Brush.verticalGradient(
            colors = listOf(
                Color(0xFF0284C7),
                Color(0xFF032B43)
            )
        )
}

private val SharedModuleLightColorScheme = lightColorScheme(
    primary = SharedExpenseThemeTokens.KashBlue,
    onPrimary = Color.White,
    primaryContainer = SharedExpenseThemeTokens.KashSurfaceTint,
    onPrimaryContainer = SharedExpenseThemeTokens.KashNavy,
    secondary = SharedExpenseThemeTokens.KashCyan,
    onSecondary = Color.White,
    background = SharedExpenseThemeTokens.KashLightBg,
    onBackground = SharedExpenseThemeTokens.KashTextPrimary,
    surface = Color.White,
    onSurface = SharedExpenseThemeTokens.KashTextPrimary,
    surfaceVariant = SharedExpenseThemeTokens.KashSurfaceMuted,
    onSurfaceVariant = SharedExpenseThemeTokens.KashTextSecondary,
    outline = SharedExpenseThemeTokens.KashBorder,
    error = SharedExpenseThemeTokens.PayableRose
)

/**
 * Envuelve cualquier pantalla o modal de Gastos Compartidos forzando el esquema de color claro
 * azul de KashApp y `LocalContentColor = KashTextPrimary` (#0F172A) para que NUNCA se vea texto blanco
 * sobre fondo blanco aunque el sistema operativo esté en modo oscuro.
 */
@Composable
fun SharedExpenseThemeWrapper(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SharedModuleLightColorScheme
    ) {
        CompositionLocalProvider(
            LocalContentColor provides SharedExpenseThemeTokens.KashTextPrimary
        ) {
            content()
        }
    }
}

/**
 * Colores explícitos para todos los OutlinedTextField del módulo:
 * texto escrito siempre en Azul Oscuro (#0F172A), borde activo en Azul KashApp (#0284C7) y fondo blanco/celeste suave.
 */
@Composable
fun kashOutlinedTextFieldColors(): TextFieldColors = OutlinedTextFieldDefaults.colors(
    focusedTextColor = SharedExpenseThemeTokens.KashTextPrimary,
    unfocusedTextColor = SharedExpenseThemeTokens.KashTextPrimary,
    disabledTextColor = SharedExpenseThemeTokens.KashTextSecondary,
    errorTextColor = SharedExpenseThemeTokens.PayableRose,
    focusedContainerColor = Color.White,
    unfocusedContainerColor = SharedExpenseThemeTokens.KashSurfaceMuted,
    disabledContainerColor = SharedExpenseThemeTokens.KashSurfaceMuted,
    cursorColor = SharedExpenseThemeTokens.KashBlue,
    focusedBorderColor = SharedExpenseThemeTokens.KashBlue,
    unfocusedBorderColor = SharedExpenseThemeTokens.KashBorder,
    focusedLabelColor = SharedExpenseThemeTokens.KashBlue,
    unfocusedLabelColor = SharedExpenseThemeTokens.KashTextSecondary,
    focusedPlaceholderColor = SharedExpenseThemeTokens.KashTextMuted,
    unfocusedPlaceholderColor = SharedExpenseThemeTokens.KashTextMuted,
    focusedLeadingIconColor = SharedExpenseThemeTokens.KashBlue,
    unfocusedLeadingIconColor = SharedExpenseThemeTokens.KashTextSecondary,
    focusedTrailingIconColor = SharedExpenseThemeTokens.KashBlue,
    unfocusedTrailingIconColor = SharedExpenseThemeTokens.KashTextSecondary
)

/**
 * Cabecera superior corporativa en el azul oficial de KashApp con `statusBarsPadding()`,
 * botón de regreso circular siempre visible y acciones responsivas.
 */
@Composable
fun KashBlueTopBar(
    title: String,
    subtitle: String,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    backButtonTestTag: String = "shared_back_btn",
    actions: @Composable RowScope.() -> Unit = {}
) {
    val responsive = rememberSharedResponsiveLayoutInfo()
    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
        shadowElevation = 6.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(SharedExpenseThemeTokens.HeaderBlueGradient)
                .statusBarsPadding()
                .padding(
                    horizontal = responsive.horizontalPadding,
                    vertical = if (responsive.isUltraCompactWidth) 10.dp else 14.dp
                )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.18f))
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver atrás",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = title,
                            fontSize = if (responsive.isUltraCompactWidth) 16.sp else 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFE0F2FE),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    content = actions
                )
            }
        }
    }
}

/**
 * Campo de texto estándar con contraste garantizado (#0F172A sobre fondo claro) y borde azul KashApp.
 */
@Composable
fun KashStandardTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    leadingIcon: ImageVector? = null,
    singleLine: Boolean = true,
    maxLines: Int = if (singleLine) 1 else 3,
    keyboardType: KeyboardType = KeyboardType.Text,
    capitalization: KeyboardCapitalization = KeyboardCapitalization.Sentences,
    imeAction: ImeAction = ImeAction.Next
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = {
            Text(
                text = label,
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = SharedExpenseThemeTokens.KashTextSecondary
            )
        },
        placeholder = if (placeholder.isNotBlank()) {
            {
                Text(
                    text = placeholder,
                    fontSize = 12.sp,
                    color = SharedExpenseThemeTokens.KashTextMuted
                )
            }
        } else null,
        leadingIcon = if (leadingIcon != null) {
            {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    tint = SharedExpenseThemeTokens.KashBlue,
                    modifier = Modifier.size(18.dp)
                )
            }
        } else null,
        trailingIcon = if (value.isNotEmpty() && singleLine) {
            {
                IconButton(onClick = { onValueChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Limpiar campo",
                        tint = SharedExpenseThemeTokens.KashTextMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        } else null,
        textStyle = TextStyle(
            color = SharedExpenseThemeTokens.KashTextPrimary,
            fontSize = 14.sp,
            fontWeight = FontWeight.SemiBold
        ),
        singleLine = singleLine,
        maxLines = maxLines,
        shape = RoundedCornerShape(14.dp),
        colors = kashOutlinedTextFieldColors(),
        keyboardOptions = KeyboardOptions(
            keyboardType = keyboardType,
            capitalization = capitalization,
            imeAction = imeAction
        ),
        modifier = modifier
    )
}

/**
 * Transformación visual para mostrar separadores de miles con punto (ej. 125000 -> 125.000)
 * mientras el usuario escribe dígitos numéricos.
 */
class ThousandSeparatorVisualTransformation : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val raw = text.text
        if (raw.isEmpty() || !raw.all { it.isDigit() }) {
            return TransformedText(text, OffsetMapping.Identity)
        }
        val reversed = raw.reversed()
        val formattedReversed = StringBuilder()
        for (i in reversed.indices) {
            if (i > 0 && i % 3 == 0) {
                formattedReversed.append('.')
            }
            formattedReversed.append(reversed[i])
        }
        val formatted = formattedReversed.reverse().toString()

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return 0
                val digitsAfter = raw.length - offset
                var dotsAfter = 0
                var count = 0
                for (i in raw.length - 1 downTo offset) {
                    count++
                    if (count % 3 == 0 && i > 0) dotsAfter++
                }
                val totalDots = (raw.length - 1).coerceAtLeast(0) / 3
                val dotsBefore = (totalDots - dotsAfter).coerceAtLeast(0)
                return (offset + dotsBefore).coerceAtMost(formatted.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                if (offset <= 0) return 0
                val sub = formatted.take(offset)
                val dots = sub.count { it == '.' }
                return (offset - dots).coerceIn(0, raw.length)
            }
        }
        return TransformedText(AnnotatedString(formatted), offsetMapping)
    }
}

/**
 * Casilla de Moneda con formato en vivo ($ 150.000 COP), insignia de divisa,
 * teclado numérico y botones rápidos opcionales.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun KashFormattedCurrencyField(
    label: String,
    modifier: Modifier = Modifier,
    amount: Double? = null,
    onAmountChange: ((Double) -> Unit)? = null,
    value: Double? = null,
    onValueChange: ((Double) -> Unit)? = null,
    currencyCode: String = "COP",
    currencySymbol: String = currencyCode,
    quickPresets: List<Long> = emptyList(),
    onQuickAdd: ((Long) -> Unit)? = null
) {
    val effectiveAmount = value ?: amount ?: 0.0
    val effectiveCurrency = if (currencySymbol != "COP" && currencyCode == "COP") currencySymbol else currencyCode
    val rawDigits = if (effectiveAmount <= 0.0) "" else effectiveAmount.roundToLong().toString()
    val visualTransformation = remember { ThousandSeparatorVisualTransformation() }

    val handleAmountChange: (Double) -> Unit = { newAmt ->
        onAmountChange?.invoke(newAmt)
        onValueChange?.invoke(newAmt)
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        OutlinedTextField(
            value = rawDigits,
            onValueChange = { input ->
                val digitsOnly = input.filter { it.isDigit() }.take(12)
                handleAmountChange(digitsOnly.toDoubleOrNull() ?: 0.0)
            },
            label = {
                Text(
                    text = label,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedExpenseThemeTokens.KashTextSecondary
                )
            },
            placeholder = {
                Text(
                    text = "0",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharedExpenseThemeTokens.KashTextMuted
                )
            },
            leadingIcon = {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.14f),
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = "$ $effectiveCurrency",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = SharedExpenseThemeTokens.KashBlue,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            },
            trailingIcon = if (effectiveAmount > 0.0) {
                {
                    IconButton(onClick = { handleAmountChange(0.0) }) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Borrar monto",
                            tint = SharedExpenseThemeTokens.KashTextMuted,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            } else null,
            visualTransformation = visualTransformation,
            textStyle = TextStyle(
                color = SharedExpenseThemeTokens.KashTextPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            ),
            singleLine = true,
            shape = RoundedCornerShape(14.dp),
            colors = kashOutlinedTextFieldColors(),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.NumberPassword,
                imeAction = ImeAction.Done
            ),
            modifier = Modifier.fillMaxWidth()
        )

        if (quickPresets.isNotEmpty()) {
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                quickPresets.forEach { presetVal ->
                    val chipLabel = when {
                        presetVal >= 1_000_000L -> "+ $${presetVal / 1_000_000L}M"
                        presetVal >= 1_000L -> "+ $${presetVal / 1_000L}k"
                        else -> "+ $$presetVal"
                    }
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SharedExpenseThemeTokens.KashSurfaceTint,
                        border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBlueBorder),
                        modifier = Modifier.clickable {
                            if (onQuickAdd != null) {
                                onQuickAdd(presetVal)
                            } else {
                                handleAmountChange(effectiveAmount + presetVal)
                            }
                        }
                    ) {
                        Text(
                            text = chipLabel,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SharedExpenseThemeTokens.KashBlueDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Casilla interactiva de Fecha con formato legible en español ("16 oct 2026 • 2026-10-16"),
 * calendario Material 3 DatePickerDialog y atajos de quincena (Hoy, Día 15, Día 30).
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun KashFormattedDatePickerField(
    label: String,
    modifier: Modifier = Modifier,
    dateIso: String? = null,
    valueIso: String? = null,
    onDateSelected: (String) -> Unit = {},
    showQuickPaydayChips: Boolean = false
) {
    var showDialog by remember { mutableStateOf(false) }
    val effectiveDateIso = valueIso ?: dateIso ?: LocalDate.now().toString()
    val parsedDate = remember(effectiveDateIso) {
        runCatching { LocalDate.parse(effectiveDateIso) }.getOrElse { LocalDate.now() }
    }
    val formattedDisplay = remember(parsedDate) {
        val esFormatter = DateTimeFormatter.ofPattern("dd MMM yyyy", Locale("es", "CO"))
        "${parsedDate.format(esFormatter)} ($parsedDate)"
    }

    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(14.dp),
            color = Color.White,
            border = BorderStroke(1.dp, SharedExpenseThemeTokens.KashBlue.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { showDialog = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = SharedExpenseThemeTokens.KashSurfaceTint
                    ) {
                        Icon(
                            imageVector = Icons.Default.CalendarMonth,
                            contentDescription = null,
                            tint = SharedExpenseThemeTokens.KashBlue,
                            modifier = Modifier
                                .padding(8.dp)
                                .size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = SharedExpenseThemeTokens.KashTextSecondary
                        )
                        Text(
                            text = formattedDisplay,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SharedExpenseThemeTokens.KashTextPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Text(
                    text = "Cambiar",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = SharedExpenseThemeTokens.KashBlue
                )
            }
        }

        if (showQuickPaydayChips) {
            val today = LocalDate.now()
            val next15 = if (today.dayOfMonth <= 15) today.withDayOfMonth(15) else today.plusMonths(1).withDayOfMonth(15)
            val endOfMonth = today.withDayOfMonth(today.lengthOfMonth().coerceAtMost(30))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "Hoy (${today})" to today.toString(),
                    "Quincena 15 ($next15)" to next15.toString(),
                    "Corte 30 ($endOfMonth)" to endOfMonth.toString()
                ).forEach { (chipTitle, isoVal) ->
                    val isSelected = dateIso == isoVal
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashSurfaceTint,
                        border = BorderStroke(1.dp, if (isSelected) SharedExpenseThemeTokens.KashBlue else SharedExpenseThemeTokens.KashBlueBorder),
                        modifier = Modifier.clickable { onDateSelected(isoVal) }
                    ) {
                        Text(
                            text = chipTitle,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isSelected) Color.White else SharedExpenseThemeTokens.KashBlueDark,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
        }
    }

    if (showDialog) {
        val initialMillis = parsedDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)
        DatePickerDialog(
            onDismissRequest = { showDialog = false },
            confirmButton = {
                Button(
                    onClick = {
                        val millis = datePickerState.selectedDateMillis
                        if (millis != null) {
                            val selectedLocal = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            onDateSelected(selectedLocal.toString())
                        }
                        showDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.KashBlue)
                ) {
                    Text("Aplicar Fecha", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancelar", color = SharedExpenseThemeTokens.KashTextSecondary)
                }
            },
            colors = DatePickerDefaults.colors(containerColor = Color.White)
        ) {
            DatePicker(
                state = datePickerState,
                colors = DatePickerDefaults.colors(
                    containerColor = Color.White,
                    titleContentColor = SharedExpenseThemeTokens.KashNavy,
                    headlineContentColor = SharedExpenseThemeTokens.KashBlue,
                    weekdayContentColor = SharedExpenseThemeTokens.KashTextSecondary,
                    dayContentColor = SharedExpenseThemeTokens.KashTextPrimary,
                    selectedDayContainerColor = SharedExpenseThemeTokens.KashBlue,
                    selectedDayContentColor = Color.White,
                    todayContentColor = SharedExpenseThemeTokens.KashBlue,
                    todayDateBorderColor = SharedExpenseThemeTokens.KashBlue
                )
            )
        }
    }
}

fun formatWholeNumberWithDots(value: Double): String {
    val nf = NumberFormat.getIntegerInstance(Locale("es", "CO"))
    return nf.format(value.roundToLong())
}

/**
 * Tokens adaptativos y responsivos para teléfonos compactos (<360dp), estándar (360-599dp),
 * tablets/plegables medianos (600-839dp) y pantallas expandidas/landscape (>=840dp).
 */
@Immutable
data class SharedResponsiveLayoutInfo(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val isUltraCompactWidth: Boolean,
    val isMediumOrExpandedWidth: Boolean,
    val isExpandedTabletWidth: Boolean,
    val isLandscape: Boolean,
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val maxContentWidth: Dp,
    val gridColumnsCount: Int
)

@Composable
fun rememberSharedResponsiveLayoutInfo(): SharedResponsiveLayoutInfo {
    val adaptive = com.example.core.ui.adaptive.rememberKashAdaptiveInfo()
    return remember(adaptive) {
        SharedResponsiveLayoutInfo(
            screenWidthDp = adaptive.screenWidthDp,
            screenHeightDp = adaptive.screenHeightDp,
            isUltraCompactWidth = adaptive.isUltraCompact,
            isMediumOrExpandedWidth = adaptive.isMedium || adaptive.isExpanded,
            isExpandedTabletWidth = adaptive.isExpanded,
            isLandscape = adaptive.isLandscape,
            horizontalPadding = adaptive.horizontalPadding,
            verticalSpacing = adaptive.verticalSpacing,
            maxContentWidth = adaptive.maxContentWidth,
            gridColumnsCount = adaptive.gridColumnsCount
        )
    }
}
