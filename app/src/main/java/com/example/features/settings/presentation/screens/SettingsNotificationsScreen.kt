package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Notifications & Financial Reminders Settings Screen.
 */
@Composable
fun SettingsNotificationsScreen(
    budgetAlerts: Boolean,
    paymentReminders: Boolean,
    weeklySummary: Boolean,
    onToggleBudgetAlerts: (Boolean) -> Unit,
    onTogglePaymentReminders: (Boolean) -> Unit,
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isWeeklyOn by remember { mutableStateOf(weeklySummary) }
    var isCardAlertOn by remember { mutableStateOf(paymentReminders) }
    var isOverdraftAlertOn by remember { mutableStateOf(true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Alertas Financieras Granulares", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alertas de Presupuesto por Sobres", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Avisar al superar el 80% del límite de la categoría", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = budgetAlerts,
                        onCheckedChange = {
                            onToggleBudgetAlerts(it)
                            onShowMessage(if (it) "Alertas de presupuesto activadas" else "Alertas de presupuesto desactivadas")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Fechas de Corte y Pago de Tarjetas", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Notificar 3 días antes de la fecha límite", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isCardAlertOn,
                        onCheckedChange = {
                            isCardAlertOn = it
                            onTogglePaymentReminders(it)
                            onShowMessage(if (it) "Recordatorios de tarjeta activados" else "Recordatorios de tarjeta desactivados")
                        },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Alertas de Protección Contra Sobregiro", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Avisar de inmediato si una cuenta bloquea un gasto por saldo insuficiente", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isOverdraftAlertOn,
                        onCheckedChange = { isOverdraftAlertOn = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Resumen Financiero Semanal", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Balance consolidado cada lunes a las 8:00 AM", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = isWeeklyOn,
                        onCheckedChange = { isWeeklyOn = it },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }
            }
        }

        // Test notification button
        Button(
            onClick = { onShowMessage("🔔 Notificación de prueba enviada: Tu saldo consolidado está al día.") },
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Icon(Icons.Default.NotificationsNone, contentDescription = null, tint = Color.White)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Probar envío de notificación push", fontWeight = FontWeight.Bold)
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
