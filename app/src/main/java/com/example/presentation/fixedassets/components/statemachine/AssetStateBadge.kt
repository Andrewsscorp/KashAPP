package com.example.presentation.fixedassets.components.statemachine

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
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
import com.example.data.local.entity.FixedAssetEntity
import com.example.domain.model.fixedassets.statemachine.AssetStateMachine

/**
 * Interactive State Badge featuring the colored status tag,
 * pulsating dot, and the explicit Help Symbol (?) that opens
 * the IFRS / NIIF accounting explainer sheet.
 */
@Composable
fun AssetStateBadge(
    asset: FixedAssetEntity,
    onHelpClick: () -> Unit,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val state = AssetStateMachine.resolveState(asset.status)

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Status Pill
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = state.color.copy(alpha = 0.18f),
            border = BorderStroke(1.dp, state.color.copy(alpha = 0.8f)),
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable(enabled = onClick != null) { onClick?.invoke() }
                .testTag("asset_state_badge")
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(7.dp)
                        .clip(CircleShape)
                        .background(state.color)
                )
                Text(
                    text = state.shortBadgeLabel,
                    fontSize = 11.5.sp,
                    fontWeight = FontWeight.Bold,
                    color = state.color
                )
            }
        }

        // Explanatory Help Symbol (?)
        Surface(
            shape = CircleShape,
            color = Color(0xFF0F223D),
            border = BorderStroke(1.dp, Color(0xFF1E3A8A)),
            modifier = Modifier
                .size(22.dp)
                .clip(CircleShape)
                .clickable { onHelpClick() }
                .testTag("asset_state_help_button")
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = "Ayuda contable del estado",
                    tint = Color(0xFF93C5FD),
                    modifier = Modifier.size(13.dp)
                )
            }
        }
    }
}
