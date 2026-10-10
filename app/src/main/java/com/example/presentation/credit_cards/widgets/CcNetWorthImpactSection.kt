package com.example.presentation.credit_cards.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.credit_cards.state.CreditCardFormState
import java.util.Locale

private val DarkSlate = Color(0xFF1E293B)
private val PurpleAccent = Color(0xFF8B5CF6)
private val PurpleBg = Color(0xFFF3E8FF)
private val BorderColor = Color(0xFFE2E8F0)

/**
 * Accordion 6: Impacto en el Patrimonio Neto (CFO / Architecture Core)
 * Ensures user understands credit cards are revolving liabilities:
 * Total quota does NOT add to Net Worth; used quota subtracts from Assets as debt.
 */
@Composable
fun CcNetWorthImpactSection(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onIncludeInNetWorthChanged: (Boolean) -> Unit,
    onFinancesAssetChanged: (Boolean) -> Unit,
    onRelatedAssetChanged: (String) -> Unit,
    onAssetValueChanged: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_net_worth_impact_section"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpand() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(PurpleBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = PurpleAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Impacto en el Patrimonio Neto",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isNetWorthExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isNetWorthExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    // CFO Analysis Card
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFFFEF2F2),
                        border = CardDefaults.outlinedCardBorder(),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Análisis CFO: Pasivo vs Límite",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF991B1B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "El Cupo Total ($ ${String.format(Locale.GERMANY, "%,.0f", state.totalQuota)}) es sólo un límite crediticio. KashApp registrará el Cupo Utilizado ($ ${String.format(Locale.GERMANY, "%,.0f", state.usedQuota)}) como un PASIVO (Deuda) que restará a tus activos en el Dashboard.",
                                fontSize = 11.sp,
                                color = Color(0xFF7F1D1D)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch 1: Incluir en el patrimonio neto
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Incluir en el patrimonio neto",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Text(
                                text = "Esta deuda se tendrá en cuenta en el cálculo de tu patrimonio neto.",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = state.includeInNetWorth,
                            onCheckedChange = onIncludeInNetWorthChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleAccent
                            )
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Switch 2: Financia un activo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Financia un activo",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkSlate
                            )
                            Text(
                                text = "Indica si esta deuda se utiliza para financiar un activo (ej. vehículo, propiedad, equipo, etc.)",
                                fontSize = 10.sp,
                                color = Color(0xFF64748B)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Switch(
                            checked = state.financesAsset,
                            onCheckedChange = onFinancesAssetChanged,
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = PurpleAccent
                            )
                        )
                    }

                    if (state.financesAsset) {
                        Spacer(modifier = Modifier.height(12.dp))
                        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                            val isNarrow = maxWidth < 480.dp

                            if (isNarrow) {
                                Column(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FormSelectorField(
                                        label = "Activo relacionado (opcional)",
                                        value = state.relatedAsset,
                                        leadingIcon = Icons.Default.DirectionsCar,
                                        onClick = {}
                                    )

                                    CurrencyInputField(
                                        label = "Valor del activo (opcional)",
                                        amount = state.assetValue,
                                        onAmountChange = onAssetValueChanged
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    FormSelectorField(
                                        label = "Activo relacionado (opcional)",
                                        value = state.relatedAsset,
                                        leadingIcon = Icons.Default.DirectionsCar,
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    )
                                    CurrencyInputField(
                                        label = "Valor del activo (opcional)",
                                        amount = state.assetValue,
                                        modifier = Modifier.weight(1f),
                                        onAmountChange = onAssetValueChanged
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                }
            }
        }
    }
}

/**
 * Collapsible extra accordion helper for items at the bottom of the screen:
 * - Documentos
 * - Recordatorios
 * - Favoritos
 * - Más detalles
 */
@Composable
fun CcExtraCollapsibleCard(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconTint: Color,
    iconBg: Color,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        border = CardDefaults.outlinedCardBorder()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .clip(CircleShape)
                            .background(iconBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = iconTint,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = title,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = DarkSlate
                        )
                        Text(
                            text = subtitle,
                            fontSize = 10.sp,
                            color = Color(0xFF64748B)
                        )
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Box(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                    content()
                }
            }
        }
    }
}
