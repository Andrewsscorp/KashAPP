package com.example.features.settings.presentation.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

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
                Text("Versión 9.1.0 (Build 2026.10)", fontSize = 12.sp, color = Color(0xFF64748B))

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
        SettingsSubRoute.AUDITORIA -> "Bóveda de Auditoría"
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
