package com.example.core.ui.components

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
 * Universal, production-grade CurrencyText component promoted to :core-ui.
 *
 * Guarantees:
 * 1. maxLines = 1 and softWrap = false (Strictly prevents orphan line breaks).
 * 2. TextOverflow.Ellipsis (Clean ellipsis truncation if space is insufficient).
 * 3. Dynamic Auto-Shrink / Font Scaling: scales font down during composition
 *    and layout phases down to minFontSize (default 11.sp).
 * 4. Support for optional prefix (e.g., "-", "+", "Techo: ", "Gastado: ")
 *    and suffix (e.g., "/día", "/mes", " COP").
 * 5. Full Privacy Mode masking ("••••••").
 * 6. Multi-type inputs: BigDecimal, Double, Long, or rawText.
 */
@Composable
fun CurrencyText(
    modifier: Modifier = Modifier,
    amount: BigDecimal? = null,
    amountDouble: Double? = null,
    amountLong: Long? = null,
    rawText: String? = null,
    prefix: String = "",
    suffix: String = "",
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
    val displayText = remember(
        amount,
        amountDouble,
        amountLong,
        rawText,
        prefix,
        suffix,
        isPrivacyMode,
        compact,
        currencyCode,
        showCurrencySuffix
    ) {
        val baseFormatted = when {
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

        if (isPrivacyMode && rawText == null) {
            if (prefix.isNotEmpty() || suffix.isNotEmpty()) {
                "$prefix••••••$suffix"
            } else {
                "••••••"
            }
        } else if (rawText != null) {
            baseFormatted
        } else {
            "$prefix$baseFormatted$suffix"
        }
    }

    // Dynamic heuristic font reduction based on string character length
    val baseFontSizeVal = if (fontSize != TextUnit.Unspecified) fontSize.value else 16f
    val minFontSizeVal = if (minFontSize != TextUnit.Unspecified) minFontSize.value else 11f

    val calculatedFontSize = remember(displayText, baseFontSizeVal, minFontSizeVal) {
        val len = displayText.length
        val scaleFactor = when {
            len > 24 -> 0.65f
            len > 19 -> 0.72f
            len > 15 -> 0.82f
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
