package com.example.presentation.dashboard.components.topbar

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
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
import com.example.presentation.dashboard.model.PeriodState
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale

/**
 * 1. DashPeriodSelector.kt: Controls the time window for financial dashboard aggregations.
 */
@Composable
fun DashPeriodSelector(
    periodState: PeriodState,
    onPeriodSelected: (YearMonth) -> Unit,
    modifier: Modifier = Modifier
) {
    var isExpanded by remember { mutableStateOf(false) }
    val colombianLocale = remember { Locale("es", "CO") }

    val monthName = periodState.currentMonth.month.getDisplayName(TextStyle.FULL, colombianLocale)
        .replaceFirstChar { if (it.isLowerCase()) it.titlecase(colombianLocale) else it.toString() }
    val year = periodState.currentMonth.year

    val labelText = if (periodState.isCurrent) {
        "$monthName $year (Actual)"
    } else {
        "$monthName $year"
    }

    Box(modifier = modifier) {
        Surface(
            color = Color(0xFFF1F5F9),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .clickable { isExpanded = true }
                .testTag("dash_period_selector")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = Color(0xFF0284C7),
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = labelText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF0F172A)
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Cambiar periodo",
                    tint = Color(0xFF64748B),
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        DropdownMenu(
            expanded = isExpanded,
            onDismissRequest = { isExpanded = false },
            modifier = Modifier.background(Color.White)
        ) {
            // Generate last 6 months
            val currentYearMonth = YearMonth.now()
            (0..5).forEach { offset ->
                val targetMonth = currentYearMonth.minusMonths(offset.toLong())
                val targetName = targetMonth.month.getDisplayName(TextStyle.FULL, colombianLocale)
                    .replaceFirstChar { if (it.isLowerCase()) it.titlecase(colombianLocale) else it.toString() }
                val isSelected = targetMonth == periodState.currentMonth

                DropdownMenuItem(
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = if (targetMonth == currentYearMonth) "$targetName ${targetMonth.year} (Actual)" else "$targetName ${targetMonth.year}",
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A)
                            )
                        }
                    },
                    onClick = {
                        isExpanded = false
                        onPeriodSelected(targetMonth)
                    }
                )
            }
        }
    }
}
