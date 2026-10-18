package com.example.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import java.math.BigDecimal

/**
 * Backward-compatible delegator to universal CurrencyText in :core-ui.
 * Preserves 100% binary and source compatibility for all calls inside :app.
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
    com.example.core.ui.components.CurrencyText(
        modifier = modifier,
        amount = amount,
        amountDouble = amountDouble,
        amountLong = amountLong,
        rawText = rawText,
        prefix = prefix,
        suffix = suffix,
        currencyCode = currencyCode,
        showCurrencySuffix = showCurrencySuffix,
        isPrivacyMode = isPrivacyMode,
        fontSize = fontSize,
        minFontSize = minFontSize,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        fontFamily = fontFamily,
        color = color,
        textAlign = textAlign,
        lineHeight = lineHeight,
        letterSpacing = letterSpacing,
        compact = compact,
        testTag = testTag,
        style = style
    )
}
