package com.example.presentation.credit_cards.widgets

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.presentation.credit_cards.state.CreditCardFormState

private val GreenAccent = Color(0xFF10B981)
private val GreenBg = Color(0xFFECFDF5)
private val DarkSlate = Color(0xFF1E293B)
private val BorderColor = Color(0xFFE2E8F0)
private val LabelGray = Color(0xFF475569)

/**
 * Accordion 1: Información básica
 * Matches the user screenshot with full MDM bank, currency, franchise and category selectors.
 */
@Composable
fun CcBasicInfoAccordion(
    state: CreditCardFormState,
    onToggleExpand: () -> Unit,
    onDebtNameChanged: (String) -> Unit,
    onBankChanged: (String) -> Unit,
    onCurrencyChanged: (String, String) -> Unit,
    onFranchiseChanged: (String) -> Unit,
    onCategoryChanged: (String) -> Unit,
    onAcquisitionDateChanged: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var bankDropdownOpen by remember { mutableStateOf(false) }
    var currencyDropdownOpen by remember { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("cc_basic_info_accordion"),
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
                            .background(GreenBg),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Description,
                            contentDescription = null,
                            tint = GreenAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Información básica",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = DarkSlate
                    )
                }

                Icon(
                    imageVector = if (state.isBasicInfoExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = null,
                    tint = Color(0xFF64748B)
                )
            }

            AnimatedVisibility(
                visible = state.isBasicInfoExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        val isNarrow = maxWidth < 480.dp

                        if (isNarrow) {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                // Row 1: Tipo de deuda
                                FormSelectorField(
                                    label = "Tipo de deuda *",
                                    value = state.debtType,
                                    leadingIcon = Icons.Default.CreditCard,
                                    onClick = {}
                                )

                                // Nombre de la deuda
                                FormInputField(
                                    label = "Nombre de la deuda *",
                                    value = state.debtName,
                                    placeholder = "Ej. Tarjeta de Crédito, Préstamo...",
                                    onValueChange = onDebtNameChanged
                                )

                                // Row 2: Acreedor
                                Box {
                                    FormSelectorField(
                                        label = "Acreedor *",
                                        value = state.creditorBank,
                                        onClick = { bankDropdownOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = bankDropdownOpen,
                                        onDismissRequest = { bankDropdownOpen = false }
                                    ) {
                                        state.availableBanks.forEach { bank ->
                                            DropdownMenuItem(
                                                text = { Text(bank) },
                                                onClick = {
                                                    onBankChanged(bank)
                                                    bankDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Moneda
                                Box {
                                    FormSelectorField(
                                        label = "Moneda *",
                                        value = state.currencyName,
                                        onClick = { currencyDropdownOpen = true }
                                    )
                                    DropdownMenu(
                                        expanded = currencyDropdownOpen,
                                        onDismissRequest = { currencyDropdownOpen = false }
                                    ) {
                                        DropdownMenuItem(
                                            text = { Text("COP - Peso colombiano") },
                                            onClick = {
                                                onCurrencyChanged("COP", "COP - Peso colombiano")
                                                currencyDropdownOpen = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("USD - Dólar estadounidense") },
                                            onClick = {
                                                onCurrencyChanged("USD", "USD - Dólar estadounidense")
                                                currencyDropdownOpen = false
                                            }
                                        )
                                        DropdownMenuItem(
                                            text = { Text("EUR - Euro") },
                                            onClick = {
                                                onCurrencyChanged("EUR", "EUR - Euro")
                                                currencyDropdownOpen = false
                                            }
                                        )
                                    }
                                }

                                // Row 3: Fecha de inicio / adquisición
                                FormInputField(
                                    label = "Fecha de inicio / adquisición *",
                                    value = state.acquisitionDate,
                                    placeholder = "dd/mm/aaaa",
                                    trailingIcon = Icons.Default.CalendarToday,
                                    onValueChange = onAcquisitionDateChanged
                                )

                                // Estado de la deuda
                                FormStatusBadgeField(
                                    label = "Estado de la deuda *",
                                    status = state.debtStatus
                                )
                            }
                        } else {
                            // Two-column layout for tablets / wider screens
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FormSelectorField(
                                        label = "Tipo de deuda *",
                                        value = state.debtType,
                                        leadingIcon = Icons.Default.CreditCard,
                                        modifier = Modifier.weight(1f),
                                        onClick = {}
                                    )
                                    FormInputField(
                                        label = "Nombre de la deuda *",
                                        value = state.debtName,
                                        placeholder = "Ej. Tarjeta de Crédito, Préstamo...",
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onDebtNameChanged
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Box(modifier = Modifier.weight(1f)) {
                                        FormSelectorField(
                                            label = "Acreedor *",
                                            value = state.creditorBank,
                                            onClick = { bankDropdownOpen = true }
                                        )
                                        DropdownMenu(
                                            expanded = bankDropdownOpen,
                                            onDismissRequest = { bankDropdownOpen = false }
                                        ) {
                                            state.availableBanks.forEach { bank ->
                                                DropdownMenuItem(
                                                    text = { Text(bank) },
                                                    onClick = {
                                                        onBankChanged(bank)
                                                        bankDropdownOpen = false
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    Box(modifier = Modifier.weight(1f)) {
                                        FormSelectorField(
                                            label = "Moneda *",
                                            value = state.currencyName,
                                            onClick = { currencyDropdownOpen = true }
                                        )
                                        DropdownMenu(
                                            expanded = currencyDropdownOpen,
                                            onDismissRequest = { currencyDropdownOpen = false }
                                        ) {
                                            DropdownMenuItem(
                                                text = { Text("COP - Peso colombiano") },
                                                onClick = {
                                                    onCurrencyChanged("COP", "COP - Peso colombiano")
                                                    currencyDropdownOpen = false
                                                }
                                            )
                                            DropdownMenuItem(
                                                text = { Text("USD - Dólar estadounidense") },
                                                onClick = {
                                                    onCurrencyChanged("USD", "USD - Dólar estadounidense")
                                                    currencyDropdownOpen = false
                                                }
                                            )
                                        }
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    FormInputField(
                                        label = "Fecha de inicio / adquisición *",
                                        value = state.acquisitionDate,
                                        placeholder = "dd/mm/aaaa",
                                        trailingIcon = Icons.Default.CalendarToday,
                                        modifier = Modifier.weight(1f),
                                        onValueChange = onAcquisitionDateChanged
                                    )
                                    FormStatusBadgeField(
                                        label = "Estado de la deuda *",
                                        status = state.debtStatus,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Franquicia (Horizontal chips / marquee)
                    Text(
                        text = "Franquicia de la tarjeta",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LabelGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("VISA", "MASTERCARD", "AMEX", "DINERS").forEach { franchise ->
                            val isSelected = state.franchise == franchise
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { onFranchiseChanged(franchise) }
                            ) {
                                Text(
                                    text = franchise,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DarkSlate,
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Categoría (Horizontal chips / marquee)
                    Text(
                        text = "Categoría del plástico",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium,
                        color = LabelGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("CLÁSICA", "ORO", "PLATINUM", "SIGNATURE", "BLACK", "INFINITE").forEach { cat ->
                            val isSelected = state.category.uppercase() == cat
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                modifier = Modifier.clickable { onCategoryChanged(cat) }
                            ) {
                                Text(
                                    text = cat,
                                    fontSize = 11.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) Color.White else DarkSlate,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                }
            }
        }
    }
}

@Composable
fun FormInputField(
    label: String,
    value: String,
    placeholder: String = "",
    trailingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(placeholder, fontSize = 13.sp, color = Color(0xFF94A3B8)) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(10.dp),
            trailingIcon = if (trailingIcon != null) {
                {
                    Icon(
                        imageVector = trailingIcon,
                        contentDescription = null,
                        tint = Color(0xFF64748B),
                        modifier = Modifier.size(18.dp)
                    )
                }
            } else null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF0284C7),
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = Color.White,
                unfocusedContainerColor = Color.White
            )
        )
    }
}

@Composable
fun FormSelectorField(
    label: String,
    value: String,
    leadingIcon: androidx.compose.ui.graphics.vector.ImageVector? = null,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .clickable { onClick() }
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                if (leadingIcon != null) {
                    Icon(
                        imageVector = leadingIcon,
                        contentDescription = null,
                        tint = Color(0xFF0284C7),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
                Text(
                    text = value,
                    fontSize = 13.sp,
                    color = DarkSlate,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF64748B)
            )
        }
    }
}

@Composable
fun FormStatusBadgeField(
    label: String,
    status: String,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier) {
        Text(
            text = label,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = LabelGray
        )
        Spacer(modifier = Modifier.height(4.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp)
                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(GreenAccent)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = status,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = DarkSlate
                )
            }
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
                tint = Color(0xFF64748B)
            )
        }
    }
}
