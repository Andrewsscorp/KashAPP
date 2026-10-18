package com.example.core.ui.adaptive

import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KashWindowAdaptiveInfoTest {

    @Test
    fun calculate_ultraCompactPhone_returnsExpectedTokens() {
        val info = KashWindowAdaptiveInfo.calculate(widthDp = 320, heightDp = 640, fontScale = 1.0f)
        assertEquals(KashWidthSizeClass.ULTRA_COMPACT, info.widthClass)
        assertEquals(KashHeightSizeClass.MEDIUM, info.heightClass)
        assertTrue(info.isUltraCompact)
        assertFalse(info.isCompact)
        assertFalse(info.isMedium)
        assertFalse(info.isExpanded)
        assertFalse(info.isLandscape)
        assertFalse(info.isLargeFontScale)
        assertEquals(12.dp, info.horizontalPadding)
        assertEquals(10.dp, info.verticalSpacing)
        assertEquals(12.dp, info.cardPadding)
        assertEquals(360.dp, info.maxContentWidth)
        assertEquals(1, info.gridColumnsCount)
    }

    @Test
    fun calculate_compactStandardPhone_returnsExpectedTokens() {
        val info = KashWindowAdaptiveInfo.calculate(widthDp = 360, heightDp = 800, fontScale = 1.0f)
        assertEquals(KashWidthSizeClass.COMPACT, info.widthClass)
        assertEquals(KashHeightSizeClass.MEDIUM, info.heightClass)
        assertFalse(info.isUltraCompact)
        assertTrue(info.isCompact)
        assertFalse(info.isMedium)
        assertFalse(info.isExpanded)
        assertFalse(info.isLandscape)
        assertEquals(16.dp, info.horizontalPadding)
        assertEquals(14.dp, info.verticalSpacing)
        assertEquals(14.dp, info.cardPadding)
        assertEquals(600.dp, info.maxContentWidth)
        assertEquals(1, info.gridColumnsCount)
    }

    @Test
    fun calculate_mediumFoldableOrTablet_returnsTwoColumns() {
        val info = KashWindowAdaptiveInfo.calculate(widthDp = 600, heightDp = 960, fontScale = 1.0f)
        assertEquals(KashWidthSizeClass.MEDIUM, info.widthClass)
        assertEquals(KashHeightSizeClass.EXPANDED, info.heightClass)
        assertFalse(info.isUltraCompact)
        assertFalse(info.isCompact)
        assertTrue(info.isMedium)
        assertFalse(info.isExpanded)
        assertEquals(22.dp, info.horizontalPadding)
        assertEquals(16.dp, info.verticalSpacing)
        assertEquals(16.dp, info.cardPadding)
        assertEquals(760.dp, info.maxContentWidth)
        assertEquals(2, info.gridColumnsCount)
    }

    @Test
    fun calculate_expandedTabletOrDesktop_returnsMaxTokens() {
        val info = KashWindowAdaptiveInfo.calculate(widthDp = 840, heightDp = 1200, fontScale = 1.0f)
        assertEquals(KashWidthSizeClass.EXPANDED, info.widthClass)
        assertEquals(KashHeightSizeClass.EXPANDED, info.heightClass)
        assertTrue(info.isExpanded)
        assertEquals(28.dp, info.horizontalPadding)
        assertEquals(20.dp, info.verticalSpacing)
        assertEquals(20.dp, info.cardPadding)
        assertEquals(960.dp, info.maxContentWidth)
        assertEquals(2, info.gridColumnsCount)
    }

    @Test
    fun calculate_landscapeMode_detectsLandscapeAndCompactHeight() {
        val info = KashWindowAdaptiveInfo.calculate(widthDp = 800, heightDp = 360, fontScale = 1.0f)
        assertTrue(info.isLandscape)
        assertEquals(KashHeightSizeClass.COMPACT, info.heightClass)
    }

    @Test
    fun calculate_largeFontScale_detectsAccessibilityScale() {
        val normal = KashWindowAdaptiveInfo.calculate(widthDp = 360, heightDp = 800, fontScale = 1.0f)
        assertFalse(normal.isLargeFontScale)

        val large = KashWindowAdaptiveInfo.calculate(widthDp = 360, heightDp = 800, fontScale = 1.3f)
        assertTrue(large.isLargeFontScale)
    }
}
