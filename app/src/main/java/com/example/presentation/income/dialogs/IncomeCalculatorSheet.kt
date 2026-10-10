package com.example.presentation.income.dialogs

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.example.presentation.expense.formatLiveNumberString
import com.example.presentation.expense.formatMoneyDisplay

/**
 * Auxiliary Financial Calculator Bottom Sheet for calculating gross and net income.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IncomeCalculatorSheet(
    visible: Boolean,
    initialValue: String,
    currencyCode: String,
    currencySymbol: String = "$",
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0A192F),
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
    ) {
        IncomeCalculatorContent(
            initialValue = initialValue,
            currencyCode = currencyCode,
            currencySymbol = currencySymbol,
            onConfirm = {
                onConfirm(it)
                onDismiss()
            },
            onClose = onDismiss
        )
    }
}

@Composable
private fun IncomeCalculatorContent(
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
        } catch (_: Exception) {
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
            .navigationBarsPadding()
            .padding(horizontal = 24.dp, vertical = 8.dp)
            .testTag("income_calculator_sheet")
    ) {
        // Top Bar of Calculator Sheet
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF10B981).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Calculate,
                        contentDescription = null,
                        tint = Color(0xFF10B981),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Calculadora Financiera",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Calcula importes brutos, comisiones o deducciones",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0x33FFFFFF))
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Cerrar",
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Display Screen of Calculator
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color(0xFF021727),
            border = BorderStroke(1.dp, Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "$currencySymbol $formattedExprDisplay",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 2,
                    textAlign = TextAlign.End
                )

                if (currentResult != null && displayExpr.any { it in listOf('+', '-', '×', '÷') }) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "= ${formatMoneyDisplay(currentResult, currencyCode)}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF10B981)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Keypad Grid
        val buttonRows = listOf(
            listOf("C", "÷", "×", "DEL"),
            listOf("7", "8", "9", "-"),
            listOf("4", "5", "6", "+"),
            listOf("1", "2", "3", "="),
            listOf("0", "00", ".", "USE")
        )

        buttonRows.forEach { row ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                row.forEach { key ->
                    val isAction = key in listOf("USE", "=")
                    val isOperator = key in listOf("+", "-", "×", "÷")
                    val isClear = key in listOf("C", "DEL")

                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            key == "USE" -> Color(0xFF10B981)
                            key == "=" -> Color(0xFF0284C7)
                            isOperator -> Color(0xFF1E293B)
                            isClear -> Color(0xFF334155)
                            else -> Color(0xFF0F172A)
                        },
                        border = BorderStroke(1.dp, Color(0xFF1E293B)),
                        modifier = Modifier
                            .weight(1f)
                            .height(54.dp)
                            .clickable {
                                when (key) {
                                    "C" -> displayExpr = "0"
                                    "DEL" -> {
                                        displayExpr = if (displayExpr.length <= 1) "0" else displayExpr.dropLast(1)
                                    }
                                    "=" -> {
                                        val res = evaluate(displayExpr)
                                        if (res != null) {
                                            displayExpr = if (res % 1.0 == 0.0) res.toLong().toString() else res.toString()
                                        }
                                    }
                                    "USE" -> {
                                        val finalVal = currentResult ?: displayExpr.toDoubleOrNull() ?: 0.0
                                        val strVal = if (finalVal % 1.0 == 0.0) finalVal.toLong().toString() else finalVal.toString()
                                        onConfirm(strVal)
                                    }
                                    in listOf("+", "-", "×", "÷") -> {
                                        if (displayExpr.isNotEmpty() && displayExpr.last() !in listOf('+', '-', '×', '÷')) {
                                            displayExpr += key
                                        }
                                    }
                                    "." -> {
                                        val lastToken = displayExpr.split(Regex("[-+×÷]")).lastOrNull() ?: ""
                                        if (!lastToken.contains(".")) {
                                            displayExpr += "."
                                        }
                                    }
                                    "00" -> {
                                        if (displayExpr != "0") displayExpr += "00"
                                    }
                                    else -> {
                                        displayExpr = if (displayExpr == "0") key else displayExpr + key
                                    }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (key == "DEL") {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Backspace,
                                    contentDescription = "Borrar",
                                    tint = Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            } else if (key == "USE") {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Usar",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = Color.White
                                    )
                                }
                            } else {
                                Text(
                                    text = key,
                                    fontSize = if (isOperator || key == "=") 22.sp else 18.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isOperator -> Color(0xFF38BDF8)
                                        isClear -> Color(0xFFCBD5E1)
                                        else -> Color.White
                                    }
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
