package com.example.domain.usecase

import java.util.Locale

/**
 * Domain calculation result for real-time reactive budget computation.
 */
data class BudgetSummaryResult(
    val totalAmount: Double,
    val allocatedAmount: Double,
    val unassignedAmount: Double,
    val progressPercentage: Float, // 0 to 100+
    val isOverbudget: Boolean,
    val isZeroBasedBalanced: Boolean,
    val ruleTitle: String,
    val ruleMessage: String,
    val ruleBannerColorHex: String,
    val isFormSubmittable: Boolean
)

/**
 * Reactive calculation engine for budgets and zero-based budgeting enforcement.
 */
class CalculateBudgetSummaryUseCase {

    operator fun invoke(
        name: String,
        totalAmount: Double,
        envelopeAmounts: List<Double>
    ): BudgetSummaryResult {
        val allocated = envelopeAmounts.sum()
        val unassigned = totalAmount - allocated
        val progress = if (totalAmount > 0.0) {
            ((allocated / totalAmount) * 100.0).toFloat()
        } else {
            0f
        }

        val isOver = totalAmount > 0.0 && allocated > totalAmount
        val isZeroBalanced = totalAmount > 0.0 && Math.abs(unassigned) < 0.01 && allocated > 0.0

        val (title, message, colorHex) = when {
            totalAmount <= 0.0 -> Triple(
                "Define tu techo financiero",
                "Ingresa el monto total que esperas administrar en este periodo.",
                "#0284C7"
            )
            isOver -> Triple(
                "Presupuesto sobreasignado",
                "Has asignado $ ${formatCurrency(Math.abs(unassigned))} más que tu techo total ($ ${formatCurrency(totalAmount)}). Reduce los montos de las categorías para poder guardar.",
                "#EF4444"
            )
            isZeroBalanced -> Triple(
                "¡Presupuesto Base Cero 100% equilibrado!",
                "Excelente disciplina financiera: cada peso tiene un destino específico asignado.",
                "#10B981"
            )
            allocated == 0.0 -> Triple(
                "Sin categorías asignadas",
                "Comienza agregando tus categorías de gasto (Alimentación, Vivienda, etc.) con sus montos proyectados.",
                "#64748B"
            )
            else -> Triple(
                "Presupuesto en construcción",
                "Tienes $ ${formatCurrency(unassigned)} sin asignar. Puedes distribuirlo en otras categorías o reservarlo como ahorro de colchón.",
                "#0284C7"
            )
        }

        val isNameValid = name.trim().isNotBlank()
        val isSubmittable = isNameValid && totalAmount > 0.0 && !isOver

        return BudgetSummaryResult(
            totalAmount = totalAmount,
            allocatedAmount = allocated,
            unassignedAmount = unassigned,
            progressPercentage = progress,
            isOverbudget = isOver,
            isZeroBasedBalanced = isZeroBalanced,
            ruleTitle = title,
            ruleMessage = message,
            ruleBannerColorHex = colorHex,
            isFormSubmittable = isSubmittable
        )
    }

    private fun formatCurrency(value: Double): String {
        return String.format(Locale.GERMANY, "%,.0f", value)
    }
}
