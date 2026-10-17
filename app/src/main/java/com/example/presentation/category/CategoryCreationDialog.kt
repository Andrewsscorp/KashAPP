package com.example.presentation.category

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Enterprise Category Creation Dialog with Searchable 100+ Icon Catalog & Color Palette
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryCreationDialog(
    categoryType: String = "EXPENSE", // "EXPENSE" or "INCOME"
    existingCategoryNames: List<String> = emptyList(),
    onDismiss: () -> Unit,
    onCategoryCreated: (name: String, iconKey: String, colorHex: String) -> Unit
) {
    var categoryName by remember { mutableStateOf("") }
    var selectedIconId by remember { mutableStateOf(if (categoryType == "INCOME") "work" else "restaurant") }
    var selectedColorHex by remember { mutableStateOf(if (categoryType == "INCOME") "#10B981" else "#EF4444") }
    var iconSearchQuery by remember { mutableStateOf("") }
    var selectedGroupFilter by remember { mutableStateOf("Todos") }
    var showDuplicatePopup by remember { mutableStateOf(false) }

    // Filter icons by search query and category group
    val filteredIcons = remember(iconSearchQuery, selectedGroupFilter) {
        val query = iconSearchQuery.trim().lowercase()
        FullCategoryIconCatalog.filter { item ->
            val matchesGroup = selectedGroupFilter == "Todos" || item.group.equals(selectedGroupFilter, ignoreCase = true)
            val matchesQuery = query.isBlank() ||
                    item.name.lowercase().contains(query) ||
                    item.id.lowercase().contains(query) ||
                    item.keywords.lowercase().contains(query)
            matchesGroup && matchesQuery
        }
    }

    val selectedIconItem = remember(selectedIconId) {
        FullCategoryIconCatalog.find { it.id == selectedIconId } ?: FullCategoryIconCatalog[0]
    }

    val parsedColor = remember(selectedColorHex) {
        try {
            Color(android.graphics.Color.parseColor(selectedColorHex))
        } catch (_: Exception) {
            Color(0xFF0284C7)
        }
    }

    val titleText = if (categoryType == "INCOME") "Nueva Categoría de Ingreso" else "Nueva Categoría de Gasto"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = titleText,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = Color(0xFF0F172A)
                    )
                    Text(
                        text = "Elige nombre, icono y color para tu categoría",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // 1. LIVE PREVIEW CARD
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(parsedColor.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = selectedIconItem.icon,
                                contentDescription = null,
                                tint = parsedColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (categoryName.isNotBlank()) categoryName.trim() else "Nombre de la categoría",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (categoryName.isNotBlank()) Color(0xFF0F172A) else Color(0xFF94A3B8),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Icono: ${selectedIconItem.name}",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // 2. NAME INPUT FIELD
                Text(
                    text = "Nombre de la categoría:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(6.dp))
                OutlinedTextField(
                    value = categoryName,
                    onValueChange = { if (it.length <= 40) categoryName = it },
                    placeholder = {
                        Text(
                            text = if (categoryType == "INCOME") "Ej: Sueldo, Rendimientos, Ventas..." else "Ej: Restaurante, Almuerzos, Gimnasio...",
                            color = Color(0xFF94A3B8),
                            fontSize = 14.sp
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = parsedColor,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color(0xFFF8FAFC),
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        cursorColor = parsedColor
                    ),
                    textStyle = TextStyle(
                        color = Color(0xFF0F172A),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("new_category_name_input")
                )

                Spacer(modifier = Modifier.height(16.dp))

                // 3. COLOR PALETTE
                Text(
                    text = "Color temático:",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF334155)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CategoryColorPalette.forEach { hex ->
                        val isColorSelected = selectedColorHex.equals(hex, ignoreCase = true)
                        val colorVal = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(colorVal)
                                .border(
                                    width = if (isColorSelected) 3.dp else 1.dp,
                                    color = if (isColorSelected) Color(0xFF0F172A) else Color.Transparent,
                                    shape = CircleShape
                                )
                                .clickable { selectedColorHex = hex }
                                .testTag("color_swatch_$hex"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isColorSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // 4. ICON SEARCH BAR AND FILTER CHIPS (100+ ICONS)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Elige un icono (+100 disponibles):",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF334155)
                    )
                    Text(
                        text = "${filteredIcons.size} iconos",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B),
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))

                // Search field
                OutlinedTextField(
                    value = iconSearchQuery,
                    onValueChange = { iconSearchQuery = it },
                    placeholder = {
                        Text("Buscar icono (ej: tenedores, comida, auto, banco...)", fontSize = 12.sp, color = Color(0xFF94A3B8))
                    },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B), modifier = Modifier.size(18.dp))
                    },
                    trailingIcon = {
                        if (iconSearchQuery.isNotEmpty()) {
                            IconButton(onClick = { iconSearchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(Icons.Default.Close, contentDescription = "Limpiar", tint = Color(0xFF64748B), modifier = Modifier.size(16.dp))
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = parsedColor,
                        unfocusedBorderColor = Color(0xFFE2E8F0),
                        focusedContainerColor = Color(0xFFF1F5F9),
                        unfocusedContainerColor = Color(0xFFF1F5F9)
                    ),
                    textStyle = TextStyle(fontSize = 13.sp, color = Color(0xFF0F172A)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("icon_search_input")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Category Group Filter Pills
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    CategoryFilterGroups.forEach { groupName ->
                        val isGroupSelected = selectedGroupFilter == groupName
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isGroupSelected) parsedColor else Color(0xFFF1F5F9),
                            border = BorderStroke(1.dp, if (isGroupSelected) parsedColor else Color(0xFFE2E8F0)),
                            modifier = Modifier.clickable { selectedGroupFilter = groupName }
                        ) {
                            Text(
                                text = groupName,
                                fontSize = 11.sp,
                                fontWeight = if (isGroupSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isGroupSelected) Color.White else Color(0xFF475569),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Scrollable Grid of 100+ Icons
                if (filteredIcons.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No se encontraron iconos para \"$iconSearchQuery\"",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                } else {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        filteredIcons.forEach { iconItem ->
                            val isIconSelected = selectedIconId == iconItem.id
                            Surface(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedIconId = iconItem.id }
                                    .border(
                                        width = if (isIconSelected) 2.dp else 1.dp,
                                        color = if (isIconSelected) parsedColor else Color(0xFFE2E8F0),
                                        shape = RoundedCornerShape(10.dp)
                                    )
                                    .testTag("icon_option_${iconItem.id}"),
                                color = if (isIconSelected) parsedColor.copy(alpha = 0.15f) else Color(0xFFF8FAFC)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = iconItem.icon,
                                        contentDescription = iconItem.name,
                                        tint = if (isIconSelected) parsedColor else Color(0xFF475569),
                                        modifier = Modifier.size(22.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val clean = categoryName.trim()
                    if (clean.isNotBlank()) {
                        val isDuplicate = existingCategoryNames.any { it.trim().equals(clean, ignoreCase = true) }
                        if (isDuplicate) {
                            showDuplicatePopup = true
                        } else {
                            onCategoryCreated(clean, selectedIconId, selectedColorHex)
                        }
                    }
                },
                enabled = categoryName.trim().isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = parsedColor,
                    disabledContainerColor = Color(0xFFCBD5E1)
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("submit_create_category_btn")
            ) {
                Text("Crear categoría", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF64748B))
            }
        },
        containerColor = Color.White,
        shape = RoundedCornerShape(20.dp)
    )

    if (showDuplicatePopup) {
        AlertDialog(
            onDismissRequest = { showDuplicatePopup = false },
            title = {
                Text(
                    text = "Categoría Duplicada",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color(0xFF0F172A)
                )
            },
            text = {
                Text(
                    text = "Ya existe una categoría registrada con el nombre \"${categoryName.trim()}\". Por favor elige un nombre distinto para evitar duplicados.",
                    fontSize = 14.sp,
                    color = Color(0xFF334155)
                )
            },
            confirmButton = {
                Button(
                    onClick = { showDuplicatePopup = false },
                    colors = ButtonDefaults.buttonColors(containerColor = parsedColor),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Text("Entendido", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = Color.White,
            shape = RoundedCornerShape(16.dp)
        )
    }
}
