package com.example.presentation.budget.widgets

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Shared Category Creation Dialog (identical to CreateExpenseScreen and other modules).
 * High contrast input text, visible navy text color, error validation, and atomic save.
 */
@Composable
fun CreateCustomCategoryDialog(
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, colorHex: String, icon: String) -> Unit
) {
    var newCategoryName by remember { mutableStateOf("") }
    var categoryError by remember { mutableStateOf<String?>(null) }
    val categoryColorHex = "#0284C7"
    val categoryIcon = "category"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Categoría",
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Ingresa el nombre de la categoría a guardar en la base de datos:",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = {
                        newCategoryName = it.take(40)
                        categoryError = null
                    },
                    label = { Text("Nombre de la categoría") },
                    placeholder = { Text("Ej. Mascotas, Gimnasio, Libros", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_new_category_name"),
                    textStyle = TextStyle(
                        color = KashNavy,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KashNavy,
                        unfocusedTextColor = KashNavy,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        cursorColor = KashBlue,
                        focusedLabelColor = KashBlue,
                        unfocusedLabelColor = Color(0xFF64748B),
                        focusedPlaceholderColor = Color(0xFF94A3B8),
                        unfocusedPlaceholderColor = Color(0xFF94A3B8)
                    )
                )
                if (categoryError != null) {
                    Text(
                        text = categoryError ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = newCategoryName.trim()
                    if (trimmed.isBlank()) {
                        categoryError = "El nombre no puede estar vacío."
                        return@Button
                    }
                    onSaveCategory(trimmed, categoryColorHex, categoryIcon)
                },
                colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                modifier = Modifier.testTag("confirm_create_category_button")
            ) {
                Text("Guardar categoría", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color.Gray)
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(16.dp)
    )
}
