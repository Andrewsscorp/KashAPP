package com.example.presentation.fixedassets.ai

import com.example.domain.model.fixedassets.AssetCategory
import com.example.domain.model.fixedassets.AssetType
import com.example.domain.model.fixedassets.DepreciationMethod

data class AssetAiSuggestion(
    val suggestedUsefulLifeMonths: Int,
    val suggestedUsefulLifeYears: Double,
    val suggestedResidualPercent: Double,
    val suggestedDepreciationMethod: DepreciationMethod,
    val justification: String,
    val accountingStandard: String = "NIC 16 / NIIF (IAS 16 Property, Plant and Equipment)",
    val isAutoDepreciableAtMonthEnd: Boolean = true
)

object AssetAiSuggestionEngine {

    /**
     * Sugiere los parámetros contables ideales para un activo según su nombre, categoría y naturaleza.
     * Basado en la Norma Internacional de Contabilidad NIC 16 y preparado para delegar a la API de IA.
     */
    fun suggestParameters(
        name: String,
        category: AssetCategory,
        type: AssetType,
        acquisitionValue: Double
    ): AssetAiSuggestion {
        val lowerName = name.lowercase().trim()

        return when {
            // Vehículos y Flota
            category == AssetCategory.VEHICLES || lowerName.contains("carro") || lowerName.contains("camioneta") ||
                    lowerName.contains("moto") || lowerName.contains("camión") || lowerName.contains("vehículo") || lowerName.contains("automóvil") -> {
                val isHeavy = lowerName.contains("camión") || lowerName.contains("pesado") || lowerName.contains("volqueta")
                val months = if (isHeavy) 120 else 60
                val residualPct = 0.15 // 15% valor de salvamento típico en automotores
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "Bajo NIC 16 §50, los vehículos comerciales se deprecian típicamente en ${months / 12} años con un valor residual del ${(residualPct * 100).toInt()}% debido a su valor de reventa en el mercado secundario automotriz."
                )
            }

            // Tecnología, Cómputo y Servidores
            category == AssetCategory.TECHNOLOGY || lowerName.contains("laptop") || lowerName.contains("macbook") ||
                    lowerName.contains("computador") || lowerName.contains("pc") || lowerName.contains("servidor") ||
                    lowerName.contains("ipad") || lowerName.contains("celular") || lowerName.contains("impresora") -> {
                val isServer = lowerName.contains("servidor") || lowerName.contains("rack") || lowerName.contains("switch")
                val months = if (isServer) 48 else 36 // 3 o 4 años por obsolescencia tecnológica
                val residualPct = 0.0 // Obsolescencia rápida
                val method = if (lowerName.contains("laptop") || lowerName.contains("celular")) {
                    DepreciationMethod.DECLINING_BALANCE // Mayor desgaste los primeros años
                } else {
                    DepreciationMethod.STRAIGHT_LINE
                }
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = method,
                    justification = "Equipos electrónicos y de cómputo sufren obsolescencia tecnológica acelerada. Se recomienda una vida útil de ${months / 12} años sin valor residual ($0 COP) y método ${if (method == DepreciationMethod.DECLINING_BALANCE) "Saldos Decrecientes" else "Línea Recta"} según NIC 16 §60."
                )
            }

            // Inmuebles, Edificaciones y Terrenos
            category == AssetCategory.REAL_ESTATE || lowerName.contains("edificio") || lowerName.contains("bodega") ||
                    lowerName.contains("oficina") || lowerName.contains("local") || lowerName.contains("apartamento") -> {
                val isWarehouse = lowerName.contains("bodega") || lowerName.contains("nave")
                val months = if (isWarehouse) 360 else 480 // 30 o 40 años
                val residualPct = 0.20 // 20% salvamento atribuible a estructura remanente
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "Las edificaciones e inmuebles comerciales poseen vidas útiles prolongadas (20 a 40 años). Se proyecta un valor residual del ${(residualPct * 100).toInt()}% para cubrir el valor residual de la construcción según NIC 16 §58."
                )
            }

            // Maquinaria y Equipos Industriales
            category == AssetCategory.MACHINERY || lowerName.contains("máquina") || lowerName.contains("torno") ||
                    lowerName.contains("planta") || lowerName.contains("generador") || lowerName.contains("montacargas") -> {
                val months = 120 // 10 años
                val residualPct = 0.10
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "La maquinaria industrial operativa mantiene una vida útil media de 10 años (120 meses). El valor residual del 10% representa su valor de chatarreo o recuperación técnica garantizada bajo NIC 16."
                )
            }

            // Muebles y Enseres
            category == AssetCategory.FURNITURE || lowerName.contains("escritorio") || lowerName.contains("silla") ||
                    lowerName.contains("mesa") || lowerName.contains("archivador") || lowerName.contains("mueble") -> {
                val months = 120 // 10 años
                val residualPct = 0.05
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "Mobiliario y enseres corporativos poseen depreciación lineal a 10 años con un valor residual conservador del 5% al finalizar su vida económica útil."
                )
            }

            // Intangibles: Software, Licencias, Marcas, Patentes
            type == AssetType.INTANGIBLE || category == AssetCategory.SOFTWARE_LICENSES || category == AssetCategory.PATENTS_BRANDS ||
                    lowerName.contains("software") || lowerName.contains("licencia") || lowerName.contains("erp") ||
                    lowerName.contains("patente") || lowerName.contains("marca") -> {
                val months = if (lowerName.contains("patente")) 120 else 36 // 3 años para software, 10 para patentes
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = 0.0,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "Bajo NIC 38 (Activos Intangibles), los programas informáticos y licencias se amortizan en su periodo de vigencia económica (usualmente 3 a 5 años) con valor residual nulo ($0 COP)."
                )
            }

            // Valor por defecto estándar
            else -> {
                val months = category.defaultUsefulLifeMonths
                val residualPct = category.defaultResidualPercent
                AssetAiSuggestion(
                    suggestedUsefulLifeMonths = months,
                    suggestedUsefulLifeYears = months / 12.0,
                    suggestedResidualPercent = residualPct,
                    suggestedDepreciationMethod = DepreciationMethod.STRAIGHT_LINE,
                    justification = "Parámetro estimado para la categoría ${category.title} conforme a los estándares estándar NIIF Pymes / NIC 16."
                )
            }
        }
    }
}
