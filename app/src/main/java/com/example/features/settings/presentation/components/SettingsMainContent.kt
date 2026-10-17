package com.example.features.settings.presentation.components

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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudQueue
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.NotificationsNone
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.PersonOutline
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.ViewQuilt
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.features.settings.domain.model.SettingsSubRoute
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Main Settings Menu list showing Profile Hero, Account, Finance, Preferences, Data, and Logout.
 */
@Composable
fun SettingsMainContent(
    userName: String,
    userEmail: String,
    onNavigateTo: (SettingsSubRoute) -> Unit,
    onRequestLogout: () -> Unit,
    onRequestDeleteAccount: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Top User Profile Quick Card
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = Color.White,
            border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
            shadowElevation = 1.dp,
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onNavigateTo(SettingsSubRoute.MI_PERFIL) }
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.linearGradient(
                                listOf(Color(0xFF0284C7), Color(0xFF0369A1))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = userName.split(" ").mapNotNull { it.firstOrNull()?.toString() }.take(2).joinToString("").ifEmpty { "AF" },
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = userName,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFFDCFCE7)
                        ) {
                            Text(
                                text = "PRO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF166534),
                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 1.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = userEmail,
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Toca para ver o editar tu perfil empresarial",
                        fontSize = 10.sp,
                        color = Color(0xFF0284C7),
                        fontWeight = FontWeight.Medium
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 1. CUENTA
        SettingsSection(title = "CUENTA") {
            SettingsTileItem(
                icon = Icons.Default.PersonOutline,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE),
                title = "Mi perfil",
                subtitle = "Nombre, correo y datos personales.",
                onClick = { onNavigateTo(SettingsSubRoute.MI_PERFIL) },
                testTag = "settings_tile_mi_perfil"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.Security,
                iconTint = Color(0xFF059669),
                iconBg = Color(0xFFDCFCE7),
                title = "Seguridad",
                subtitle = "Bloqueo biométrico, PIN y sesiones activas.",
                onClick = { onNavigateTo(SettingsSubRoute.SEGURIDAD) },
                testTag = "settings_tile_seguridad"
            )
        }

        // 2. FINANZAS
        SettingsSection(title = "FINANZAS") {
            SettingsTileItem(
                icon = Icons.Default.NotificationsNone,
                iconTint = Color(0xFFEA580C),
                iconBg = Color(0xFFFFEDD5),
                title = "Notificaciones",
                subtitle = "Alertas de presupuesto y recordatorios de pago.",
                onClick = { onNavigateTo(SettingsSubRoute.NOTIFICACIONES) },
                testTag = "settings_tile_notificaciones"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.MonetizationOn,
                iconTint = Color(0xFF16A34A),
                iconBg = Color(0xFFDCFCE7),
                title = "Moneda y TRM",
                subtitle = "Divisa base y calculadora de tasas de cambio.",
                onClick = { onNavigateTo(SettingsSubRoute.MONEDA_TRM) },
                testTag = "settings_tile_moneda_trm"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.Tag,
                iconTint = Color(0xFF7C3AED),
                iconBg = Color(0xFFEDE9FE),
                title = "Categorías",
                subtitle = "Personaliza tus categorías de gastos e ingresos.",
                onClick = { onNavigateTo(SettingsSubRoute.CATEGORIAS) },
                testTag = "settings_tile_categorias"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.AccountBalance,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE),
                title = "Cuentas y entidades",
                subtitle = "Gestión bancaria y control de sobregiro.",
                onClick = { onNavigateTo(SettingsSubRoute.CUENTAS_ENTIDADES) },
                testTag = "settings_tile_cuentas_entidades"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.Storefront,
                iconTint = Color(0xFFD97706),
                iconBg = Color(0xFFFEF3C7),
                title = "Tienda y Comercios",
                subtitle = "Establecimientos habituales y comercios.",
                onClick = { onNavigateTo(SettingsSubRoute.TIENDA) },
                testTag = "settings_tile_tienda"
            )
        }

        // 3. PREFERENCIAS
        SettingsSection(title = "PREFERENCIAS") {
            SettingsTileItem(
                icon = Icons.Default.Palette,
                iconTint = Color(0xFF9333EA),
                iconBg = Color(0xFFF3E8FF),
                title = "Apariencia",
                subtitle = "Tema claro/oscuro y modo privacidad.",
                onClick = { onNavigateTo(SettingsSubRoute.APARIENCIA) },
                testTag = "settings_tile_apariencia"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.ViewQuilt,
                iconTint = Color(0xFF0284C7),
                iconBg = Color(0xFFE0F2FE),
                title = "Personalizar inicio",
                subtitle = "Activa u oculta módulos del tablero principal.",
                onClick = { onNavigateTo(SettingsSubRoute.PERSONALIZAR_INICIO) },
                testTag = "settings_tile_personalizar_inicio"
            )
        }

        // 4. DATOS
        SettingsSection(title = "DATOS") {
            SettingsTileItem(
                icon = Icons.Default.Upload,
                iconTint = Color(0xFF2563EB),
                iconBg = Color(0xFFDBEAFE),
                title = "Exportar datos",
                subtitle = "Descarga reportes en CSV, Excel, PDF y JSON.",
                onClick = { onNavigateTo(SettingsSubRoute.EXPORTAR_DATOS) },
                testTag = "settings_tile_exportar_datos"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.CloudQueue,
                iconTint = Color(0xFF0891B2),
                iconBg = Color(0xFFCFFAFE),
                title = "Copia de seguridad",
                subtitle = "Respaldo y restauración de base de datos Room ACID.",
                onClick = { onNavigateTo(SettingsSubRoute.COPIA_SEGURIDAD) },
                testTag = "settings_tile_copia_seguridad"
            )
        }

        // 5. AYUDA
        SettingsSection(title = "AYUDA") {
            SettingsTileItem(
                icon = Icons.Default.HelpOutline,
                iconTint = Color(0xFF64748B),
                iconBg = Color(0xFFF1F5F9),
                title = "Ayuda y soporte",
                subtitle = "Preguntas frecuentes, tutoriales y soporte oficial.",
                onClick = { onNavigateTo(SettingsSubRoute.AYUDA_SOPORTE) },
                testTag = "settings_tile_ayuda_soporte"
            )
            HorizontalDivider(color = Color(0xFFF8FAFC), thickness = 1.dp)
            SettingsTileItem(
                icon = Icons.Default.Info,
                iconTint = Color(0xFF475569),
                iconBg = Color(0xFFF1F5F9),
                title = "Acerca de KashApp",
                subtitle = "Versión v9.1.0, términos legales y privacidad.",
                onClick = { onNavigateTo(SettingsSubRoute.ACERCA_DE) },
                testTag = "settings_tile_acerca_de"
            )
        }

        // 6. ACCIÓN CRÍTICA: CERRAR SESIÓN
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF1F2)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFECDD3)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRequestLogout() }
                .testTag("settings_tile_cerrar_sesion")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.ExitToApp,
                        contentDescription = "Cerrar sesión",
                        tint = Color(0xFFEF4444),
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Cerrar sesión",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFEF4444)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Salir de tu cuenta actual.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFF94A3B8),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 7. ZONA DE PELIGRO: ELIMINAR CUENTA (ESTÁNDAR GOOGLE PLAY)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFFFEF2F2)),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFCA5A5)),
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onRequestDeleteAccount() }
                .testTag("settings_tile_eliminar_cuenta")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFFEE2E2)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.DeleteForever,
                        contentDescription = "Eliminar cuenta permanentemente",
                        tint = Color(0xFFDC2626),
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Eliminar cuenta",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFDC2626)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = "Borrar permanentemente tus datos en la nube y en el dispositivo.",
                        fontSize = 12.sp,
                        color = Color(0xFF64748B)
                    )
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color(0xFFDC2626),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

