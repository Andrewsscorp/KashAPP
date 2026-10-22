package com.example.feature.impuestosdian.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.impuestosdian.domain.model.AccountGmfMonthlyStatus
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.DianThresholdStatus
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import java.text.NumberFormat
import java.util.Locale

// Paleta de tonos azules corporativos KashApp
private val BlueDeepNavy = Color(0xFF0F172A)
private val BlueRoyalDark = Color(0xFF1E3A8A)
private val BluePrimary = Color(0xFF0284C7)
private val BlueOcean = Color(0xFF2563EB)
private val BlueSkyAccent = Color(0xFF38BDF8)
private val BlueSoftBg = Color(0xFFF0F7FF)
private val BlueCardSubtle = Color(0xFFEFF6FF)
private val BlueBorder = Color(0xFFBFDBFE)
private val BlueSteelText = Color(0xFF334155)
private val BlueMutedText = Color(0xFF64748B)

@Composable
fun DianTaxRadarScreen(
    report: DianTaxRadarReport,
    onToggleAccountTaxExempt: (accountId: String, isExempt: Boolean) -> Unit,
    onExportPreRentaPdf: () -> Unit,
    onOpenConfigureUvtDialog: () -> Unit,
    onSelectJurisdiction: (TaxJurisdiction) -> Unit = {},
    onNavigateBack: (() -> Unit)? = null,
    statusMessage: String? = null,
    modifier: Modifier = Modifier
) {
    val numberFormatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }
    val cur = report.configProfile.currency
    val unitName = report.configProfile.taxUnitName
    val jur = report.configProfile.jurisdiction

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = BlueSoftBg,
        topBar = {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = BlueDeepNavy,
                shadowElevation = 6.dp
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xFF0369A1),
                                    Color(0xFF0284C7),
                                    Color(0xFF1E3A8A)
                                )
                            )
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .widthIn(max = 980.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            if (onNavigateBack != null) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color.White.copy(alpha = 0.18f),
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(CircleShape)
                                        .clickable { onNavigateBack() }
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                            contentDescription = "Volver",
                                            tint = Color.White,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                            }
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "${jur.flagEmoji} Radar Tributario • ${jur.authorityAcronym}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${jur.countryName} ($cur) • Datos reales en vivo de Cuentas, Movimientos y Activos",
                                    fontSize = 11.sp,
                                    color = Color.White.copy(alpha = 0.85f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = onOpenConfigureUvtDialog,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.16f))
                        ) {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = "Configuración Tributaria",
                                tint = Color.White
                            )
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            val isWideScreen = maxWidth >= 680.dp
            val horizontalPad = if (isWideScreen) 24.dp else 16.dp

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 980.dp)
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding()
                    .padding(horizontal = horizontalPad, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 0. SELECTOR RÁPIDO MULTI-PAÍS EN TONOS AZULES
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TaxJurisdiction.entries.forEach { item ->
                        val selected = item == jur
                        Surface(
                            color = if (selected) BlueOcean else Color.White,
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, if (selected) BlueOcean else BlueBorder),
                            shadowElevation = if (selected) 3.dp else 0.dp,
                            modifier = Modifier
                                .clip(RoundedCornerShape(14.dp))
                                .clickable { onSelectJurisdiction(item) }
                        ) {
                            Text(
                                text = "${item.flagEmoji} ${item.countryName} (${item.authorityAcronym})",
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.SemiBold,
                                color = if (selected) Color.White else BlueRoyalDark,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                if (!statusMessage.isNullOrBlank()) {
                    Surface(
                        color = BlueCardSubtle,
                        shape = RoundedCornerShape(14.dp),
                        border = BorderStroke(1.dp, BlueSkyAccent),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = statusMessage,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BlueRoyalDark,
                            modifier = Modifier.padding(12.dp)
                        )
                    }
                }

                // 1. HERO EJECUTIVO AZUL PROFUNDO: ESTADO EN TIEMPO REAL Y UNIDAD TRIBUTARIA
                Card(
                    shape = RoundedCornerShape(22.dp),
                    elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = BlueDeepNavy),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.linearGradient(
                                    colors = listOf(
                                        Color(0xFF0F172A),
                                        Color(0xFF1E3A8A),
                                        Color(0xFF0284C7)
                                    )
                                )
                            )
                            .padding(20.dp)
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                            if (isWideScreen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    HeaderTitleBlock(report = report, jur = jur, modifier = Modifier.weight(1f))
                                    Spacer(modifier = Modifier.width(12.dp))
                                    UvtBadgePill(unitName = unitName, uvtValue = report.uvtValueCop, currency = cur, numberFormatter = numberFormatter)
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    HeaderTitleBlock(report = report, jur = jur, modifier = Modifier.fillMaxWidth())
                                    UvtBadgePill(unitName = unitName, uvtValue = report.uvtValueCop, currency = cur, numberFormatter = numberFormatter)
                                }
                            }

                            Text(
                                text = report.estimatedFilingDeadlineDescription,
                                fontSize = 12.sp,
                                color = Color(0xFFBAE6FD)
                            )

                            if (isWideScreen) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    ActionButtonsRow(
                                        unitName = unitName,
                                        onExportPreRentaPdf = onExportPreRentaPdf,
                                        onOpenConfigureUvtDialog = onOpenConfigureUvtDialog,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Button(
                                        onClick = onExportPreRentaPdf,
                                        colors = ButtonDefaults.buttonColors(containerColor = BlueSkyAccent),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Icon(Icons.Default.Download, contentDescription = null, tint = BlueDeepNavy, modifier = Modifier.size(16.dp))
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Exportar Borrador Pre-Renta PDF (SHA-256)",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BlueDeepNavy
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = onOpenConfigureUvtDialog,
                                        shape = RoundedCornerShape(12.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.45f)),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Configurar País / $unitName / Tasas",
                                            fontSize = 12.sp,
                                            color = Color.White,
                                            fontWeight = FontWeight.SemiBold
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 2. MONITOR DE LOS 5 TOPES ANUALES EN UVT / UNIDAD FISCAL (RESPONSIVO 1 O 2 COLUMNAS)
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = BlueCardSubtle,
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier.size(34.dp)
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Gavel, contentDescription = null, tint = BlueOcean, modifier = Modifier.size(18.dp))
                                }
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column {
                                Text(
                                    text = "5 Topes Oficiales ${jur.authorityAcronym} en $unitName (${report.fiscalYear})",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BlueDeepNavy
                                )
                                Text(
                                    text = "Calculados directamente desde tus saldos y movimientos reales en Room",
                                    fontSize = 11.sp,
                                    color = BlueMutedText
                                )
                            }
                        }

                        if (isWideScreen) {
                            val chunked = report.thresholds.chunked(2)
                            chunked.forEach { pair ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    pair.forEach { threshold ->
                                        Box(modifier = Modifier.weight(1f)) {
                                            ThresholdProgressCard(
                                                threshold = threshold,
                                                numberFormatter = numberFormatter,
                                                currency = cur,
                                                unitName = unitName
                                            )
                                        }
                                    }
                                    if (pair.size == 1) {
                                        Spacer(modifier = Modifier.weight(1f))
                                    }
                                }
                            }
                        } else {
                            report.thresholds.forEach { threshold ->
                                ThresholdProgressCard(
                                    threshold = threshold,
                                    numberFormatter = numberFormatter,
                                    currency = cur,
                                    unitName = unitName
                                )
                            }
                        }
                    }
                }

                // 3. MONITOR MENSUAL DEL 4x1000 / IMPUESTO BANCARIO POR CUENTA Y BILLETERA
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(20.dp),
                    border = BorderStroke(1.dp, BlueBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                                Surface(
                                    color = BlueCardSubtle,
                                    shape = RoundedCornerShape(10.dp),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                                    }
                                }
                                Spacer(modifier = Modifier.width(10.dp))
                                Column {
                                    Text(
                                        text = if (jur == TaxJurisdiction.CO_DIAN) {
                                            "Control Mensual del 4x1000 (${report.currentYearMonth})"
                                        } else {
                                            "Impuesto a Movimientos Bancarios (${report.currentYearMonth})"
                                        },
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BlueDeepNavy
                                    )
                                    Text(
                                        text = if (jur == TaxJurisdiction.CO_DIAN) {
                                            "Tope Exento: 350 UVT Banco • 65 UVT Billeteras (Nequi/DaviPlata)"
                                        } else {
                                            "Tasa vigente: ${"%.3f".format(report.configProfile.financialTransactionTaxRate * 100)}% • Sincronizado con Cuentas"
                                        },
                                        fontSize = 11.sp,
                                        color = BlueMutedText
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.width(8.dp))

                            Surface(
                                color = BlueCardSubtle,
                                shape = RoundedCornerShape(10.dp),
                                border = BorderStroke(1.dp, BlueBorder)
                            ) {
                                Text(
                                    text = "Mes: $${numberFormatter.format(report.totalMonthlyGmfTaxCop)} $cur",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BlueRoyalDark,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                )
                            }
                        }

                        Surface(
                            color = BlueSoftBg,
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BlueBorder),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = report.optimizationRecommendation,
                                fontSize = 12.sp,
                                color = BlueRoyalDark,
                                lineHeight = 17.sp,
                                modifier = Modifier.padding(12.dp)
                            )
                        }

                        if (report.accountGmfStatuses.isEmpty()) {
                            Surface(
                                color = BlueSoftBg,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "No tienes cuentas bancarias o billeteras registradas todavía. Al crear una cuenta en 'Mis Cuentas Bancarias', aparecerá aquí automáticamente sin datos simulados.",
                                    fontSize = 12.sp,
                                    color = BlueSteelText,
                                    modifier = Modifier.padding(14.dp)
                                )
                            }
                        } else {
                            report.accountGmfStatuses.forEach { accStatus ->
                                AccountGmfRow(
                                    status = accStatus,
                                    numberFormatter = numberFormatter,
                                    currency = cur,
                                    unitName = unitName,
                                    onToggleExempt = { isExempt ->
                                        onToggleAccountTaxExempt(accStatus.accountId, isExempt)
                                    }
                                )
                            }
                        }
                    }
                }

                // 4. DEDUCCIÓN POR FACTURA ELECTRÓNICA + 5. IMPUESTOS GENERALES (EN PARALELO EN PANTALLAS ANCHAS)
                if (isWideScreen && report.internationalTaxSummary != null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Box(modifier = Modifier.weight(1f)) {
                            ElectronicInvoiceCard(
                                report = report,
                                jur = jur,
                                cur = cur,
                                unitName = unitName,
                                numberFormatter = numberFormatter
                            )
                        }
                        Box(modifier = Modifier.weight(1f)) {
                            InternationalGeneralTaxesCard(
                                intl = report.internationalTaxSummary,
                                jur = jur,
                                cur = cur,
                                numberFormatter = numberFormatter
                            )
                        }
                    }
                } else {
                    ElectronicInvoiceCard(
                        report = report,
                        jur = jur,
                        cur = cur,
                        unitName = unitName,
                        numberFormatter = numberFormatter
                    )
                    report.internationalTaxSummary?.let { intl ->
                        InternationalGeneralTaxesCard(
                            intl = intl,
                            jur = jur,
                            cur = cur,
                            numberFormatter = numberFormatter
                        )
                    }
                }

                // Nota legal informativa
                Text(
                    text = "Nota Legal (Ley 43 de 1990): Herramienta informativa de planeación tributaria basada exclusivamente en los registros reales del usuario. No sustituye la liquidación oficial firmada por un Contador Público.",
                    fontSize = 11.sp,
                    color = BlueMutedText,
                    lineHeight = 15.sp,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun HeaderTitleBlock(
    report: DianTaxRadarReport,
    jur: TaxJurisdiction,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = "RADAR TRIBUTARIO ${jur.authorityAcronym} • AÑO GRAVABLE ${report.fiscalYear}",
            fontSize = 11.sp,
            fontWeight = FontWeight.ExtraBold,
            color = BlueSkyAccent
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = when {
                report.isObligatedToDeclareRenta -> "Superaste ${report.exceededThresholdsCount} tope(s) para Declarar Renta"
                report.warningThresholdsCount > 0 -> "${report.warningThresholdsCount} tope(s) en zona preventiva (>70%)"
                else -> "Por debajo de todos los topes (${jur.authorityAcronym})"
            },
            fontSize = 18.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color.White
        )
    }
}

