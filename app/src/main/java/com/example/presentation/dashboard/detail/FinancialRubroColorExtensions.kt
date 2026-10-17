package com.example.presentation.dashboard.detail

import androidx.compose.ui.graphics.Color
import com.example.domain.models.FinancialRubroType

val FinancialRubroType.primaryColor: Color
    get() = try {
        Color(android.graphics.Color.parseColor(primaryColorHex))
    } catch (_: Exception) {
        Color(0xFF0284C7)
    }

val FinancialRubroType.bgSoftColor: Color
    get() = try {
        Color(android.graphics.Color.parseColor(bgSoftColorHex))
    } catch (_: Exception) {
        Color(0xFFE0F2FE)
    }
