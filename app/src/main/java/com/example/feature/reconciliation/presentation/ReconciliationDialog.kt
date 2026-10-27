package com.example.feature.reconciliation.presentation

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.ui.components.KashResponsiveDialog
import com.example.data.local.entity.AccountEntity
import com.example.feature.reconciliation.model.ReconciliationStatus
import com.example.ui.components.IPhoneAdaptiveDatePickerField
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * Currency VisualTransformation formatting raw digits into thousand dot-separated values (e.g. 285888 -> $ 285.888).
 */
class ReconciliationCurrencyVisualTransformation(
    private val prefix: String = "$ "
) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val original = text.text
        if (original.isEmpty()) {
            return TransformedText(text, OffsetMapping.Identity)
        }

        val isNeg = original.startsWith("-")
        val unsigned = if (isNeg) original.substring(1) else original
        val parts = unsigned.split(".")
        val intPart = parts[0]
        val decPart = if (parts.size > 1) parts[1] else null

        val symbols = DecimalFormatSymbols(Locale("es", "CO")).apply {
            groupingSeparator = '.'
        }
        val formatter = DecimalFormat("#,###", symbols)
        val formattedInt = if (intPart.isNotEmpty()) {
            val num = intPart.toLongOrNull() ?: 0L
            val formatted = formatter.format(num)
            if (isNeg) "-$formatted" else formatted
        } else {
            if (isNeg) "-" else ""
        }

        val formattedWithDec = when {
            decPart != null -> "$formattedInt,$decPart"
            unsigned.contains(".") -> "$formattedInt,"
            else -> formattedInt
        }

        val output = "$prefix$formattedWithDec"

        val offsetMapping = object : OffsetMapping {
            override fun originalToTransformed(offset: Int): Int {
                if (offset <= 0) return prefix.length
                val sub = original.take(offset)
                val subIsNeg = sub.startsWith("-")
                val subUnsigned = if (subIsNeg) sub.substring(1) else sub
                val subParts = subUnsigned.split(".")
                val subInt = subParts[0]
                val subDec = if (subParts.size > 1) subParts[1] else null
                val formattedSubInt = if (subInt.isNotEmpty()) {
                    val n = subInt.toLongOrNull() ?: 0L
                    val formatted = formatter.format(n)
                    if (subIsNeg) "-$formatted" else formatted
                } else {
                    if (subIsNeg) "-" else ""
                }
                val res = when {
                    subDec != null -> "$formattedSubInt,$subDec"
                    subUnsigned.contains(".") -> "$formattedSubInt,"
                    else -> formattedSubInt
                }
                return (prefix.length + res.length).coerceAtMost(output.length)
            }

            override fun transformedToOriginal(offset: Int): Int {
                val offsetWithoutPrefix = (offset - prefix.length).coerceAtLeast(0)
                var origCount = 0
                var transCount = 0
                val transformedWithoutPrefix = formattedWithDec
                while (transCount < offsetWithoutPrefix && transCount < transformedWithoutPrefix.length) {
                    val c = transformedWithoutPrefix[transCount]
                    if (c != '.') {
                        origCount++
                    }
                    transCount++
                }
                return origCount.coerceIn(0, original.length)
            }
        }

        return TransformedText(AnnotatedString(output), offsetMapping)
    }
}

/**
 * iPhone / iOS styled Responsive Account Reconciliation Modal.
 * Adapted for compact and small screens with native wheel date picker,
 * live currency formatting, glassmorphic styling, and KashApp blue accents.
 */
