package com.example.presentation.fixedassets.create.components

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.LaptopChromebook
import androidx.compose.material.icons.filled.MedicalServices
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TwoWheeler
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetCategoryEntity
import com.example.domain.model.fixedassets.AssetCategory

/**
 * Compact Category Selector Card that replaces the expanded list.
 * Displays selected category cleanly and triggers the selection pop-up / modal sheet.
 */
@Composable
fun CompactCategorySelector(
    selectedCategoryName: String,
    selectedColorHex: String,
    selectedCode: String,
    selectedIconName: String,
    onClickChange: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val catColor = parseColorHex(selectedColorHex)
    val icon = getCategoryIconByName(selectedIconName)

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = BorderStroke(1.dp, cardBorder),
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClickChange() }
            .testTag("compact_category_selector")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Category,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "CATEGORÍA DEL ACTIVO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8EA5C4),
                        letterSpacing = 0.5.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF0F2746),
                    border = BorderStroke(1.dp, Color(0xFF1E4273))
                ) {
                    Text(
                        text = "Tocar para cambiar",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF38BDF8),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = Color(0xFF071222),
                border = BorderStroke(1.dp, Color(0xFF163259)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(42.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(catColor.copy(alpha = 0.18f))
                                .border(BorderStroke(1.5.dp, catColor), RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = null,
                                tint = catColor,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                if (selectedCode.isNotBlank()) {
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = catColor.copy(alpha = 0.2f),
                                        border = BorderStroke(1.dp, catColor.copy(alpha = 0.5f))
                                    ) {
                                        Text(
                                            text = selectedCode,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = catColor,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Text(
                                    text = selectedCategoryName,
                                    fontSize = 14.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                            Text(
                                text = "Base de datos Room • Clase de activo fija",
                                fontSize = 11.sp,
                                color = Color(0xFF8EA5C4)
                            )
                        }
                    }

                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cambiar",
                        tint = Color(0xFF8EA5C4),
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
        }
    }
}

/**
 * Modal Bottom Sheet popup to select or create fixed asset categories from DB.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryPickerBottomSheet(
    categories: List<AssetCategoryEntity>,
    selectedCategoryName: String,
    onSelectCategory: (AssetCategoryEntity) -> Unit,
    onCreateCategoryClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val bgDark = Color(0xFF071426)
    val cardBg = Color(0xFF0C1F38)
    val cardBorder = Color(0xFF132F52)
    val primaryBlue = Color(0xFF007AFF)

    val filteredCategories = remember(categories, searchQuery) {
        if (searchQuery.isBlank()) categories
        else categories.filter {
            it.name.contains(searchQuery, ignoreCase = true) ||
            it.code.contains(searchQuery, ignoreCase = true)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = bgDark,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Seleccionar Categoría",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "Base de datos de categorías de activos",
                        fontSize = 12.sp,
                        color = Color(0xFF8EA5C4)
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cerrar",
                        tint = Color(0xFF8EA5C4)
                    )
                }
            }

            // Create New Category Action Button (Prompt requirement)
            Button(
                onClick = onCreateCategoryClick,
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("open_create_category_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Crear Nueva Categoría",
                    fontSize = 13.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Buscar categoría...", color = Color(0xFF64748B)) },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = primaryBlue,
                    unfocusedBorderColor = cardBorder,
                    cursorColor = primaryBlue
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            )

            // Category List from DB
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 380.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(filteredCategories, key = { it.id }) { cat ->
                    val isSelected = cat.name.equals(selectedCategoryName, ignoreCase = true)
                    val catColor = parseColorHex(cat.colorHex)
                    val icon = getCategoryIconByName(cat.iconName)

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = if (isSelected) Color(0xFF0E2A4D) else cardBg,
                        border = BorderStroke(1.dp, if (isSelected) primaryBlue else cardBorder),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable {
                                onSelectCategory(cat)
                                onDismiss()
                            }
                            .testTag("category_item_${cat.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(catColor.copy(alpha = 0.2f))
                                        .border(BorderStroke(1.dp, catColor), RoundedCornerShape(10.dp)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = icon,
                                        contentDescription = null,
                                        tint = catColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        if (cat.code.isNotBlank()) {
                                            Surface(
                                                shape = RoundedCornerShape(4.dp),
                                                color = catColor.copy(alpha = 0.15f)
                                            ) {
                                                Text(
                                                    text = cat.code,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = catColor,
                                                    modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = cat.name,
                                            fontSize = 13.5.sp,
                                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                            color = Color.White
                                        )
                                    }
                                    Text(
                                        text = if (cat.isSystem) "Sistema NIIF" else "Personalizada por el usuario",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8EA5C4)
                                    )
                                }
                            }

                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Seleccionado",
                                    tint = primaryBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (filteredCategories.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No se encontraron categorías. ¡Crea una nueva!",
                                fontSize = 12.sp,
                                color = Color(0xFF8EA5C4)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
        }
    }
}

/**
 * Dialog to create a brand new category with custom colors and icons,
 * stored permanently in the `asset_categories` Room database table.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateAssetCategoryDialog(
    onDismiss: () -> Unit,
    onSaveCategory: (name: String, code: String, colorHex: String, iconName: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var code by remember { mutableStateOf("") }
    var selectedColorHex by remember { mutableStateOf("#0284C7") }
    var selectedIconName by remember { mutableStateOf("laptop_chromebook") }
    var defaultUsefulLifeMonths by remember { mutableIntStateOf(60) }

    val palette = listOf(
        "#0284C7", // Sky Blue
        "#10B981", // Emerald
        "#6366F1", // Indigo
        "#F59E0B", // Amber
        "#EC4899", // Pink
        "#8B5CF6", // Purple
        "#EF4444", // Red
        "#14B8A6", // Teal
        "#06B6D4", // Cyan
        "#F97316", // Orange
        "#84CC16", // Lime
        "#64748B"  // Slate
    )

    val iconOptions = listOf(
        "laptop_chromebook" to Icons.Default.LaptopChromebook,
        "directions_car" to Icons.Default.DirectionsCar,
        "apartment" to Icons.Default.Apartment,
        "precision_manufacturing" to Icons.Default.PrecisionManufacturing,
        "chair" to Icons.Default.Category,
        "terminal" to Icons.Default.Terminal,
        "phone_android" to Icons.Default.PhoneAndroid,
        "camera_alt" to Icons.Default.CameraAlt,
        "build" to Icons.Default.Build,
        "two_wheeler" to Icons.Default.TwoWheeler,
        "medical_services" to Icons.Default.MedicalServices,
        "inventory_2" to Icons.Default.Inventory2,
        "verified" to Icons.Default.Verified
    )

    val bgDark = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)

    val textFieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = Color.White,
        unfocusedTextColor = Color.White,
        focusedBorderColor = primaryBlue,
        unfocusedBorderColor = cardBorder,
        focusedLabelColor = Color(0xFF38BDF8),
        unfocusedLabelColor = Color(0xFF8EA5C4),
        cursorColor = primaryBlue
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = bgDark,
        title = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = "Nueva Categoría de Activo",
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Se guardará en la tabla de categorías de la base de datos",
                    fontSize = 11.sp,
                    color = Color(0xFF8EA5C4)
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        if (code.isBlank() && it.isNotBlank()) {
                            code = it.take(3).uppercase()
                        }
                    },
                    label = { Text("Nombre de la categoría *") },
                    placeholder = { Text("Ej: Equipos Audiovisuales") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("category_name_input")
                )

                // Code Field
                OutlinedTextField(
                    value = code,
                    onValueChange = { code = it.take(4).uppercase() },
                    label = { Text("Código / Prefijo (Máx 4 letras)") },
                    placeholder = { Text("Ej: EAV") },
                    singleLine = true,
                    colors = textFieldColors,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("category_code_input")
                )

                // Color Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Color identificador",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8EA5C4)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        palette.forEach { hex ->
                            val color = parseColorHex(hex)
                            val isSel = hex.equals(selectedColorHex, ignoreCase = true)
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .clip(CircleShape)
                                    .background(color)
                                    .border(
                                        width = if (isSel) 2.5.dp else 1.dp,
                                        color = if (isSel) Color.White else Color.Transparent,
                                        shape = CircleShape
                                    )
                                    .clickable { selectedColorHex = hex },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isSel) {
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
                }

                // Icon Picker
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = "Ícono representativo",
                        fontSize = 11.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF8EA5C4)
                    )
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        iconOptions.forEach { (iconKey, vector) ->
                            val isSel = iconKey == selectedIconName
                            val selColor = parseColorHex(selectedColorHex)
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSel) selColor.copy(alpha = 0.25f) else Color(0xFF071222),
                                border = BorderStroke(1.dp, if (isSel) selColor else cardBorder),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedIconName = iconKey }
                            ) {
                                Box(
                                    modifier = Modifier.size(38.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = vector,
                                        contentDescription = iconKey,
                                        tint = if (isSel) selColor else Color(0xFF8EA5C4),
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Suggested useful life
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "Vida útil sugerida",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8EA5C4)
                        )
                        Text(
                            text = "$defaultUsefulLifeMonths meses (${"%.1f".format(defaultUsefulLifeMonths / 12.0)} años)",
                            fontSize = 11.5.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                    }
                    Slider(
                        value = defaultUsefulLifeMonths.toFloat(),
                        onValueChange = { defaultUsefulLifeMonths = it.toInt() },
                        valueRange = 12f..240f,
                        steps = 18,
                        colors = SliderDefaults.colors(
                            thumbColor = primaryBlue,
                            activeTrackColor = primaryBlue,
                            inactiveTrackColor = Color(0xFF132B4F)
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isNotBlank()) {
                        val finalCode = if (code.isNotBlank()) code else name.take(3).uppercase()
                        onSaveCategory(name.trim(), finalCode, selectedColorHex, selectedIconName)
                        onDismiss()
                    }
                },
                enabled = name.isNotBlank(),
                colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("confirm_create_category_button")
            ) {
                Text("Guardar Categoría", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar", color = Color(0xFF8EA5C4))
            }
        }
    )
}

fun parseColorHex(hex: String?): Color {
    if (hex.isNullOrBlank()) return Color(0xFF0284C7)
    return try {
        val clean = hex.removePrefix("#")
        val colorInt = clean.toLong(16).toInt()
        if (clean.length == 6) Color(colorInt or 0xFF000000.toInt())
        else Color(colorInt)
    } catch (_: Exception) {
        Color(0xFF0284C7)
    }
}

fun getCategoryIconByName(iconName: String?): ImageVector {
    return when (iconName?.lowercase()) {
        "directions_car", "vehiculos", "car" -> Icons.Default.DirectionsCar
        "apartment", "inmuebles", "building" -> Icons.Default.Apartment
        "precision_manufacturing", "maquinaria", "factory" -> Icons.Default.PrecisionManufacturing
        "laptop_chromebook", "tecnologia", "tech", "laptop" -> Icons.Default.LaptopChromebook
        "terminal", "software", "code" -> Icons.Default.Terminal
        "phone_android", "smartphone" -> Icons.Default.PhoneAndroid
        "camera_alt", "camera" -> Icons.Default.CameraAlt
        "build", "tools" -> Icons.Default.Build
        "two_wheeler", "moto" -> Icons.Default.TwoWheeler
        "medical_services", "salud" -> Icons.Default.MedicalServices
        "verified", "marcas" -> Icons.Default.Verified
        else -> Icons.Default.Inventory2
    }
}