/**
 * Settings bottom navigation bar
 */
@Composable
fun SettingsBottomNavBar(
    onHomeClick: () -> Unit,
    onTransactionsClick: () -> Unit,
    onFabClick: () -> Unit,
    onSummaryClick: () -> Unit,
    onSettingsClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            BottomNavActionItem(label = "Inicio", icon = Icons.Default.Home, isSelected = false, onClick = onHomeClick)
            BottomNavActionItem(label = "Movimientos", icon = Icons.Default.SwapHoriz, isSelected = false, onClick = onTransactionsClick)
            
            // Central action button
            Surface(
                shape = CircleShape,
                color = KashBlue,
                shadowElevation = 4.dp,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable { onFabClick() }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Nuevo movimiento", tint = Color.White, modifier = Modifier.size(24.dp))
                }
            }

            BottomNavActionItem(label = "Resumen", icon = Icons.Default.TableChart, isSelected = false, onClick = onSummaryClick)
            BottomNavActionItem(label = "Ajustes", icon = Icons.Default.Settings, isSelected = true, onClick = onSettingsClick)
        }
    }
}

@Composable
fun BottomNavActionItem(
    label: String,
    icon: ImageVector,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (isSelected) KashBlue else Color(0xFF94A3B8),
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            color = if (isSelected) KashBlue else Color(0xFF94A3B8)
        )
    }
}