@Composable
fun ReconciliationDialog(
    account: AccountEntity,
    viewModel: ReconciliationViewModel,
    onDismiss: () -> Unit,
    onViewHistory: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scrollState = rememberScrollState()

    val currencyFormat = remember {
        NumberFormat.getCurrencyInstance(Locale("es", "CO")).apply {
            maximumFractionDigits = 0
        }
    }

    val kashAppBlue = Color(0xFF0284C7)
    val kashAppLightBlue = Color(0xFF38BDF8)
    val glassCardBg = Color(0xFF1E293B).copy(alpha = 0.85f)
    val glassBorderColor = Color(0xFF38BDF8).copy(alpha = 0.22f)

    KashResponsiveDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF0F172A),
        borderColor = glassBorderColor
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 16.dp)
                .verticalScroll(scrollState),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // iOS Modal Header: Title + Single-Line Historial Pill + Close Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Conciliar Cuenta",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Ajuste de saldo contra extracto",
                        fontSize = 11.5.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Historial Pill (Single line, no wrapping)
                    Surface(
                        onClick = onViewHistory,
                        shape = RoundedCornerShape(20.dp),
                        color = kashAppBlue.copy(alpha = 0.18f),
                        border = BorderStroke(1.dp, kashAppLightBlue.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "Historial",
                                tint = kashAppLightBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = "Historial",
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = kashAppLightBlue,
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }

                    // Close icon
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF1E293B))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // Glassmorphic Account Header Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(glassCardBg)
                    .border(1.dp, glassBorderColor, RoundedCornerShape(16.dp))
                    .padding(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(kashAppBlue.copy(alpha = 0.2f))
                            .border(1.dp, kashAppLightBlue.copy(alpha = 0.4f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalance,
                            contentDescription = null,
                            tint = kashAppLightBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = account.name,
                            fontSize = 14.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Saldo KashApp:",
                                fontSize = 11.sp,
                                color = Color(0xFF94A3B8)
                            )
                            Text(
                                text = currencyFormat.format(account.balance),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = kashAppLightBlue
                            )
                        }
                    }
                }
            }

            // Input: Saldo Real del Extracto / Efectivo with Real-time Currency Format
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Saldo Real del Extracto / Efectivo",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                OutlinedTextField(
                    value = uiState.realBalanceInput,
                    onValueChange = { viewModel.onRealBalanceChanged(it) },
                    placeholder = { Text("Ej: 1.500.000", color = Color(0xFF64748B), fontSize = 14.sp) },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Payments,
                            contentDescription = null,
                            tint = kashAppLightBlue,
                            modifier = Modifier.size(18.dp)
                        )
                    },
                    visualTransformation = ReconciliationCurrencyVisualTransformation("$ "),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        focusedBorderColor = kashAppBlue,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = kashAppLightBlue
                    )
                )
            }

            // iPhone Adaptive Wheel Date Picker
            IPhoneAdaptiveDatePickerField(
                value = uiState.reconciliationDate,
                onValueChange = { viewModel.onDateChanged(it) },
                label = "Fecha de Corte del Extracto",
                modifier = Modifier.fillMaxWidth(),
                isDarkTheme = true
            )

            // Input: Notas de Auditoría (Opcional)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Notas de Auditoría (Opcional)",
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCBD5E1)
                )
                OutlinedTextField(
                    value = uiState.notesInput,
                    onValueChange = { viewModel.onNotesChanged(it) },
                    placeholder = { Text("Ej: Cuadre con extracto bancario de Octubre", color = Color(0xFF64748B), fontSize = 13.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 2,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color(0xFF1E293B),
                        unfocusedContainerColor = Color(0xFF1E293B).copy(alpha = 0.6f),
                        focusedBorderColor = kashAppBlue,
                        unfocusedBorderColor = Color(0xFF334155),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = kashAppLightBlue
                    )
                )
            }

            // Live Discrepancy Calculation Banner (iOS Glassmorphic Card)
            uiState.calculatedDiscrepancy?.let { disc ->
                val (tintColor, icon, statusTitle, statusSubtitle) = when (disc.status) {
                    ReconciliationStatus.BALANCED -> Quadruple(
                        Color(0xFF10B981),
                        Icons.Default.CheckCircle,
                        "Cuenta 100% Cuadrada",
                        "El saldo del extracto coincide exactamente. No requiere movimiento de ajuste."
                    )
                    ReconciliationStatus.ADJUSTED_SURPLUS -> Quadruple(
                        Color(0xFF38BDF8),
                        Icons.Default.Info,
                        "Ajuste por Superávit (+ Ingreso)",
                        "Saldo real mayor por +${currencyFormat.format(disc.discrepancyAmount)}. Se creará un abono de conciliación."
                    )
                    ReconciliationStatus.ADJUSTED_DEFICIT -> Quadruple(
                        Color(0xFFF59E0B),
                        Icons.Default.Warning,
                        "Ajuste por Déficit (- Gasto)",
                        "Saldo real menor por ${currencyFormat.format(disc.discrepancyAmount)}. Se creará un gasto de conciliación."
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(tintColor.copy(alpha = 0.12f))
                        .border(1.dp, tintColor.copy(alpha = 0.35f), RoundedCornerShape(14.dp))
                        .padding(12.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(tintColor.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = tintColor,
                                modifier = Modifier.size(18.dp)
                            )
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = statusTitle,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.5.sp,
                                color = tintColor
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = statusSubtitle,
                                fontSize = 11.5.sp,
                                color = Color(0xFFCBD5E1),
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Error feedback
            AnimatedVisibility(visible = uiState.errorMessage != null) {
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFEF4444),
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Buttons Row: [Cancelar] [Confirmar y Conciliar]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    enabled = !uiState.isExecuting,
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, Color(0xFF475569)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text("Cancelar", color = Color(0xFF94A3B8), fontSize = 13.5.sp)
                }

                Button(
                    onClick = {
                        viewModel.submitReconciliation {
                            onDismiss()
                        }
                    },
                    enabled = !uiState.isExecuting,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = kashAppBlue),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    if (uiState.isExecuting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                    }
                    Text(
                        text = "Confirmar",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }
    }
}

private data class Quadruple<A, B, C, D>(val first: A, val second: B, val third: C, val fourth: D)
