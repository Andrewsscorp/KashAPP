package com.example.presentation.fixedassets.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity

data class AdvancedDepreciationSettings(
    val method: String = "Línea recta",
    val depreciableBase: String = "Valor de adquisición",
    val startConvention: String = "Desde fecha de adquisición",
    val frequency: String = "Mensual",
    val rounding: String = "Al peso",
    val impairmentTracking: Boolean = true,
    val revaluationEnabled: Boolean = false,
    val allowLifeChange: Boolean = false,
    val allowResidualChange: Boolean = false,
    val allowMethodChange: Boolean = false
)

fun FixedAssetEntity.toAdvancedSettings(): AdvancedDepreciationSettings {
    val methodLabel = when {
        depreciationMethod.contains("DIGITS", ignoreCase = true) || depreciationMethod.contains("dígitos", ignoreCase = true) || depreciationMethod.contains("digitos", ignoreCase = true) -> "Suma de dígitos"
        depreciationMethod.contains("DECLINING", ignoreCase = true) || depreciationMethod.contains("decrecientes", ignoreCase = true) -> "Saldos decrecientes"
        else -> "Línea recta"
    }
    return AdvancedDepreciationSettings(
        method = methodLabel,
        depreciableBase = depreciableBase,
        startConvention = startConvention,
        frequency = frequency,
        rounding = rounding,
        impairmentTracking = impairmentTracking,
        revaluationEnabled = revaluationEnabled,
        allowLifeChange = allowLifeChange,
        allowResidualChange = allowResidualChange,
        allowMethodChange = allowMethodChange
    )
}

