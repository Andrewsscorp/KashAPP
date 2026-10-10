package com.example.presentation.fixedassets.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Chair
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.local.entity.AssetCategoryEntity

@Composable
fun AssetCategoryPickerModal(
    categories: List<AssetCategoryEntity>,
    selectedCategoryName: String,
    onCategorySelected: (AssetCategoryEntity) -> Unit,
    onCreateCategory: (name: String, code: String, colorHex: String, iconName: String) -> Unit,
    onDismiss: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var isCreatingCategory by remember { mutableStateOf(false) }

    val bgDark = Color(0xFF071221)
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val filtered = categories.filter {
        it.name.contains(searchQuery, ignoreCase = true) || it.code.contains(searchQuery, ignoreCase = true)
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(24.dp),
            color = bgDark,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .padding(vertical = 24.dp)
        ) {
            if (isCreatingCategory) {
                CreateCategoryForm(
                    onSave = { name, code, color, icon ->
                        onCreateCategory(name, code, color, icon)
                        isCreatingCategory = false
                    },
                    onCancel = { isCreatingCategory = false }
                )
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFF0E2849)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Category, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(18.dp))
                            }
                            Column {
                                Text("Categoría de Activo", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                                Text("Selecciona o crea una categoría", fontSize = 11.5.sp, color = textSecondary)
                            }
                        }

                        IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = textSecondary, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Search input
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Buscar categoría...", color = textSecondary, fontSize = 13.sp) },
                        leadingIcon = { Icon(Icons.Default.Search, null, tint = textSecondary, modifier = Modifier.size(18.dp)) },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = cardBorder,
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = cardBg,
                            unfocusedContainerColor = cardBg
                        )
                    )

                    // Button "+ Crear nueva categoría"
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF0E2545),
                        border = BorderStroke(1.dp, Color(0xFF194073)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { isCreatingCategory = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Icon(Icons.Default.Add, null, tint = Color(0xFF38BDF8), modifier = Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Crear nueva categoría", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                        }
                    }

                    // Category List
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(300.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filtered, key = { it.id }) { cat ->
                            val isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true)
                            val catColor = try {
                                Color(android.graphics.Color.parseColor(cat.colorHex))
                            } catch (e: Exception) {
                                primaryBlue
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (isSelected) Color(0xFF0F2C54) else cardBg,
                                border = BorderStroke(1.dp, if (isSelected) primaryBlue else cardBorder),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onCategorySelected(cat)
                                        onDismiss()
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 14.dp, vertical = 11.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(34.dp)
                                                .clip(RoundedCornerShape(9.dp))
                                                .background(catColor.copy(alpha = 0.2f))
                                                .border(BorderStroke(1.dp, catColor.copy(alpha = 0.5f)), RoundedCornerShape(9.dp)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = resolveCategoryIcon(cat.iconName),
                                                contentDescription = null,
                                                tint = catColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }

                                        Column {
                                            Text(
                                                text = cat.name,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = Color.White
                                            )
                                            if (cat.code.isNotBlank()) {
                                                Text(
                                                    text = "Código: ${cat.code}",
                                                    fontSize = 10.5.sp,
                                                    color = textSecondary
                                                )
                                            }
                                        }
                                    }

                                    if (isSelected) {
                                        Box(
                                            modifier = Modifier
                                                .size(22.dp)
                                                .clip(CircleShape)
                                                .background(primaryBlue),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateCategoryForm(
    onSave: (name: String, code: String, color: String, icon: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf("#0284C7") }
    var selectedIcon by remember { mutableStateOf("laptop_chromebook") }

    val colorOptions = listOf(
        "#0284C7", "#10B981", "#6366F1", "#8B5CF6",
        "#EC4899", "#F59E0B", "#14B8A6", "#EF4444", "#64748B"
    )

    val iconOptions = listOf(
        Pair("laptop_chromebook", Icons.Default.LaptopChromebook),
        Pair("directions_car", Icons.Default.DirectionsCar),
        Pair("apartment", Icons.Default.Apartment),
        Pair("precision_manufacturing", Icons.Default.PrecisionManufacturing),
        Pair("chair", Icons.Default.Chair),
        Pair("terminal", Icons.Default.Terminal),
        Pair("inventory_2", Icons.Default.Inventory2),
        Pair("build", Icons.Default.Build),
        Pair("camera_alt", Icons.Default.CameraAlt),
        Pair("security", Icons.Default.Security)
    )

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = primaryBlue,
        unfocusedBorderColor = cardBorder,
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedContainerColor = cardBg,
        unfocusedContainerColor = cardBg
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text("Nueva Categoría", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                Text("Configura el nombre, color e icono", fontSize = 11.5.sp, color = textSecondary)
            }
            IconButton(onClick = onCancel, modifier = Modifier.size(30.dp)) {
                Icon(Icons.Default.Close, null, tint = textSecondary, modifier = Modifier.size(18.dp))
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Nombre de la categoría *") },
            placeholder = { Text("Ej. Equipos de Laboratorio") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors
        )

        OutlinedTextField(
            value = code,
            onValueChange = { code = it.uppercase().take(4) },
            label = { Text("Código de identificación (opcional)") },
            placeholder = { Text("Ej. LAB") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = textFieldColors
        )

        // Color selector
        Text("Color de la categoría", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colorOptions.forEach { hex ->
                val c = Color(android.graphics.Color.parseColor(hex))
                val isSel = selectedColor == hex
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(c)
                        .clickable { selectedColor = hex }
                        .border(
                            BorderStroke(if (isSel) 2.5.dp else 0.dp, Color.White),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isSel) {
                        Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                    }
                }
            }
        }

        // Icon selector
        Text("Icono representativo", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = textPrimary)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            iconOptions.take(5).forEach { (key, vector) ->
                val isSel = selectedIcon == key
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSel) Color(0xFF0F2C54) else cardBg,
                    border = BorderStroke(1.dp, if (isSel) primaryBlue else cardBorder),
                    modifier = Modifier
                        .size(36.dp)
                        .clickable { selectedIcon = key }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(vector, null, tint = if (isSel) Color(0xFF38BDF8) else textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // Action Buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFF0E223D),
                border = BorderStroke(1.dp, Color(0xFF193B66)),
                modifier = Modifier
                    .weight(1f)
                    .clickable { onCancel() }
            ) {
                Box(modifier = Modifier.padding(vertical = 12.dp), contentAlignment = Alignment.Center) {
                    Text("Cancelar", fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.SemiBold)
                }
            }

            Button(
                onClick = { onSave(name.trim(), code.trim(), selectedColor, selectedIcon) },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text("Guardar", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }
    }
}

fun resolveCategoryIcon(iconName: String): ImageVector {
    return when (iconName) {
        "apartment" -> Icons.Default.Apartment
        "directions_car" -> Icons.Default.DirectionsCar
        "laptop_chromebook" -> Icons.Default.LaptopChromebook
        "precision_manufacturing" -> Icons.Default.PrecisionManufacturing
        "chair" -> Icons.Default.Chair
        "terminal" -> Icons.Default.Terminal
        "verified" -> Icons.Default.Verified
        "build" -> Icons.Default.Build
        "camera_alt" -> Icons.Default.CameraAlt
        "security" -> Icons.Default.Security
        else -> Icons.Default.Inventory2
    }
}
