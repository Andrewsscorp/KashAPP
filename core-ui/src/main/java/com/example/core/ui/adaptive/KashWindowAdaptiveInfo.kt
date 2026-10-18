package com.example.core.ui.adaptive

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Standard width size classes for adaptive layouts in KashApp.
 * Aligns with Android WindowSizeClass specifications with an additional
 * ULTRA_COMPACT threshold for small mobile screens.
 */
enum class KashWidthSizeClass {
    ULTRA_COMPACT, // < 360.dp
    COMPACT,       // 360..599.dp
    MEDIUM,        // 600..839.dp
    EXPANDED       // >= 840.dp
}

/**
 * Standard height size classes for adaptive layouts in KashApp.
 * Enables compact adjustments when virtual keyboard is open or in landscape mode.
 */
enum class KashHeightSizeClass {
    COMPACT,       // < 480.dp
    MEDIUM,        // 480..899.dp
    EXPANDED       // >= 900.dp
}

/**
 * Central immutable responsive layout metrics and tokens provided across all
 * modules in KashApp.
 */
@Immutable
data class KashWindowAdaptiveInfo(
    val screenWidthDp: Int,
    val screenHeightDp: Int,
    val widthClass: KashWidthSizeClass,
    val heightClass: KashHeightSizeClass,
    val isUltraCompact: Boolean,
    val isCompact: Boolean,
    val isMedium: Boolean,
    val isExpanded: Boolean,
    val isLandscape: Boolean,
    val isLargeFontScale: Boolean,
    val fontScale: Float,
    val horizontalPadding: Dp,
    val verticalSpacing: Dp,
    val cardPadding: Dp,
    val maxContentWidth: Dp,
    val gridColumnsCount: Int
) {
    val isCompactHeight: Boolean get() = heightClass == KashHeightSizeClass.COMPACT || isLandscape

    companion object {
        fun calculate(
            widthDp: Int,
            heightDp: Int,
            fontScale: Float = 1.0f
        ): KashWindowAdaptiveInfo {
            val isUltraCompact = widthDp < 360
            val isCompact = widthDp in 360..599
            val isMedium = widthDp in 600..839
            val isExpanded = widthDp >= 840
            val isLandscape = widthDp > heightDp
            val isLargeFontScale = fontScale >= 1.25f

            val widthClass = when {
                isUltraCompact -> KashWidthSizeClass.ULTRA_COMPACT
                isCompact -> KashWidthSizeClass.COMPACT
                isMedium -> KashWidthSizeClass.MEDIUM
                else -> KashWidthSizeClass.EXPANDED
            }

            val heightClass = when {
                heightDp < 480 -> KashHeightSizeClass.COMPACT
                heightDp < 900 -> KashHeightSizeClass.MEDIUM
                else -> KashHeightSizeClass.EXPANDED
            }

            val horizontalPadding = when (widthClass) {
                KashWidthSizeClass.ULTRA_COMPACT -> 12.dp
                KashWidthSizeClass.COMPACT -> 16.dp
                KashWidthSizeClass.MEDIUM -> 22.dp
                KashWidthSizeClass.EXPANDED -> 28.dp
            }

            val verticalSpacing = when (widthClass) {
                KashWidthSizeClass.ULTRA_COMPACT -> 10.dp
                KashWidthSizeClass.COMPACT -> 14.dp
                KashWidthSizeClass.MEDIUM -> 16.dp
                KashWidthSizeClass.EXPANDED -> 20.dp
            }

            val cardPadding = when (widthClass) {
                KashWidthSizeClass.ULTRA_COMPACT -> 12.dp
                KashWidthSizeClass.COMPACT -> 14.dp
                KashWidthSizeClass.MEDIUM -> 16.dp
                KashWidthSizeClass.EXPANDED -> 20.dp
            }

            val maxContentWidth = when (widthClass) {
                KashWidthSizeClass.ULTRA_COMPACT -> 360.dp
                KashWidthSizeClass.COMPACT -> 600.dp
                KashWidthSizeClass.MEDIUM -> 760.dp
                KashWidthSizeClass.EXPANDED -> 960.dp
            }

            val gridColumnsCount = when (widthClass) {
                KashWidthSizeClass.ULTRA_COMPACT -> 1
                KashWidthSizeClass.COMPACT -> 1
                KashWidthSizeClass.MEDIUM -> 2
                KashWidthSizeClass.EXPANDED -> 2
            }

            return KashWindowAdaptiveInfo(
                screenWidthDp = widthDp,
                screenHeightDp = heightDp,
                widthClass = widthClass,
                heightClass = heightClass,
                isUltraCompact = isUltraCompact,
                isCompact = isCompact,
                isMedium = isMedium,
                isExpanded = isExpanded,
                isLandscape = isLandscape,
                isLargeFontScale = isLargeFontScale,
                fontScale = fontScale,
                horizontalPadding = horizontalPadding,
                verticalSpacing = verticalSpacing,
                cardPadding = cardPadding,
                maxContentWidth = maxContentWidth,
                gridColumnsCount = gridColumnsCount
            )
        }
    }
}

/**
 * CompositionLocal providing access to current window adaptive layout info
 * across the entire Composable hierarchy.
 */
val LocalKashAdaptiveInfo = compositionLocalOf {
    KashWindowAdaptiveInfo.calculate(360, 800, 1.0f)
}

/**
 * Convenience helper to observe current window adaptive configuration.
 */
@Composable
fun rememberKashAdaptiveInfo(): KashWindowAdaptiveInfo {
    val configuration = LocalConfiguration.current
    val widthDp = configuration.screenWidthDp
    val heightDp = configuration.screenHeightDp
    val fontScale = configuration.fontScale
    return remember(widthDp, heightDp, fontScale) {
        KashWindowAdaptiveInfo.calculate(widthDp, heightDp, fontScale)
    }
}
