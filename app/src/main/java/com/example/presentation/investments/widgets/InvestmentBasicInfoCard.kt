package com.example.presentation.investments.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apartment
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CurrencyBitcoin
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Handshake
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.investments.state.InvestmentFormState
import com.example.presentation.investments.state.InvestmentKeyboardTarget
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy
import java.util.Locale

private data class InvestmentTypeItem(
    val id: String,
    val name: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)

private val INVESTMENT_TYPES = listOf(
    InvestmentTypeItem("ACCIONES", "Acciones", Icons.Default.ShowChart),
    InvestmentTypeItem("CRIPTO", "Criptomonedas", Icons.Default.CurrencyBitcoin),
    InvestmentTypeItem("BIENES_RAICES", "Bienes Raíces", Icons.Default.Apartment),
    InvestmentTypeItem("FONDOS_FIC", "Fondos FIC / ETF", Icons.Default.TrendingUp),
    InvestmentTypeItem("PRESTAMO", "Préstamo a terceros", Icons.Default.Handshake),
    InvestmentTypeItem("STARTUPS", "Startups / Venture", Icons.Default.RocketLaunch),
    InvestmentTypeItem("OTROS", "Otro Activo", Icons.Default.AttachMoney)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InvestmentBasicInfoCard(
    state: InvestmentFormState,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onNameChanged: (String) -> Unit,
    onTypeSelected: (String, String, String) -> Unit,
    onInstitutionSelected: (String?, String) -> Unit,
    onOpenCreateInstitution: () -> Unit,
    onCurrencySelected: (String) -> Unit,
    onAcquisitionDateSelected: (String) -> Unit,
    onStatusSelected: (String) -> Unit,
    onOpenKeyboard: (InvestmentKeyboardTarget) -> Unit,
    onQuickAdd: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInitialAmountActive = state.activeKeyboardTarget is InvestmentKeyboardTarget.InitialAmount

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("investment_basic_info_card"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = BorderStroke(1.dp, Color(0xFFE2E8F0)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header Accordion
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE0F2FE)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Información básica",
                            tint = KashBlue,
                            modifier = Modifier.size(22.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = "1. Información básica",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = KashNavy
                        )
                        Text(
                            text = "${state.name.ifBlank { "Sin nombre" }} • ${state.typeName} (${state.currencyCode})",
                            fontSize = 12.sp,
                            color = Color(0xFF64748B),
                            modifier = Modifier.basicMarquee()
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (isExpanded) "Colapsar" else "Expandir",
                    tint = Color(0xFF64748B)
                )
            }

            // Expanded Body
            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // 1. Nombre de la inversión
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Nombre de la inversión *",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )
                        Text(
                            text = "${state.name.length}/100",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (state.name.length >= 90) Color(0xFFEF4444) else Color(0xFF64748B)
                        )
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    OutlinedTextField(
                        value = state.name,
                        onValueChange = onNameChanged,
                        placeholder = { Text("Ej. Acciones Ecopetrol, Bitcoin Vault, Apartamento 302", fontSize = 13.sp, color = Color(0xFF94A3B8)) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("investment_name_input"),
                        shape = RoundedCornerShape(12.dp),
                        singleLine = true,
                        textStyle = TextStyle(
                            color = KashNavy,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = KashNavy,
                            unfocusedTextColor = KashNavy,
                            focusedBorderColor = KashBlue,
                            unfocusedBorderColor = Color(0xFFCBD5E1),
                            focusedContainerColor = Color(0xFFF8FAFC),
                            unfocusedContainerColor = Color(0xFFF8FAFC),
                            cursorColor = KashBlue
                        )
                    )

                    // Dynamic Marquee Preview if long text
                    if (state.name.length > 25) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Vista previa marquesina: ${state.name}",
                            fontSize = 11.sp,
                            color = KashBlue,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .fillMaxWidth()
                                .basicMarquee()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // REGLA SOLICITADA: "las cajas de valor siempre van despues de del nombre para mas velocidad"
                    Text(
                        text = "Monto inicial invertido (Costo base) *",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onOpenKeyboard(InvestmentKeyboardTarget.InitialAmount) }
                            .testTag("investment_initial_amount_input"),
                        color = if (isInitialAmountActive) Color(0xFFEFF6FF) else Color(0xFFF8FAFC),
                        border = BorderStroke(
                            width = if (isInitialAmountActive) 2.dp else 1.dp,
                            color = if (isInitialAmountActive) KashBlue else Color(0xFFCBD5E1)
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 14.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Toca para abrir teclado de inversión",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFF64748B)
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "$ ${String.format(Locale.GERMANY, "%,.0f", state.initialAmount)} ${state.currencyCode}",
                                    fontSize = 22.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = KashNavy
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(if (isInitialAmountActive) KashBlue else Color(0xFFE2E8F0)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Editar monto",
                                    tint = if (isInitialAmountActive) Color.White else Color(0xFF475569),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Preset quick amount chips: +$500k, +$1M, +$5M, +$10M
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            500000.0 to "+$500k",
                            1000000.0 to "+$1M",
                            5000000.0 to "+$5M",
                            10000000.0 to "+$10M"
                        ).forEach { (amt, label) ->
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        onOpenKeyboard(InvestmentKeyboardTarget.InitialAmount)
                                        onQuickAdd(amt)
                                    }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = KashBlue
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 2. Tipo de inversión
                    Text(
                        text = "Tipo de inversión",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        INVESTMENT_TYPES.forEach { item ->
                            val isSelected = state.typeId == item.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFE2E8F0)),
                                modifier = Modifier.clickable {
                                    onTypeSelected(item.id, item.name, "show_chart")
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = item.icon,
                                        contentDescription = item.name,
                                        tint = if (isSelected) KashBlue else Color(0xFF64748B),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = item.name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) KashBlue else Color(0xFF334155)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 3. Entidad / Plataforma (MDM) + Botón Crear Nueva Entidad
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Entidad / Plataforma (MDM)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = KashNavy
                        )

                        OutlinedButton(
                            onClick = onOpenCreateInstitution,
                            shape = RoundedCornerShape(8.dp),
                            border = BorderStroke(1.dp, KashBlue),
                            modifier = Modifier.height(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Nueva Entidad",
                                tint = KashBlue,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Nueva plataforma",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashBlue
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Pre-seeded + custom banks/platforms from MDM
                        val platforms = if (state.availableInstitutions.isNotEmpty()) {
                            state.availableInstitutions.map { it.id to it.name }
                        } else {
                            listOf(
                                "inst_1" to "Bancolombia",
                                "inst_2" to "Trii",
                                "inst_3" to "Binance",
                                "inst_4" to "Hapi",
                                "inst_5" to "Tyba",
                                "inst_6" to "Davivienda",
                                "inst_7" to "Skandia"
                            )
                        }

                        platforms.forEach { (id, name) ->
                            val isSelected = state.institutionId == id || state.institutionName == name
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) KashNavy else Color(0xFFF1F5F9),
                                border = BorderStroke(1.dp, if (isSelected) KashNavy else Color(0xFFCBD5E1)),
                                modifier = Modifier.clickable { onInstitutionSelected(id, name) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    if (isSelected) {
                                        Icon(
                                            imageVector = Icons.Default.Check,
                                            contentDescription = "Seleccionado",
                                            tint = Color.White,
                                            modifier = Modifier.size(14.dp)
                                        )
                                        Spacer(modifier = Modifier.width(4.dp))
                                    }
                                    Text(
                                        text = name,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.White else Color(0xFF334155),
                                        modifier = Modifier.basicMarquee()
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // 4. Moneda y Fecha de Adquisición en 2 Columnas (Flexbox / Auto-layout)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Moneda
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Moneda",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                listOf("COP", "USD", "EUR").forEach { curr ->
                                    val isSelected = state.currencyCode == curr
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (isSelected) Color(0xFFE0F2FE) else Color(0xFFF8FAFC),
                                        border = BorderStroke(1.dp, if (isSelected) KashBlue else Color(0xFFCBD5E1)),
                                        modifier = Modifier
                                            .weight(1f)
                                            .clickable { onCurrencySelected(curr) }
                                    ) {
                                        Box(
                                            modifier = Modifier.padding(vertical = 10.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = curr,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) KashBlue else Color(0xFF475569)
                                            )
                                        }
                                    }
                                }
                            }
                            if (state.currencyCode == "USD") {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Se aplicará regla FX (TRM del día) en Dashboard",
                                    fontSize = 10.sp,
                                    color = Color(0xFF0284C7)
                                )
                            }
                        }

                        // Fecha
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Fecha de adquisición",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = KashNavy
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = state.acquisitionDate,
                                onValueChange = onAcquisitionDateSelected,
                                leadingIcon = {
                                    Icon(
                                        imageVector = Icons.Default.CalendarMonth,
                                        contentDescription = "Fecha",
                                        tint = KashBlue,
                                        modifier = Modifier.size(18.dp)
                                    )
                                },
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(10.dp),
                                singleLine = true,
                                textStyle = TextStyle(
                                    color = KashNavy,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedTextColor = KashNavy,
                                    unfocusedTextColor = KashNavy,
                                    focusedBorderColor = KashBlue,
                                    unfocusedBorderColor = Color(0xFFCBD5E1),
                                    focusedContainerColor = Color(0xFFF8FAFC),
                                    unfocusedContainerColor = Color(0xFFF8FAFC)
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // 5. Estado de la inversión
                    Text(
                        text = "Estado",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = KashNavy
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            "ACTIVE" to "Activa",
                            "PAUSED" to "Pausada",
                            "CLOSED" to "Cerrada / Liquidada"
                        ).forEach { (status, label) ->
                            val isSelected = state.status == status
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) Color(0xFFF0FDF4) else Color(0xFFF8FAFC),
                                border = BorderStroke(1.dp, if (isSelected) Color(0xFF10B981) else Color(0xFFCBD5E1)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable { onStatusSelected(status) }
                            ) {
                                Box(
                                    modifier = Modifier.padding(vertical = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = label,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color(0xFF16A34A) else Color(0xFF475569)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}
