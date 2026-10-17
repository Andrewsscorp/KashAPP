package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.KashAppApplication
import com.example.features.settings.presentation.components.formatMoney
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch

@Composable
fun SettingsAccountsSubScreen(
    onShowMessage: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    userId: String = "user_default"
) {
    val context = LocalContext.current
    val appContainer = remember(context) { (context.applicationContext as? KashAppApplication)?.appContainer }
    val financialRepository = appContainer?.financialRepository
    val coroutineScope = rememberCoroutineScope()

    val accounts by (financialRepository?.getAccounts(userId) ?: flowOf(emptyList()))
        .collectAsStateWithLifecycle(initialValue = emptyList())

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

                if (accounts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = null,
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(44.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                "Sin cuentas registradas",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "No tienes cuentas bancarias registradas en la base de datos.",
                                fontSize = 12.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    accounts.forEachIndexed { index, acc ->
                        val mask = acc.accountNumberMasked ?: "•••• ${acc.id.takeLast(4)}"
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
                                    Text("$mask • Saldo: ${formatMoney(acc.balance, acc.currency)}", fontSize = 12.sp, color = Color(0xFF059669), fontWeight = FontWeight.SemiBold)
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
                                            coroutineScope.launch {
                                                financialRepository?.updateAccountAllowNegative(acc.id, isChecked)
                                                onShowMessage(
                                                    if (isChecked) "Sobregiro habilitado para ${acc.name}"
                                                    else "Protección activa: ${acc.name} no aceptará saldos negativos"
                                                )
                                            }
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

                        if (index < accounts.size - 1) {
                            HorizontalDivider(color = Color(0xFFF1F5F9), modifier = Modifier.padding(vertical = 4.dp))
                        }
                    }
                }
            }
        }
    }
}
