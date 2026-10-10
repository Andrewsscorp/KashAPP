package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.StarOutline
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.features.settings.presentation.components.formatMoney
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

// ==========================================
// SUB-SCREEN: APARIENCIA
// ==========================================
@Composable
fun SettingsAppearanceSubScreen(
    currentTheme: String,
    privacyMode: Boolean,
    onSelectTheme: (String) -> Unit,
    onTogglePrivacyMode: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var fontScale by remember { mutableStateOf("Normal") }

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
                Text("Tema de la Interfaz", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("SYSTEM" to "Sistema", "LIGHT" to "Claro", "DARK" to "Oscuro").forEach { (code, label) ->
                        val isSelected = currentTheme.equals(code, ignoreCase = true)
                        FilterChip(
                            selected = isSelected,
                            onClick = { onSelectTheme(code) },
                            label = { Text(label, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = KashBlue,
                                selectedLabelColor = Color.White
                            )
                        )
                    }
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Modo Privacidad", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = KashNavy)
                        Text("Ocultar saldos con asteriscos (••••) en todo el tablero", fontSize = 12.sp, color = Color(0xFF64748B))
                    }
                    Switch(
                        checked = privacyMode,
                        onCheckedChange = { onTogglePrivacyMode() },
                        colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
                    )
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text("Escala de Tipografía", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = KashNavy)
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    listOf("Compacto", "Normal", "Grande").forEach { scale ->
                        val isSelected = fontScale == scale
                        FilterChip(
                            selected = isSelected,
                            onClick = { fontScale = scale },
                            label = { Text(scale) },
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

// ==========================================
// SUB-SCREEN: CATEGORÍAS
// ==========================================
@Composable
fun SettingsCategoriesSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTab by remember { mutableStateOf("EXPENSE") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCatName by remember { mutableStateOf("") }

    val expenseCategories = remember {
        mutableStateOf(
            listOf(
                "Alimentación & Supermercado" to "#EF4444",
                "Transporte & Combustible" to "#F97316",
                "Vivienda & Servicios" to "#0284C7",
                "Salud & Bienestar" to "#10B981",
                "Educación & Cursos" to "#6366F1",
                "Entretenimiento & Ocio" to "#EC4899",
                "Compras & Ropa" to "#8B5CF6",
                "Obligaciones Financieras" to "#E11D48"
            )
        )
    }

    val incomeCategories = remember {
        mutableStateOf(
            listOf(
                "Nómina & Sueldos" to "#10B981",
                "Honorarios & Servicios" to "#0284C7",
                "Rendimientos e Inversiones" to "#6366F1",
                "Ventas y Negocio" to "#F59E0B",
                "Reembolsos y Otros" to "#14B8A6"
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Button(
                onClick = { selectedTab = "EXPENSE" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "EXPENSE") Color(0xFFEF4444) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Gastos (${expenseCategories.value.size})",
                    color = if (selectedTab == "EXPENSE") Color.White else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }

            Button(
                onClick = { selectedTab = "INCOME" },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (selectedTab == "INCOME") Color(0xFF10B981) else Color(0xFFF1F5F9)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    "Ingresos (${incomeCategories.value.size})",
                    color = if (selectedTab == "INCOME") Color.White else Color(0xFF64748B),
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                val currentList = if (selectedTab == "EXPENSE") expenseCategories.value else incomeCategories.value

                currentList.forEach { (catName, colorHex) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(Color(android.graphics.Color.parseColor(colorHex)).copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = if (selectedTab == "EXPENSE") Icons.Default.TrendingDown else Icons.Default.TrendingUp,
                                contentDescription = null,
                                tint = Color(android.graphics.Color.parseColor(colorHex)),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = catName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy,
                            modifier = Modifier.weight(1f)
                        )
                        Surface(color = Color(0xFFDCFCE7), shape = RoundedCornerShape(6.dp)) {
                            Text("Activa ✓", fontSize = 10.sp, color = Color(0xFF166534), fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }

                Spacer(modifier = Modifier.height(4.dp))

                Button(
                    onClick = { showAddCategoryDialog = true },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Añadir nueva categoría", fontWeight = FontWeight.Bold)
                }
            }
        }
    }

    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = { Text("Nueva Categoría de ${if (selectedTab == "EXPENSE") "Gasto" else "Ingreso"}", fontWeight = FontWeight.Bold, color = KashNavy) },
            text = {
                OutlinedTextField(
                    value = newCatName,
                    onValueChange = { newCatName = it },
                    label = { Text("Nombre de la categoría") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 15.sp, fontWeight = FontWeight.SemiBold),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color(0xFF0F172A),
                        unfocusedTextColor = Color(0xFF0F172A),
                        focusedBorderColor = KashBlue,
                        unfocusedBorderColor = Color(0xFFCBD5E1),
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color(0xFFF8FAFC),
                        cursorColor = KashBlue
                    )
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCatName.isNotBlank()) {
                            if (selectedTab == "EXPENSE") {
                                expenseCategories.value = expenseCategories.value + (newCatName to "#0284C7")
                            } else {
                                incomeCategories.value = incomeCategories.value + (newCatName to "#10B981")
                            }
                            onShowMessage("Categoría \"$newCatName\" creada")
                            newCatName = ""
                            showAddCategoryDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue)
                ) {
                    Text("Guardar", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text("Cancelar", color = Color(0xFF64748B))
                }
            }
        )
    }
}

// ==========================================
// SUB-SCREEN: CUENTAS Y CONTROL DE SOBREGIRO
// ==========================================
data class AccountItemState(
    val id: String,
    val name: String,
    val mask: String,
    val balance: Double,
    val currency: String,
    val hasOverdraft: Boolean
)

@Composable
fun SettingsAccountsSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accounts = remember {
        mutableStateOf(
            listOf(
                AccountItemState("acc_bancolombia", "Bancolombia Principal", "•••• 4208", 4850000.0, "COP", hasOverdraft = false),
                AccountItemState("acc_nequi", "Nequi Billetera", "•••• 9831", 620000.0, "COP", hasOverdraft = false),
                AccountItemState("acc_davivienda", "Davivienda Nómina", "•••• 1120", 1250000.0, "COP", hasOverdraft = true),
                AccountItemState("acc_nu", "Cuenta Nu Ahorros", "•••• 7744", 3100000.0, "COP", hasOverdraft = false)
            )
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = Color(0xFFEFF6FF),
            border = BorderStroke(1.dp, Color(0xFFBFDBFE)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = KashBlue, modifier = Modifier.size(24.dp))
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Protección de Saldo Negativo (Sobregiro)", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Si una cuenta tiene el sobregiro desactivado, la aplicación bloqueará automáticamente cualquier gasto o transferencia que supere el saldo disponible.",
                        fontSize = 11.sp,
                        color = Color(0xFF475569),
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text("Mis Cuentas Bancarias y Monederos", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                accounts.value.forEachIndexed { index, acc ->
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color(0xFFE0F2FE)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.AccountBalance, contentDescription = null, tint = KashBlue, modifier = Modifier.size(20.dp))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(acc.name, fontWeight = FontWeight.Bold, fontSize = 14.sp, color = KashNavy)
                                Text("${acc.mask} • Saldo: ${formatMoney(acc.balance, acc.currency)}", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = if (acc.hasOverdraft) Color(0xFFFFFBEB) else Color(0xFFF8FAFC),
                            border = BorderStroke(1.dp, if (acc.hasOverdraft) Color(0xFFFDE68A) else Color(0xFFE2E8F0)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = if (acc.hasOverdraft) "Permite saldo negativo (Sobregiro activo)" else "Bloquear si saldo es negativo (Seguro)",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (acc.hasOverdraft) Color(0xFFB45309) else Color(0xFF334155)
                                    )
                                    Text(
                                        text = if (acc.hasOverdraft) "Puede debitar más del dinero disponible" else "Evita transacciones sin fondos suficientes",
                                        fontSize = 10.sp,
                                        color = Color(0xFF64748B)
                                    )
                                }
                                Switch(
                                    checked = acc.hasOverdraft,
                                    onCheckedChange = { isChecked ->
                                        accounts.value = accounts.value.toMutableList().also { list ->
                                            list[index] = acc.copy(hasOverdraft = isChecked)
                                        }
                                        onShowMessage(
                                            if (isChecked) "Sobregiro habilitado para ${acc.name}"
                                            else "Protección activa: ${acc.name} no aceptará saldos negativos"
                                        )
                                    },
                                    colors = SwitchDefaults.colors(
                                        checkedThumbColor = Color.White,
                                        checkedTrackColor = Color(0xFFD97706),
                                        uncheckedThumbColor = Color.White,
                                        uncheckedTrackColor = Color(0xFFCBD5E1)
                                    )
                                )
                            }
                        }
                    }

                    if (index < accounts.value.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN: TIENDA Y COMERCIOS
// ==========================================
@Composable
fun SettingsMerchantsSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val merchants = remember {
        mutableStateOf(
            listOf(
                "Supermercados Éxito" to "Mercado & Hogar",
                "Carulla FreshMarket" to "Alimentación Premium",
                "Uber & Didi" to "Movilidad Urbana",
                "Netflix & Spotify" to "Suscripciones Digitales",
                "EPM Servicios Públicos" to "Servicios Esenciales",
                "Tiendas D1 & Ara" to "Mercado Diario"
            )
        )
    }

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
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Comercios y Establecimientos Registrados", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                merchants.value.forEach { (name, cat) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFEF3C7)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.Storefront, contentDescription = null, tint = Color(0xFFD97706), modifier = Modifier.size(18.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(name, fontWeight = FontWeight.Bold, fontSize = 13.sp, color = KashNavy)
                            Text(cat, fontSize = 11.sp, color = Color(0xFF64748B))
                        }
                        Icon(Icons.Default.StarOutline, contentDescription = "Frecuente", tint = Color(0xFFD97706), modifier = Modifier.size(20.dp))
                    }
                    HorizontalDivider(color = Color(0xFFF8FAFC))
                }

                Button(
                    onClick = { onShowMessage("Comercio agregado a tus favoritos") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = KashBlue),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Registrar nuevo comercio", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN: PERSONALIZAR INICIO (DASHBOARD LAYOUT)
// ==========================================
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
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(checkedThumbColor = Color.White, checkedTrackColor = KashBlue)
        )
    }
}

