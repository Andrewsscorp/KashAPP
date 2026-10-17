package com.example.presentation.account.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CurrencyExchange
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.CurrencyEntity
import com.example.domain.repository.FinancialRepository
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Modal to register a custom Master Data Currency with its Manual Exchange Rate (TRM).
 * Incorporates the proprietary KashApp In-App Numeric Keyboard for exchange rate input.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddCurrencyModal(
    financialRepository: FinancialRepository,
    userId: String = "user_default",
    onCurrencyCreated: (CurrencyEntity) -> Unit,
    onDismiss: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var code by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var symbol by remember { mutableStateOf("$") }
    var rawRate by remember { mutableStateOf("1000") }
    var showInAppKeypad by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSaving by remember { mutableStateOf(false) }

    val formattedRate = remember(rawRate) {
        val num = rawRate.toDoubleOrNull() ?: 0.0
        "$ " + String.format(Locale.GERMANY, "%,.2f", num) + " COP"
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
                    .width(40.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0xFFCBD5E1))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 12.dp)
                .testTag("add_currency_modal")
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
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFFEFF6FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.CurrencyExchange,
                            contentDescription = null,
                            tint = KashBlue,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Nueva divisa personalizada",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color.Gray)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (errorMessage != null) {
                Surface(
                    color = Color(0xFFFEF2F2),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFFECACA)),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = Color(0xFFDC2626),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Spacer(modifier = Modifier.height(12.dp))
            }

            // ISO Code Field
            Text(
                text = "Código ISO (3 - 5 letras)*",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = code,
                onValueChange = {
                    if (it.length <= 5) code = it.uppercase()
                },
                placeholder = { Text("Ej: CAD, GBP, USDT", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_code_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Name Field
            Text(
                text = "Nombre de la divisa*",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text("Ej: Dólar Canadiense", color = Color(0xFF94A3B8)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("currency_name_input"),
                shape = RoundedCornerShape(12.dp),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Symbol Selection / Input
            Text(
                text = "Símbolo*",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("$", "€", "£", "¥", "₩", "₮").forEach { sym ->
                    val isSel = symbol == sym
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSel) Color(0xFFEFF6FF) else Color(0xFFF8FAFC))
                            .border(1.dp, if (isSel) KashBlue else Color(0xFFE2E8F0), RoundedCornerShape(8.dp))
                            .clickable { symbol = sym },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = sym,
                            fontWeight = FontWeight.Bold,
                            color = if (isSel) KashBlue else KashNavy,
                            fontSize = 16.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Manual Exchange Rate (TRM) Input with In-App Keyboard Trigger
            Text(
                text = "Tasa de cambio manual (TRM en COP)*",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = KashNavy
            )
            Spacer(modifier = Modifier.height(6.dp))
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .border(1.dp, Color(0xFFCBD5E1), RoundedCornerShape(12.dp))
                    .clickable { showInAppKeypad = !showInAppKeypad }
                    .testTag("currency_rate_trigger"),
                color = Color(0xFFF8FAFC)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formattedRate,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = KashNavy
                    )
                    Text(
                        text = if (showInAppKeypad) "Ocultar teclado ▲" else "Editar tasa ▼",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashBlue
                    )
                }
            }

            // In-App Numeric Keyboard for Exchange Rate
            if (showInAppKeypad) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF1F5F9)
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val rows = listOf(
                            listOf("1", "2", "3"),
                            listOf("4", "5", "6"),
                            listOf("7", "8", "9"),
                            listOf("C", "0", "DEL")
                        )
                        rows.forEach { rowKeys ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                rowKeys.forEach { key ->
                                    Button(
                                        onClick = {
                                            when (key) {
                                                "C" -> rawRate = "0"
                                                "DEL" -> {
                                                    rawRate = if (rawRate.length > 1) {
                                                        rawRate.dropLast(1)
                                                    } else {
                                                        "0"
                                                    }
                                                }
                                                else -> {
                                                    rawRate = if (rawRate == "0") key else (rawRate + key).take(12)
                                                }
                                            }
                                        },
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp),
                                        shape = RoundedCornerShape(8.dp),
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (key in listOf("C", "DEL")) Color(0xFFE2E8F0) else Color.White
                                        ),
                                        contentPadding = PaddingValues(0.dp)
                                    ) {
                                        if (key == "DEL") {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Borrar",
                                                tint = KashNavy,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        } else {
                                            Text(
                                                text = key,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = KashNavy
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Action Button
            Button(
                onClick = {
                    val cleanCode = code.trim().uppercase()
                    val cleanName = name.trim()
                    val rate = rawRate.toDoubleOrNull() ?: 0.0

                    if (cleanCode.length < 2) {
                        errorMessage = "Por favor ingresa un código de divisa válido (mínimo 2 letras)."
                        return@Button
                    }
                    if (cleanName.isBlank()) {
                        errorMessage = "Por favor ingresa el nombre de la divisa."
                        return@Button
                    }
                    if (rate <= 0.0) {
                        errorMessage = "La tasa de cambio (TRM) debe ser mayor a cero."
                        return@Button
                    }

                    isSaving = true
                    errorMessage = null

                    coroutineScope.launch {
                        val result = financialRepository.createCurrency(
                            userId = userId,
                            code = cleanCode,
                            name = cleanName,
                            symbol = symbol,
                            manualExchangeRate = rate
                        )
                        result.onSuccess { createdCurrency ->
                            isSaving = false
                            onCurrencyCreated(createdCurrency)
                            onDismiss()
                        }.onFailure { ex ->
                            isSaving = false
                            errorMessage = ex.message ?: "Error al registrar la divisa."
                        }
                    }
                },
                enabled = !isSaving,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .testTag("save_currency_button"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
            ) {
                if (isSaving) {
                    CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                } else {
                    Text("Guardar Divisa", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = Color.White)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}
