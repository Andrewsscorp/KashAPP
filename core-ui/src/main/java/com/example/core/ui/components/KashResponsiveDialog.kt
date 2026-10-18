package com.example.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.core.ui.adaptive.rememberKashAdaptiveInfo

/**
 * Universal Responsive Dialog container for KashApp.
 *
 * Golden Rules:
 * 1. DialogProperties(usePlatformDefaultWidth = false) prevents rigid system dialog clipping.
 * 2. Adaptive width: 92% of screen width in mobile (< 600dp), 75% capped at 540dp in tablets.
 * 3. Adaptive height: strictly heightIn(max = (screenHeightDp * 0.88f).dp) preventing off-screen buttons.
 * 4. imePadding() allows the dialog to smoothly shift above the virtual keyboard.
 * 5. Safe nesting: does NOT impose verticalScroll on the root container so child LazyColumns can scroll freely.
 */
@Composable
fun KashResponsiveDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    subtitle: String? = null,
    onCloseClick: (() -> Unit)? = null,
    properties: DialogProperties = DialogProperties(usePlatformDefaultWidth = false),
    containerColor: Color = Color(0xFF0F172A),
    contentColor: Color = Color.White,
    borderColor: Color = Color(0xFF1E293B),
    testTag: String? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val adaptive = rememberKashAdaptiveInfo()
    val widthFraction = if (adaptive.isCompact || adaptive.isUltraCompact) 0.92f else 0.75f
    val maxHeight = (adaptive.screenHeightDp * 0.88f).dp

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        val tagModifier = if (testTag != null) modifier.testTag(testTag) else modifier
        val shape = RoundedCornerShape(20.dp)

        Surface(
            modifier = tagModifier
                .fillMaxWidth(widthFraction)
                .widthIn(max = 540.dp)
                .heightIn(max = maxHeight)
                .imePadding(),
            shape = shape,
            color = containerColor,
            border = BorderStroke(1.dp, borderColor),
            shadowElevation = 12.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp)
            ) {
                // Header (if title or onCloseClick provided)
                if (title != null || onCloseClick != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            if (title != null) {
                                Text(
                                    text = title,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = contentColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            if (!subtitle.isNullOrBlank()) {
                                Text(
                                    text = subtitle,
                                    fontSize = 12.sp,
                                    color = Color(0xFF94A3B8),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        val closeAction = onCloseClick ?: onDismissRequest
                        IconButton(
                            onClick = closeAction,
                            modifier = Modifier
                                .size(32.dp)
                                .background(Color(0xFF1E293B), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Cerrar",
                                tint = Color(0xFF94A3B8),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Dialog Content Body
                content()
            }
        }
    }
}
