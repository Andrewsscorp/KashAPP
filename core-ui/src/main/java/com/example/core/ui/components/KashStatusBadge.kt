package com.example.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * Universal, non-collapsing status badge component for KashApp.
 * Strictly guarantees:
 * 1. maxLines = 1, softWrap = false, overflow = TextOverflow.Ellipsis
 * 2. Minimum text font size floor of 11.sp
 * 3. Never fractures text vertically into 1-2 character columns.
 */
@Composable
fun KashStatusBadge(
    text: String,
    modifier: Modifier = Modifier,
    textColor: Color = Color(0xFF38BDF8),
    containerColor: Color = Color(0xFF0F172A).copy(alpha = 0.8f),
    borderColor: Color = Color(0xFF38BDF8).copy(alpha = 0.4f),
    icon: ImageVector? = null
) {
    Row(
        modifier = modifier
            .background(containerColor, RoundedCornerShape(6.dp))
            .border(BorderStroke(1.dp, borderColor), RoundedCornerShape(6.dp))
            .padding(horizontal = 8.dp, vertical = 3.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(12.dp)
            )
        }
        Text(
            text = text,
            color = textColor,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            softWrap = false,
            overflow = TextOverflow.Ellipsis
        )
    }
}
