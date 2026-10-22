package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

@Composable
fun SettingsAppearanceSubScreen(
    currentTheme: String,
    privacyMode: Boolean,
    onSelectTheme: (String) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontScale by remember { mutableStateOf("Normal") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Tema de la Interfaz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Oscuro").forEach { (code, label) ->
                        val isSelected = currentTheme.equals(code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTheme(code) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KashBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Privacidad", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Ocultar saldos con asteriscos (••••) en todo el tablero", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    IosStyleSwitch(
                        checked = privacyMode,
                        onCheckedChange = { onTogglePrivacyMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("Escala de Tipografía", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = KashNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Compacto", "Normal", "Grande").forEach { scale ->
                        val isSelected = fontScale == scale
                        FilterChip(
                            selected = isSelected,
                            onClick = { fontScale = scale },
                            label = { Text(scale) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KashBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }
            }
        }
    }
}
