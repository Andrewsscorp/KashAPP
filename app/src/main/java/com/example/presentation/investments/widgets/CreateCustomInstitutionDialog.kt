package com.example.presentation.investments.widgets

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

@Composable
fun CreateCustomInstitutionDialog(
    onDismiss: () -> Unit,
    onSaveInstitution: (name: String, code: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Nueva Entidad o Plataforma",
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = "Registra una plataforma de inversión (ej. Trii, Binance, Hapi, Tyba):",
                    fontSize = 13.sp,
                    color = Color(0xFF64748B)
                )

                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it.take(50)
                        error = null
                    },
                    label = { Text("Nombre de la entidad") },
                    placeholder = { Text("Ej. Trii, Binance, Interactive Brokers", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_institution_name"),
                    textStyle = TextStyle(color = KashNavy, fontSize = 14.sp, fontWeight = FontWeight.Normal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KashNavy,
                        unfocusedTextColor = KashNavy,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        cursorColor = KashBlue,
                        focusedLabelColor = KashBlue,
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(10).uppercase() },
                    label = { Text("Código / Sigla (opcional)") },
                    placeholder = { Text("Ej. TRII, BNCE, HAPI", color = Color(0xFF94A3B8)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    textStyle = TextStyle(color = KashNavy, fontSize = 14.sp, fontWeight = FontWeight.Normal),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = KashNavy,
                        unfocusedTextColor = KashNavy,
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        cursorColor = KashBlue,
                        focusedLabelColor = KashBlue,
                        unfocusedLabelColor = Color(0xFF64748B)
                    )
                )

                if (error != null) {
                    Text(text = error ?: "", color = Color(0xFFDC2626), fontSize = 12.sp)
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val trimmed = name.trim()
                    if (trimmed.isBlank()) {
                        error = "El nombre no puede estar vacío."
                        return@Button
                    }
                    onSaveInstitution(trimmed, code.trim())
                },
                colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                modifier = Modifier.testTag("confirm_create_institution_button")
            ) {
                Text("Guardar entidad", color = Color.White, fontWeight = FontWeight.Bold)
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
