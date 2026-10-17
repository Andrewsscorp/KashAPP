package com.example.presentation.fixedassets.create.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EditCalendar
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import java.time.LocalDate

private val MONTH_NAMES = listOf(
    "Enero", "Febrero", "Marzo", "Abril", "Mayo", "Junio",
    "Julio", "Agosto", "Septiembre", "Octubre", "Noviembre", "Diciembre"
)

private val MONTH_SHORT_NAMES = listOf(
    "Ene", "Feb", "Mar", "Abr", "May", "Jun",
    "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
)

/**
 * Modern, responsive date picker field with native calendar dialog.
 * Supports quick presets (Hoy, Ayer, Hace 1 mes), visual wheel selection, and direct manual input.
 */
@Composable
fun IPhoneAdaptiveDatePickerField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    placeholder: String = "AAAA-MM-DD",
    title: String = label,
    isDarkTheme: Boolean = true
) {
    var showPickerDialog by remember { mutableStateOf(false) }

    val parsedDate = remember(value) {
        try {
            LocalDate.parse(value.take(10))
        } catch (_: Exception) {
            LocalDate.now()
        }
    }

    val labelColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val focusedBorder = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val unfocusedBorder = if (isDarkTheme) Color(0xFF1E3A8A).copy(alpha = 0.5f) else Color(0xFFCBD5E1)
    val textColor = if (isDarkTheme) Color.White else Color(0xFF0F172A)
    val containerColor = if (isDarkTheme) Color(0xFF071221) else Color.White
    val iconColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)
    val cursorColor = if (isDarkTheme) Color(0xFF38BDF8) else Color(0xFF0284C7)

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = label,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            color = labelColor
        )

        OutlinedTextField(
            value = value,
            onValueChange = { input ->
                onValueChange(input)
            },
            placeholder = { Text(placeholder, color = Color(0xFF64748B)) },
            singleLine = true,
            trailingIcon = {
                IconButton(
                    onClick = { showPickerDialog = true },
                    modifier = Modifier.size(40.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarMonth,
                        contentDescription = "Abrir calendario",
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = focusedBorder,
                unfocusedBorderColor = unfocusedBorder,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor,
                focusedContainerColor = containerColor,
                unfocusedContainerColor = containerColor,
                cursorColor = cursorColor
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }

    if (showPickerDialog) {
        AdaptiveWheelDatePickerDialog(
            initialDate = parsedDate,
            onDismiss = { showPickerDialog = false },
            onDateSelected = { newDate ->
                onValueChange(newDate.toString())
                showPickerDialog = false
            },
            title = title
        )
    }
}

/**
 * Backward compatibility alias
 */
@Composable
fun IPhoneWheelDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    title: String = "Seleccionar Fecha"
) {
    AdaptiveWheelDatePickerDialog(initialDate, onDismiss, onDateSelected, title)
}

@Composable
fun AdaptiveWheelDatePickerDialog(
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onDateSelected: (LocalDate) -> Unit,
    title: String = "Seleccionar Fecha"
) {
    var selectedYear by remember { mutableIntStateOf(initialDate.year) }
    var selectedMonth by remember { mutableIntStateOf(initialDate.monthValue) }
    var selectedDay by remember { mutableIntStateOf(initialDate.dayOfMonth) }

    val daysInMonth = remember(selectedYear, selectedMonth) {
        try {
            LocalDate.of(selectedYear, selectedMonth, 1).lengthOfMonth()
        } catch (_: Exception) {
            30
        }
    }

    LaunchedEffect(daysInMonth) {
        if (selectedDay > daysInMonth) {
            selectedDay = daysInMonth
        }
    }

    var isManualInputMode by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 4.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0C1F38)),
            border = BorderStroke(1.dp, Color(0xFF2563EB).copy(alpha = 0.4f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.CalendarMonth,
                                contentDescription = null,
                                tint = Color(0xFF38BDF8),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = title,
                                color = Color.White,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isManualInputMode) "Entrada manual de dígitos" else "Selección visual organizada",
                                color = Color(0xFF38BDF8),
                                fontSize = 11.5.sp
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier
                            .size(32.dp)
                            .background(Color(0xFF161F30), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Presets rápidos organizados: [Hoy] [Ayer] [Hace 1 mes] [Inicio de año]
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = LocalDate.now()
                    DateShortcutChip(
                        label = "Hoy",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedYear = now.year
                            selectedMonth = now.monthValue
                            selectedDay = now.dayOfMonth
                        }
                    )
                    DateShortcutChip(
                        label = "Ayer",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val yesterday = now.minusDays(1)
                            selectedYear = yesterday.year
                            selectedMonth = yesterday.monthValue
                            selectedDay = yesterday.dayOfMonth
                        }
                    )
                    DateShortcutChip(
                        label = "-1 Mes",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            val lastMonth = now.minusMonths(1)
                            selectedYear = lastMonth.year
                            selectedMonth = lastMonth.monthValue
                            selectedDay = lastMonth.dayOfMonth.coerceAtMost(lastMonth.lengthOfMonth())
                        }
                    )
                    DateShortcutChip(
                        label = "Ene 1",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            selectedYear = now.year
                            selectedMonth = 1
                            selectedDay = 1
                        }
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Date Preview Card
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFF071221),
                    border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "FECHA ELEGIDA",
                                color = Color(0xFF64748B),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = String.format("%02d de %s, %04d", selectedDay, MONTH_NAMES[selectedMonth - 1], selectedYear),
                                color = Color(0xFF38BDF8),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        IconButton(
                            onClick = { isManualInputMode = !isManualInputMode },
                            modifier = Modifier
                                .size(34.dp)
                                .background(if (isManualInputMode) Color(0xFF2563EB) else Color(0xFF16253D), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.EditCalendar,
                                contentDescription = "Alternar modo manual",
                                tint = Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                if (isManualInputMode) {
                    // Manual numeric input
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = "Escribe directamente los números:",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8EA5C4),
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedTextField(
                                value = selectedDay.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { d -> selectedDay = d.coerceIn(1, daysInMonth) }
                                },
                                label = { Text("Día") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF1E3A8A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            )

                            OutlinedTextField(
                                value = selectedMonth.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { m -> selectedMonth = m.coerceIn(1, 12) }
                                },
                                label = { Text("Mes (1-12)") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF1E3A8A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.2f)
                            )

                            OutlinedTextField(
                                value = selectedYear.toString(),
                                onValueChange = {
                                    it.toIntOrNull()?.let { y -> if (y in 1950..2050) selectedYear = y }
                                },
                                label = { Text("Año") },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Color(0xFF38BDF8),
                                    unfocusedBorderColor = Color(0xFF1E3A8A),
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White
                                ),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1.3f)
                            )
                        }
                    }
                } else {
                    // Ordered 3 Columns: Día, Mes, Año
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Día
                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "DÍA",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            WheelColumn(
                                items = (1..daysInMonth).map { String.format("%02d", it) },
                                selectedIndex = (selectedDay - 1).coerceAtLeast(0),
                                onSelect = { selectedDay = it + 1 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Mes
                        Column(
                            modifier = Modifier.weight(1.3f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "MES",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            WheelColumn(
                                items = MONTH_SHORT_NAMES,
                                selectedIndex = (selectedMonth - 1).coerceAtLeast(0),
                                onSelect = { selectedMonth = it + 1 },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        // Año
                        val years = remember { (1995..2035).toList() }
                        Column(
                            modifier = Modifier.weight(1.2f),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "AÑO",
                                color = Color(0xFF64748B),
                                fontSize = 10.5.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 4.dp)
                            )
                            val yearIndex = years.indexOf(selectedYear).coerceAtLeast(0)
                            WheelColumn(
                                items = years.map { it.toString() },
                                selectedIndex = yearIndex,
                                onSelect = { selectedYear = years[it] },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Actions
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF94A3B8)),
                        border = BorderStroke(1.dp, Color(0xFF334155)),
                        modifier = Modifier
                            .weight(1f)
                            .height(46.dp)
                    ) {
                        Text("Cancelar", fontSize = 13.sp, fontWeight = FontWeight.Medium)
                    }

                    Button(
                        onClick = {
                            val safeDay = selectedDay.coerceIn(1, daysInMonth)
                            val finalDate = LocalDate.of(selectedYear, selectedMonth, safeDay)
                            onDateSelected(finalDate)
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2563EB)),
                        modifier = Modifier
                            .weight(1.3f)
                            .height(46.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Confirmar", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun DateShortcutChip(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF10223D),
        border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.6f)),
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier.padding(vertical = 6.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = label,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )
        }
    }
}

@Composable
private fun WheelColumn(
    items: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    LaunchedEffect(selectedIndex) {
        if (selectedIndex in items.indices) {
            listState.animateScrollToItem(selectedIndex)
        }
    }

    Box(
        modifier = modifier
            .background(Color(0xFF071221), RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            items(items.size) { index ->
                val isSelected = index == selectedIndex
                Box(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (isSelected) Color(0xFF2563EB).copy(alpha = 0.35f) else Color.Transparent
                        )
                        .clickable { onSelect(index) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = items[index],
                        color = if (isSelected) Color(0xFF38BDF8) else Color(0xFF94A3B8),
                        fontSize = if (isSelected) 14.sp else 12.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }
    }
}
