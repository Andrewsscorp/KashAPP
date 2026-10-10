package com.example.presentation.credit_cards.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue

/**
 * Interactive Installments Picker (1 to 36+ installments).
 * Features Stepper with keyboard input and fast selection chips (1 cuota 0%, 3, 6, 12, 24, 36).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InstallmentsSliderPicker(
    installmentsCount: Int,
    onInstallmentsChanged: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
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
                Column {
                    Text(
                        text = "NÚMERO DE CUOTAS (DIFERIDO)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.8.sp
                    )
                    Text(
                        text = if (installmentsCount == 1) "1 cuota = Sin cobro de interés corriente" else "$installmentsCount cuotas mensuales",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (installmentsCount == 1) Color(0xFF16A34A) else KashBlue
                    )
                }

                // Stepper (- / Editable text / +) with direct keyboard typing
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 2.dp)
                    ) {
                        IconButton(
                            onClick = { if (installmentsCount > 1) onInstallmentsChanged(installmentsCount - 1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("-", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
                        }

                        var isEditingInstallments by remember { mutableStateOf(false) }
                        var tempInstallmentsText by remember { mutableStateOf("") }

                        Box(
                            modifier = Modifier
                                .widthIn(min = 40.dp)
                                .padding(horizontal = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            BasicTextField(
                                value = if (isEditingInstallments) tempInstallmentsText else "$installmentsCount",
                                onValueChange = { input ->
                                    isEditingInstallments = true
                                    val clean = input.filter { it.isDigit() }.take(3)
                                    tempInstallmentsText = clean
                                    val parsed = clean.toIntOrNull()
                                    if (parsed != null && parsed in 1..120) {
                                        onInstallmentsChanged(parsed)
                                    }
                                },
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Number,
                                    imeAction = ImeAction.Done
                                ),
                                keyboardActions = KeyboardActions(
                                    onDone = {
                                        isEditingInstallments = false
                                        val parsed = tempInstallmentsText.toIntOrNull()
                                        if (parsed != null && parsed in 1..120) {
                                            onInstallmentsChanged(parsed)
                                        }
                                    }
                                ),
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center
                                ),
                                decorationBox = { innerTextField ->
                                    Box(
                                        contentAlignment = Alignment.Center,
                                        modifier = Modifier
                                            .background(Color.White, RoundedCornerShape(6.dp))
                                            .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(6.dp))
                                            .padding(horizontal = 6.dp, vertical = 3.dp)
                                    ) {
                                        innerTextField()
                                    }
                                }
                            )
                        }

                        IconButton(
                            onClick = { if (installmentsCount < 120) onInstallmentsChanged(installmentsCount + 1) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Text("+", fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = Color(0xFF334155))
                        }
                    }
                }
            }

            // Fast Selection Chips (1, 2, 3, 6, 12, 18, 24, 36)
            val standardChips = listOf(1, 2, 3, 6, 12, 18, 24, 36)
            val allChips = if (installmentsCount !in standardChips && installmentsCount > 1) {
                (standardChips + installmentsCount).sorted()
            } else standardChips

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                allChips.forEach { count ->
                    val isSelected = installmentsCount == count
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) KashBlue else Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0)),
                        modifier = Modifier.clickable { onInstallmentsChanged(count) }
                    ) {
                        Text(
                            text = if (count == 1) "1 cuota (0%)" else "$count cuotas",
                            fontSize = 11.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFF334155),
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }
    }
}