@Composable
private fun UvtBadgePill(
    unitName: String,
    uvtValue: Double,
    currency: String,
    numberFormatter: NumberFormat
) {
    Surface(
        color = Color.White.copy(alpha = 0.14f),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BlueSkyAccent.copy(alpha = 0.5f))
    ) {
        Text(
            text = "1 $unitName = $${numberFormatter.format(uvtValue)} $currency",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
        )
    }
}

@Composable
private fun ActionButtonsRow(
    unitName: String,
    onExportPreRentaPdf: () -> Unit,
    onOpenConfigureUvtDialog: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Button(
            onClick = onExportPreRentaPdf,
            colors = ButtonDefaults.buttonColors(containerColor = BlueSkyAccent),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.weight(1f)
        ) {
            Icon(Icons.Default.Download, contentDescription = null, tint = BlueDeepNavy, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Exportar Borrador Pre-Renta PDF",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = BlueDeepNavy
            )
        }

        OutlinedButton(
            onClick = onOpenConfigureUvtDialog,
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.5f)),
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = "Configurar País / $unitName",
                fontSize = 12.sp,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun ElectronicInvoiceCard(
    report: DianTaxRadarReport,
    jur: TaxJurisdiction,
    cur: String,
    unitName: String,
    numberFormatter: NumberFormat
) {
    val fe = report.electronicInvoiceBenefit
    val pctLabel = "${"%.1f".format(report.configProfile.electronicInvoiceDeductionRate * 100)}%"
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BlueBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = BlueCardSubtle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = BlueOcean, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = if (jur == TaxJurisdiction.CO_DIAN) {
                            "Deducción 1% Factura Electrónica (Ley 2277/2022)"
                        } else {
                            "Deducción por Gastos con Factura ($pctLabel)"
                        },
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueDeepNavy
                    )
                    Text(
                        text = "Límite anual: ${fe.maxAllowedDeductionUvt.toInt()} $unitName ($${numberFormatter.format(fe.maxAllowedDeductionCop)} $cur)",
                        fontSize = 11.sp,
                        color = BlueMutedText
                    )
                }
            }

            Surface(
                color = BlueSoftBg,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, BlueBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Compras electrónicas (${fe.eligibleTransactionsCount})", fontSize = 11.sp, color = BlueMutedText)
                        Text(
                            text = "$${numberFormatter.format(fe.eligiblePurchasesTotalCop)} $cur",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = BlueDeepNavy
                        )
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Deducción acumulada ($pctLabel)", fontSize = 11.sp, color = BlueMutedText)
                        Text(
                            text = "$${numberFormatter.format(fe.appliedDeductionCop)} $cur (${"%.1f".format(fe.deductionOnePercentUvt)} $unitName)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BlueOcean
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun InternationalGeneralTaxesCard(
    intl: com.example.feature.impuestosdian.domain.model.InternationalTaxSummary,
    jur: TaxJurisdiction,
    cur: String,
    numberFormatter: NumberFormat
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, BlueBorder),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    color = BlueCardSubtle,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = BluePrimary, modifier = Modifier.size(18.dp))
                    }
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "Impuestos Generales (${jur.flagEmoji} ${jur.countryName})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueDeepNavy
                    )
                    Text(
                        text = "IVA/Sales Tax, Retenciones, Renta y Carga Fiscal sobre datos reales",
                        fontSize = 11.sp,
                        color = BlueMutedText
                    )
                }
            }

            HorizontalDivider(color = BlueBorder)

            GeneralTaxMetricRow(
                label = "IVA / IGV / Sales Tax estimado en compras (${"%.1f".format(intl.vatRateApplied * 100)}%)",
                value = "$${numberFormatter.format(intl.estimatedVatOrSalesTaxPaid)} $cur"
            )
            GeneralTaxMetricRow(
                label = "Retención s/ rendimientos de inversiones y CDTs",
                value = "$${numberFormatter.format(intl.estimatedInvestmentWithholdingTax)} $cur"
            )
            if (intl.deductibleBankTaxFiftyPercent > 0.0) {
                GeneralTaxMetricRow(
                    label = "50% del 4x1000 deducible en Renta (Art. 115 E.T.)",
                    value = "$${numberFormatter.format(intl.deductibleBankTaxFiftyPercent)} $cur"
                )
            }
            GeneralTaxMetricRow(
                label = "Base Gravable / Renta Líquida Estimada",
                value = "$${numberFormatter.format(intl.estimatedTaxableNetIncome)} $cur"
            )
            GeneralTaxMetricRow(
                label = "Impuesto sobre la Renta Anual Estimado",
                value = "$${numberFormatter.format(intl.estimatedAnnualIncomeTax)} $cur"
            )

            HorizontalDivider(color = BlueBorder)

            Surface(
                color = BlueCardSubtle,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Carga Fiscal Total (${"%.1f".format(intl.effectiveTaxPressurePercentage)}%)",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BlueDeepNavy
                    )
                    Text(
                        text = "$${numberFormatter.format(intl.totalEstimatedTaxBurden)} $cur",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BlueOcean
                    )
                }
            }
        }
    }
}

