package com.example.presentation.goals.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TrackChanges
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.goals.state.GoalFormState
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

private val goalPalette = listOf(
    "#6D28D9", // Deep Purple
    "#8B5CF6", // Violet (Active in design)
    "#A78BFA", // Lilac
    "#C4B5FD", // Light Violet
    "#F43F5E", // Rose / Pink
    "#F97316", // Orange
    "#FBBF24", // Amber
    "#34D399", // Light Green
    "#06B6D4", // Cyan
    "#38BDF8"  // Sky Blue
)

@Composable
fun GoalBasicInfoCard(
    state: GoalFormState,
    onToggleExpand: () -> Unit,
    onNameChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onDescriptionChanged: (String) -> Unit,
    onColorChanged: (String) -> Unit,
    onIconChanged: (String) -> Unit,
    onOpenKeyboard: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var categoryMenuExpanded by remember { mutableStateOf(false) }
    var iconMenuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("goal_basic_info_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Target icon inside green circular container + Title + Chevron
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFD1FAE5)), // Light emerald/green
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.TrackChanges,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = "Información básica",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "Dale un nombre, elige una categoría y personaliza tu objetivo.",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (state.isBasicInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(visible = state.isBasicInfoExpanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Row 1: Nombre del objetivo (with 18/50 counter) + Categoría (Dropdown)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Nombre del objetivo
                        Column(modifier = Modifier.weight(1.3f)) {
                            Text(
                                text = "Nombre del objetivo *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            OutlinedTextField(
                                value = state.name,
                                onValueChange = onNameChanged,
                                placeholder = { Text("Ej. Maestría en Auditoría", fontSize = 13.sp) },
                                singleLine = true,
                                trailingIcon = {
                                    Text(
                                        text = "${state.nameLength}/50",
                                        fontSize = 11.sp,
                                        color = if (state.nameLength >= 50) Color(0xFFEF4444) else Color(0xFF94A3B8),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("goal_name_input"),
                                shape = RoundedCornerShape(10.dp),
                                textStyle = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KashNavy),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1)
                                )
                            )
                        }

                        // Categoría
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Categoría *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Box(modifier = Modifier.fillMaxWidth()) {
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(52.dp)
                                        .clickable { categoryMenuExpanded = true }
                                        .testTag("goal_category_picker"),
                                    shape = RoundedCornerShape(10.dp),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                    color = Color.White
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(26.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFEDE9FE)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.School,
                                                    contentDescription = null,
                                                    tint = Color(0xFF8B5CF6),
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                            Text(
                                                text = state.categoryName,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = KashNavy
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = categoryMenuExpanded,
                                    onDismissRequest = { categoryMenuExpanded = false }
                                ) {
                                    listOf(
                                        "Educación" to Icons.Default.School,
                                        "Vivienda" to Icons.Default.Home,
                                        "Vehículo" to Icons.Default.DirectionsCar,
                                        "Ahorro" to Icons.Default.Savings,
                                        "Viajes" to Icons.Default.Flight,
                                        "Inversión" to Icons.Default.TrendingUp
                                    ).forEach { (catName, iconVector) ->
                                        DropdownMenuItem(
                                            leadingIcon = {
                                                Icon(iconVector, contentDescription = null, tint = KashBlue, modifier = Modifier.size(16.dp))
                                            },
                                            text = { Text(catName, fontSize = 12.sp) },
                                            onClick = {
                                                onCategoryChanged(catName)
                                                categoryMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // Monto de la meta (DIRECTAMENTE DEBAJO DEL NOMBRE PARA MÁXIMA VELOCIDAD)
                    if (onOpenKeyboard != null) {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Text(
                                text = "Meta de ahorro ($) *",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { onOpenKeyboard("TARGET_AMOUNT") }
                                    .testTag("goal_target_amount_fast_input"),
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                color = Color(0xFFF8FAFC)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = "$ ${String.format(java.util.Locale.GERMANY, "%,.0f", state.targetAmount)}",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashNavy
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Editar meta",
                                        tint = KashBlue,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Row 2: Descripción (opcional) with 86/200 counter
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Descripción (opcional)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        OutlinedTextField(
                            value = state.description,
                            onValueChange = onDescriptionChanged,
                            placeholder = { Text("Escribe notas o el propósito del ahorro...", fontSize = 12.sp) },
                            minLines = 2,
                            maxLines = 3,
                            trailingIcon = {
                                Box(
                                    modifier = Modifier
                                        .padding(bottom = 6.dp, end = 8.dp)
                                ) {
                                    Text(
                                        text = "${state.descriptionLength}/200",
                                        fontSize = 11.sp,
                                        color = if (state.descriptionLength >= 200) Color(0xFFEF4444) else Color(0xFF94A3B8)
                                    )
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("goal_description_input"),
                            shape = RoundedCornerShape(10.dp),
                            textStyle = TextStyle(fontSize = 12.sp, color = KashNavy),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = KashBlue,
                                unfocusedBorderColor = Color(0xFFCBD5E1)
                            )
                        )
                    }

                    // Row 3: Icono y color
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Icono y color",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            // Icon selector button with graduation cap and arrow down
                            Box {
                                Surface(
                                    modifier = Modifier
                                        .height(40.dp)
                                        .clickable { iconMenuExpanded = true }
                                        .testTag("goal_icon_selector"),
                                    shape = RoundedCornerShape(10.dp),
                                    color = Color(0xFFF8FAFC),
                                    border = BorderStroke(1.dp, Color(0xFFCBD5E1))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(Color(android.graphics.Color.parseColor(state.selectedColorHex))),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = when (state.selectedIconName) {
                                                    "school" -> Icons.Default.School
                                                    "home" -> Icons.Default.Home
                                                    "car" -> Icons.Default.DirectionsCar
                                                    "savings" -> Icons.Default.Savings
                                                    "flight" -> Icons.Default.Flight
                                                    else -> Icons.Default.School
                                                },
                                                contentDescription = null,
                                                tint = Color.White,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }
                                        Icon(
                                            imageVector = Icons.Default.KeyboardArrowDown,
                                            contentDescription = null,
                                            tint = Color(0xFF64748B),
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                }

                                DropdownMenu(
                                    expanded = iconMenuExpanded,
                                    onDismissRequest = { iconMenuExpanded = false }
                                ) {
                                    listOf(
                                        "school" to ("Educación" to Icons.Default.School),
                                        "home" to ("Vivienda" to Icons.Default.Home),
                                        "car" to ("Vehículo" to Icons.Default.DirectionsCar),
                                        "savings" to ("Ahorro" to Icons.Default.Savings),
                                        "flight" to ("Viajes" to Icons.Default.Flight)
                                    ).forEach { (iconKey, pair) ->
                                        DropdownMenuItem(
                                            leadingIcon = { Icon(pair.second, contentDescription = null, tint = KashBlue) },
                                            text = { Text(pair.first, fontSize = 12.sp) },
                                            onClick = {
                                                onIconChanged(iconKey)
                                                iconMenuExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // Color selection circles palette
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                items(goalPalette) { hex ->
                                    val isSelected = state.selectedColorHex.equals(hex, ignoreCase = true)
                                    val colorObj = Color(android.graphics.Color.parseColor(hex))
                                    Box(
                                        modifier = Modifier
                                            .size(28.dp)
                                            .clip(CircleShape)
                                            .background(colorObj)
                                            .clickable { onColorChanged(hex) }
                                            .testTag("color_pill_$hex"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Default.Check,
                                                contentDescription = "Seleccionado",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }

                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