// ==========================================
// SUB-SCREEN: AYUDA Y SOPORTE
// ==========================================
@Composable
fun SettingsHelpSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var expandedFaqIndex by remember { mutableStateOf<Int?>(null) }
    var searchQuery by remember { mutableStateOf("") }

    val faqs = listOf(
        "¿Cómo funcionan las transferencias entre cuentas?" to "Al registrar una transferencia, KashApp realiza un movimiento contable atómico en Room: debita el dinero de la cuenta origen y lo acredita inmediatamente en la cuenta destino, actualizando los saldos en tiempo real.",
        "¿Por qué se bloquea una transacción por sobregiro?" to "Si una cuenta tiene la opción de sobregiro desactivada, el sistema evita que el saldo quede en números rojos. Puedes permitir sobregiros ingresando en Ajustes → Cuentas y activando el switch correspondiente.",
        "¿Dónde se guardan mis datos?" to "KashApp opera bajo un modelo offline-first con SQLite Room ACID cifrado con SQLCipher AES-256. Tus datos permanecen en tu dispositivo de forma privada y segura.",
        "¿Cómo creo nuevas categorías?" to "Puedes ir a Ajustes → Categorías o tocar '+ Añadir nueva' al momento de registrar un gasto o ingreso."
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Buscar en preguntas frecuentes...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF64748B)) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            textStyle = TextStyle(color = Color(0xFF0F172A), fontSize = 14.sp, fontWeight = FontWeight.Medium),
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = Color(0xFF0F172A),
                unfocusedTextColor = Color(0xFF0F172A),
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White,
                focusedBorderColor = KashBlue,
                unfocusedBorderColor = Color(0xFFE2E8F0),
                cursorColor = KashBlue
            )
        )

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Preguntas Frecuentes", fontWeight = FontWeight.Bold, fontSize = 16.sp, color = KashNavy)

                faqs.forEachIndexed { index, (question, answer) ->
                    val isExpanded = expandedFaqIndex == index
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (isExpanded) Color(0xFFF8FAFC) else Color.Transparent)
                            .clickable { expandedFaqIndex = if (isExpanded) null else index }
                            .padding(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = question,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy,
                                modifier = Modifier.weight(1f)
                            )
                            Icon(
                                imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        if (isExpanded) {
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(text = answer, fontSize = 12.sp, color = Color(0xFF475569), lineHeight = 16.sp)
                        }
                    }
                    if (index < faqs.size - 1) {
                        HorizontalDivider(color = Color(0xFFF1F5F9))
                    }
                }
            }
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Canales Oficiales de Soporte", fontWeight = FontWeight.Bold, fontSize = 15.sp, color = KashNavy)

                Button(
                    onClick = { onShowMessage("Abriendo canal de soporte técnico...") },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Phone, contentDescription = null, tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("WhatsApp Soporte Oficial (+57 300 123 4567)", fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = { onShowMessage("Correo enviado a soporte@kashapp.co") },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Email, contentDescription = null, tint = KashNavy)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Enviar Correo Electrónico (soporte@kashapp.co)", color = KashNavy, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ==========================================
// SUB-SCREEN: ACERCA DE KASHAPP
// ==========================================
@Composable
fun SettingsAboutSubScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
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
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = KashBlue,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text("K", fontSize = 32.sp, fontWeight = FontWeight.Black, color = Color.White)
                    }
                }

                Text("KashApp Enterprise", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                Text("Versión 2.4.0 (Build 2026.10)", fontSize = 12.sp, color = Color(0xFF64748B))

                Surface(color = Color(0xFFE0F2FE), shape = RoundedCornerShape(8.dp)) {
                    Text("Room ACID Local Engine • Offline-First", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = KashBlue, modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp))
                }

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Text(
                    text = "Plataforma empresarial de gestión financiera, control de tesorería y seguimiento patrimonial diseñada con arquitectura limpia en Jetpack Compose M3.",
                    fontSize = 12.sp,
                    color = Color(0xFF475569),
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                HorizontalDivider(color = Color(0xFFF1F5F9))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Cifrado", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("AES-256 GCM Hardware Keystore", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                }

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text("Desarrollado para", fontSize = 12.sp, color = Color(0xFF64748B))
                    Text("Android 14 / 15+", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = KashNavy)
                }
            }
        }
    }
}

