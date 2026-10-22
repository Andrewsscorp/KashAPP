package com.example.presentation.trm.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddManualRateDialog(
    initialFrom: String = "USD",
    initialTo: String = "COP",
    onDismiss: () -> Unit,
    onSave: (from: String, to: String, rate: Double, isActive: Boolean, validFrom: String, validTo: String, note: String) -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("yyyy-MM-dd", Locale.US) }
    val today = remember { dateFormat.format(Date()) }
    val nextMonth = remember {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, 1)
        dateFormat.format(cal.time)
    }

    var fromCurrency by remember { mutableStateOf(initialFrom) }
    var toCurrency by remember { mutableStateOf(initialTo) }
    var rateInput by remember { mutableStateOf("") }
    var validFromDate by remember { mutableStateOf(today) }
    var validToDate by remember { mutableStateOf(nextMonth) }
    var noteInput by remember { mutableStateOf("Ajuste manual · Usuario") }
    var isActive by remember { mutableStateOf(true) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 6.dp)
                    .width(42.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 22.dp, vertical = 8.dp)
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Agregar Tasa Manual",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Tendrá prioridad sobre la tasa automática",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFF1F5F9))
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Par de monedas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "DE (Origen)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = fromCurrency,
                        onValueChange = { fromCurrency = it.uppercase().take(3) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "A (Destino)",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = toCurrency,
                        onValueChange = { toCurrency = it.uppercase().take(3) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Valor de la tasa
            Text(
                text = "VALOR DE LA TASA (1 $fromCurrency = ? $toCurrency)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = rateInput,
                onValueChange = { input ->
                    val filtered = input.filter { it.isDigit() || it == '.' || it == ',' }.replace(',', '.')
                    if (filtered.count { it == '.' } <= 1) {
                        rateInput = filtered
                    }
                },
                placeholder = { Text("Ej. 4050.00", color = Color(0xFF94A3B8), fontSize = 13.sp) },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedBorderColor = KashBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = TextStyle(fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color(0xFF0F172A))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Vigencia Fechas
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vigencia Desde",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = validFromDate,
                        onValueChange = { validFromDate = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A))
                    )
                }

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Vigencia Hasta",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF475569)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = validToDate,
                        onValueChange = { validToDate = it },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.DateRange, null, modifier = Modifier.size(16.dp), tint = Color(0xFF64748B)) },
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color(0xFF0F172A),
                            unfocusedTextColor = Color(0xFF0F172A),
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1)
                        ),
                        textStyle = TextStyle(fontSize = 12.sp, color = Color(0xFF0F172A))
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Nota / Motivo
            Text(
                text = "MOTIVO / NOTA",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF475569)
            )
            Spacer(modifier = Modifier.height(4.dp))
            OutlinedTextField(
                value = noteInput,
                onValueChange = { noteInput = it },
                singleLine = true,
                placeholder = { Text("Ej. Tasa negociada con mesa de dinero", color = Color(0xFF94A3B8), fontSize = 12.sp) },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color(0xFF0F172A),
                    unfocusedTextColor = Color(0xFF0F172A),
                    focusedBorderColor = KashBlue,
                    unfocusedBorderColor = Color(0xFFCBD5E1)
                ),
                textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A))
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Activar inmediatamente toggle
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Activar de inmediato", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                        Text("Reemplaza la tasa de Frankfurter", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    IosStyleSwitch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Botón Guardar
            val isValid = rateInput.toDoubleOrNull() != null && fromCurrency.isNotBlank() && toCurrency.isNotBlank()
            Button(
                onClick = {
                    val rate = rateInput.toDoubleOrNull() ?: return@Button
                    onSave(fromCurrency.trim(), toCurrency.trim(), rate, isActive, validFromDate, validToDate, noteInput.trim())
                    onDismiss()
                },
                enabled = isValid,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = KashBlue,
                    disabledContainerColor = Color(0xFFCBD5E1)
                )
            ) {
                Text(
                    text = "Guardar Tasa Manual",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(18.dp))
        }
    }
}