@Composable
private fun GeneralTaxMetricRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = BlueSteelText,
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = BlueDeepNavy
        )
    }
}

@Composable
private fun ThresholdProgressCard(
    threshold: DianThresholdStatus,
    numberFormatter: NumberFormat,
    currency: String,
    unitName: String
) {
    val barColor = when (threshold.alertLevel) {
        TaxAlertLevel.EXCEEDED -> BlueRoyalDark
        TaxAlertLevel.WARNING -> BlueOcean
        TaxAlertLevel.SAFE -> BluePrimary
    }

    val badgeLabel = when (threshold.alertLevel) {
        TaxAlertLevel.EXCEEDED -> "TOPE SUPERADO"
        TaxAlertLevel.WARNING -> "ZONA PREVENTIVA"
        TaxAlertLevel.SAFE -> "RANGO SEGURO"
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BlueSoftBg)
            .padding(14.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(threshold.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BlueDeepNavy)
                Text(threshold.legalReference, fontSize = 10.sp, color = BlueMutedText)
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${"%.1f".format(threshold.accumulatedUvt)} / ${threshold.limitUvt.toInt()} $unitName",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = barColor
                )
                Text(
                    text = badgeLabel,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueOcean
                )
            }
        }

        LinearProgressIndicator(
            progress = { (threshold.consumptionPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = BlueBorder,
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(4.dp))
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Acumulado: $${numberFormatter.format(threshold.accumulatedCop)} $currency (${"%.0f".format(threshold.consumptionPercentage)}%)",
                fontSize = 11.sp,
                color = BlueSteelText,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (threshold.remainingCopBeforeLimit > 0.0) {
                    "Disponible: $${numberFormatter.format(threshold.remainingCopBeforeLimit)} $currency"
                } else {
                    "Límite: $${numberFormatter.format(threshold.limitCop)} $currency"
                },
                fontSize = 11.sp,
                color = barColor,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun AccountGmfRow(
    status: AccountGmfMonthlyStatus,
    numberFormatter: NumberFormat,
    currency: String,
    unitName: String,
    onToggleExempt: (Boolean) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(BlueSoftBg)
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${status.accountName} (${status.bankName})",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BlueDeepNavy
                )
                Text(
                    text = when {
                        status.isMarkedTaxExempt350Uvt -> "Cuenta Exenta (${status.exemptLimitUvt.toInt()} $unitName/mes = $${numberFormatter.format(status.exemptLimitCop)} $currency)"
                        status.isLowAmountDigitalWallet65Uvt -> "Billetera Bajo Monto: Exenta ${status.exemptLimitUvt.toInt()} $unitName/mes ($${numberFormatter.format(status.exemptLimitCop)} $currency)"
                        else -> "Sin exención activa (Grava desde $1 $currency)"
                    },
                    fontSize = 11.sp,
                    color = BlueMutedText
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exenta", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = BlueRoyalDark)
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = status.isMarkedTaxExempt350Uvt,
                    onCheckedChange = onToggleExempt,
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = Color.White,
                        checkedTrackColor = BlueOcean,
                        uncheckedThumbColor = Color.White,
                        uncheckedTrackColor = BlueBorder
                    )
                )
            }
        }

        HorizontalDivider(color = BlueBorder)

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Salidas mes: $${numberFormatter.format(status.monthlyOutflowsCop)} $currency (${"%.1f".format(status.monthlyOutflowsUvt)} $unitName)",
                fontSize = 11.sp,
                color = BlueSteelText
            )
            Text(
                text = if (status.estimatedGmfTaxPaidCop > 0.0) {
                    "Impuesto causado: $${numberFormatter.format(status.estimatedGmfTaxPaidCop)} $currency"
                } else {
                    "Cupo libre: $${numberFormatter.format(status.remainingExemptCop)} $currency"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (status.estimatedGmfTaxPaidCop > 0.0) BlueRoyalDark else BluePrimary
            )
        }
    }
}
