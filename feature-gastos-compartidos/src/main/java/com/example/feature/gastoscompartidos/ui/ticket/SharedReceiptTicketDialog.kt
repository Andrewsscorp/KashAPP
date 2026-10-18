package com.example.feature.gastoscompartidos.ui.ticket

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine.SharedDigitalTicketModel
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo

@Composable
fun SharedReceiptTicketDialog(
    ticket: SharedDigitalTicketModel?,
    onShareWhatsAppPng: (SharedDigitalTicketModel) -> Unit,
    onExportPdf: (SharedDigitalTicketModel) -> Unit,
    onDismiss: () -> Unit
) {
    if (ticket == null) return
    val responsive = rememberSharedResponsiveLayoutInfo()

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        SharedExpenseThemeWrapper {
            Surface(
                shape = RoundedCornerShape(26.dp),
                color = Color.White,
                shadowElevation = 24.dp,
            modifier = Modifier
                .fillMaxWidth(if (responsive.isMediumOrExpandedWidth) 0.72f else 0.94f)
                .widthIn(max = 520.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                // Borde Superior Perforado de Tirilla Térmica dibujado en Canvas
                PerforatedThermalEdgeBar()

                // Cabecera Corporativa del Ticket
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SharedExpenseThemeTokens.KashNavy)
                        .padding(18.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "KASHAPP ENTERPRISE • COMPROBANTE OFICIAL",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = SharedExpenseThemeTokens.KashCyanLight,
                            letterSpacing = 0.7.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = ticket.primaryTicketConsecutive,
                            fontSize = if (responsive.isUltraCompactWidth) 18.sp else 21.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White
                        )
                        if (!ticket.relatedExpenseConsecutive.isNullOrBlank()) {
                            Text(
                                text = "Gasto Asociado: ${ticket.relatedExpenseConsecutive}",
                                fontSize = 11.sp,
                                color = Color(0xFFCBD5E1)
                            )
                        }
                        Text(
                            text = ticket.dateTimeFormatted,
                            fontSize = 11.sp,
                            color = SharedExpenseThemeTokens.KashTextMuted
                        )
                    }
                }

                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Monto Total Destacado
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFFF0F9FF),
                        border = BorderStroke(1.dp, Color(0xFFBAE6FD)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = ticket.headlineTitle,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = SharedExpenseThemeTokens.KashNavy,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = ticket.grandTotalFormatted,
                                fontSize = if (responsive.isUltraCompactWidth) 20.sp else 24.sp,
                                fontWeight = FontWeight.Black,
                                color = SharedExpenseThemeTokens.KashBlue
                            )
                            Text(
                                text = "Modalidad: ${ticket.splitMethodLabel}",
                                fontSize = 11.sp,
                                color = SharedExpenseThemeTokens.KashTextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    // Tabla de Participantes y Cuotas
                    if (ticket.participantRows.isNotEmpty()) {
                        Text(
                            text = "DESGLOSE POR PARTICIPANTE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = SharedExpenseThemeTokens.KashTextSecondary
                        )
                        ticket.participantRows.forEach { row ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "${row.displayName} (${row.statusBadgeText})",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = SharedExpenseThemeTokens.KashTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "Cuota: ${row.totalOwedShareFormatted}",
                                        fontSize = 11.sp,
                                        color = SharedExpenseThemeTokens.KashTextSecondary
                                    )
                                }
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = row.netBalanceFormatted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = if (row.isCreditor) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.PayableRose
                                )
                            }
                            HorizontalDivider(color = SharedExpenseThemeTokens.KashBorder)
                        }
                    }

                    // Datos de Recaudo Bancario / Llave Bre-B / Nequi
                    ticket.collectionInstructions?.let { collect ->
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = SharedExpenseThemeTokens.ReceivableTealBg,
                            border = BorderStroke(1.dp, SharedExpenseThemeTokens.ReceivableTealBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "DATOS PARA TRANSFERENCIA / PAGO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.ReceivableTeal
                                )
                                Text(
                                    text = "Acreedor: ${collect.collectorDisplayName}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashNavy
                                )
                                Text(
                                    text = "${collect.preferredMethodLabel}: ${collect.accountOrKeyDisplay}",
                                    fontSize = 12.sp,
                                    color = SharedExpenseThemeTokens.KashTextPrimary
                                )
                            }
                        }
                    }

                    // Sello Criptográfico SHA-256
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = SharedExpenseThemeTokens.SettledEmerald,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = ticket.verificationFooterText,
                            fontSize = 11.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = SharedExpenseThemeTokens.KashTextSecondary,
                            textAlign = TextAlign.Center
                        )
                    }

                    // Botones de Exportación y Compartición (Responsivos)
                    if (responsive.isUltraCompactWidth) {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onShareWhatsAppPng(ticket) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.SettledEmerald),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                            }
                            OutlinedButton(
                                onClick = { onExportPdf(ticket) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Oficial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onExportPdf(ticket) },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.PictureAsPdf, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("PDF Oficial", fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                            }

                            Button(
                                onClick = { onShareWhatsAppPng(ticket) },
                                shape = RoundedCornerShape(12.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.SettledEmerald),
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Share, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("WhatsApp PNG", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, maxLines = 1)
                            }
                        }
                    }
                }

                PerforatedThermalEdgeBar()
            }
        }
    }
}
}

@Composable
private fun PerforatedThermalEdgeBar() {
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(12.dp)
            .background(SharedExpenseThemeTokens.KashNavy)
    ) {
        val step = 28.dp.toPx()
        val radius = 6.dp.toPx()
        var x = step / 2f
        while (x < size.width) {
            drawCircle(
                color = Color.White,
                radius = radius,
                center = Offset(x, 0f)
            )
            x += step
        }
    }
}
