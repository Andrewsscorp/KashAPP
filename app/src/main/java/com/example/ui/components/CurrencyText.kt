package com.example.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.util.MoneyFormatter
import java.math.BigDecimal

/**
 * Centralized, production-grade CurrencyText component.
 *
 * Enforces:
 * 1. maxLines = 1 and softWrap = false (Strictly prevents ugly orphan line-breaks).
 * 2. TextOverflow.Ellipsis (Safe truncation if container is too small).
 * 3. Dynamic Auto-Shrink / 'adjustsFontSizeToFit' logic: intelligently scales font size
 *    down based on string length and visual layout constraints before falling back to ellipsis.
 * 4. Privacy masking mode support ("••••••").
 * 5. Flexible inputs: BigDecimal, Double, Long, or raw string.
 */
@Composable
fun CurrencyText(
    modifier: Modifier = Modifier,
    amount: BigDecimal? = null,
    amountDouble: Double? = null,
    amountLong: Long? = null,
    rawText: String? = null,
    currencyCode: String = "COP",
    showCurrencySuffix: Boolean = false,
    isPrivacyMode: Boolean = false,
    fontSize: TextUnit = 16.sp,
    minFontSize: TextUnit = 11.sp,
    fontWeight: FontWeight = FontWeight.Bold,
    fontStyle: FontStyle? = null,
    fontFamily: FontFamily? = null,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
    lineHeight: TextUnit = TextUnit.Unspecified,
    letterSpacing: TextUnit = TextUnit.Unspecified,
    compact: Boolean = false,
    testTag: String? = null,
    style: TextStyle = LocalTextStyle.current
) {
    val displayText = remember(amount, amountDouble, amountLong, rawText, isPrivacyMode, compact, currencyCode, showCurrencySuffix) {
        when {
            rawText != null -> {
                if (isPrivacyMode) "••••••" else rawText
            }
            amount != null -> {
                if (compact) {
                    MoneyFormatter.formatCompact(amount, isPrivacyMode, currencyCode)
                } else {
                    MoneyFormatter.format(amount, isPrivacyMode, currencyCode)
                }
            }
            amountDouble != null -> {
                val bd = BigDecimal.valueOf(amountDouble)
                if (compact) {
                    MoneyFormatter.formatCompact(bd, isPrivacyMode, currencyCode)
                } else {
                    MoneyFormatter.format(bd, isPrivacyMode, currencyCode)
                }
            }
            amountLong != null -> {
                val bd = BigDecimal.valueOf(amountLong)
                if (compact) {
                    MoneyFormatter.formatCompact(bd, isPrivacyMode, currencyCode)
                } else {
                    MoneyFormatter.format(bd, isPrivacyMode, currencyCode)
                }
            }
            else -> {
                if (isPrivacyMode) "••••••" else "$ 0"
            }
        }
    }

    // Dynamic heuristic font reduction based on string character length
    val baseFontSizeVal = if (fontSize != TextUnit.Unspecified) fontSize.value else 16f
    val minFontSizeVal = if (minFontSize != TextUnit.Unspecified) minFontSize.value else 11f

    val calculatedFontSize = remember(displayText, baseFontSizeVal, minFontSizeVal) {
        val len = displayText.length
        val scaleFactor = when {
            len > 22 -> 0.65f
            len > 18 -> 0.72f
            len > 14 -> 0.82f
            len > 11 -> 0.90f
            else -> 1.0f
        }
        (baseFontSizeVal * scaleFactor).coerceAtLeast(minFontSizeVal).sp
    }

    var effectiveFontSize by remember(calculatedFontSize) { mutableStateOf(calculatedFontSize) }

    val resolvedModifier = if (testTag != null) {
        modifier.testTag(testTag)
    } else {
        modifier
    }

    Text(
        text = displayText,
        modifier = resolvedModifier,
        color = color,
        fontSize = effectiveFontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        letterSpacing = letterSpacing,
        textAlign = textAlign,
        lineHeight = if (lineHeight != TextUnit.Unspecified) lineHeight else (effectiveFontSize.value * 1.15f).sp,
        maxLines = 1,
        softWrap = false,
        overflow = TextOverflow.Ellipsis,
        style = style,
        onTextLayout = { textLayoutResult ->
            // Auto-shrink on overflow during layout phase
            if (textLayoutResult.hasVisualOverflow && effectiveFontSize.value > minFontSizeVal) {
                val nextSize = (effectiveFontSize.value * 0.9f).coerceAtLeast(minFontSizeVal)
                if (nextSize < effectiveFontSize.value) {
                    effectiveFontSize = nextSize.sp
                }
            }
        }
    )
}
