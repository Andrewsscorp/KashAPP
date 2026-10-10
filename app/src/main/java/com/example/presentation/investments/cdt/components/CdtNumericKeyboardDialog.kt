package com.example.presentation.investments.cdt.components

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Backspace
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

enum class CdtKeypadTarget {
    AMOUNT,
    RATE
}

/**
 * Proprietary KashApp In-App Numeric Keyboard for CDT Inputs.
 * Injects $ and % masks visually while maintaining clean numeric state.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CdtNumericKeyboardDialog(
    target: CdtKeypadTarget,
    currentRawValue: String,
    onValueChange: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val title = when (target) {
        CdtKeypadTarget.AMOUNT -> "Ingresar monto inicial"
        CdtKeypadTarget.RATE -> "Ingresar tasa de interés"
    }

    val displayMaskedValue = when (target) {
        CdtKeypadTarget.AMOUNT -> {
            val num = currentRawValue.toDoubleOrNull() ?: 0.0
            "$ " + String.format(Locale.GERMANY, "%,.0f", num)
        }
        CdtKeypadTarget.RATE -> {
            "$currentRawValue %"
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 6.dp)
                    .width(36.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp)
                .testTag("cdt_keypad_sheet")
        ) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
            Spacer(modifier = Modifier.height(10.dp))

            // Display Box with mask
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = displayMaskedValue,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = KashBlue
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Keypad grid
            val rows = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf(if (target == CdtKeypadTarget.RATE) "," else "00", "0", "BACKSPACE")
            )

            rows.forEach { rowKeys ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    rowKeys.forEach { key ->
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    when (target) {
                                        CdtKeypadTarget.AMOUNT -> {
                                            when (key) {
                                                "BACKSPACE" -> {
                                                    val newVal = if (currentRawValue.length > 1) currentRawValue.dropLast(1) else "0"
                                                    onValueChange(newVal)
                                                }
                                                "00" -> {
                                                    if (currentRawValue != "0" && currentRawValue.isNotBlank() && currentRawValue.length < 12) {
                                                        onValueChange(currentRawValue + "00")
                                                    }
                                                }
                                                else -> {
                                                    val newVal = if (currentRawValue == "0" || currentRawValue.isBlank()) key
                                                    else if (currentRawValue.length < 12) currentRawValue + key
                                                    else currentRawValue
                                                    onValueChange(newVal)
                                                }
                                            }
                                        }
                                        CdtKeypadTarget.RATE -> {
                                            when (key) {
                                                "BACKSPACE" -> {
                                                    val newVal = if (currentRawValue.length > 1) currentRawValue.dropLast(1) else "0"
                                                    onValueChange(newVal)
                                                }
                                                "," -> {
                                                    if (!currentRawValue.contains(",") && currentRawValue.isNotBlank()) {
                                                        onValueChange("$currentRawValue,")
                                                    }
                                                }
                                                else -> {
                                                    val newVal = if (currentRawValue == "0" || currentRawValue.isBlank()) key
                                                    else if (currentRawValue.length < 6) currentRawValue + key
                                                    else currentRawValue
                                                    onValueChange(newVal)
                                                }
                                            }
                                        }
                                    }
                                }
                                .testTag("cdt_keypad_$key"),
                            color = if (key == "BACKSPACE") Color(0xFFF1F5F9) else Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                        ) {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                if (key == "BACKSPACE") {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.Backspace,
                                        contentDescription = "Borrar",
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(20.dp)
                                    )
                                } else {
                                    Text(
                                        text = key,
                                        fontSize = 18.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            Spacer(modifier = Modifier.height(10.dp))

            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("cdt_keypad_done_btn"),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
            ) {
                Text(
                    text = "Aceptar",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
            Spacer(modifier = Modifier.height(14.dp))
        }
    }
}
