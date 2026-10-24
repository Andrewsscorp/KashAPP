package com.example.features.settings.presentation.screens

import android.app.TimePickerDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NightlightRound
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import com.example.core.ui.components.IosStyleSwitch
import com.example.feature.notificaciones.dispatcher.RichSystemNotificationDispatcher
import com.example.features.settings.domain.model.BudgetAlertPolicy
import com.example.features.settings.domain.model.NotificationSettingsConfig
import com.example.features.settings.domain.model.PaymentAndCardAlertPolicy
import com.example.features.settings.domain.model.PrivacyAndDeliveryPolicy
import com.example.features.settings.domain.model.SecurityAndAnomalyAlertPolicy
import com.example.features.settings.domain.model.TaxAndPatrimonyAlertPolicy
import com.example.features.settings.domain.model.WeeklyDigestPolicy
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Pantalla Empresarial de Notificaciones y Alertas Financieras (Estética iPhone Cupertino).
 * Incluye:
 * - Solución definitiva de contraste para el botón de prueba (#0F172A con contentColor = Color.White)
 * - Banner de diagnóstico de permisos del sistema operativo (POST_NOTIFICATIONS)
 * - Divulgación Progresiva (Progressive Disclosure) para Presupuestos, Tarjetas, DIAN, Patrimonio y Privacidad
 * - Despacho de notificaciones reales con enmascaramiento en pantalla de bloqueo (ISO 27001)
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SettingsNotificationsScreen(
    userId: String = "user_default",
    config: NotificationSettingsConfig = NotificationSettingsConfig(),
    onUpdateConfig: (NotificationSettingsConfig) -> Unit = {},
    budgetAlerts: Boolean = config.budgetPolicy.isEnabled,
    paymentReminders: Boolean = config.paymentPolicy.isEnabled,
    weeklySummary: Boolean = config.weeklyDigestPolicy.isEnabled,
    onToggleBudgetAlerts: (Boolean) -> Unit = {},
    onTogglePaymentReminders: (Boolean) -> Unit = {},
    onShowMessage: (String) -> Unit = {},
    onBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val dispatcher = remember(context) { RichSystemNotificationDispatcher(context) }

    var areSystemNotificationsEnabled by remember {
        mutableStateOf(NotificationManagerCompat.from(context).areNotificationsEnabled())
    }

    // Estados locales para expansión progresiva
    var expandBudgetDetails by remember { mutableStateOf(false) }
    var expandCardDetails by remember { mutableStateOf(false) }
    var expandWeeklyDetails by remember { mutableStateOf(false) }
    var expandQuietHoursDetails by remember { mutableStateOf(false) }

    // Sincronización de estado reactivo
    var currentConfig by remember(config, budgetAlerts, paymentReminders, weeklySummary) {
        mutableStateOf(
            config.copy(
                budgetPolicy = config.budgetPolicy.copy(isEnabled = budgetAlerts),
                paymentPolicy = config.paymentPolicy.copy(isEnabled = paymentReminders),
                weeklyDigestPolicy = config.weeklyDigestPolicy.copy(isEnabled = weeklySummary)
            )
        )
    }

    fun updateConfigAndPropagate(newConfig: NotificationSettingsConfig) {
        currentConfig = newConfig
        onUpdateConfig(newConfig)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // =====================================================================
        // BANNER DE SALUD DE PERMISOS DEL SISTEMA OPERATIVO
        // =====================================================================
        if (!areSystemNotificationsEnabled) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("notification_permission_warning_card")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .background(Color(0xFFFEE2E2), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.NotificationsOff,
                            contentDescription = null,
                            tint = Color(0xFFDC2626),
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Notificaciones del Sistema Desactivadas",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF991B1B)
                        )
                        Text(
                            text = "Para recibir alertas críticas de vencimiento de tarjetas y sobregiro, habilita los permisos de Android.",
                            fontSize = 11.sp,
                            color = Color(0xFFB91C1C)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Button(
                            onClick = {
                                val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                                    putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                                }
                                context.startActivity(intent)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.height(32.dp)
                        ) {
                            Text(
                                text = "Habilitar en Ajustes",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TARJETA 1: ALERTAS FINANCIERAS GRANULARES (CORE)
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                    Icon(
                        imageVector = Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = KashBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Alertas Financieras Granulares",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = KashNavy
                    )
                }

                // 1.1 Presupuestos por Sobres
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentConfig.budgetPolicy.isEnabled) {
                                        expandBudgetDetails = !expandBudgetDetails
                                    }
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Alertas de Presupuesto por Sobres",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashNavy
                                )
                                if (currentConfig.budgetPolicy.isEnabled) {
                                    Icon(
                                        imageVector = if (expandBudgetDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Avisar al superar el ${currentConfig.budgetPolicy.warningThresholdPercent}% del límite de la categoría",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IosStyleSwitch(
                            checked = currentConfig.budgetPolicy.isEnabled,
                            onCheckedChange = { enabled ->
                                val updated = currentConfig.copy(
                                    budgetPolicy = currentConfig.budgetPolicy.copy(isEnabled = enabled)
                                )
                                updateConfigAndPropagate(updated)
                                onToggleBudgetAlerts(enabled)
                                onShowMessage(if (enabled) "Alertas de presupuesto activadas" else "Alertas de presupuesto desactivadas")
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    // Divulgación progresiva: Slider de umbral de presupuesto
                    AnimatedVisibility(
                        visible = currentConfig.budgetPolicy.isEnabled && expandBudgetDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Sensibilidad del aviso preventivo:", fontSize = 11.sp, color = Color(0xFF475569))
                                    Text(
                                        "${currentConfig.budgetPolicy.warningThresholdPercent}%",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue
                                    )
                                }
                                Slider(
                                    value = currentConfig.budgetPolicy.warningThresholdPercent.toFloat(),
                                    onValueChange = { value ->
                                        val step = ((value / 5).toInt() * 5).coerceIn(50, 95)
                                        val updated = currentConfig.copy(
                                            budgetPolicy = currentConfig.budgetPolicy.copy(warningThresholdPercent = step)
                                        )
                                        updateConfigAndPropagate(updated)
                                    },
                                    valueRange = 50f..95f,
                                    steps = 8,
                                    colors = SliderDefaults.colors(
                                        thumbColor = KashBlue,
                                        activeTrackColor = KashBlue,
                                        inactiveTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Alerta crítica al alcanzar el 100%", fontSize = 11.sp, color = KashNavy)
                                    IosStyleSwitch(
                                        checked = currentConfig.budgetPolicy.notifyOnCritical100Percent,
                                        onCheckedChange = { active ->
                                            val updated = currentConfig.copy(
                                                budgetPolicy = currentConfig.budgetPolicy.copy(notifyOnCritical100Percent = active)
                                            )
                                            updateConfigAndPropagate(updated)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                                    )
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 1.2 Fechas de Corte y Pago de Tarjetas
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val daysStr = currentConfig.paymentPolicy.daysBeforeDueDate.sortedDescending().joinToString(", ") { "${it}d" }
                    val hourFormatted = "%02d:%02d".format(
                        currentConfig.paymentPolicy.preferredAlertHour,
                        currentConfig.paymentPolicy.preferredAlertMinute
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentConfig.paymentPolicy.isEnabled) {
                                        expandCardDetails = !expandCardDetails
                                    }
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Fechas de Corte y Pago de Tarjetas",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashNavy
                                )
                                if (currentConfig.paymentPolicy.isEnabled) {
                                    Icon(
                                        imageVector = if (expandCardDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Notificar $daysStr antes a las $hourFormatted",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IosStyleSwitch(
                            checked = currentConfig.paymentPolicy.isEnabled,
                            onCheckedChange = { enabled ->
                                val updated = currentConfig.copy(
                                    paymentPolicy = currentConfig.paymentPolicy.copy(isEnabled = enabled)
                                )
                                updateConfigAndPropagate(updated)
                                onTogglePaymentReminders(enabled)
                                onShowMessage(if (enabled) "Recordatorios de tarjeta activados" else "Recordatorios de tarjeta desactivados")
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    // Divulgación progresiva: Chips de días y hora
                    AnimatedVisibility(
                        visible = currentConfig.paymentPolicy.isEnabled && expandCardDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                Text("Días de anticipación para avisarte:", fontSize = 11.sp, color = Color(0xFF475569))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    val availableDays = listOf(7, 5, 3, 1, 0)
                                    availableDays.forEach { d ->
                                        val isSelected = d in currentConfig.paymentPolicy.daysBeforeDueDate
                                        val label = if (d == 0) "El día del corte" else "${d} días antes"
                                        FilterChip(
                                            selected = isSelected,
                                            onClick = {
                                                val currentDays = currentConfig.paymentPolicy.daysBeforeDueDate.toMutableSet()
                                                if (isSelected) {
                                                    if (currentDays.size > 1) currentDays.remove(d)
                                                } else {
                                                    currentDays.add(d)
                                                }
                                                val updated = currentConfig.copy(
                                                    paymentPolicy = currentConfig.paymentPolicy.copy(daysBeforeDueDate = currentDays)
                                                )
                                                updateConfigAndPropagate(updated)
                                            },
                                            label = { Text(label, fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = KashBlue,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hora del recordatorio:", fontSize = 11.sp, color = Color(0xFF475569))
                                    OutlinedButton(
                                        onClick = {
                                            TimePickerDialog(
                                                context,
                                                { _, hour, minute ->
                                                    val updated = currentConfig.copy(
                                                        paymentPolicy = currentConfig.paymentPolicy.copy(
                                                            preferredAlertHour = hour,
                                                            preferredAlertMinute = minute
                                                        )
                                                    )
                                                    updateConfigAndPropagate(updated)
                                                },
                                                currentConfig.paymentPolicy.preferredAlertHour,
                                                currentConfig.paymentPolicy.preferredAlertMinute,
                                                true
                                            ).show()
                                        },
                                        shape = RoundedCornerShape(8.dp),
                                        modifier = Modifier.height(32.dp)
                                    ) {
                                        Icon(Icons.Default.AccessTime, contentDescription = null, modifier = Modifier.size(14.dp))
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text(hourFormatted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        }
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 1.3 Protección Contra Sobregiro e IA
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Alertas de Protección Contra Sobregiro",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Text(
                                text = "Avisar de inmediato si una cuenta bloquea un gasto por saldo insuficiente",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IosStyleSwitch(
                            checked = currentConfig.securityPolicy.overdraftImmediateAlert,
                            onCheckedChange = { active ->
                                val updated = currentConfig.copy(
                                    securityPolicy = currentConfig.securityPolicy.copy(overdraftImmediateAlert = active)
                                )
                                updateConfigAndPropagate(updated)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Detección de gastos inusuales por IA",
                            fontSize = 12.sp,
                            color = Color(0xFF334155)
                        )
                        IosStyleSwitch(
                            checked = currentConfig.securityPolicy.mlUnusualExpenseAlert,
                            onCheckedChange = { active ->
                                val updated = currentConfig.copy(
                                    securityPolicy = currentConfig.securityPolicy.copy(mlUnusualExpenseAlert = active)
                                )
                                updateConfigAndPropagate(updated)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 1.4 Resumen Financiero Semanal
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    val dayNames = listOf("Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado", "Domingo")
                    val dayName = dayNames.getOrElse(currentConfig.weeklyDigestPolicy.dayOfWeekIso - 1) { "Lunes" }
                    val weeklyHourStr = "%02d:%02d".format(
                        currentConfig.weeklyDigestPolicy.hourOfDay,
                        currentConfig.weeklyDigestPolicy.minute
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentConfig.weeklyDigestPolicy.isEnabled) {
                                        expandWeeklyDetails = !expandWeeklyDetails
                                    }
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Resumen Financiero Semanal",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashNavy
                                )
                                if (currentConfig.weeklyDigestPolicy.isEnabled) {
                                    Icon(
                                        imageVector = if (expandWeeklyDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = "Balance consolidado cada $dayName a las $weeklyHourStr",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IosStyleSwitch(
                            checked = currentConfig.weeklyDigestPolicy.isEnabled,
                            onCheckedChange = { enabled ->
                                val updated = currentConfig.copy(
                                    weeklyDigestPolicy = currentConfig.weeklyDigestPolicy.copy(isEnabled = enabled)
                                )
                                updateConfigAndPropagate(updated)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    // Selector de día y hora del resumen
                    AnimatedVisibility(
                        visible = currentConfig.weeklyDigestPolicy.isEnabled && expandWeeklyDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Día de entrega del balance:", fontSize = 11.sp, color = Color(0xFF475569))
                                FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    dayNames.forEachIndexed { idx, name ->
                                        val iso = idx + 1
                                        FilterChip(
                                            selected = currentConfig.weeklyDigestPolicy.dayOfWeekIso == iso,
                                            onClick = {
                                                val updated = currentConfig.copy(
                                                    weeklyDigestPolicy = currentConfig.weeklyDigestPolicy.copy(dayOfWeekIso = iso)
                                                )
                                                updateConfigAndPropagate(updated)
                                            },
                                            label = { Text(name.take(3), fontSize = 11.sp) },
                                            colors = FilterChipDefaults.filterChipColors(
                                                selectedContainerColor = KashBlue,
                                                selectedLabelColor = Color.White
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // TARJETA 2: RADAR TRIBUTARIO (DIAN), PATRIMONIO Y TERCEROS
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                    Icon(
                        imageVector = Icons.Default.AccountBalance,
                        contentDescription = null,
                        tint = KashBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Radar Tributario (DIAN) & Patrimonio",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = KashNavy
                    )
                }

                // 2.1 Calendario Tributario DIAN y Topes UVT
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Calendario Tributario DIAN & UVT",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Text(
                            text = "Aviso a los 15, 7 y 1 día antes del vencimiento por NIT y al 85% de topes",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IosStyleSwitch(
                        checked = currentConfig.taxPolicy.dianTaxCalendarAlerts,
                        onCheckedChange = { active ->
                            val updated = currentConfig.copy(
                                taxPolicy = currentConfig.taxPolicy.copy(dianTaxCalendarAlerts = active)
                            )
                            updateConfigAndPropagate(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 2.2 Activos Fijos y Vencimiento de CDTs
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Activos Fijos y Vencimiento de CDTs",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Text(
                            text = "Alertas de depreciación mensual, mantenimientos y renovación de CDTs",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IosStyleSwitch(
                        checked = currentConfig.taxPolicy.cdtMaturityAlerts,
                        onCheckedChange = { active ->
                            val updated = currentConfig.copy(
                                taxPolicy = currentConfig.taxPolicy.copy(
                                    cdtMaturityAlerts = active,
                                    fixedAssetMaintenanceAndDepreciationAlerts = active
                                )
                            )
                            updateConfigAndPropagate(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 2.3 Gastos Compartidos y Deudas
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Gastos Compartidos y Deudas",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Text(
                            text = "Recordatorios automáticos de cobros a terceros y liquidaciones grupales",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IosStyleSwitch(
                        checked = currentConfig.taxPolicy.sharedDebtRemindersEnabled,
                        onCheckedChange = { active ->
                            val updated = currentConfig.copy(
                                taxPolicy = currentConfig.taxPolicy.copy(sharedDebtRemindersEnabled = active)
                            )
                            updateConfigAndPropagate(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }
            }
        }

        // =====================================================================
        // TARJETA 3: PRIVACIDAD EN PANTALLA DE BLOQUEO Y HORARIO SILENCIOSO (ISO 27001)
        // =====================================================================
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
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
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = KashBlue,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "Privacidad & Horario Silencioso (ISO 27001)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = KashNavy
                    )
                }

                // 3.1 Enmascaramiento de saldos en pantalla de bloqueo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Ocultar Saldos en Pantalla de Bloqueo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Text(
                            text = "Muestra '$ ••••••' en la pantalla de bloqueo hasta que desbloquees tu teléfono",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                    IosStyleSwitch(
                        checked = currentConfig.privacyPolicy.maskAmountsOnLockScreen,
                        onCheckedChange = { active ->
                            val updated = currentConfig.copy(
                                privacyPolicy = currentConfig.privacyPolicy.copy(maskAmountsOnLockScreen = active)
                            )
                            updateConfigAndPropagate(updated)
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                // 3.2 Horario Silencioso (No Molestar Financiero)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .clickable {
                                    if (currentConfig.privacyPolicy.quietHoursEnabled) {
                                        expandQuietHoursDetails = !expandQuietHoursDetails
                                    }
                                }
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = "Horario Silencioso (No Molestar)",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = KashNavy
                                )
                                if (currentConfig.privacyPolicy.quietHoursEnabled) {
                                    Icon(
                                        imageVector = if (expandQuietHoursDetails) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                        contentDescription = null,
                                        tint = Color(0xFF64748B),
                                        modifier = Modifier.size(18.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (currentConfig.privacyPolicy.quietHoursEnabled)
                                    "Silenciar notificaciones de ${currentConfig.privacyPolicy.quietStartHour}:00 a ${currentConfig.privacyPolicy.quietEndHour}:00"
                                else
                                    "Sin horario de silencio activo",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        IosStyleSwitch(
                            checked = currentConfig.privacyPolicy.quietHoursEnabled,
                            onCheckedChange = { enabled ->
                                val updated = currentConfig.copy(
                                    privacyPolicy = currentConfig.privacyPolicy.copy(quietHoursEnabled = enabled)
                                )
                                updateConfigAndPropagate(updated)
                            },
                            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                        )
                    }

                    // Selector de horas de silencio
                    AnimatedVisibility(
                        visible = currentConfig.privacyPolicy.quietHoursEnabled && expandQuietHoursDetails,
                        enter = fadeIn() + expandVertically(),
                        exit = fadeOut() + shrinkVertically()
                    ) {
                        Surface(
                            color = Color(0xFFF8FAFC),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Inicio: ${currentConfig.privacyPolicy.quietStartHour}:00", fontSize = 11.sp, color = Color(0xFF475569))
                                    Text("Fin: ${currentConfig.privacyPolicy.quietEndHour}:00", fontSize = 11.sp, color = Color(0xFF475569))
                                }
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Permitir siempre alertas críticas de sobregiro", fontSize = 11.sp, color = KashNavy)
                                    IosStyleSwitch(
                                        checked = currentConfig.privacyPolicy.allowCriticalSecurityInQuietHours,
                                        onCheckedChange = { allow ->
                                            val updated = currentConfig.copy(
                                                privacyPolicy = currentConfig.privacyPolicy.copy(allowCriticalSecurityInQuietHours = allow)
                                            )
                                            updateConfigAndPropagate(updated)
                                        },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // =====================================================================
        // BOTÓN PRINCIPAL CON CONTRASTE 100% GARANTIZADO Y DESPACHO REAL
        // =====================================================================
        Button(
            onClick = {
                dispatcher.dispatchTestNotification(
                    userId = userId,
                    maskOnLockScreen = currentConfig.privacyPolicy.maskAmountsOnLockScreen
                )
                onShowMessage("🔔 Notificación interactiva de prueba enviada a la barra de estado de Android")
            },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF0F172A),
                contentColor = Color.White
            ),
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .testTag("btn_send_test_notification")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = Icons.Default.NotificationsActive,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = "Enviar notificación de prueba",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

/**
 * Backward compatibility alias for SettingsNotificationsSubScreen
 */
@Composable
fun SettingsNotificationsSubScreen(
    budgetAlerts: Boolean,
    paymentReminders: Boolean,
    weeklySummary: Boolean,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onTogglePaymentReminders: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    SettingsNotificationsScreen(
        budgetAlerts = budgetAlerts,
        paymentReminders = paymentReminders,
        weeklySummary = weeklySummary,
        onToggleBudgetAlerts = onToggleBudgetAlerts,
        onTogglePaymentReminders = onTogglePaymentReminders,
        onShowMessage = onShowMessage,
        onBack = onBack,
        modifier = modifier
    )
}
