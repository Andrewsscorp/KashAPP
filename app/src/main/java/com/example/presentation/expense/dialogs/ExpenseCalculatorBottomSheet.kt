package com.example.presentation.expense.dialogs

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.expense.model.formatLiveNumberString
import com.example.presentation.expense.model.formatMoneyDisplay

/**
 * Bottom Sheet container for the integrated KashApp financial calculator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpenseCalculatorBottomSheet(
    visible: Boolean,
    initialValue: String,
    currencyCode: String,
    currencySymbol: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        KashAppCalculatorSheet(
            initialValue = initialValue,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            onConfirm = { result ->
                onConfirm(result)
                onDismiss()
            },
            onClose = onDismiss
        )
    }
}

/**
 * KashApp Integrated Financial Calculator Composable with Full Currency Formatting
 */
@Composable
fun KashAppCalculatorSheet(
    initialValue: String,
    currencyCode: String,
    currencySymbol: String,
    onConfirm: (String) -> Unit,
    onClose: () -> Unit
) {
    var displayExpr by remember { mutableStateOf(if (initialValue.isBlank()) "0" else initialValue) }

    fun evaluate(expr: String): Double? {
        return try {
            val sanitized = expr.replace("×", "*").replace("÷", "/")
            val tokens = sanitized.split(Regex("(?<=[-+*/])|(?=[-+*/])")).map { it.trim() }.filter { it.isNotEmpty() }
            if (tokens.isEmpty()) return null

            var result = tokens[0].toDoubleOrNull() ?: return null
            var i = 1
            while (i < tokens.size - 1) {
                val op = tokens[i]
                val next = tokens[i + 1].toDoubleOrNull() ?: return null
                when (op) {
                    "+" -> result += next
                    "-" -> result -= next
                    "*" -> result *= next
                    "/" -> if (next != 0.0) result /= next
                }
                i += 2
            }
            result
        } catch (e: Exception) {
            null
        }
    }

    val currentResult = remember(displayExpr) {
        if (displayExpr.any { it in listOf('+', '-', '×', '÷') }) {
            evaluate(displayExpr)
        } else {
            displayExpr.toDoubleOrNull()
        }
    }

    // Currency Formatted Expression
    val formattedExprDisplay = remember(displayExpr) {
        val parts = displayExpr.split(Regex("(?<=[-+×÷*/])|(?=[-+×÷*/])"))
        parts.joinToString(" ") { part ->
            val num = part.toDoubleOrNull()
            if (num != null && part !in listOf("+", "-", "×", "÷", "*", "/")) {
                formatLiveNumberString(part)
            } else {
                part
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 14.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0284C7)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Calculate, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Calculadora Financiera",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Moneda activa: $currencyCode ($currencySymbol)",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(0x26FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Calculator Screen Display with Full Currency Format
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF032B43),
            border = BorderStroke(1.5.dp, Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 14.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$currencySymbol $formattedExprDisplay",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )

                if (currentResult != null) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "= ${formatMoneyDisplay(currentResult, currencyCode)}",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        textAlign = TextAlign.End
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        val buttonRows = listOf(
            listOf("C", "÷", "×", "⌫"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "="),
            listOf("0", "00", ".", "USAR")
        )

        buttonRows.forEach { rowKeys ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowKeys.forEach { key ->
                    val isOperator = key in listOf("+", "-", "×", "÷")
                    val isAction = key in listOf("C", "⌫", "=", "USAR")
                    val isUsar = key == "USAR"
                    val isEquals = key == "="

                    val containerColor = when {
                        isUsar -> Color(0xFF10B981)
                        isEquals -> Color(0xFF0284C7)
                        isOperator -> Color(0xFF1E3A5F)
                        key in listOf("C", "⌫") -> Color(0xFF334155)
                        else -> Color(0xFF1E293B)
                    }

                    val textColor = when {
                        isUsar || isEquals -> Color.White
                        isOperator -> Color(0xFF38BDF8)
                        else -> Color.White
                    }

                    Surface(
                        modifier = Modifier
                            .weight(if (isUsar) 1.2f else 1f)
                            .height(52.dp)
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                when (key) {
                                    "C" -> displayExpr = "0"
                                    "⌫" -> {
                                        displayExpr = if (displayExpr.length > 1) displayExpr.dropLast(1) else "0"
                                    }
                                    "=" -> {
                                        val res = evaluate(displayExpr)
                                        if (res != null) {
                                            displayExpr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                        }
                                    }
                                    "USAR" -> {
                                        val res = evaluate(displayExpr) ?: displayExpr.toDoubleOrNull() ?: 0.0
                                        val formatted = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                        onConfirm(formatted)
                                    }
                                    "+", "-", "×", "÷" -> {
                                        val last = displayExpr.lastOrNull()
                                        if (last in listOf('+', '-', '×', '÷')) {
                                            displayExpr = displayExpr.dropLast(1) + key
                                        } else {
                                            displayExpr += key
                                        }
                                    }
                                    "00" -> {
                                        if (displayExpr != "0") displayExpr += "00"
                                    }
                                    "." -> {
                                        val lastNum = displayExpr.split(Regex("[-+×÷]")).lastOrNull() ?: ""
                                        if (!lastNum.contains(".")) {
                                            displayExpr += "."
                                        }
                                    }
                                    else -> {
                                        if (displayExpr == "0") {
                                            displayExpr = key
                                        } else {
                                            displayExpr += key
                                        }
                                    }
                                }
                            }
                            .testTag("calc_key_$key"),
                        shape = RoundedCornerShape(14.dp),
                        color = containerColor,
                        shadowElevation = 2.dp
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            if (key == "⌫") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Borrar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else {
                                Text(
                                    text = if (key == "USAR") "✓ Usar" else key,
                                    fontSize = if (key == "USAR") 14.sp else 19.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textColor
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))
    }
}
