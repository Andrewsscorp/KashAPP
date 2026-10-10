package com.example.feature.informes.ui.filter

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.informes.domain.model.ComparisonType
import com.example.feature.informes.domain.model.FinancialPeriodFilter
import com.example.feature.informes.domain.model.PeriodPreset
import com.example.feature.informes.domain.model.ReportFrequency

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalFilterBottomSheet(
    currentFilter: FinancialPeriodFilter,
    onApplyFilter: (FinancialPeriodFilter) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedPreset by remember { mutableStateOf(currentFilter.preset) }
    var selectedComparison by remember { mutableStateOf(currentFilter.comparison) }
    var selectedFrequency by remember { mutableStateOf(currentFilter.frequency) }
    var selectedCurrency by remember { mutableStateOf(currentFilter.currency) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color(0xFF0F172A),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState())
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.DateRange,
                        contentDescription = "Filtros",
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Filtros Globales del Informe",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF94A3B8)
                    )
                }
            }

            Text(
                text = "Estos filtros se aplican de forma transversal en todas las secciones y estados financieros.",
                color = Color(0xFF94A3B8),
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 18.dp)
            )

            // Section 1: Period Preset
            Text(
                text = "PERÍODO DE CONSULTA",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                PeriodPreset.values().forEach { preset ->
                    val isSelected = selectedPreset == preset
                    FilterSelectableRow(
                        label = preset.label,
                        isSelected = isSelected,
                        onClick = { selectedPreset = preset }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 2: Comparison Type
            Text(
                text = "COMPARAR CON",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ComparisonType.values().forEach { comp ->
                    val isSelected = selectedComparison == comp
                    FilterSelectableRow(
                        label = comp.label,
                        isSelected = isSelected,
                        onClick = { selectedComparison = comp }
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 3: Frequency
            Text(
                text = "FRECUENCIA DE AGRUPACIÓN",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ReportFrequency.values().forEach { freq ->
                    val isSelected = selectedFrequency == freq
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedFrequency = freq },
                        label = { Text(freq.label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF0284C7),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Section 4: Currency
            Text(
                text = "MONEDA DEL REPORTE",
                color = Color(0xFF64748B),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("COP" to "Peso Colombiano (COP)", "USD" to "Dólar Estadounidense (USD)").forEach { (code, label) ->
                    val isSelected = selectedCurrency == code
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCurrency = code },
                        label = { Text(label, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color(0xFF10B981),
                            selectedLabelColor = Color.White,
                            containerColor = Color(0xFF1E293B),
                            labelColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Apply Button
            Button(
                onClick = {
                    val newFilter = currentFilter.copy(
                        preset = selectedPreset,
                        comparison = selectedComparison,
                        frequency = selectedFrequency,
                        currency = selectedCurrency
                    )
                    onApplyFilter(newFilter)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
            ) {
                Text(
                    text = "Aplicar Filtros Globales",
                    color = Color(0xFF0F172A),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun FilterSelectableRow(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = if (isSelected) Color(0xFF1E293B) else Color(0xFF151F33),
        border = if (isSelected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = if (isSelected) Color.White else Color(0xFF94A3B8),
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF38BDF8),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
