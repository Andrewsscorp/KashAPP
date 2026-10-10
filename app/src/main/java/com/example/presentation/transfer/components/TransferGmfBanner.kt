package com.example.presentation.transfer.components

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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.transfer.model.formatTransferMoney

/**
 * Informative Banner and Dynamic Summary Card calculating the Colombian 4x1000 (GMF tax).
 * Allows toggling between an exempt account (0%) and a taxed movement (0.4% debit).
 */
@Composable
fun TransferGmfBanner(
    amount: Double,
    currency: String,
    applyGmf: Boolean,
    onToggleGmf: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val gmfAmount = if (applyGmf) amount * 0.004 else 0.0
    val totalDeduction = amount + gmfAmount

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, if (applyGmf) Color(0xFFFED7AA) else Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header: Title and Exemption Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(if (applyGmf) Color(0xFFFFF7ED) else Color(0xFFDCFCE7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (applyGmf) Icons.Default.Percent else Icons.Default.Shield,
                            contentDescription = null,
                            tint = if (applyGmf) Color(0xFFEA580C) else Color(0xFF16A34A),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Gravamen Financiero (4x1000)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = if (applyGmf) "Aplica 0.4% sobre el débito" else "Cuenta marcada como exenta",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (applyGmf) Color(0xFFFEF3C7) else Color(0xFFDCFCE7)
                ) {
                    Text(
                        text = if (applyGmf) "APLICA 4x1000" else "EXENTA",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (applyGmf) Color(0xFFB45309) else Color(0xFF15803D),
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Switch Row: Toggle GMF Exemption
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = Color(0xFFF8FAFC),
                border = BorderStroke(1.dp, Color(0xFFF1F5F9)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calcular impuesto 4x1000",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF334155)
                        )
                        Text(
                            text = "Desactívalo si tu cuenta origen está exonerada ante la DIAN",
                            fontSize = 10.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                    Switch(
                        checked = applyGmf,
                        onCheckedChange = onToggleGmf,
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = Color.White,
                            checkedTrackColor = Color(0xFFEA580C),
                            uncheckedThumbColor = Color.White,
                            uncheckedTrackColor = Color(0xFFCBD5E1)
                        ),
                        modifier = Modifier.testTag("toggle_gmf_switch")
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Importe a transferir", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(
                        text = formatTransferMoney(amount, currency),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF0F172A)
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = "Comisión bancaria", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text(
                        text = "$0.00 (Gratis)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF16A34A)
                    )
                }

                if (applyGmf) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Impuesto 4x1000 (0.4%)", fontSize = 12.sp, color = Color(0xFFEA580C))
                        Text(
                            text = "+${formatTransferMoney(gmfAmount, currency)}",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFEA580C)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total a debitar de origen",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        if (applyGmf) {
                            Text(
                                text = "Incluye impuesto GMF",
                                fontSize = 10.sp,
                                color = Color(0xFFEA580C)
                            )
                        }
                    }
                    Text(
                        text = formatTransferMoney(totalDeduction, currency),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (applyGmf) Color(0xFFEA580C) else Color(0xFF0284C7)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Informative Footnote
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = Color(0xFFF0F9FF),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Transferencia interna inmediata sin costo interbancario.",
                        fontSize = 11.sp,
                        color = Color(0xFF0369A1)
                    )
                }
            }
        }
    }
}