/**
 * iPhone-style dark modal for advanced depreciation settings, matching Image 1.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun AdvancedDepreciationOptionsIPhoneSheet(
    asset: FixedAssetEntity,
    initialSettings: AdvancedDepreciationSettings = asset.toAdvancedSettings(),
    onSaveSettings: (AdvancedDepreciationSettings) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    var settings by remember(asset) { mutableStateOf(initialSettings) }

    var isCalculationExpanded by remember { mutableStateOf(true) }
    var isAccountingExpanded by remember { mutableStateOf(true) }
    var isSubsequentChangesExpanded by remember { mutableStateOf(true) }

    val bgDark = Color(0xFF071426)
    val cardBg = Color(0xFF0C1F38)
    val cardBorder = Color(0xFF132F52)
    val primaryBlue = Color(0xFF007AFF)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    IPhoneActionModal(
        title = "Opciones avanzadas",
        subtitle = "Personaliza el cálculo y tratamiento de la depreciación.",
        icon = Icons.Default.Settings,
        iconTint = primaryBlue,
        containerColor = bgDark,
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // SECTION 1: CÁLCULO
            AccordionCard(
                title = "Cálculo",
                subtitle = "Define cómo se calculará la depreciación del activo.",
                icon = Icons.Default.Tune,
                isExpanded = isCalculationExpanded,
                onToggle = { isCalculationExpanded = !isCalculationExpanded },
                cardBg = cardBg,
                cardBorder = cardBorder,
                iconColor = primaryBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelectorField(
                                label = "Método de depreciación",
                                selectedValue = settings.method,
                                options = listOf("Línea recta", "Suma de dígitos", "Saldos decrecientes"),
                                onSelect = { settings = settings.copy(method = it) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelectorField(
                                label = "Base depreciable",
                                selectedValue = settings.depreciableBase,
                                options = listOf("Valor de adquisición", "Valor neto ajustado", "Valor de reposición"),
                                onSelect = { settings = settings.copy(depreciableBase = it) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelectorField(
                                label = "Convención de inicio",
                                selectedValue = settings.startConvention,
                                options = listOf("Desde fecha de adquisición", "Primer día del mes", "Mes siguiente"),
                                onSelect = { settings = settings.copy(startConvention = it) }
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            DropdownSelectorField(
                                label = "Frecuencia",
                                selectedValue = settings.frequency,
                                options = listOf("Mensual", "Trimestral", "Semestral", "Anual"),
                                onSelect = { settings = settings.copy(frequency = it) }
                            )
                        }
                    }

                    DropdownSelectorField(
                        label = "Redondeo",
                        selectedValue = settings.rounding,
                        options = listOf("Al peso", "A la centena", "Al millar", "Sin redondeo"),
                        onSelect = { settings = settings.copy(rounding = it) }
                    )
                }
            }

            // SECTION 2: CONTABLE
            AccordionCard(
                title = "Contable",
                subtitle = "Define el tratamiento contable y de valoración.",
                icon = Icons.Default.Description,
                isExpanded = isAccountingExpanded,
                onToggle = { isAccountingExpanded = !isAccountingExpanded },
                cardBg = cardBg,
                cardBorder = cardBorder,
                iconColor = primaryBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OptionSwitchRow(
                        title = "Seguimiento de deterioro",
                        description = "Permite registrar y controlar el deterioro del activo.",
                        checked = settings.impairmentTracking,
                        onCheckedChange = { settings = settings.copy(impairmentTracking = it) }
                    )

                    OptionSwitchRow(
                        title = "Revaluación",
                        description = "Permite revaluaciones del activo en el tiempo.",
                        checked = settings.revaluationEnabled,
                        onCheckedChange = { settings = settings.copy(revaluationEnabled = it) }
                    )
                }
            }

            // SECTION 3: CAMBIOS POSTERIORES
            AccordionCard(
                title = "Cambios posteriores",
                subtitle = "Permite modificar parámetros después de la creación.",
                icon = Icons.Default.TrackChanges,
                isExpanded = isSubsequentChangesExpanded,
                onToggle = { isSubsequentChangesExpanded = !isSubsequentChangesExpanded },
                cardBg = cardBg,
                cardBorder = cardBorder,
                iconColor = primaryBlue
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    OptionSwitchRow(
                        title = "Permitir cambio de vida útil",
                        description = "Podrás ajustar la vida útil del activo en el futuro.",
                        checked = settings.allowLifeChange,
                        onCheckedChange = { settings = settings.copy(allowLifeChange = it) }
                    )

                    OptionSwitchRow(
                        title = "Permitir cambio de valor residual",
                        description = "Podrás ajustar el valor residual del activo en el futuro.",
                        checked = settings.allowResidualChange,
                        onCheckedChange = { settings = settings.copy(allowResidualChange = it) }
                    )

                    OptionSwitchRow(
                        title = "Permitir cambio de método",
                        description = "Podrás cambiar el método de depreciación del activo en el futuro.",
                        checked = settings.allowMethodChange,
                        onCheckedChange = { settings = settings.copy(allowMethodChange = it) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Botón Guardar cambios
            Button(
                onClick = {
                    onSaveSettings(settings)
                    onDismiss()
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text(
                    text = "Guardar cambios",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
private fun AccordionCard(
    title: String,
    subtitle: String,
    icon: ImageVector,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    cardBg: Color,
    cardBorder: Color,
    iconColor: Color,
    content: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = cardBg,
        border = BorderStroke(1.dp, cardBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color(0xFF132D50)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = title,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = subtitle,
                            fontSize = 11.5.sp,
                            color = Color(0xFF8EA5C4)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF8EA5C4)
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    content()
                }
            }
        }
    }
}

@Composable
private fun DropdownSelectorField(
    label: String,
    selectedValue: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = label,
                fontSize = 11.5.sp,
                color = Color(0xFFCBD5E1),
                fontWeight = FontWeight.Medium
            )
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = Color(0xFF64748B),
                modifier = Modifier.size(12.dp)
            )
        }

        Box {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF081628),
                border = BorderStroke(1.dp, Color(0xFF18385E)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = true }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = selectedValue,
                        fontSize = 12.5.sp,
                        color = Color.White,
                        fontWeight = FontWeight.SemiBold
                    )
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = null,
                        tint = Color(0xFF8EA5C4),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = Modifier.background(Color(0xFF0C1F38))
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = option,
                                color = if (option == selectedValue) Color(0xFF38BDF8) else Color.White,
                                fontSize = 13.sp,
                                fontWeight = if (option == selectedValue) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        onClick = {
                            onSelect(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun OptionSwitchRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 13.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color.White
            )
            Text(
                text = description,
                fontSize = 11.sp,
                color = Color(0xFF8EA5C4)
            )
        }

        Spacer(modifier = Modifier.width(10.dp))

        IosStyleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFF007AFF),
                uncheckedThumbColor = Color(0xFF94A3B8),
                uncheckedTrackColor = Color(0xFF1E293B)
            )
        )
    }
}
