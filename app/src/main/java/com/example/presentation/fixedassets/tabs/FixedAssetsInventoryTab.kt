package com.example.presentation.fixedassets.tabs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.AssetCategoryEntity
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetFinancialCalculations
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine
import com.example.presentation.fixedassets.ResponsiveDarkNavyAssetCard
import java.text.DecimalFormat

@Composable
fun FixedAssetsInventoryTab(
    assets: List<FixedAssetEntity>,
    categories: List<AssetCategoryEntity>,
    searchQuery: String,
    selectedCategory: String,
    selectedStatus: String,
    onSearchChange: (String) -> Unit,
    onCategoryChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onResetFilters: () -> Unit,
    onSelectAsset: (FixedAssetEntity) -> Unit,
    onEditAsset: (FixedAssetEntity) -> Unit,
    onCreateAsset: () -> Unit,
    modifier: Modifier = Modifier
) {
    val moneyFmt = remember { DecimalFormat("$ #,##0") }
    var selectedLocationFilter by remember { mutableStateOf("Todas") }

    // Aggregate values
    val nonWrittenOffAssets = assets.filter {
        val s = AssetStateMachine.resolveState(it.status)
        !s.isTerminal && it.deletedAt == null
    }
    val activeAcquisitionTotal = nonWrittenOffAssets.sumOf { it.acquisitionValue }
    val activeBookValueTotal = nonWrittenOffAssets.sumOf {
        AssetFinancialCalculations.calculateCurrentBookValue(it.acquisitionValue, it.accumulatedDepreciation, it.residualValue)
    }

    // Extract unique locations from assets
    val locations = remember(assets) {
        val locList = assets.mapNotNull { it.location?.trim() }.filter { it.isNotBlank() }.distinct()
        listOf("Todas") + locList
    }

    // Filter displayed items
    val displayedAssets = remember(assets, selectedLocationFilter) {
        if (selectedLocationFilter == "Todas") {
            assets
        } else {
            assets.filter { it.location?.equals(selectedLocationFilter, ignoreCase = true) == true }
        }
    }

    val cardBg = Color(0xFF0B172B)
    val cardBorder = Color(0xFF132B4F)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. BANNER RESUMEN MAESTRO DE INVENTARIO
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = cardBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color(0xFF0E2849)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Inventory2, contentDescription = null, tint = accentCyan, modifier = Modifier.size(22.dp))
                        }
                        Column {
                            Text(
                                text = "Maestro de Inventario",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                            Text(
                                text = "${assets.size} activos registrados • Control patrimonial",
                                fontSize = 11.5.sp,
                                color = textSecondary
                            )
                        }
                    }

                    Button(
                        onClick = onCreateAsset,
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.testTag("btn_crear_activo_maestro")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("+ Crear", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }

                // Balance contable del maestro
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Costo de Adquisición", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = moneyFmt.format(activeAcquisitionTotal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text("Excluye bajas patrimoniales", fontSize = 9.5.sp, color = Color(0xFF10B981))
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF071222),
                        border = BorderStroke(1.dp, Color(0xFF163259)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text("Valor en Libros Neto", fontSize = 11.sp, color = textSecondary)
                            Text(
                                text = moneyFmt.format(activeBookValueTotal),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = accentCyan
                            )
                            Text("Valuación NIIF vigente", fontSize = 9.5.sp, color = textSecondary)
                        }
                    }
                }
            }
        }

        // 2. BUSCADOR INTEGRADO
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchChange,
            placeholder = { Text("Buscar por nombre, código, marca, placa...", color = Color(0xFF64748B), fontSize = 13.sp) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = accentCyan, modifier = Modifier.size(18.dp)) },
            trailingIcon = {
                if (searchQuery.isNotBlank()) {
                    IconButton(onClick = { onSearchChange("") }) {
                        Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = textSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = cardBg,
                unfocusedContainerColor = cardBg,
                focusedBorderColor = primaryBlue,
                unfocusedBorderColor = cardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            ),
            shape = RoundedCornerShape(14.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .testTag("input_buscar_activos")
        )

        // 3. FILTROS DINÁMICOS POR CATEGORÍA
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val catOptions = listOf("Todas") + categories.map { it.name }.distinct()
            catOptions.forEach { catName ->
                val isSelected = selectedCategory == catName
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) primaryBlue else Color(0xFF0E223D),
                    border = BorderStroke(1.dp, if (isSelected) primaryBlue else Color(0xFF1E3A8A)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onCategoryChange(catName) }
                ) {
                    Text(
                        text = catName,
                        fontSize = 11.5.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // 4. FILTROS DINÁMICOS POR ESTADO
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val statusList = listOf(
                Pair("Todos", "Todos"),
                Pair("ACTIVE", "En uso (Operativo)"),
                Pair("MAINTENANCE", "En mantenimiento"),
                Pair("WRITTEN_OFF", "Dado de baja")
            )
            statusList.forEach { (code, label) ->
                val isSelected = selectedStatus == code
                val dotColor = when (code) {
                    "ACTIVE" -> Color(0xFF10B981)
                    "MAINTENANCE" -> Color(0xFF38BDF8)
                    "WRITTEN_OFF" -> Color(0xFF94A3B8)
                    else -> primaryBlue
                }
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) Color(0xFF132F52) else Color(0xFF091629),
                    border = BorderStroke(1.dp, if (isSelected) dotColor else Color(0xFF163259)),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStatusChange(code) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(6.dp).clip(CircleShape).background(dotColor))
                        Text(
                            text = label,
                            fontSize = 11.5.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = if (isSelected) Color.White else textSecondary
                        )
                    }
                }
            }
        }

        // 5. FILTROS POR UBICACIÓN / RESPONSABLE
        if (locations.size > 2) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                locations.forEach { loc ->
                    val isSelected = selectedLocationFilter == loc
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isSelected) Color(0xFF1E3A8A) else Color(0xFF060E1A),
                        border = BorderStroke(1.dp, if (isSelected) accentCyan else Color(0xFF122846)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { selectedLocationFilter = loc }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Place, contentDescription = null, tint = if (isSelected) accentCyan else textSecondary, modifier = Modifier.size(11.dp))
                            Text(
                                text = loc,
                                fontSize = 10.5.sp,
                                color = if (isSelected) Color.White else textSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                            )
                        }
                    }
                }
            }
        }

        // 6. ENCABEZADO Y CONTADOR DE RESULTADOS
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mostrando ${displayedAssets.size} activos",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = textPrimary
            )

            if (selectedCategory != "Todas" || selectedStatus != "Todos" || selectedLocationFilter != "Todas" || searchQuery.isNotBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF162B44),
                    modifier = Modifier.clickable {
                        onResetFilters()
                        selectedLocationFilter = "Todas"
                    }
                ) {
                    Text(
                        text = "Restablecer filtros",
                        fontSize = 11.sp,
                        color = accentCyan,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
            }
        }

        // 7. LISTA DE TARJETAS DE ACTIVOS
        if (displayedAssets.isEmpty()) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.FilterList, contentDescription = null, tint = textSecondary, modifier = Modifier.size(36.dp))
                    Text("No hay activos con estos filtros", fontSize = 14.5.sp, fontWeight = FontWeight.Bold, color = textPrimary)
                    Text("Intenta cambiar de categoría, estado o limpiar la búsqueda.", fontSize = 11.5.sp, color = textSecondary, textAlign = TextAlign.Center)
                    Spacer(modifier = Modifier.height(4.dp))
                    Button(
                        onClick = {
                            onResetFilters()
                            selectedLocationFilter = "Todas"
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Ver todos los activos", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            displayedAssets.forEach { asset ->
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ResponsiveDarkNavyAssetCard(
                        asset = asset,
                        moneyFmt = moneyFmt,
                        onClick = { onSelectAsset(asset) },
                        onEdit = { onEditAsset(asset) }
                    )
                }
            }
        }
    }
}
