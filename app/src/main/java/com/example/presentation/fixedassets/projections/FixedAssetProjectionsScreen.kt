package com.example.presentation.fixedassets.projections

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.usecase.fixedassets.ProjectAssetValuesUseCase
import java.text.DecimalFormat

@Composable
fun FixedAssetProjectionsScreen(
    assets: List<FixedAssetEntity>,
    projectAssetValuesUseCase: ProjectAssetValuesUseCase = remember { ProjectAssetValuesUseCase() },
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler { onDismiss() }

    var selectedHorizonYears by remember { mutableIntStateOf(5) }
    val horizons = listOf(1, 3, 5, 10)
    val moneyFmt = DecimalFormat("$ #,##0")

    val projection = remember(assets, selectedHorizonYears) {
        projectAssetValuesUseCase.execute(assets, selectedHorizonYears)
    }

    val bgDark = Color(0xFF071426)
    val topBarBg = Color(0xFF0B172B)
    val cardBg = Color(0xFF0B172B)
    val cardInnerBg = Color(0xFF0E223D)
    val cardBorder = Color(0xFF132F52)
    val borderSubtle = Color(0xFF1E3A8A).copy(alpha = 0.5f)
    val primaryBlue = Color(0xFF007AFF)
    val accentCyan = Color(0xFF38BDF8)
    val textPrimary = Color.White
    val textSecondary = Color(0xFF8EA5C4)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Top App Bar
        Surface(
            color = topBarBg,
            border = BorderStroke(1.dp, cardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Volver",
                        tint = textPrimary
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Column {
                    Text(
                        text = "Proyecciones Financieras",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = textPrimary
                    )
                    Text(
                        text = "Valor en Libros, Mercado y Reemplazo de Activos",
                        fontSize = 11.5.sp,
                        color = textSecondary
                    )
                }
            }
        }

        // Body
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. HORIZON SELECTOR TABS
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    horizons.forEach { h ->
                        val isSelected = selectedHorizonYears == h
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) primaryBlue else Color.Transparent,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable { selectedHorizonYears = h }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$h ${if (h == 1) "Año" else "Años"}",
                                    fontSize = 12.5.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 2. HERO PROJECTION SUMMARY CARD
            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .background(Color(0xFF8B5CF6).copy(alpha = 0.2f), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(0xFFA78BFA),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "PROYECCIÓN A $selectedHorizonYears AÑOS",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFA78BFA),
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = "Horizonte Patrimonial Consolidado",
                                fontSize = 13.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = textPrimary
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardInnerBg,
                            border = BorderStroke(1.dp, borderSubtle),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "VALOR EN LIBROS FINAL",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = textSecondary
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = moneyFmt.format(projection.projectedEndBookValue),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = textPrimary
                                )
                                Text(
                                    text = "Actual: ${moneyFmt.format(projection.currentTotalBookValue)}",
                                    fontSize = 11.sp,
                                    color = textSecondary
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardInnerBg,
                            border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.4f)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Text(
                                    text = "VALOR MERCADO ESTIMADO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF34D399)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = moneyFmt.format(projection.projectedEndMarketValue),
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFF34D399)
                                )
                                Text(
                                    text = "Plusvalía incluida",
                                    fontSize = 11.sp,
                                    color = Color(0xFF6EE7B7)
                                )
                            }
                        }
                    }

                    // Total Period Depreciation
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(0xFF7F1D1D).copy(alpha = 0.25f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.4f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFFF87171),
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "Depreciación Contable en los Próximos $selectedHorizonYears Años",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFCA5A5)
                                )
                                Text(
                                    text = "Total a depreciar: ${moneyFmt.format(projection.totalProjectedDepreciation)} (impacto deducible en renta)",
                                    fontSize = 11.5.sp,
                                    color = Color(0xFFF87171)
                                )
                            }
                        }
                    }
                }
            }

            // 3. ANNUAL MILESTONE TIMELINE (Year by Year Table)
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Text(
                        text = "EVOLUCIÓN AÑO POR AÑO",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = accentCyan,
                        letterSpacing = 0.5.sp
                    )

                    projection.points.forEach { pt ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardInnerBg,
                            border = BorderStroke(1.dp, borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = pt.yearLabel,
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = textPrimary
                                    )
                                    Text(
                                        text = "V. Libros: ${moneyFmt.format(pt.projectedBookValue)}",
                                        fontSize = 11.sp,
                                        color = textSecondary
                                    )
                                }

                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = moneyFmt.format(pt.projectedMarketValue),
                                        fontSize = 13.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF34D399)
                                    )
                                    if (pt.yearOffset > 0) {
                                        Text(
                                            text = "Dep. Año: -${moneyFmt.format(pt.depreciationInYear)}",
                                            fontSize = 11.sp,
                                            color = Color(0xFFF87171)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. OBSOLESCENCE & REINVESTMENT RADAR
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                border = BorderStroke(1.dp, cardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.WarningAmber,
                            contentDescription = null,
                            tint = Color(0xFFFBBF24),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "RADAR DE OBSOLESCENCIA Y REEMPLAZO (${projection.milestones.size})",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFBBF24),
                            letterSpacing = 0.5.sp
                        )
                    }

                    if (projection.milestones.isEmpty()) {
                        Text(
                            text = "No hay activos próximos a agotar su vida útil en este horizonte.",
                            fontSize = 12.sp,
                            color = textSecondary
                        )
                    } else {
                        projection.milestones.forEach { m ->
                            val badgeColor = when (m.urgencyStatus) {
                                "CRÍTICO" -> Color(0xFFEF4444)
                                "PRÓXIMO A VENCER" -> Color(0xFFF59E0B)
                                else -> Color(0xFF10B981)
                            }

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardInnerBg,
                                border = BorderStroke(1.dp, borderSubtle),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = m.assetName,
                                            fontSize = 13.5.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = textPrimary,
                                            modifier = Modifier.weight(1f)
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = badgeColor.copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = m.urgencyStatus,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = badgeColor,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(6.dp))

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Vida útil restante: ${m.remainingLifeMonths} meses",
                                            fontSize = 11.5.sp,
                                            color = textSecondary
                                        )
                                        Text(
                                            text = "Reemplazo est.: Año ${m.estimatedReplacementYear}",
                                            fontSize = 11.5.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = accentCyan
                                        )
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Costo estimado de reposición (+inflación 4%): ${moneyFmt.format(m.estimatedReplacementCost)}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = accentCyan
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
        }
    }
}
