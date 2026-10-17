package com.example.presentation.investments.cdt.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Percent
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MenuDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.KashBlue
import com.example.ui.theme.KashNavy

/**
 * Isolated component for "Rentabilidad".
 * Manages Interest Rate input (EA), Rate Type, Payout Frequency, and Compounding.
 */
@Composable
fun CdtProfitabilityCard(
    interestRateFormatted: String,
    rateType: String,
    onRateTypeSelected: (String) -> Unit,
    payoutFrequency: String,
    onPayoutFrequencySelected: (String) -> Unit,
    compounding: Boolean,
    onCompoundingChanged: (Boolean) -> Unit,
    onRequestEditRate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showRateTypeMenu by remember { mutableStateOf(false) }
    var showPayoutFrequencyMenu by remember { mutableStateOf(false) }
    var showCompoundingMenu by remember { mutableStateOf(false) }

    val rateTypeOptions = listOf(
        "EA - Efectiva anual",
        "Nominal mes vencido (NMV)",
        "Nominal trimestre vencido (NTV)"
    )

    val payoutFrequencyOptions = listOf(
        "Al vencimiento",
        "Mensual",
        "Trimestral",
        "Semestral"
    )

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.5.dp),
        border = BorderStroke(1.dp, Color(0xFFF1F5F9))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(0xFFE0F2FE)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Percent,
                        contentDescription = null,
                        tint = KashBlue,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = "Rentabilidad",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = KashNavy
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Row 1: Tasa * | Tipo de tasa
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Tasa *
                Surface(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onRequestEditRate() }
                        .testTag("cdt_rate_input"),
                    shape = RoundedCornerShape(12.dp),
                    color = Color(0xFFF8FAFC),
                    border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Tasa *",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFF64748B)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFEFF6FF)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Percent,
                                    contentDescription = null,
                                    tint = KashBlue,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = interestRateFormatted,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = KashNavy
                            )
                        }
                    }
                }

                // Tipo de tasa
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showRateTypeMenu = true }
                            .testTag("cdt_rate_type_selector"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Tipo de tasa",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CreditCard,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = rateType,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showRateTypeMenu,
                        onDismissRequest = { showRateTypeMenu = false },
                        containerColor = Color.White
                    ) {
                        rateTypeOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (rateType == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (rateType == option) KashBlue else KashNavy
                                    )
                                },
                                onClick = {
                                    onRateTypeSelected(option)
                                    showRateTypeMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = KashNavy
                                )
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Row 2: Pago de intereses | ¿Capitalización de intereses?
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Pago de intereses
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showPayoutFrequencyMenu = true }
                            .testTag("cdt_payout_frequency_selector"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "Pago de intereses",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B)
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CalendarMonth,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = payoutFrequency,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy,
                                        maxLines = 1
                                    )
                                }
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showPayoutFrequencyMenu,
                        onDismissRequest = { showPayoutFrequencyMenu = false },
                        containerColor = Color.White
                    ) {
                        payoutFrequencyOptions.forEach { option ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = option,
                                        fontSize = 13.sp,
                                        fontWeight = if (payoutFrequency == option) FontWeight.Bold else FontWeight.Normal,
                                        color = if (payoutFrequency == option) KashBlue else KashNavy
                                    )
                                },
                                onClick = {
                                    onPayoutFrequencySelected(option)
                                    showPayoutFrequencyMenu = false
                                },
                                colors = MenuDefaults.itemColors(
                                    textColor = KashNavy
                                )
                            )
                        }
                    }
                }

                // ¿Capitalización de intereses?
                Box(modifier = Modifier.weight(1f)) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { showCompoundingMenu = true }
                            .testTag("cdt_compounding_selector"),
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFF8FAFC),
                        border = BorderStroke(1.dp, Color(0xFFE2E8F0))
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp)
                        ) {
                            Text(
                                text = "¿Capitalización de intereses?",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF64748B),
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.weight(1f),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(24.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFFEFF6FF)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Autorenew,
                                            contentDescription = null,
                                            tint = KashBlue,
                                            modifier = Modifier.size(14.dp)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = if (compounding) "Sí" else "No",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = KashNavy
                                    )
                                }
                                Icon(
                                    imageVector = Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    tint = Color(0xFF64748B),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }

                    DropdownMenu(
                        expanded = showCompoundingMenu,
                        onDismissRequest = { showCompoundingMenu = false },
                        containerColor = Color.White
                    ) {
                        DropdownMenuItem(
                            text = {
                                Text("Sí", fontSize = 13.sp, color = KashNavy)
                            },
                            onClick = {
                                onCompoundingChanged(true)
                                showCompoundingMenu = false
                            },
                            colors = MenuDefaults.itemColors(textColor = KashNavy)
                        )
                        DropdownMenuItem(
                            text = {
                                Text("No", fontSize = 13.sp, color = KashNavy)
                            },
                            onClick = {
                                onCompoundingChanged(false)
                                showCompoundingMenu = false
                            },
                            colors = MenuDefaults.itemColors(textColor = KashNavy)
                        )
                    }
                }
            }
        }
    }
}
