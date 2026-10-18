package com.example.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

/**
 * Universal Key-Value Row for KashApp.
 * Strictly guarantees that the left key has weight(1f) and never pushes or crushes the right value.
 */
@Composable
fun KashKeyValueRow(
    keyText: String,
    modifier: Modifier = Modifier,
    valueText: String? = null,
    valueContent: (@Composable () -> Unit)? = null,
    keyColor: Color = Color(0xFF94A3B8),
    valueColor: Color = Color.White,
    testTag: String? = null
) {
    val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Row(
        modifier = tagModifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = keyText,
            color = keyColor,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )

        if (valueContent != null) {
            valueContent()
        } else if (valueText != null) {
            Text(
                text = valueText,
                color = valueColor,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Universal Transaction Item Row for KashApp.
 *
 * Golden Architectural Rules:
 * 1. The left Column (title + notes/date) is strictly assigned Modifier.weight(1f).padding(end = 12.dp),
 *    wrapping long descriptions (up to 90+ characters) without compressing the right amount.
 * 2. The right side uses CurrencyText measured with intrinsic width, guaranteeing lineCount == 1.
 * 3. Never truncates amounts or displaces them outside the viewport.
 */
@Composable
fun KashTransactionItemRow(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    date: String? = null,
    amountDouble: Double? = null,
    amount: BigDecimal? = null,
    prefix: String = "",
    currencyCode: String = "COP",
    isExpense: Boolean = false,
    icon: ImageVector? = null,
    iconColor: Color = if (isExpense) Color(0xFFF87171) else Color(0xFF34D399),
    iconContainerColor: Color = Color(0xFF1E293B),
    isPrivacyMode: Boolean = false,
    onClick: (() -> Unit)? = null,
    testTag: String? = null
) {
    val clickModifier = if (onClick != null) {
        Modifier.clickable(onClick = onClick)
    } else {
        Modifier
    }
    val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier

    Row(
        modifier = tagModifier
            .fillMaxWidth()
            .then(clickModifier)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Column with Icon + Descriptions
        Row(
            modifier = Modifier
                .weight(1f)
                .padding(end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            if (icon != null) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(iconContainerColor, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    color = Color.White,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        color = Color(0xFF94A3B8),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (!date.isNullOrBlank()) {
                    Text(
                        text = date,
                        color = Color(0xFF64748B),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Right side: CurrencyText measured with intrinsic width (never crushed)
        CurrencyText(
            amount = amount,
            amountDouble = amountDouble,
            prefix = prefix,
            currencyCode = currencyCode,
            color = if (isExpense) Color(0xFFF87171) else Color(0xFF34D399),
            fontSize = 14.sp,
            minFontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            isPrivacyMode = isPrivacyMode
        )
    }
}
