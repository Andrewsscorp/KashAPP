package com.example.feature.gastoscompartidos.ui.ocr

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.SharedParticipantEntity
import com.example.feature.gastoscompartidos.domain.engine.SharedSplitCalculatorEngine
import com.example.feature.gastoscompartidos.domain.ticket.SharedReceiptTicketEngine
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeTokens
import com.example.feature.gastoscompartidos.ui.theme.SharedExpenseThemeWrapper
import com.example.feature.gastoscompartidos.ui.theme.rememberSharedResponsiveLayoutInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OcrItemizedSplitModal(
    visible: Boolean,
    merchantName: String,
    externalInvoiceNumber: String?,
    currency: String,
    lineItems: List<SharedSplitCalculatorEngine.OcrLineItemInput>,
    participants: List<SharedParticipantEntity>,
    onToggleParticipantOnItem: (itemId: String, participantId: String) -> Unit,
    onAssignAllParticipantsOnItem: (itemId: String) -> Unit = {},
    onExpandMultiUnitItem: (itemId: String) -> Unit = {},
    onConfirmItemizedSplit: () -> Unit,
    onDismiss: () -> Unit
) {
    if (!visible) return
    val responsive = rememberSharedResponsiveLayoutInfo()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
    ) {
        SharedExpenseThemeWrapper {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = responsive.horizontalPadding, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
            Text(
                text = "🧾 División Ítem por Ítem (Factura OCR)",
                fontSize = if (responsive.isUltraCompactWidth) 16.sp else 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = SharedExpenseThemeTokens.KashNavy
            )
            if (merchantName.isNotBlank() || !externalInvoiceNumber.isNullOrBlank()) {
                Text(
                    text = listOfNotNull(
                        merchantName.takeIf { it.isNotBlank() },
                        externalInvoiceNumber?.let { "Factura: $it" }
                    ).joinToString(" • "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = SharedExpenseThemeTokens.KashBlue
                )
            }
            Text(
                text = "Toca los avatares debajo de cada producto para indicar quiénes lo consumieron. El IVA, Impoconsumo y la propina se prorratean automáticamente:",
                fontSize = 12.sp,
                color = SharedExpenseThemeTokens.KashTextSecondary
            )

            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 200.dp, max = if (responsive.isLandscape) 240.dp else 380.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(lineItems, key = { it.itemId }) { item ->
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = SharedExpenseThemeTokens.KashSurfaceMuted,
                        border = BorderStroke(
                            1.dp,
                            if (item.assignedParticipantIds.isEmpty()) SharedExpenseThemeTokens.PayableRose else SharedExpenseThemeTokens.KashBorder
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.description,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SharedExpenseThemeTokens.KashTextPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = SharedReceiptTicketEngine.formatMoney(item.lineTotalPrice, currency),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = SharedExpenseThemeTokens.KashBlue
                                )
                            }

                            if (item.quantity >= 2.0) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.End
                                ) {
                                    TextButton(onClick = { onExpandMultiUnitItem(item.itemId) }) {
                                        Text(
                                            text = "Desglosar ${item.quantity.toInt()} unidades en líneas individuales",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SharedExpenseThemeTokens.ReceivableTeal
                                        )
                                    }
                                }
                            }

                            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                item {
                                    val allSelected = participants.isNotEmpty() &&
                                        item.assignedParticipantIds.containsAll(participants.map { it.id })
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (allSelected) SharedExpenseThemeTokens.ReceivableTeal else Color.White,
                                        border = BorderStroke(
                                            1.dp,
                                            if (allSelected) SharedExpenseThemeTokens.ReceivableTeal else SharedExpenseThemeTokens.KashBorder
                                        ),
                                        modifier = Modifier.clickable {
                                            onAssignAllParticipantsOnItem(item.itemId)
                                        }
                                    ) {
                                        Text(
                                            text = "Todos",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = if (allSelected) Color.White else SharedExpenseThemeTokens.KashTextSecondary,
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                                items(participants, key = { it.id }) { p ->
                                    val isAssigned = item.assignedParticipantIds.contains(p.id)
                                    Surface(
                                        shape = RoundedCornerShape(20.dp),
                                        color = if (isAssigned) SharedExpenseThemeTokens.KashNavy else Color.White,
                                        border = BorderStroke(1.dp, if (isAssigned) SharedExpenseThemeTokens.KashNavy else SharedExpenseThemeTokens.KashBorder),
                                        modifier = Modifier.clickable {
                                            onToggleParticipantOnItem(item.itemId, p.id)
                                        }
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isAssigned) SharedExpenseThemeTokens.KashCyan else SharedExpenseThemeTokens.KashBorder),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = p.avatarInitials,
                                                    fontSize = 9.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = if (isAssigned) Color.White else SharedExpenseThemeTokens.KashTextPrimary
                                                )
                                            }
                                            Spacer(modifier = Modifier.size(6.dp))
                                            Text(
                                                text = p.displayName.substringBefore(" "),
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.SemiBold,
                                                color = if (isAssigned) Color.White else SharedExpenseThemeTokens.KashTextSecondary
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Button(
                onClick = onConfirmItemizedSplit,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SharedExpenseThemeTokens.SettledEmerald),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
            ) {
                Text(
                    text = "Confirmar Asignación de Productos",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
}
