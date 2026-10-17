package com.example.domain.model.fixedassets

import androidx.compose.ui.graphics.Color

/**
 * High-level categorization of fixed assets.
 */
enum class AssetType(val label: String) {
    TANGIBLE("Activo Tangible"),
    INTANGIBLE("Activo Intangible")
}

/**
 * Standard enterprise fixed asset categories with Colombian NIIF / IFRS standard useful lives.
 */
enum class AssetCategory(
    val title: String,
    val iconName: String,
    val colorHex: String,
    val defaultUsefulLifeMonths: Int,
    val defaultResidualPercent: Double,
    val isAppreciable: Boolean = false // Real estate typically appreciates in market value
) {
    REAL_ESTATE(
        title = "Bienes Raíces e Inmuebles",
        iconName = "apartment",
        colorHex = "#10B981",
        defaultUsefulLifeMonths = 240, // 20 years
        defaultResidualPercent = 0.50,
        isAppreciable = true
    ),
    VEHICLES(
        title = "Vehículos y Transporte",
        iconName = "directions_car",
        colorHex = "#0284C7",
        defaultUsefulLifeMonths = 60, // 5 years
        defaultResidualPercent = 0.20
    ),
    TECHNOLOGY(
        title = "Tecnología y Cómputo",
        iconName = "laptop_chromebook",
        colorHex = "#6366F1",
        defaultUsefulLifeMonths = 36, // 3 years
        defaultResidualPercent = 0.10
    ),
    MACHINERY(
        title = "Maquinaria y Equipo",
        iconName = "precision_manufacturing",
        colorHex = "#F59E0B",
        defaultUsefulLifeMonths = 120, // 10 years
        defaultResidualPercent = 0.15
    ),
    FURNITURE(
        title = "Muebles y Enseres",
        iconName = "chair",
        colorHex = "#EC4899",
        defaultUsefulLifeMonths = 60, // 5 years
        defaultResidualPercent = 0.10
    ),
    SOFTWARE_LICENSES(
        title = "Software y Licencias",
        iconName = "code",
        colorHex = "#8B5CF6",
        defaultUsefulLifeMonths = 36, // 3 years
        defaultResidualPercent = 0.0
    ),
    PATENTS_BRANDS(
        title = "Marcas y Patentes",
        iconName = "verified",
        colorHex = "#14B8A6",
        defaultUsefulLifeMonths = 120, // 10 years
        defaultResidualPercent = 0.0
    ),
    OTHER(
        title = "Otros Activos Fijos",
        iconName = "inventory_2",
        colorHex = "#64748B",
        defaultUsefulLifeMonths = 60,
        defaultResidualPercent = 0.10
    );

    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF0284C7)
        }

    companion object {
        fun fromString(value: String): AssetCategory {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.title.equals(value, ignoreCase = true) ||
                value.contains(it.name, ignoreCase = true) ||
                value.contains(it.title, ignoreCase = true)
            } ?: OTHER
        }
    }
}

/**
 * Standard accounting depreciation methods.
 */
enum class DepreciationMethod(
    val title: String,
    val description: String
) {
    STRAIGHT_LINE(
        title = "Línea Recta (Constante)",
        description = "Monto de depreciación uniforme cada mes durante la vida útil."
    ),
    DIGITS_SUM(
        title = "Suma de Dígitos (Acelerada)",
        description = "Mayor depreciación en los primeros periodos de uso del activo."
    ),
    DECLINING_BALANCE(
        title = "Saldos Decrecientes",
        description = "Aplica un factor porcentual fijo sobre el valor en libros remanente."
    );

    companion object {
        fun fromString(value: String): DepreciationMethod {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.title.equals(value, ignoreCase = true) ||
                (value.contains("recta", ignoreCase = true) && it == STRAIGHT_LINE) ||
                (value.contains("dígitos", ignoreCase = true) && it == DIGITS_SUM) ||
                (value.contains("digitos", ignoreCase = true) && it == DIGITS_SUM) ||
                (value.contains("decrecientes", ignoreCase = true) && it == DECLINING_BALANCE)
            } ?: STRAIGHT_LINE
        }
    }
}

/**
 * Operational lifecycle status of a fixed asset.
 */
enum class AssetStatus(val label: String, val colorHex: String) {
    ACTIVE("En uso", "#10B981"),
    MAINTENANCE("En mantenimiento", "#38BDF8"),
    SOLD("Vendido", "#0284C7"),
    WRITTEN_OFF("Dado de baja", "#94A3B8");

    val color: Color
        get() = try {
            Color(android.graphics.Color.parseColor(colorHex))
        } catch (_: Exception) {
            Color(0xFF10B981)
        }

    companion object {
        fun fromString(value: String): AssetStatus {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.label.equals(value, ignoreCase = true) ||
                (value.contains("uso", ignoreCase = true) && it == ACTIVE) ||
                (value.contains("mantenimiento", ignoreCase = true) && it == MAINTENANCE) ||
                (value.contains("baja", ignoreCase = true) && it == WRITTEN_OFF)
            } ?: ACTIVE
        }
    }
}

/**
 * Financial and operational movement types recorded against an asset.
 */
enum class AssetMovementType(
    val label: String,
    val defaultTitle: String,
    val isDebit: Boolean // True increases asset book value, False decreases it
) {
    ACQUISITION("Adquisición", "Compra y alta de activo", true),
    DEPRECIATION("Depreciación", "Depreciación contable del periodo", false),
    AMORTIZATION("Amortización", "Amortización de intangible", false),
    IMPROVEMENT("Mejora / Adición", "Inversión en mejora que incrementa vida útil o valor", true),
    REVALUATION("Revalúo Técnico", "Ajuste por avalúo comercial o plusvalía", true),
    IMPAIRMENT("Deterioro de Valor", "Reconocimiento de pérdida por deterioro de valor", false),
    MAINTENANCE("Mantenimiento", "Mantenimiento preventivo / correctivo mayor", false),
    SALE("Venta de Activo", "Disposición y enajenación del bien", false),
    DISPOSAL("Baja Definitiva", "Baja por obsolescencia o siniestro", false),
    STATUS_CHANGE("Cambio de Estado", "Transición en ciclo de vida del activo", false);

    companion object {
        fun fromString(value: String): AssetMovementType {
            return entries.firstOrNull {
                it.name.equals(value, ignoreCase = true) ||
                it.label.equals(value, ignoreCase = true) ||
                (value.contains("estado", ignoreCase = true) && it == STATUS_CHANGE) ||
                (value.contains("transicion", ignoreCase = true) && it == STATUS_CHANGE) ||
                (value.contains("mantenimiento", ignoreCase = true) && it == MAINTENANCE) ||
                (value.contains("mejora", ignoreCase = true) && it == IMPROVEMENT) ||
                (value.contains("revalu", ignoreCase = true) && it == REVALUATION) ||
                (value.contains("deterioro", ignoreCase = true) && it == IMPAIRMENT) ||
                (value.contains("baja", ignoreCase = true) && it == DISPOSAL) ||
                (value.contains("compra", ignoreCase = true) && it == ACQUISITION) ||
                (value.contains("adquisici", ignoreCase = true) && it == ACQUISITION) ||
                (value.contains("deprecia", ignoreCase = true) && it == DEPRECIATION)
            } ?: STATUS_CHANGE
        }
    }
}
