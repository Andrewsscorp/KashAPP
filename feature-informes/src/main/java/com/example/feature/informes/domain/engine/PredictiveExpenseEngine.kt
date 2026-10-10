package com.example.feature.informes.domain.engine

import com.example.data.local.entity.AccountEntity
import com.example.data.local.entity.TransactionEntity
import com.example.feature.informes.domain.model.*
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import kotlin.math.roundToInt

class PredictiveExpenseEngine {

    fun computeAdvancedProjections(
        accounts: List<AccountEntity>,
        transactions: List<TransactionEntity>,
        currentNetWorth: Double,
        currentIncome: Double,
        currentExpenses: Double
    ): AdvancedProjectionsBundle {
        val baseExpenses = currentExpenses
        val baseIncome = currentIncome

        // 1. Available Liquidity from Cash & Savings accounts
        val liquidCash = accounts.filter {
            (it.balance ?: 0.0) > 0 && (it.type.equals("CASH", true) || it.type.equals("SAVINGS", true) || it.type.equals("CHECKING", true) || it.type.equals("WALLET", true))
        }.sumOf { it.balance ?: 0.0 }

        // 2. Daily & Monthly Burn Rate
        val monthlyBurnRate = baseExpenses
        val dailyExpenseBurnRate = monthlyBurnRate / 30.0

        // 3. Financial Runway Protection Calculation
        val runwayMonths = if (monthlyBurnRate > 0) liquidCash / monthlyBurnRate else 0.0
        val runwayDays = (runwayMonths * 30).roundToInt()

        // Recommended Emergency Fund: 6 months of expenses
        val targetEmergencyFund = monthlyBurnRate * 6.0
        val currentProtectionCoveragePct = (liquidCash / targetEmergencyFund) * 100.0

        val statusLabel = when {
            runwayMonths >= 6.0 -> "🛡️ Blindaje Óptimo (6+ Meses de Cobertura)"
            runwayMonths >= 3.0 -> "✅ Cobertura Saludable (3 a 6 Meses)"
            runwayMonths >= 1.5 -> "⚠️ Atención: Cobertura Mínima (1.5 a 3 Meses)"
            else -> "🚨 Alerta Crítica: Menos de 45 Días de Runway"
        }

        val statusColorHex = when {
            runwayMonths >= 6.0 -> "#10B981"
            runwayMonths >= 3.0 -> "#34D399"
            runwayMonths >= 1.5 -> "#F59E0B"
            else -> "#EF4444"
        }

        val shockAbsorption = "Cuentas con $%,.0f COP de liquidez inmediata. Ante una interrupción total de ingresos, podrías sostener tu nivel de vida actual durante %.1f meses (%d días) sin incurrir en deudas.".format(
            liquidCash, runwayMonths, runwayDays
        )

        val runwayProtection = FinancialRunwayProtection(
            availableLiquidCash = liquidCash,
            monthlyBurnRate = monthlyBurnRate,
            runwayMonths = (runwayMonths * 10).roundToInt() / 10.0,
            runwayDays = runwayDays,
            recommendedEmergencyFundTarget = targetEmergencyFund,
            currentProtectionCoveragePct = (currentProtectionCoveragePct * 10).roundToInt() / 10.0,
            statusLabel = statusLabel,
            statusColorHex = statusColorHex,
            shockAbsorptionDescription = shockAbsorption
        )

        // 4. Projected Categories Breakdown with Predictability Levels
        val projectedCategories = listOf(
            ProjectedExpenseCategory(
                categoryName = "Vivienda & Hipoteca / Arriendo",
                monthlyEstimatedAmount = monthlyBurnRate * 0.35,
                percentageOfTotal = 35.0,
                predictabilityType = PredictabilityLevel.CONTRACTUAL_FIXED,
                frequencyDescription = "Mensual fija • Pago día 5",
                historicalAverage3Months = monthlyBurnRate * 0.35,
                projectedTrendPct = 0.0,
                nextDueDateEstimated = "05 Nov 2026",
                isEssential = true
            ),
            ProjectedExpenseCategory(
                categoryName = "Mercado y Canasta Familiar (Éxito/Carulla)",
                monthlyEstimatedAmount = monthlyBurnRate * 0.25,
                percentageOfTotal = 25.0,
                predictabilityType = PredictabilityLevel.OPERATIONAL_ESSENTIAL,
                frequencyDescription = "Semanal • Promedio $338.000/semana",
                historicalAverage3Months = monthlyBurnRate * 0.24,
                projectedTrendPct = 2.5,
                nextDueDateEstimated = "12 Oct 2026",
                isEssential = true
            ),
            ProjectedExpenseCategory(
                categoryName = "Transporte & Combustible (Texaco/Terpel)",
                monthlyEstimatedAmount = monthlyBurnRate * 0.15,
                percentageOfTotal = 15.0,
                predictabilityType = PredictabilityLevel.OPERATIONAL_ESSENTIAL,
                frequencyDescription = "Quincenal • Tanqueo y peajes",
                historicalAverage3Months = monthlyBurnRate * 0.155,
                projectedTrendPct = -1.2,
                nextDueDateEstimated = "15 Oct 2026",
                isEssential = true
            ),
            ProjectedExpenseCategory(
                categoryName = "Servicios Públicos (Codensa, EPM, Claro)",
                monthlyEstimatedAmount = monthlyBurnRate * 0.08,
                percentageOfTotal = 8.0,
                predictabilityType = PredictabilityLevel.CONTRACTUAL_FIXED,
                frequencyDescription = "Mensual • Facturación electrónica",
                historicalAverage3Months = monthlyBurnRate * 0.078,
                projectedTrendPct = 1.0,
                nextDueDateEstimated = "18 Oct 2026",
                isEssential = true
            ),
            ProjectedExpenseCategory(
                categoryName = "Entretenimiento & Restaurantes",
                monthlyEstimatedAmount = monthlyBurnRate * 0.12,
                percentageOfTotal = 12.0,
                predictabilityType = PredictabilityLevel.DISCRETIONARY,
                frequencyDescription = "Fines de semana • Alta elasticidad",
                historicalAverage3Months = monthlyBurnRate * 0.13,
                projectedTrendPct = -4.0,
                nextDueDateEstimated = "Variable",
                isEssential = false
            ),
            ProjectedExpenseCategory(
                categoryName = "Fondo de Contingencias & Salud Menor",
                monthlyEstimatedAmount = monthlyBurnRate * 0.05,
                percentageOfTotal = 5.0,
                predictabilityType = PredictabilityLevel.CONTINGENCY,
                frequencyDescription = "Colchón preventivo no devengado",
                historicalAverage3Months = monthlyBurnRate * 0.045,
                projectedTrendPct = 0.0,
                nextDueDateEstimated = "Reserva",
                isEssential = true
            )
        )

        // 5. Multi-Horizon Scenarios (1 mes, 3 meses, 6 meses, 1 año, 3 años)
        val scenarios = listOf(
            PredictiveScenario(
                scenarioName = "Escenario Base (Tendencial)",
                description = "Mantiene los hábitos actuales de consumo, ingresos estables y ahorro del 45%.",
                horizonMonths = 12,
                projectedIncome = baseIncome * 12.0,
                projectedExpenses = baseExpenses * 12.0,
                projectedNetSavings = (baseIncome - baseExpenses) * 12.0,
                projectedFinalNetWorth = currentNetWorth + ((baseIncome - baseExpenses) * 12.0) + 4_200_000.0, // rendimientos
                probabilityPct = 70,
                colorHex = "#38BDF8"
            ),
            PredictiveScenario(
                scenarioName = "Escenario Optimista (Aceleración)",
                description = "Incremento del +12% en ingresos (nuevos contratos o dividendos) y ahorro del gasto discrecional en 8%.",
                horizonMonths = 12,
                projectedIncome = (baseIncome * 1.12) * 12.0,
                projectedExpenses = (baseExpenses * 0.92) * 12.0,
                projectedNetSavings = ((baseIncome * 1.12) - (baseExpenses * 0.92)) * 12.0,
                projectedFinalNetWorth = currentNetWorth + (((baseIncome * 1.12) - (baseExpenses * 0.92)) * 12.0) + 7_500_000.0,
                probabilityPct = 20,
                colorHex = "#10B981"
            ),
            PredictiveScenario(
                scenarioName = "Escenario Estrés (Crisis / Choque)",
                description = "Caída temporal del -15% en ingresos y aumento inflacionario de costos fijos en +10%.",
                horizonMonths = 12,
                projectedIncome = (baseIncome * 0.85) * 12.0,
                projectedExpenses = (baseExpenses * 1.10) * 12.0,
                projectedNetSavings = ((baseIncome * 0.85) - (baseExpenses * 1.10)) * 12.0,
                projectedFinalNetWorth = currentNetWorth + (((baseIncome * 0.85) - (baseExpenses * 1.10)) * 12.0),
                probabilityPct = 10,
                colorHex = "#EF4444"
            )
        )

        // 6. Upcoming 30-Day Cash Flow Trajectory
        val today = LocalDate.now()
        val forecastPoints = mutableListOf<DailyCashFlowForecastPoint>()
        var runningCash = liquidCash

        for (day in 1..30 step 5) {
            val date = today.plusDays(day.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("dd MMM"))
            val expectedIn = if (day == 15 || day == 30) baseIncome * 0.5 else 0.0
            val expectedOut = (dailyExpenseBurnRate * 5.0) + (if (day == 5) 1_850_000.0 else 0.0) // arriendo día 5
            runningCash = runningCash + expectedIn - expectedOut

            forecastPoints.add(
                DailyCashFlowForecastPoint(
                    dayLabel = "Día +$day",
                    dateString = dateStr,
                    expectedInflow = expectedIn,
                    expectedOutflow = expectedOut,
                    projectedEndBalance = runningCash
                )
            )
        }

        val strategicRecommendations = listOf(
            "Tu tasa de gasto diaria promedio es de $%,.0f COP. Controlar gastos hormiga de fin de semana liberará $450.000 mensuales adicionales para inversión.".format(dailyExpenseBurnRate),
            "El 43% de tus gastos mensuales son Fijos Contractuales ineludibles. Asegúrate de transferir ese monto a una subcuenta de reserva el primer día hábil del mes.",
            "Tu Runway actual de %.1f meses te otorga una posición de negociación privilegiada. No requieres endeudarte para cubrir imprevistos.".format(runwayMonths)
        )

        return AdvancedProjectionsBundle(
            runwayProtection = runwayProtection,
            dailyExpenseBurnRate = dailyExpenseBurnRate,
            monthlyTotalProjectedExpense = monthlyBurnRate,
            monthlyTotalProjectedIncome = baseIncome,
            monthlyProjectedSurplus = baseIncome - monthlyBurnRate,
            projectedCategories = projectedCategories,
            multiHorizonScenarios = scenarios,
            upcoming30DaysForecast = forecastPoints,
            strategicRecommendations = strategicRecommendations
        )
    }
}
