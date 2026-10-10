package com.example.presentation.fixedassets.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.fixedassets.AssetCategory

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FixedAssetFilterIPhoneSheet(
    selectedCategory: String,
    selectedStatus: String,
    onCategoryChange: (String) -> Unit,
    onStatusChange: (String) -> Unit,
    onResetFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    val categories = listOf("Todas") + AssetCategory.entries.map { it.title }
    val statuses = listOf("Todos", "Activo Operativo", "En Mantenimiento", "Vendido", "Dado de Baja")

    IPhoneActionModal(
        title = "Filtros de Activos Fijos",
        subtitle = "Refina la visualización del inventario patrimonial",
        icon = Icons.Default.FilterList,
        iconTint = Color(0xFF38BDF8),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Category section
            Text(
                text = "CATEGORÍA DEL ACTIVO",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8EA5C4),
                letterSpacing = 0.5.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                categories.forEach { cat ->
                    val isSelected = selectedCategory.equals(cat, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF007AFF) else Color(0xFF0E1E38),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF132B4F)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onCategoryChange(cat) }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (isSelected) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.padding(end = 4.dp)
                                )
                            }
                            Text(
                                text = cat,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF8EA5C4)
                            )
                        }
                    }
                }
            }

            // Status section
            Text(
                text = "ESTADO OPERACIONAL",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8EA5C4),
                letterSpacing = 0.5.sp
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                statuses.forEach { st ->
                    val isSelected = selectedStatus.equals(st, ignoreCase = true)
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF007AFF) else Color(0xFF0E1E38),
                        border = if (isSelected) null else BorderStroke(1.dp, Color(0xFF132B4F)),
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onStatusChange(st) }
                    ) {
                        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                            Text(
                                text = st,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else Color(0xFF8EA5C4)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onResetFilters,
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color(0xFF132B4F)),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFF8EA5C4)),
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                ) {
                    Text(text = "Restablecer", color = Color(0xFF8EA5C4), fontWeight = FontWeight.SemiBold)
                }

                Button(
                    onClick = onDismiss,
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF007AFF)),
                    modifier = Modifier
                        .weight(1.5f)
                        .height(48.dp)
                ) {
                    Text(text = "Aplicar Filtros", fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
        }
    }
}
