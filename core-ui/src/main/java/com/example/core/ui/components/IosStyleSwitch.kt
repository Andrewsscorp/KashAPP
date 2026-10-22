package com.example.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp

/**
 * Interruptor moderno estilo iOS (iPhone UISwitch) para KashApp.
 *
 * - Riel en forma de cápsula (50.dp x 30.dp) sin los bordes oscuros pesados de Material 3.
 * - Estado inactivo: gris claro suave estilo iOS (#E5E5EA) con borde sutil (#D1D5DB).
 * - Estado activo: color vibrante (azul corporativo #0284C7 / #007AFF o el color personalizado del módulo).
 * - Círculo (thumb) blanco puro de 26.dp con sombra suave y expansión táctil al presionar.
 * - Compatible 1:1 con la firma de Material3 `Switch(...)` y con todos los `testTag` / semánticas de UI test.
 */
@Composable
fun IosStyleSwitch(
    checked: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    modifier: Modifier = Modifier,
    thumbContent: (@Composable () -> Unit)? = null,
    enabled: Boolean = true,
    colors: SwitchColors = SwitchDefaults.colors(),
    interactionSource: MutableInteractionSource? = null,
    activeTrackColor: Color? = null,
    inactiveTrackColor: Color = Color(0xFFE5E5EA)
) {
    val resolvedInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by resolvedInteractionSource.collectIsPressedAsState()

    val defaultPrimary = MaterialTheme.colorScheme.primary
    val resolvedActiveColor = when {
        activeTrackColor != null -> activeTrackColor
        colors.checkedTrackColor != Color.Unspecified && colors.checkedTrackColor != defaultPrimary -> {
            colors.checkedTrackColor
        }
        else -> Color(0xFF0284C7) // KashBlue / iOS vibrant blue
    }

    val trackColor by animateColorAsState(
        targetValue = if (checked) resolvedActiveColor else inactiveTrackColor,
        animationSpec = tween(durationMillis = 220),
        label = "ios_switch_track_color"
    )

    val borderColor by animateColorAsState(
        targetValue = if (checked) resolvedActiveColor else Color(0xFFD1D5DB),
        animationSpec = tween(durationMillis = 220),
        label = "ios_switch_border_color"
    )

    // Thumb stretches slightly when pressed (authentic iOS tactile behavior)
    val thumbWidth by animateDpAsState(
        targetValue = if (isPressed && enabled) 29.dp else 26.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "ios_switch_thumb_width"
    )

    val thumbOffsetX by animateDpAsState(
        targetValue = when {
            checked && isPressed && enabled -> 19.dp
            checked -> 22.dp
            else -> 2.dp
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "ios_switch_thumb_offset"
    )

    val toggleModifier = if (onCheckedChange != null) {
        Modifier.toggleable(
            value = checked,
            interactionSource = resolvedInteractionSource,
            indication = null,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        )
    } else {
        Modifier
    }

    Box(
        modifier = modifier
            .width(50.dp)
            .height(30.dp)
            .alpha(if (enabled) 1f else 0.5f)
            .clip(RoundedCornerShape(percent = 50))
            .background(trackColor)
            .border(
                width = if (checked) 0.dp else 0.8.dp,
                color = borderColor,
                shape = RoundedCornerShape(percent = 50)
            )
            .then(toggleModifier),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset { IntOffset(x = thumbOffsetX.roundToPx(), y = 0) }
                .width(thumbWidth)
                .height(26.dp)
                .shadow(
                    elevation = if (enabled) 2.5.dp else 0.5.dp,
                    shape = CircleShape,
                    clip = false
                )
                .clip(CircleShape)
                .background(Color.White),
            contentAlignment = Alignment.Center
        ) {
            if (thumbContent != null) {
                Box(modifier = Modifier.size(16.dp), contentAlignment = Alignment.Center) {
                    thumbContent()
                }
            }
        }
    }
}
