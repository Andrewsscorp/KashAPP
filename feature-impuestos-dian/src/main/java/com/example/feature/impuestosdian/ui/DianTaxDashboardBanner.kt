package com.example.feature.impuestosdian.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Gavel
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import com.example.core.ui.components.IosStyleSwitch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.feature.impuestosdian.domain.model.DianTaxRadarReport
import com.example.feature.impuestosdian.domain.model.GlobalTaxJurisdictionCatalog
import com.example.feature.impuestosdian.domain.model.TaxConfigurationProfile
import java.text.NumberFormat
import java.util.Locale

@Composable
fun DianTaxRadarInteractiveContainer(
    viewModel: DianTaxViewModel,
    onNavigateBack: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val report = state.report

    if (state.isLoading || report == null) {
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFF2563EB))
        }
        return
    }

    DianTaxRadarScreen(
        report = report,
        onToggleAccountTaxExempt = { accId, exempt -> viewModel.toggleAccountTaxExempt(accId, exempt) },
        onExportPreRentaPdf = { viewModel.exportPreRentaPdf() },
        onOpenConfigureUvtDialog = { viewModel.openConfigDialog() },
        onSelectJurisdiction = { jur -> viewModel.selectJurisdictionQuick(jur) },
        onNavigateBack = onNavigateBack,
        statusMessage = state.statusMessage,
        modifier = modifier
    )

    if (state.isConfigDialogOpen) {
        TaxConfigurationDialog(
            initialProfile = report.configProfile,
            onDismiss = { viewModel.closeConfigDialog() },
            onSave = { updated -> viewModel.saveTaxConfiguration(updated) }
        )
    }
}

@Composable
fun DianTaxDashboardBanner(
    report: DianTaxRadarReport?,
    onClickOpenRadar: () -> Unit,
    modifier: Modifier = Modifier
) {
    val jur = report?.configProfile?.jurisdiction
    val numberFormatter = NumberFormat.getNumberInstance(Locale("es", "CO")).apply {
        maximumFractionDigits = 0
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(1.dp, Color(0xFF334155)),
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClickOpenRadar() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    color = Color(0xFF1E293B),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Gavel,
                        contentDescription = null,
                        tint = Color(0xFF38BDF8),
                        modifier = Modifier
                            .padding(10.dp)
                            .size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = if (jur != null) {
                            "${jur.flagEmoji} Radar Tributario ${jur.authorityAcronym} • ${report.fiscalYear}"
                        } else {
                            "🇨🇴 Radar Tributario DIAN & Impuestos Internacionales"
                        },
                        fontSize = 14.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White
                    )
                    Text(
                        text = if (report != null) {
                            val statusText = when {
                                report.isObligatedToDeclareRenta -> "${report.exceededThresholdsCount} tope(s) superado(s)"
                                report.warningThresholdsCount > 0 -> "${report.warningThresholdsCount} tope(s) en alerta (>70%)"
                                else -> "Topes en rango seguro"
                            }
                            "$statusText • Imp. bancario mes: $${numberFormatter.format(report.totalMonthlyGmfTaxCop)} ${report.configProfile.currency}"
                        } else {
                            "Control de 5 topes UVT, 4x1000 (350/65 UVT), 1% Factura Electrónica e IVA multi-país"
                        },
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Abrir Radar Tributario",
                tint = Color(0xFF38BDF8)
            )
        }
    }
}

/**
 * Componente reutilizable para incrustar en la Creación/Edición de Cuentas (`CreateAccountModal`),
 * vinculado automáticamente con el perfil del Módulo de Configuración Tributaria.
 */
@Composable
fun AccountCreationTaxLinkCard(
    profile: TaxConfigurationProfile,
    accountType: String,
    isTaxExempt: Boolean,
    onTaxExemptChange: (Boolean) -> Unit,
    onOpenTaxConfig: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val hint = GlobalTaxJurisdictionCatalog.getAccountCreationTaxGuidance(
        profile = profile,
        accountType = accountType,
        isMarkedTaxExempt = isTaxExempt
    )

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8FAFC)),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = Color(0xFF2563EB),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "${profile.jurisdiction.flagEmoji} ${hint.switchLabel}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF0F172A)
                        )
                        Text(
                            text = "Vinculado con Radar Tributario (${profile.jurisdiction.authorityAcronym} • 1 ${profile.taxUnitName} = ${profile.taxUnitValue.toInt()} ${profile.currency})",
                            fontSize = 10.sp,
                            color = Color(0xFF2563EB)
                        )
                    }
                }

                IosStyleSwitch(
                    checked = isTaxExempt,
                    onCheckedChange = onTaxExemptChange
                )
            }

            Text(
                text = hint.switchSubtitle,
                fontSize = 11.sp,
                color = Color(0xFF475569),
                lineHeight = 15.sp
            )

            if (onOpenTaxConfig != null) {
                TextButton(onClick = onOpenTaxConfig) {
                    Text(
                        text = "Cambiar país o configurar parámetros tributarios",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF2563EB)
                    )
                }
            }
        }
    }
}
