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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import com.example.core.ui.components.IosStyleSwitch
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

@Composable
fun SettingsDashboardLayoutSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showNetWorth by remember { mutableStateOf(true) }
    var showHealthScore by remember { mutableStateOf(true) }
    var showMonthPulse by remember { mutableStateOf(true) }
    var showCashFlow by remember { mutableStateOf(true) }
    var showCategories by remember { mutableStateOf(true) }
    var showBankAccounts by remember { mutableStateOf(true) }
    var showRecentActivity by remember { mutableStateOf(true) }
    var showGoals by remember { mutableStateOf(true) }
    var showKashAi by remember { mutableStateOf(true) }

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
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Módulos Visibles en el Tablero", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)
                Text("Activa o desactiva las tarjetas para personalizar tu pantalla principal:", fontSize = 12.sp, color = Color(0xFF64748B))

                DashboardModuleToggleRow("Tu Posición (Patrimonio Neto)", showNetWorth) { showNetWorth = it }
                DashboardModuleToggleRow("Tu Salud Financiera (Score 66/100)", showHealthScore) { showHealthScore = it }
                DashboardModuleToggleRow("El Pulso de tu Mes (KPIs)", showMonthPulse) { showMonthPulse = it }
                DashboardModuleToggleRow("Flujo de Caja (Gráfica Bézier)", showCashFlow) { showCashFlow = it }
                DashboardModuleToggleRow("Gastos por Categoría (Donut)", showCategories) { showCategories = it }
                DashboardModuleToggleRow("Mis Cuentas Bancarias", showBankAccounts) { showBankAccounts = it }
                DashboardModuleToggleRow("Actividad Reciente (Transacciones)", showRecentActivity) { showRecentActivity = it }
                DashboardModuleToggleRow("Metas y Objetivos de Ahorro", showGoals) { showGoals = it }
                DashboardModuleToggleRow("Asistente KashAI RAG", showKashAi) { showKashAi = it }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { onShowMessage("Configuración de inicio guardada ✓") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Aplicar configuración de tablero", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun DashboardModuleToggleRow(
    title: String,
    checked: Boolean,
    modifier: Modifier = Modifier,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(title, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = KashNavy, modifier = Modifier.weight(1f))
        IosStyleSwitch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
        )
    }
}
