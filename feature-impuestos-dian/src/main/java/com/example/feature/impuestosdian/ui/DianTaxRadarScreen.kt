package com.example.feature.impuestosdian.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.impuestosdian.domain.model.AccountGmfMonthlyStatus
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.DianThresholdStatus
import com.example.feature.impuestosdian.domain.model.TaxAlertLevel
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import java.text.NumberFormat
import java.util.Locale

private val DianNavy = Color(0xFF0F172A)
private val DianBlue = Color(0xFF2563EB)
private val DianGreen = Color(0xFF16A34A)
private val DianAmber = Color(0xFFD97706)
private val DianRed = Color(0xFFDC2626)

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

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFFF1F5F9))
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 0. BARRA SUPERIOR CON NAVEGACIÓN Y SELECTOR MULTI-PAÍS
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onNavigateBack != null) {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver",
                            tint = DianNavy
                        )
                    }
                }
                Column {
                    Text(
                        text = "${jur.flagEmoji} Radar Tributario • ${jur.authorityAcronym}",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = DianNavy
                    )
                    Text(
                        text = "${jur.countryName} (${cur}) • Vinculado con Cuentas, Movimientos y Activos",
                        fontSize = 11.sp,
                        color = Color(0xFF64748B)
                    )
                }
            }

            IconButton(onClick = onOpenConfigureUvtDialog) {
                Icon(
                    imageVector = Icons.Default.Settings,
                    contentDescription = "Configuración Tributaria",
                    tint = DianBlue
                )
            }
        }

        // Selector rápido de Jurisdicción / País
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TaxJurisdiction.entries.forEach { item ->
                val selected = item == jur
                Surface(
                    color = if (selected) DianNavy else Color.White,
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, if (selected) DianNavy else Color(0xFFCBD5E1)),
                    modifier = Modifier.clickable { onSelectJurisdiction(item) }
                ) {
                    Text(
                        text = "${item.flagEmoji} ${item.countryName} (${item.authorityAcronym})",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (selected) Color.White else Color(0xFF334155),
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                    )
                }
            }
        }

        if (!statusMessage.isNullOrBlank()) {
            Surface(
                color = Color(0xFFECFDF5),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color(0xFF6EE7B7)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = statusMessage,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF065F46),
                    modifier = Modifier.padding(12.dp)
                )
            }
        }

        // 1. CABECERA EJECUTIVA: ESTADO DE OBLIGACIÓN DE DECLARAR RENTA Y UNIDAD TRIBUTARIA ACTIVA
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(22.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "RADAR TRIBUTARIO ${jur.authorityAcronym} • AÑO GRAVABLE ${report.fiscalYear}",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DianBlue
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (report.isObligatedToDeclareRenta) {
                                "Superaste ${report.exceededThresholdsCount} tope(s) para Declarar Renta"
                            } else if (report.warningThresholdsCount > 0) {
                                "${report.warningThresholdsCount} tope(s) en zona preventiva (>70%)"
                            } else {
                                "Por debajo de todos los topes (${jur.authorityAcronym})"
                            },
                            fontSize = 17.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = when {
                                report.isObligatedToDeclareRenta -> DianRed
                                report.warningThresholdsCount > 0 -> DianAmber
                                else -> DianGreen
                            }
                        )
                    }

                    Surface(
                        color = Color(0xFFEFF6FF),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "1 $unitName = $${numberFormatter.format(report.uvtValueCop)} $cur",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = DianBlue,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                Text(
                    text = report.estimatedFilingDeadlineDescription,
                    fontSize = 12.sp,
                    color = Color(0xFF475569)
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onExportPreRentaPdf,
                        colors = ButtonDefaults.buttonColors(containerColor = DianNavy),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Exportar Borrador Pre-Renta", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onOpenConfigureUvtDialog,
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Configurar País / $unitName", fontSize = 12.sp, color = DianNavy, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
        }

        // 2. MONITOR DE LOS 5 TOPES ANUALES EN UVT / UNIDAD FISCAL
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Gavel, contentDescription = null, tint = DianNavy, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "5 Topes Oficiales ${jur.authorityAcronym} en $unitName (${report.fiscalYear})",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DianNavy
                    )
                }

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

        // 3. MONITOR MENSUAL DEL 4x1000 / IMPUESTO BANCARIO POR CUENTA Y BILLETERA
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                        Icon(Icons.Default.AccountBalance, contentDescription = null, tint = DianBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (jur == TaxJurisdiction.CO_DIAN) {
                                    "Control Mensual del 4x1000 (${report.currentYearMonth})"
                                } else {
                                    "Impuesto Movimientos Bancarios (${report.currentYearMonth})"
                                },
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DianNavy
                            )
                            Text(
                                text = if (jur == TaxJurisdiction.CO_DIAN) {
                                    "Tope Exento: 350 UVT Banco • 65 UVT Billeteras (Nequi/DaviPlata)"
                                } else {
                                    "Tasa aplicada: ${"%.3f".format(report.configProfile.financialTransactionTaxRate * 100)}% • Sincronizado con Cuentas"
                                },
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    Surface(
                        color = if (report.totalMonthlyGmfTaxCop > 0.0) Color(0xFFFEF2F2) else Color(0xFFF0FDF4),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text(
                            text = "Mes: $${numberFormatter.format(report.totalMonthlyGmfTaxCop)} $cur",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (report.totalMonthlyGmfTaxCop > 0.0) DianRed else DianGreen,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                        )
                    }
                }

                Surface(
                    color = Color(0xFFF8FAFC),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = report.optimizationRecommendation,
                        fontSize = 12.sp,
                        color = Color(0xFF334155),
                        lineHeight = 17.sp,
                        modifier = Modifier.padding(12.dp)
                    )
                }

                if (report.accountGmfStatuses.isEmpty()) {
                    Text(
                        text = "No hay cuentas bancarias o billeteras activas registradas.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
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

        // 4. DEDUCCIÓN DEL 1% POR COMPRAS CON FACTURA ELECTRÓNICA (LEY 2277 DE 2022 / INTERNACIONAL)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            val fe = report.electronicInvoiceBenefit
            val pctLabel = "${"%.1f".format(report.configProfile.electronicInvoiceDeductionRate * 100)}%"
            Column(
                modifier = Modifier.padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.ReceiptLong, contentDescription = null, tint = DianGreen, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = if (jur == TaxJurisdiction.CO_DIAN) {
                                "Deducción 1% por Factura Electrónica (Ley 2277/2022)"
                            } else {
                                "Deducción por Gastos Soportados con Factura ($pctLabel)"
                            },
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DianNavy
                        )
                        Text(
                            text = "Límite anual: ${fe.maxAllowedDeductionUvt.toInt()} $unitName ($${numberFormatter.format(fe.maxAllowedDeductionCop)} $cur)",
                            fontSize = 11.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Compras con soporte (${fe.eligibleTransactionsCount})", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text("$${numberFormatter.format(fe.eligiblePurchasesTotalCop)} $cur", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = DianNavy)
                    }
                    Column(horizontalAlignment = Alignment.End) {
                        Text("Deducción acumulada ($pctLabel)", fontSize = 11.sp, color = Color(0xFF64748B))
                        Text(
                            text = "$${numberFormatter.format(fe.appliedDeductionCop)} $cur (${"%.1f".format(fe.deductionOnePercentUvt)} $unitName)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DianGreen
                        )
                    }
                }
            }
        }

        // 5. IMPUESTOS GENERALES INTERNACIONALES (IVA / IGV / SALES TAX, RETENCIÓN E IMPUESTO DE RENTA)
        report.internationalTaxSummary?.let { intl ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color.White),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Public, contentDescription = null, tint = DianBlue, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "Impuestos Generales y Carga Fiscal (${jur.flagEmoji} ${jur.countryName})",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = DianNavy
                            )
                            Text(
                                text = "Estimación integral de IVA/Sales Tax, Retenciones, Renta y Presión Fiscal",
                                fontSize = 11.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                    }

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    GeneralTaxMetricRow(
                        label = "IVA / IGV / Sales Tax estimado en compras (${"%.1f".format(intl.vatRateApplied * 100)}%)",
                        value = "$${numberFormatter.format(intl.estimatedVatOrSalesTaxPaid)} $cur"
                    )
                    GeneralTaxMetricRow(
                        label = "Retención estimada s/ rendimientos de inversión y CDTs",
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

                    HorizontalDivider(color = Color(0xFFE2E8F0))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Carga Fiscal Total Estimada (${"%.1f".format(intl.effectiveTaxPressurePercentage)}%)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DianNavy
                        )
                        Text(
                            text = "$${numberFormatter.format(intl.totalEstimatedTaxBurden)} $cur",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = DianBlue
                        )
                    }
                }
            }
        }

        // Descargo legal Ley 43 de 1990 (Junta Central de Contadores)
        Text(
            text = "Nota Legal (Ley 43 de 1990): Este módulo es una herramienta gerencial e informativa de planeación tributaria desarrollada por Andrés Suárez. No constituye asesoría tributaria oficial ni sustituye la liquidación firmada por un Contador Público.",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8),
            lineHeight = 15.sp,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 6.dp)
        )
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
            color = Color(0xFF475569),
            modifier = Modifier.weight(1f)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = value,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = DianNavy
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
        TaxAlertLevel.EXCEEDED -> DianRed
        TaxAlertLevel.WARNING -> DianAmber
        TaxAlertLevel.SAFE -> DianGreen
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF8FAFC))
            .padding(12.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(threshold.title, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = DianNavy)
                Text(threshold.legalReference, fontSize = 10.sp, color = Color(0xFF64748B))
            }
            Text(
                text = "${"%.1f".format(threshold.accumulatedUvt)} / ${threshold.limitUvt.toInt()} $unitName",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = barColor
            )
        }

        LinearProgressIndicator(
            progress = { (threshold.consumptionPercentage.toFloat() / 100f).coerceIn(0f, 1f) },
            color = barColor,
            trackColor = Color(0xFFE2E8F0),
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
                color = Color(0xFF334155),
                fontWeight = FontWeight.Medium
            )
            Text(
                text = if (threshold.remainingCopBeforeLimit > 0.0) {
                    "Margen libre: $${numberFormatter.format(threshold.remainingCopBeforeLimit)} $currency"
                } else {
                    "Tope superado ($${numberFormatter.format(threshold.limitCop)} $currency)"
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
            .background(Color(0xFFF8FAFC))
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
                    color = DianNavy
                )
                Text(
                    text = when {
                        status.isMarkedTaxExempt350Uvt -> "Cuenta Exenta (${status.exemptLimitUvt.toInt()} $unitName/mes = $${numberFormatter.format(status.exemptLimitCop)} $currency)"
                        status.isLowAmountDigitalWallet65Uvt -> "Billetera Bajo Monto: Exenta ${status.exemptLimitUvt.toInt()} $unitName/mes ($${numberFormatter.format(status.exemptLimitCop)} $currency)"
                        else -> "Sin exención activa (Grava desde $1 $currency)"
                    },
                    fontSize = 11.sp,
                    color = Color(0xFF64748B)
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Exenta", fontSize = 10.sp, color = Color(0xFF475569))
                Spacer(modifier = Modifier.width(6.dp))
                Switch(
                    checked = status.isMarkedTaxExempt350Uvt,
                    onCheckedChange = onToggleExempt
                )
            }
        }

        HorizontalDivider(color = Color(0xFFE2E8F0))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = "Salidas mes: $${numberFormatter.format(status.monthlyOutflowsCop)} $currency (${"%.1f".format(status.monthlyOutflowsUvt)} $unitName)",
                fontSize = 11.sp,
                color = Color(0xFF334155)
            )
            Text(
                text = if (status.estimatedGmfTaxPaidCop > 0.0) {
                    "Impuesto causado: $${numberFormatter.format(status.estimatedGmfTaxPaidCop)} $currency"
                } else {
                    "Cupo libre: $${numberFormatter.format(status.remainingExemptCop)} $currency"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = if (status.estimatedGmfTaxPaidCop > 0.0) DianRed else DianGreen
            )
        }
    }
}