/**
 * Returns user-facing title for subroute.
 */
fun getSubRouteTitle(route: SettingsSubRoute): String {
    return when (route) {
        SettingsSubRoute.MAIN -> "Ajustes"
        SettingsSubRoute.MI_PERFIL -> "Mi perfil"
        SettingsSubRoute.SEGURIDAD -> "Seguridad"
        SettingsSubRoute.NOTIFICACIONES -> "Notificaciones"
        SettingsSubRoute.MONEDA_TRM -> "Moneda y TRM"
        SettingsSubRoute.CATEGORIAS -> "Categorías"
        SettingsSubRoute.CUENTAS_ENTIDADES -> "Cuentas y entidades"
        SettingsSubRoute.TIENDA -> "Tienda y Comercios"
        SettingsSubRoute.APARIENCIA -> "Apariencia"
        SettingsSubRoute.PERSONALIZAR_INICIO -> "Personalizar inicio"
        SettingsSubRoute.EXPORTAR_DATOS -> "Exportar datos"
        SettingsSubRoute.COPIA_SEGURIDAD -> "Copia de seguridad"
        SettingsSubRoute.AYUDA_SOPORTE -> "Ayuda y soporte"
        SettingsSubRoute.ACERCA_DE -> "Acerca de KashApp"
    }
}
