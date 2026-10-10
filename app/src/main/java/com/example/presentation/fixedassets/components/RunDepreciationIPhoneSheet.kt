package com.example.presentation.fixedassets.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.AutoMode
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.usecase.fixedassets.CalculateAssetDepreciationUseCase.AssetPendingDepreciation
import java.text.DecimalFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RunDepreciationIPhoneSheet(
    pendingList: List<AssetPendingDepreciation>,
    isLoading: Boolean,
    onConfirmRun: () -> Unit,
    onDismiss: () -> Unit
) {
    val moneyFmt = DecimalFormat("$ #,##0")
    val totalPending = pendingList.sumOf { it.totalPendingAmount }

    IPhoneActionModal(
        title = "Depreciación Contable Automática",
        subtitle = "Cierre de periodo contable bajo NIIF / IFRS",
        icon = Icons.Default.AutoMode,
        iconTint = Color(0xFF0284C7),
        onDismiss = onDismiss
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Impact Banner
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFF0E223D),
                border = BorderStroke(1.dp, Color(0xFF1E3A8A).copy(alpha = 0.6f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .background(Color(0xFF0284C7).copy(alpha = 0.25f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ajuste de Valor en Libros",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Text(
                            text = "Esta acción calcula y asienta el gasto por depreciación de los activos configurados, actualizando su valor neto.",
                            fontSize = 11.5.sp,
                            color = Color(0xFF8EA5C4),
                            lineHeight = 15.sp
                        )
                    }
                }
            }

            // Summary Metric Card
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = Color(0xFF0E1E38),
                border = BorderStroke(1.dp, Color(0xFF132B4F)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "MONTO TOTAL A DEPRECIAR EN ESTE CORTE",
                        fontSize = 10.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF8EA5C4),
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = moneyFmt.format(totalPending),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = if (totalPending > 0) Color(0xFF38BDF8) else Color(0xFF10B981)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "${pendingList.size} activos con depreciación pendiente acumulada",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF8EA5C4)
                    )
                }
            }

            // List of pending assets
            if (pendingList.isNotEmpty()) {
                Text(
                    text = "DETALLE POR ACTIVO",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF8EA5C4),
                    letterSpacing = 0.5.sp
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(pendingList) { item ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = Color(0xFF0E1E38),
                            border = BorderStroke(1.dp, Color(0xFF132B4F)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = item.asset.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${item.asset.category} • ${item.monthsToDepreciate} mes(es) pendientes",
                                        fontSize = 11.sp,
                                        color = Color(0xFF8EA5C4)
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "-${moneyFmt.format(item.totalPendingAmount)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFF87171)
                                    )
                                    Text(
                                        text = "Nuevo V. Libros: ${moneyFmt.format(item.newBookValue)}",
                                        fontSize = 10.sp,
                                        color = Color(0xFF34D399)
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFF064E3B).copy(alpha = 0.3f),
                    border = BorderStroke(1.dp, Color(0xFF059669).copy(alpha = 0.5f)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF10B981),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = "¡Al día! No hay activos pendientes por depreciar para este periodo.",
                            fontSize = 12.5.sp,
                            color = Color(0xFF34D399),
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Action Button
            Button(
                onClick = onConfirmRun,
                enabled = pendingList.isNotEmpty() && !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF007AFF),
                    disabledContainerColor = Color(0xFF102644)
                ),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("confirm_run_depreciation_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = "Procesando depreciación...", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(
                        imageVector = Icons.Default.AutoMode,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (pendingList.isNotEmpty()) "Ejecutar Depreciación (${pendingList.size} activos)" else "Depreciación al día",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
