package com.example.ui.components.picker

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
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.text.TextNormalizer

/**
 * Universal inline creation bottom sheet for categories and master entities.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EntityCreateBottomSheet(
    title: String,
    entityLabel: String,
    onDismiss: () -> Unit,
    onConfirmCreate: (name: String, colorHex: String) -> Unit,
    isSubmitting: Boolean = false,
    errorMessage: String? = null,
    testTag: String = "entity_create_sheet"
) {
    var nameInput by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0284C7") }
    var localValidation by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val colorPalette = listOf(
        "#0284C7", "#10B981", "#F59E0B", "#EF4444",
        "#8B5CF6", "#EC4899", "#06B6D4", "#64748B"
    )

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
        modifier = Modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 8.dp)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF0F172A)
            )

            Text(
                text = "Crea un nuevo $entityLabel para que esté disponible de inmediato en todas las pantallas y registros contables.",
                fontSize = 13.sp,
                color = Color(0xFF64748B),
                lineHeight = 18.sp
            )

            OutlinedTextField(
                value = nameInput,
                onValueChange = {
                    nameInput = it.take(50)
                    localValidation = null
                },
                label = { Text("Nombre de $entityLabel") },
                placeholder = { Text("Ej. Supermercado, Gimnasio, Mascotas") },
                isError = localValidation != null || errorMessage != null,
                supportingText = {
                    val err = localValidation ?: errorMessage
                    if (err != null) {
                        Text(text = err, color = Color(0xFFEF4444), fontSize = 11.sp)
                    }
                },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("${testTag}_name_input"),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Color(0xFF0284C7),
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                )
            )

            Text(
                text = "Color distintivo",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF334155)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                colorPalette.forEach { hex ->
                    val isSelected = selectedColor.equals(hex, ignoreCase = true)
                    val parsedColor = try {
                        Color(android.graphics.Color.parseColor(hex))
                    } catch (_: Exception) {
                        Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(parsedColor)
                            .border(
                                width = if (isSelected) 3.dp else 1.dp,
                                color = if (isSelected) Color(0xFF0F172A) else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable { selectedColor = hex }
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancelar", color = Color(0xFF64748B), fontWeight = FontWeight.Medium)
                }

                Button(
                    onClick = {
                        val sanitized = TextNormalizer.sanitizeDisplayName(nameInput)
                        if (sanitized.isBlank()) {
                            localValidation = "El nombre no puede estar vacío."
                            return@Button
                        }
                        if (sanitized.length < 2) {
                            localValidation = "El nombre debe tener al menos 2 caracteres."
                            return@Button
                        }
                        onConfirmCreate(sanitized, selectedColor)
                    },
                    enabled = !isSubmitting,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1.5f)
                        .testTag("${testTag}_submit_button")
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                    } else {
                        Text("Guardar y usar", fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}
