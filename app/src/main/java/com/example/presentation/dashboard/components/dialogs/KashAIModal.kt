package com.example.presentation.dashboard.components.dialogs

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
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
import androidx.compose.ui.window.Dialog
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashCyan
import com.example.ui.theme.KashNavy

data class KashAIMessage(
    val sender: String, // "user" or "kashai"
    val text: String
)

@Composable
fun KashAIModal(
    initialPrompt: String,
    onDismiss: () -> Unit
) {
    var inputQuery by remember { mutableStateOf(initialPrompt) }
    val messages = remember {
        mutableStateListOf(
            KashAIMessage(
                sender = "kashai",
                text = "¡Hola Andrés! Soy KashAI, tu copiloto financiero. Tu patrimonio actual es de $ 48.750.320 y tu tasa de ahorro este mes es del 26%. ¿Sobre qué área te gustaría recibir recomendaciones?"
            )
        )
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .height(520.dp)
                .testTag("kashai_dialog_surface"),
            shape = RoundedCornerShape(24.dp),
            color = Color.White,
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(KashBlue.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AutoAwesome,
                                contentDescription = "KashAI",
                                tint = KashBlue,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "KashAI Copiloto Financiero",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Text(
                                text = "Contexto numérico activo • Cifrado E2E",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
                        Icon(imageVector = Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Chat Messages List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(messages) { msg ->
                        val isUser = msg.sender == "user"
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(0.85f)
                                    .clip(
                                        RoundedCornerShape(
                                            topStart = 14.dp,
                                            topEnd = 14.dp,
                                            bottomStart = if (isUser) 14.dp else 2.dp,
                                            bottomEnd = if (isUser) 2.dp else 14.dp
                                        )
                                    )
                                    .background(if (isUser) KashBlue else Color(0xFFF1F5F9))
                                    .padding(12.dp)
                            ) {
                                Text(
                                    text = msg.text,
                                    fontSize = 12.sp,
                                    color = if (isUser) Color.White else KashNavy,
                                    lineHeight = 17.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Input field
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(46.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF8FAFC))
                        .padding(horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    BasicTextField(
                        value = inputQuery,
                        onValueChange = { inputQuery = it },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("kashai_modal_query_input"),
                        textStyle = androidx.compose.ui.text.TextStyle(
                            color = com.example.ui.theme.KashNavy,
                            fontSize = 14.sp
                        ),
                        cursorBrush = androidx.compose.ui.graphics.SolidColor(com.example.ui.theme.KashBlue),
                        singleLine = true,
                        decorationBox = { inner ->
                            if (inputQuery.isEmpty()) {
                                Text("Pregunta a KashAI...", fontSize = 12.sp, color = Color.Gray)
                            }
                            inner()
                        }
                    )

                    IconButton(
                        onClick = {
                            if (inputQuery.isNotBlank()) {
                                val userQ = inputQuery
                                messages.add(KashAIMessage("user", userQ))
                                inputQuery = ""

                                val aiResponse = when {
                                    userQ.contains("ahorro", ignoreCase = true) ->
                                        "Tu ahorro este mes es de $ 1.350.000 (+15,7%). Si automatizas el 10% adicional a tu fondo CDT, alcanzarás tu meta 'Laptop Gaming' en 2 semanas."
                                    userQ.contains("deuda", ignoreCase = true) || userQ.contains("tarjeta", ignoreCase = true) ->
                                        "Tu tarjeta Nu tiene un saldo de $ 1.250.000 con vencimiento el 25 de Octubre. Tu cupo utilizado es del 25%, manteniéndote en un rango crediticio excelente."
                                    else ->
                                        "Basado en tus 5 transacciones recientes y salud financiera de 78/100, estás cumpliendo el 72% de tu presupuesto. Tienes $ 8.450.000 de liquidez disponible."
                                }
                                messages.add(KashAIMessage("kashai", aiResponse))
                            }
                        },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(imageVector = Icons.AutoMirrored.Filled.Send, contentDescription = "Enviar", tint = KashBlue, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }
    }
}
