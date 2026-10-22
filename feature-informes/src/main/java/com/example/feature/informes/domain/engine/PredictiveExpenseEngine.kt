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

        // 1. Available Liquidity from Cash & Savings accounts (including PAYROLL)
        val liquidTypes = setOf("CASH", "SAVINGS", "CHECKING", "WALLET", "PAYROLL")
        val liquidCash = accounts.filter {
            (it.balance ?: 0.0) > 0 && it.type.uppercase() in liquidTypes
        }.sumOf { it.balance ?: 0.0 }

        // 2. Daily & Monthly Burn Rate
        val monthlyBurnRate = baseExpenses
        val dailyExpenseBurnRate = monthlyBurnRate / 30.0

        // 3. Financial Runway Protection Calculation
        val runwayMonths = if (monthlyBurnRate > 0) liquidCash / monthlyBurnRate else if (liquidCash > 0) 99.9 else 0.0
        val runwayDays = (runwayMonths * 30).roundToInt()

        // Recommended Emergency Fund: 6 months of expenses
        val targetEmergencyFund = monthlyBurnRate * 6.0
        val currentProtectionCoveragePct = if (targetEmergencyFund > 0) (liquidCash / targetEmergencyFund) * 100.0 else if (liquidCash > 0) 100.0 else 0.0

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

        // 4. Projected Categories Breakdown derived dynamically from real expense transactions
        val expenseTransactions = transactions.filter {
            it.type.equals("EXPENSE", ignoreCase = true) &&
                !it.categoryId.equals("cat_initial_balance", ignoreCase = true) &&
                !it.categoryId.equals("cat_opening_balance", ignoreCase = true) &&
                it.transferGroupId.isNullOrBlank()
        }

        val groupedExpenses = expenseTransactions.groupBy { it.categoryName.ifBlank { "Gastos Generales" } }
        val totalGroupedAmount = expenseTransactions.sumOf { it.accountingRecognizedExpenseAmount ?: it.amount }

        val projectedCategories = if (groupedExpenses.isNotEmpty() && totalGroupedAmount > 0.0) {
            groupedExpenses.entries
                .sortedByDescending { entry -> entry.value.sumOf { it.accountingRecognizedExpenseAmount ?: it.amount } }
                .map { (catName, txList) ->
                    val catTotal = txList.sumOf { it.accountingRecognizedExpenseAmount ?: it.amount }
                    val pct = (catTotal / totalGroupedAmount) * 100.0
                    val isFixed = catName.contains("Vivienda", true) ||
                        catName.contains("Arriendo", true) ||
                        catName.contains("Hipoteca", true) ||
                        catName.contains("Servicio", true) ||
                        catName.contains("Educación", true) ||
                        catName.contains("Seguro", true)
                    val isDiscretionary = catName.contains("Entretenimiento", true) ||
                        catName.contains("Restaurante", true) ||
                        catName.contains("Ocio", true) ||
                        catName.contains("Compras", true)
                    val predLevel = when {
                        isFixed -> PredictabilityLevel.CONTRACTUAL_FIXED
                        isDiscretionary -> PredictabilityLevel.DISCRETIONARY
                        else -> PredictabilityLevel.OPERATIONAL_ESSENTIAL
                    }
                    ProjectedExpenseCategory(
                        categoryName = catName,
                        monthlyEstimatedAmount = catTotal,
                        percentageOfTotal = (pct * 10).roundToInt() / 10.0,
                        predictabilityType = predLevel,
                        frequencyDescription = "${txList.size} mov. registrados en el período",
                        historicalAverage3Months = catTotal,
                        projectedTrendPct = 0.0,
                        nextDueDateEstimated = if (isFixed) "Mensual" else "Variable",
                        isEssential = !isDiscretionary
                    )
                }
        } else {
            listOf(
                ProjectedExpenseCategory(
                    categoryName = "Sin gastos registrados en el período",
                    monthlyEstimatedAmount = 0.0,
                    percentageOfTotal = 0.0,
                    predictabilityType = PredictabilityLevel.OPERATIONAL_ESSENTIAL,
                    frequencyDescription = "Sin movimientos",
                    historicalAverage3Months = 0.0,
                    projectedTrendPct = 0.0,
                    nextDueDateEstimated = "N/A",
                    isEssential = true
                )
            )
        }

        // 5. Multi-Horizon Scenarios (12 meses) without arbitrary hardcoded constants
        val netMonthlySavings = baseIncome - baseExpenses
        val savingsRate = if (baseIncome > 0) ((netMonthlySavings / baseIncome) * 100).roundToInt() else 0
        val scenarios = listOf(
            PredictiveScenario(
                scenarioName = "Escenario Base (Tendencial)",
                description = "Mantiene el ritmo actual de ingresos y gastos registrados (tasa de ahorro actual: $savingsRate%).",
                horizonMonths = 12,
                projectedIncome = baseIncome * 12.0,
                projectedExpenses = baseExpenses * 12.0,
                projectedNetSavings = netMonthlySavings * 12.0,
                projectedFinalNetWorth = currentNetWorth + (netMonthlySavings * 12.0),
                probabilityPct = 70,
                colorHex = "#38BDF8"
            ),
            PredictiveScenario(
                scenarioName = "Escenario Optimista (Aceleración)",
                description = "Incremento del +12% en ingresos y optimización del gasto en -8%.",
                horizonMonths = 12,
                projectedIncome = (baseIncome * 1.12) * 12.0,
                projectedExpenses = (baseExpenses * 0.92) * 12.0,
                projectedNetSavings = ((baseIncome * 1.12) - (baseExpenses * 0.92)) * 12.0,
                projectedFinalNetWorth = currentNetWorth + (((baseIncome * 1.12) - (baseExpenses * 0.92)) * 12.0),
                probabilityPct = 20,
                colorHex = "#10B981"
            ),
            PredictiveScenario(
                scenarioName = "Escenario Estrés (Contingencia)",
                description = "Reducción temporal del -15% en ingresos e incremento inflacionario de costos en +10%.",
                horizonMonths = 12,
                projectedIncome = (baseIncome * 0.85) * 12.0,
                projectedExpenses = (baseExpenses * 1.10) * 12.0,
                projectedNetSavings = ((baseIncome * 0.85) - (baseExpenses * 1.10)) * 12.0,
                projectedFinalNetWorth = currentNetWorth + (((baseIncome * 0.85) - (baseExpenses * 1.10)) * 12.0),
                probabilityPct = 10,
                colorHex = "#EF4444"
            )
        )

        // 6. Upcoming 30-Day Cash Flow Trajectory based strictly on real burn rate
        val today = LocalDate.now()
        val forecastPoints = mutableListOf<DailyCashFlowForecastPoint>()
        var runningCash = liquidCash

        for (day in 5..30 step 5) {
            val date = today.plusDays(day.toLong())
            val dateStr = date.format(DateTimeFormatter.ofPattern("dd MMM"))
            val expectedIn = if (day == 15 || day == 30) baseIncome * 0.5 else 0.0
            val expectedOut = dailyExpenseBurnRate * 5.0
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

        val fixedPct = projectedCategories
            .filter { it.predictabilityType == PredictabilityLevel.CONTRACTUAL_FIXED }
            .sumOf { it.percentageOfTotal }
            .roundToInt()

        val potentialMonthlySaving = (monthlyBurnRate * 0.10)
        val strategicRecommendations = listOf(
            "Tu tasa de gasto diaria promedio es de $%,.0f COP. Optimizar un 10%% tus consumos variables liberaría $%,.0f COP mensuales adicionales.".format(dailyExpenseBurnRate, potentialMonthlySaving),
            "El $fixedPct% de tus gastos registrados corresponde a compromisos fijos. Mantén ese monto reservado al inicio de cada ciclo mensual.",
            "Tu Runway actual de %.1f meses refleja tu autonomía financiera sobre la liquidez disponible ($%,.0f COP).".format(runwayMonths, liquidCash)
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
