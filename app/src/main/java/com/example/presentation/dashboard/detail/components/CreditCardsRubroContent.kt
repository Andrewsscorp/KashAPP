package com.example.presentation.dashboard.detail.components

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.models.CardSubPurchaseItem
import com.example.domain.models.RubroItemDisplay
import com.example.ui.components.CurrencyText
import java.math.BigDecimal
import java.text.DecimalFormat

/**
 * Dedicated content view for DEBTS rubro.
 * Displays credit cards with live quotas, deferred sub-purchases, installments progress,
 * and encapsulates the Sub-Purchase Detail Modal and Capital Advance Simulator.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreditCardsRubroContent(
    items: List<RubroItemDisplay>,
    categoryBreakdown: List<Triple<String, BigDecimal, Double>>,
    isPrivacyMode: Boolean,
    onSelectAccountForDetail: (RubroItemDisplay) -> Unit,
    modifier: Modifier = Modifier
) {
    val primaryColor = Color(0xFFEF4444)

    var selectedPurchaseForDetail by remember { mutableStateOf<CardSubPurchaseItem?>(null) }
    var showCapitalAdvanceSimulator by remember { mutableStateOf(false) }
    var simulatorTargetPurchase by remember { mutableStateOf<CardSubPurchaseItem?>(null) }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. CAPITAL ADVANCE SIMULATOR CTA BANNER
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFBEB)),
            border = BorderStroke(1.5.dp, Color(0xFFFDE68A)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f).padding(end = 10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFFEF3C7)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AutoAwesome,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Simulador de Abono a Capital",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF92400E)
                        )
                        Text(
                            text = "Proyecta reducción de tiempo o cuota sin alterar tus datos.",
                            fontSize = 11.sp,
                            color = Color(0xFFB45309)
                        )
                    }
                }

                Button(
                    onClick = {
                        simulatorTargetPurchase = items.firstOrNull()?.subPurchases?.firstOrNull()
                        showCapitalAdvanceSimulator = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD97706)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Simular", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 2. DISTRIBUTION BY DEBT CATEGORY
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        imageVector = Icons.Default.PieChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "DISTRIBUCIÓN Y CLASIFICACIÓN DE DEUDAS",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                if (categoryBreakdown.isEmpty()) {
                    Text(
                        text = "Sin deudas activas registradas con los filtros aplicados.",
                        fontSize = 13.sp,
                        color = Color(0xFF94A3B8)
                    )
                } else {
                    categoryBreakdown.forEachIndexed { idx, item ->
                        val color = when (idx % 3) {
                            0 -> Color(0xFFEF4444)
                            1 -> Color(0xFFF59E0B)
                            else -> Color(0xFF8B5CF6)
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f, fill = false),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(color)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.first,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = Color(0xFF334155),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    CurrencyText(
                                        amount = item.second,
                                        isPrivacyMode = isPrivacyMode,
                                        fontSize = 12.sp,
                                        minFontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFDC2626)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "(${"%.1f".format(item.third)}%)",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B)
                                    )
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(5.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFF1F5F9))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth((item.third / 100.0).toFloat().coerceIn(0.02f, 1f))
                                        .height(5.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                            }
                        }
                    }
                }
            }
        }

        // 3. CREDIT CARDS & DETAILED SUB-PURCHASES
        Card(
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
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
                        imageVector = Icons.Default.TableChart,
                        contentDescription = null,
                        tint = primaryColor,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "TARJETAS Y SUB-GASTOS DIFERIDOS (${items.size})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF64748B),
                        letterSpacing = 0.5.sp
                    )
                }

                if (items.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No hay tarjetas u obligaciones registradas con estos filtros.",
                            fontSize = 13.sp,
                            color = Color(0xFF94A3B8)
                        )
                    }
                } else {
                    items.forEach { item ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { onSelectAccountForDetail(item) }
                        ) {
                            Column(modifier = Modifier.padding(14.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier
                                            .weight(1f, fill = false)
                                            .padding(end = 8.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(primaryColor.copy(alpha = 0.15f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CreditCard,
                                                contentDescription = null,
                                                tint = primaryColor,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(10.dp))
                                        Column {
                                            Text(
                                                text = item.name,
                                                fontSize = 14.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFF0F172A),
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = "${item.institution} • ${item.status}",
                                                fontSize = 11.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Column(horizontalAlignment = Alignment.End) {
                                            CurrencyText(
                                                amount = item.amount,
                                                isPrivacyMode = isPrivacyMode,
                                                fontSize = 15.sp,
                                                minFontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFDC2626)
                                            )
                                            if (item.rateEa != null) {
                                                Text(
                                                    text = "${item.rateEa}% E.A.",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = Color(0xFFDC2626)
                                                )
                                            }
                                        }
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = "Ver detalle",
                                            tint = Color(0xFF94A3B8),
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }

                                if (item.quotaLimit != null && item.availableQuota != null) {
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(
                                            text = "Cupo Disponible: $ ${DecimalFormat("#,##0").format(item.availableQuota)}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF059669),
                                            fontWeight = FontWeight.SemiBold
                                        )
                                        Text(
                                            text = "Cupo Total: $ ${DecimalFormat("#,##0").format(item.quotaLimit)}",
                                            fontSize = 10.sp,
                                            color = Color(0xFF64748B)
                                        )
                                    }
                                }

                                if (item.subPurchases.isNotEmpty()) {
                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = Color(0xFFE2E8F0))
                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = "COMPRAS Y GASTOS DIFERIDOS (${item.subPurchases.size}) — Toca para ver análisis",
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFF64748B),
                                        letterSpacing = 0.5.sp
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))

                                    item.subPurchases.forEach { sub ->
                                        Surface(
                                            shape = RoundedCornerShape(10.dp),
                                            color = Color.White,
                                            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp)
                                                .clickable { selectedPurchaseForDetail = sub }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(10.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.SpaceBetween
                                            ) {
                                                Column(modifier = Modifier.weight(1f, fill = false).padding(end = 8.dp)) {
                                                    Text(
                                                        text = sub.merchant,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFF0F172A),
                                                        maxLines = 1,
                                                        overflow = TextOverflow.Ellipsis
                                                    )
                                                    Text(
                                                        text = "${sub.category} • Cuota ${sub.paidInstallments} de ${sub.totalInstallments} (${sub.dateFormatted})",
                                                        fontSize = 11.sp,
                                                        color = Color(0xFF64748B)
                                                    )
                                                }

                                                Row(verticalAlignment = Alignment.CenterVertically) {
                                                    Column(horizontalAlignment = Alignment.End) {
                                                        Text(
                                                            text = "$ ${DecimalFormat("#,##0").format(sub.currentBalance)}",
                                                            fontSize = 13.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = Color(0xFFDC2626)
                                                        )
                                                        Text(
                                                            text = "Resta por pagar",
                                                            fontSize = 9.sp,
                                                            color = Color(0xFF94A3B8)
                                                        )
                                                    }
                                                    Icon(
                                                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                                        contentDescription = null,
                                                        tint = Color(0xFF94A3B8),
                                                        modifier = Modifier.size(16.dp)
                                                    )
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
    }

    // Encapsulated Sub-Purchase Detail Bottom Sheet
    if (selectedPurchaseForDetail != null) {
        val purchase = selectedPurchaseForDetail!!
        val remainingInstallments = purchase.totalInstallments - purchase.paidInstallments
        val percentPaid = ((purchase.paidInstallments.toDouble() / purchase.totalInstallments.toDouble()) * 100).toInt()

        ModalBottomSheet(
            onDismissRequest = { selectedPurchaseForDetail = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(text = purchase.merchant, fontSize = 18.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        Text(text = "${purchase.cardName} • ${purchase.concept}", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = { selectedPurchaseForDetail = null }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Progreso de Amortización", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF334155))
                            Text("$percentPaid% Completado", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                        }
                        LinearProgressIndicator(
                            progress = { (percentPaid / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier.fillMaxWidth().height(8.dp).clip(CircleShape),
                            color = Color(0xFF0284C7),
                            trackColor = Color(0xFFE2E8F0),
                            strokeCap = StrokeCap.Round
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${purchase.paidInstallments} cuotas pagadas", fontSize = 11.sp, color = Color(0xFF64748B))
                            Text("$remainingInstallments cuotas pendientes", fontSize = 11.sp, color = Color(0xFFEF4444), fontWeight = FontWeight.SemiBold)
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Saldo Pendiente", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("$ ${DecimalFormat("#,##0").format(purchase.currentBalance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFDC2626))
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Monto Original", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("$ ${DecimalFormat("#,##0").format(purchase.originalAmount)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Cuota Mensual Est.", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("$ ${DecimalFormat("#,##0").format(purchase.monthlyPayment)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                        }
                    }
                    Surface(modifier = Modifier.weight(1f), shape = RoundedCornerShape(12.dp), color = Color(0xFFF8FAFC), border = BorderStroke(1.dp, Color(0xFFE2E8F0))) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text("Tasa de Interés", fontSize = 10.sp, color = Color(0xFF64748B))
                            Text("${purchase.rateEa}% E.A.", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD97706))
                        }
                    }
                }

                Button(
                    onClick = {
                        simulatorTargetPurchase = purchase
                        selectedPurchaseForDetail = null
                        showCapitalAdvanceSimulator = true
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Simular Abono a Capital (Reducción Tiempo/Cuota)", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                }
            }
        }
    }

    // Encapsulated Capital Advance Simulator Bottom Sheet
    if (showCapitalAdvanceSimulator) {
        val target = simulatorTargetPurchase ?: items.firstOrNull()?.subPurchases?.firstOrNull()
        var advanceAmount by remember { mutableDoubleStateOf(300000.0) }
        var simulationStrategy by remember { mutableStateOf("TIME_REDUCTION") }

        val origBal = target?.currentBalance ?: 1000000.0
        val maxAdvance = origBal.coerceAtLeast(100000.0)
        val safeAdvance = advanceAmount.coerceIn(50000.0, maxAdvance)

        val rateEa = target?.rateEa ?: 24.5
        val monthlyRate = Math.pow(1.0 + rateEa / 100.0, 1.0 / 12.0) - 1.0
        val remainingMonths = (target?.totalInstallments ?: 12) - (target?.paidInstallments ?: 0)

        val monthlyFee = target?.monthlyPayment ?: (origBal / remainingMonths)
        val monthsSaved = if (monthlyFee > 0) (safeAdvance / monthlyFee).toInt().coerceIn(1, remainingMonths.coerceAtLeast(1)) else 1
        val newRemainingMonths = (remainingMonths - monthsSaved).coerceAtLeast(1)

        val interestSavedEstimated = (safeAdvance * monthlyRate * monthsSaved * 1.35).coerceAtLeast(25000.0)
        val newMonthlyFeeReduced = if (newRemainingMonths > 0) ((origBal - safeAdvance) / remainingMonths).coerceAtLeast(10000.0) else 0.0

        ModalBottomSheet(
            onDismissRequest = { showCapitalAdvanceSimulator = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .navigationBarsPadding(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text("Simulador de Abono a Capital", fontSize = 17.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            Surface(shape = RoundedCornerShape(6.dp), color = Color(0xFFFEF3C7)) {
                                Text("Solo Proyección", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF92400E), modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                            }
                        }
                        Text(text = "Afecta el plazo/cuota de forma matemática sin alterar datos reales", fontSize = 11.sp, color = Color(0xFF64748B))
                    }
                    IconButton(onClick = { showCapitalAdvanceSimulator = false }) {
                        Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = Color(0xFF64748B))
                    }
                }

                HorizontalDivider(color = Color(0xFFE2E8F0))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Surface(
                        modifier = Modifier.weight(1f).clickable { simulationStrategy = "TIME_REDUCTION" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (simulationStrategy == "TIME_REDUCTION") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (simulationStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Speed, contentDescription = null, tint = if (simulationStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reducción de Tiempo", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (simulationStrategy == "TIME_REDUCTION") Color(0xFF0284C7) else Color(0xFF334155))
                            Text("De atrás hacia adelante", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }

                    Surface(
                        modifier = Modifier.weight(1f).clickable { simulationStrategy = "QUOTA_REDUCTION" },
                        shape = RoundedCornerShape(12.dp),
                        color = if (simulationStrategy == "QUOTA_REDUCTION") Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(1.5.dp, if (simulationStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFFE2E8F0))
                    ) {
                        Column(modifier = Modifier.padding(10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Payments, contentDescription = null, tint = if (simulationStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFF64748B), modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Reducción de Cuota", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = if (simulationStrategy == "QUOTA_REDUCTION") Color(0xFF0284C7) else Color(0xFF334155))
                            Text("Mismo plazo restante", fontSize = 9.sp, color = Color(0xFF64748B))
                        }
                    }
                }

                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Monto del Abono Extraordinario:", fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color(0xFF334155))
                        Text("$ ${DecimalFormat("#,##0").format(safeAdvance)}", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0284C7))
                    }
                    Slider(
                        value = safeAdvance.toFloat(),
                        onValueChange = { advanceAmount = it.toDouble() },
                        valueRange = 50000f..maxAdvance.toFloat(),
                        colors = SliderDefaults.colors(thumbColor = Color(0xFF0284C7), activeTrackColor = Color(0xFF0284C7))
                    )
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFECFDF5)),
                    border = BorderStroke(1.dp, Color(0xFFA7F3D0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Icon(Icons.Default.TrendingDown, contentDescription = null, tint = Color(0xFF059669), modifier = Modifier.size(20.dp))
                            Text("AHORRO ESTIMADO TOTAL EN INTERESES", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = Color(0xFF065F46))
                        }
                        Text("$ ${DecimalFormat("#,##0").format(interestSavedEstimated)} COP", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))

                        HorizontalDivider(color = Color(0xFFA7F3D0))

                        if (simulationStrategy == "TIME_REDUCTION") {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Meses / Cuotas eliminadas:", fontSize = 12.sp, color = Color(0xFF065F46))
                                Text("-$monthsSaved cuotas (De atrás adelante)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nuevo tiempo para pagar:", fontSize = 12.sp, color = Color(0xFF065F46))
                                Text("$newRemainingMonths meses (Antes $remainingMonths)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF0F172A))
                            }
                        } else {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Nueva Cuota Mensual Reducida:", fontSize = 12.sp, color = Color(0xFF065F46))
                                Text("$ ${DecimalFormat("#,##0").format(newMonthlyFeeReduced)} COP", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF059669))
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Cuota anterior:", fontSize = 12.sp, color = Color(0xFF065F46))
                                Text("$ ${DecimalFormat("#,##0").format(monthlyFee)} COP", fontSize = 12.sp, color = Color(0xFF64748B))
                            }
                        }
                    }
                }

                Button(
                    onClick = { showCapitalAdvanceSimulator = false },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Cerrar Simulación", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
