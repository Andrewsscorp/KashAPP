package com.example.presentation.fixedassets.components.movement

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.Upgrade
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.AssetMovementType

/**
 * Filtered and validated movement type selector pills based on asset accounting policies:
 * - Revaluation is hidden/restricted if asset.revaluationEnabled is false.
 * - Impairment is hidden/restricted if asset.impairmentTracking is false.
 * - Maintenance, Improvement, and Disposal are standard operations.
 */
@Composable
fun MovementTypeSelector(
    asset: FixedAssetEntity,
    selectedType: AssetMovementType,
    onTypeSelected: (AssetMovementType) -> Unit,
    modifier: Modifier = Modifier,
    initialType: AssetMovementType = AssetMovementType.IMPROVEMENT
) {
    val isInMaintenance = asset.status == "EN_MANTENIMIENTO"
    val allowedTypes = remember(asset, initialType, isInMaintenance) {
        buildList {
            // Under IFRS, an asset in maintenance workshop cannot receive capital improvements or technical appraisals
            if (!isInMaintenance) {
                add(AssetMovementType.IMPROVEMENT)
                if (asset.revaluationEnabled || initialType == AssetMovementType.REVALUATION) {
                    add(AssetMovementType.REVALUATION)
                }
            }
            add(AssetMovementType.MAINTENANCE)
            if (asset.impairmentTracking || initialType == AssetMovementType.IMPAIRMENT) {
                add(AssetMovementType.IMPAIRMENT)
            }
            add(AssetMovementType.DISPOSAL)
            add(AssetMovementType.SALE)
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "TIPO DE MOVIMIENTO / NOVEDAD",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF8EA5C4),
                letterSpacing = 0.5.sp
            )

            // Dynamic badge explaining policy
            Text(
                text = when {
                    !asset.revaluationEnabled && !asset.impairmentTracking -> "NIIF Estándar"
                    asset.revaluationEnabled && asset.impairmentTracking -> "NIIF Plenas (Revalúo + NIC 36)"
                    asset.revaluationEnabled -> "Modelo de Revalúo"
                    else -> "Con Deterioro NIC 36"
                },
                fontSize = 10.sp,
                color = Color(0xFF38BDF8),
                fontWeight = FontWeight.SemiBold
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            allowedTypes.forEach { type ->
                val isSelected = selectedType == type
                val config = getMovementTypeUiConfig(type)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = if (isSelected) config.activeColor else Color(0xFF0B172B),
                    border = BorderStroke(
                        1.dp,
                        if (isSelected) config.activeColor else Color(0xFF132B4F)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable { onTypeSelected(type) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = config.icon,
                            contentDescription = null,
                            tint = if (isSelected) Color.White else config.activeColor,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = type.label,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) Color.White else Color(0xFFE2E8F0)
                        )
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = if (isSelected) Color(0x33FFFFFF) else Color(0xFF0F223D)
                        ) {
                            Text(
                                text = config.badgeText,
                                fontSize = 9.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else config.activeColor,
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class MovementTypeUiConfig(
    val icon: ImageVector,
    val activeColor: Color,
    val badgeText: String
)

private fun getMovementTypeUiConfig(type: AssetMovementType): MovementTypeUiConfig {
    return when (type) {
        AssetMovementType.IMPROVEMENT -> MovementTypeUiConfig(
            icon = Icons.Default.Upgrade,
            activeColor = Color(0xFF0284C7),
            badgeText = "+ Activo"
        )
        AssetMovementType.REVALUATION -> MovementTypeUiConfig(
            icon = Icons.Default.TrendingUp,
            activeColor = Color(0xFF8B5CF6),
            badgeText = "Superávit"
        )
        AssetMovementType.MAINTENANCE -> MovementTypeUiConfig(
            icon = Icons.Default.Build,
            activeColor = Color(0xFFF59E0B),
            badgeText = "Gasto 100%"
        )
        AssetMovementType.IMPAIRMENT -> MovementTypeUiConfig(
            icon = Icons.Default.TrendingDown,
            activeColor = Color(0xFFEF4444),
            badgeText = "NIC 36"
        )
        AssetMovementType.DISPOSAL -> MovementTypeUiConfig(
            icon = Icons.Default.DeleteSweep,
            activeColor = Color(0xFF64748B),
            badgeText = "Baja"
        )
        AssetMovementType.SALE -> MovementTypeUiConfig(
            icon = Icons.Default.MonetizationOn,
            activeColor = Color(0xFF10B981),
            badgeText = "Venta"
        )
        else -> MovementTypeUiConfig(
            icon = Icons.Default.Upgrade,
            activeColor = Color(0xFF007AFF),
            badgeText = "Contable"
        )
    }
}
