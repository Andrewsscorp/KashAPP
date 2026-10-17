package com.example.domain.model.fixedassets.statemachine

import androidx.compose.ui.graphics.Color

/**
 * Base sealed interface representing an operational and financial lifecycle state
 * of a Fixed Asset under IFRS / NIIF standards.
 */
sealed interface AssetState {
    val code: String
    val label: String
    val shortBadgeLabel: String
    val colorHex: String
    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF10B981)
        }

    // Financial & depreciation rules
    val allowsDepreciation: Boolean
    val allowsImprovements: Boolean
    val allowsRevaluation: Boolean
    val allowsDisposal: Boolean
    val allowsMaintenance: Boolean
    val isTerminal: Boolean
    val isReadOnly: Boolean

    // Audit and documentation
    val title: String
    val financialRationale: String
    val calculationEngineBehavior: String
    val uiRestrictionsDescription: String
    val triggerDescription: String
    val allowedTargetStates: List<String>
}
