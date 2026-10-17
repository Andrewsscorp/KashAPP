package com.example.feature.gastoscompartidos.domain.engine

import com.example.core.error.DomainError
import com.example.core.result.Outcome
import com.example.domain.models.shared.RoundingRemainderTarget
import com.example.domain.models.shared.RoundingStepRule
import com.example.domain.models.shared.SplitCalculationMethod
import com.example.domain.models.shared.SplitSettlementStatus
import com.example.domain.models.shared.TaxAndTipProrationRule
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.roundToLong

object SharedSplitCalculatorEngine {

    data class ParticipantSplitInput(
        val participantId: String,
        val displayName: String,
        val isSelf: Boolean = false,
        val isIncluded: Boolean = true,
        val exactAmountInput: Double = 0.0,
        val percentageInput: Double = 0.0,
        val sharesUnitsInput: Double = 1.0,
        val adjustmentAmountInput: Double = 0.0,
        val monthlyIncomeInput: Double = 0.0,
        val paidAmountAtRegister: Double = 0.0,
        val currentUnpaidDebtWithUser: Double = 0.0,
        val creditTrustLimit: Double? = null
    )

    data class OcrLineItemInput(
        val itemId: String,
        val description: String,
        val quantity: Double = 1.0,
        val unitPrice: Double,
        val lineTotalPrice: Double,
        val isTaxExempt: Boolean = false,
        val assignedParticipantIds: List<String>,
        val participantWeightsWithinItem: Map<String, Double> = emptyMap()
    )

    data class SplitCalculationRequest(
        val subtotalAmount: Double,
        val taxIvaAmount: Double = 0.0,
        val taxConsumptionAmount: Double = 0.0,
        val tipAmount: Double = 0.0,
        val discountAmount: Double = 0.0,
        val gmf4x1000Amount: Double = 0.0,
        val creditCardInterestSurcharge: Double = 0.0,
        val prorateGmfAmongDebtors: Boolean = false,
        val passCreditCardInterestToGroup: Boolean = false,
        val splitMethod: SplitCalculationMethod,
        val taxAndTipProrationRule: TaxAndTipProrationRule = TaxAndTipProrationRule.PROPORTIONAL_TO_SUBTOTAL,
        val roundingStepRule: RoundingStepRule = RoundingStepRule.ROUND_1,
        val roundingRemainderTarget: RoundingRemainderTarget = RoundingRemainderTarget.PAYER,
        val consecutiveSequenceNumber: Long = 1L,
        val participants: List<ParticipantSplitInput>,
        val ocrLineItems: List<OcrLineItemInput> = emptyList()
    ) {
        val effectiveTotalAmount: Double
            get() {
                val baseTotal = (subtotalAmount + taxIvaAmount + taxConsumptionAmount + tipAmount - discountAmount)
                    .coerceAtLeast(0.0)
                val gmfToInclude = if (prorateGmfAmongDebtors) gmf4x1000Amount else 0.0
                val ccInterestToInclude = if (passCreditCardInterestToGroup) creditCardInterestSurcharge else 0.0
                return roundToCents(baseTotal + gmfToInclude + ccInterestToInclude)
            }
    }

    data class ComputedParticipantSplit(
        val participantId: String,
        val displayName: String,
        val isSelf: Boolean,
        val isIncluded: Boolean,
        val rawWeightOrPercentage: Double,
        val baseSubtotalShare: Double,
        val allocatedTaxAmount: Double,
        val allocatedTipAmount: Double,
        val allocatedDiscountAmount: Double,
        val allocatedGmfAndFinancialAmount: Double,
        val unroundedExactShare: Double,
        val roundingAdjustment: Double,
        val totalOwedShare: Double,
        val paidByThisParticipant: Double,
        val netBalanceForExpense: Double, // + le deben a este participante / - este participante debe pagar
        val remainingPendingAmount: Double,
        val initialSettlementStatus: SplitSettlementStatus
    )

    data class SplitCalculationSummary(
        val effectiveTotalAmount: Double,
        val subtotalAmount: Double,
        val totalTaxesAndTip: Double,
        val totalRoundingDeltaApplied: Double,
        val remainderAssignedToParticipantId: String?,
        val participantSplits: List<ComputedParticipantSplit>
    )

    /**
     * Ejecuta el cálculo completo validando todas las precondiciones contables y de límites de confianza.
     */
    fun calculateSplits(
        request: SplitCalculationRequest
    ): Outcome<SplitCalculationSummary, DomainError> {
        val totalAmount = request.effectiveTotalAmount
        if (totalAmount <= 0.0) {
            return Outcome.Failure(DomainError.Validation.NegativeOrZeroAmount(totalAmount))
        }

        val activeParticipants = request.participants.filter { it.isIncluded }
        if (activeParticipants.isEmpty()) {
            return Outcome.Failure(
                DomainError.SharedExpense.SplitTotalMismatch(
                    targetAmount = totalAmount,
                    allocatedSum = 0.0,
                    difference = totalAmount
                )
            )
        }

        // Validación de que la suma de pagadores coincida con el desembolso en caja (si se especificaron pagadores)
        val totalPaidByAll = roundToCents(request.participants.sumOf { it.paidAmountAtRegister })
        val baseRegisterTotal = roundToCents(
            (request.subtotalAmount + request.taxIvaAmount + request.taxConsumptionAmount + request.tipAmount - request.discountAmount)
                .coerceAtLeast(0.0)
        )
        if (totalPaidByAll > 0.0 && abs(totalPaidByAll - baseRegisterTotal) > 0.05 && abs(totalPaidByAll - totalAmount) > 0.05) {
            return Outcome.Failure(
                DomainError.SharedExpense.PayerTotalMismatch(
                    totalExpenseAmount = baseRegisterTotal,
                    sumOfPayers = totalPaidByAll,
                    difference = roundToCents(baseRegisterTotal - totalPaidByAll)
                )
            )
        }

        // Paso 1: Calcular la participación sobre el Subtotal Base según los 7 algoritmos
        val subtotalSharesResult: Map<String, Double> = when (request.splitMethod) {
            SplitCalculationMethod.EQUAL -> {
                val perPerson = request.subtotalAmount / activeParticipants.size.toDouble()
                activeParticipants.associate { it.participantId to perPerson }
            }

            SplitCalculationMethod.EXACT_AMOUNT -> {
                val sumExact = roundToCents(activeParticipants.sumOf { it.exactAmountInput })
                val matchesTotal = abs(sumExact - totalAmount) <= 0.05
                val matchesSubtotal = abs(sumExact - request.subtotalAmount) <= 0.05
                if (!matchesTotal && !matchesSubtotal) {
                    return Outcome.Failure(
                        DomainError.SharedExpense.SplitTotalMismatch(
                            targetAmount = totalAmount,
                            allocatedSum = sumExact,
                            difference = roundToCents(totalAmount - sumExact)
                        )
                    )
                }
                val scaleFactor = if (matchesTotal && totalAmount > 0) request.subtotalAmount / totalAmount else 1.0
                activeParticipants.associate { it.participantId to (it.exactAmountInput * scaleFactor) }
            }

            SplitCalculationMethod.PERCENTAGE -> {
                val sumPct = activeParticipants.sumOf { it.percentageInput }
                if (abs(sumPct - 100.0) > 0.05) {
                    return Outcome.Failure(DomainError.SharedExpense.InvalidPercentageTotal(sumPct))
                }
                activeParticipants.associate {
                    it.participantId to (request.subtotalAmount * (it.percentageInput / 100.0))
                }
            }

            SplitCalculationMethod.SHARES -> {
                val totalShares = activeParticipants.sumOf { it.sharesUnitsInput.coerceAtLeast(0.0) }
                if (totalShares <= 0.0) {
                    return Outcome.Failure(DomainError.Validation.NegativeOrZeroAmount(totalShares))
                }
                activeParticipants.associate {
                    val ratio = it.sharesUnitsInput.coerceAtLeast(0.0) / totalShares
                    it.participantId to (request.subtotalAmount * ratio)
                }
            }

            SplitCalculationMethod.ADJUSTMENT -> {
                val sumAdjustments = activeParticipants.sumOf { it.adjustmentAmountInput }
                val remainingPool = request.subtotalAmount - sumAdjustments
                if (remainingPool < -0.05) {
                    return Outcome.Failure(
                        DomainError.SharedExpense.SplitTotalMismatch(
                            targetAmount = request.subtotalAmount,
                            allocatedSum = sumAdjustments,
                            difference = request.subtotalAmount - sumAdjustments
                        )
                    )
                }
                val equalBase = remainingPool / activeParticipants.size.toDouble()
                activeParticipants.associate {
                    val share = (equalBase + it.adjustmentAmountInput).coerceAtLeast(0.0)
                    it.participantId to share
                }
            }

            SplitCalculationMethod.ITEMIZED_OCR -> {
                if (request.ocrLineItems.isEmpty()) {
                    val perPerson = request.subtotalAmount / activeParticipants.size.toDouble()
                    activeParticipants.associate { it.participantId to perPerson }
                } else {
                    val unassigned = request.ocrLineItems.filter { it.assignedParticipantIds.isEmpty() }
                    if (unassigned.isNotEmpty()) {
                        return Outcome.Failure(
                            DomainError.SharedExpense.UnassignedOcrLineItems(
                                unassigned.map { it.description }
                            )
                        )
                    }
                    val accumMap = activeParticipants.associate { it.participantId to 0.0 }.toMutableMap()
                    for (item in request.ocrLineItems) {
                        val consumers = item.assignedParticipantIds.filter { accumMap.containsKey(it) }
                        if (consumers.isEmpty()) continue
                        val customWeightsSum = consumers.sumOf { item.participantWeightsWithinItem[it] ?: 1.0 }
                        for (cId in consumers) {
                            val w = (item.participantWeightsWithinItem[cId] ?: 1.0) / customWeightsSum
                            accumMap[cId] = (accumMap[cId] ?: 0.0) + (item.lineTotalPrice * w)
                        }
                    }
                    accumMap
                }
            }

            SplitCalculationMethod.INCOME_PROPORTIONAL -> {
                val totalIncome = activeParticipants.sumOf {
                    it.monthlyIncomeInput.takeIf { inc -> inc > 0.0 } ?: 1.0
                }
                activeParticipants.associate {
                    val weight = (it.monthlyIncomeInput.takeIf { inc -> inc > 0.0 } ?: 1.0) / totalIncome
                    it.participantId to (request.subtotalAmount * weight)
                }
            }
        }

        // Paso 2: Prorratear IVA, Impoconsumo, Propina, Descuento y Cargos Financieros
        val totalTaxes = request.taxIvaAmount + request.taxConsumptionAmount
        val extraFinancialCharges =
            (if (request.prorateGmfAmongDebtors) request.gmf4x1000Amount else 0.0) +
                (if (request.passCreditCardInterestToGroup) request.creditCardInterestSurcharge else 0.0)

        val primaryPayerId = request.participants
            .maxByOrNull { it.paidAmountAtRegister }
            ?.participantId
            ?: activeParticipants.first().participantId

        val subtotalSum = subtotalSharesResult.values.sum().takeIf { it > 0.0 } ?: 1.0

        data class RawBreakdown(
            val participant: ParticipantSplitInput,
            val baseSubtotal: Double,
            val taxShare: Double,
            val tipShare: Double,
            val discountShare: Double,
            val extraFinancialShare: Double,
            val unroundedTotal: Double
        )

        val rawBreakdowns = activeParticipants.map { p ->
            val subShare = subtotalSharesResult[p.participantId] ?: 0.0
            val consumptionRatio = subShare / subtotalSum
            val equalRatio = 1.0 / activeParticipants.size.toDouble()

            val prorationFactor = when (request.taxAndTipProrationRule) {
                TaxAndTipProrationRule.PROPORTIONAL_TO_SUBTOTAL -> consumptionRatio
                TaxAndTipProrationRule.EQUAL_AMONG_CONSUMERS -> equalRatio
                TaxAndTipProrationRule.PAYER_ABSORBS -> if (p.participantId == primaryPayerId) 1.0 else 0.0
            }

            val taxAlloc = totalTaxes * prorationFactor
            val tipAlloc = request.tipAmount * prorationFactor
            val discountAlloc = request.discountAmount * consumptionRatio
            val financialAlloc = extraFinancialCharges * consumptionRatio

            val unrounded = if (
                request.splitMethod == SplitCalculationMethod.EXACT_AMOUNT &&
                abs(activeParticipants.sumOf { it.exactAmountInput } - totalAmount) <= 0.05
            ) {
                p.exactAmountInput
            } else {
                (subShare + taxAlloc + tipAlloc + financialAlloc - discountAlloc).coerceAtLeast(0.0)
            }

            RawBreakdown(
                participant = p,
                baseSubtotal = roundToCents(subShare),
                taxShare = roundToCents(taxAlloc),
                tipShare = roundToCents(tipAlloc),
                discountShare = roundToCents(discountAlloc),
                extraFinancialShare = roundToCents(financialAlloc),
                unroundedTotal = unrounded
            )
        }

        // Paso 3: Aplicar Escalón de Redondeo y Algoritmo del Resto Mayor (Largest Remainder Method)
        val step = request.roundingStepRule.stepValue.coerceAtLeast(0.01)
        val steppedShares = rawBreakdowns.map { b ->
            val stepped = floor((b.unroundedTotal / step) + 1e-9) * step
            b.participant.participantId to roundToCents(stepped)
        }.toMap().toMutableMap()

        val sumStepped = roundToCents(steppedShares.values.sum())
        val remainderTotal = roundToCents(totalAmount - sumStepped)

        // Determinar quién absorbe el diferencial de redondeo o cómo se reparten los pasos del resto mayor
        val targetRecipientId = resolveRemainderRecipient(
            targetRule = request.roundingRemainderTarget,
            rawBreakdowns = rawBreakdowns.map { it.participant.participantId to it.unroundedTotal },
            primaryPayerId = primaryPayerId,
            consecutiveNumber = request.consecutiveSequenceNumber
        )

        if (abs(remainderTotal) >= 0.01) {
            if (request.roundingStepRule == RoundingStepRule.EXACT_CENT || request.roundingStepRule == RoundingStepRule.ROUND_1) {
                // Distribución Largest Remainder paso a paso para máxima equidad matemática
                val unitsToDistribute = (remainderTotal / step).roundToLong()
                if (unitsToDistribute > 0 && unitsToDistribute <= activeParticipants.size) {
                    val sortedByFractionalRemainder = rawBreakdowns.sortedWith(
                        compareByDescending<RawBreakdown> {
                            val currentStepped = steppedShares[it.participant.participantId] ?: 0.0
                            it.unroundedTotal - currentStepped
                        }.thenBy { if (it.participant.participantId == targetRecipientId) 0 else 1 }
                    )
                    for (i in 0 until unitsToDistribute.toInt()) {
                        val pId = sortedByFractionalRemainder[i % sortedByFractionalRemainder.size].participant.participantId
                        steppedShares[pId] = roundToCents((steppedShares[pId] ?: 0.0) + step)
                    }
                } else {
                    steppedShares[targetRecipientId] = roundToCents((steppedShares[targetRecipientId] ?: 0.0) + remainderTotal)
                }
            } else {
                // Para redondeos gruesos ($50, $100, $1.000), el destinatario elegido absorbe el diferencial exacto
                steppedShares[targetRecipientId] = roundToCents((steppedShares[targetRecipientId] ?: 0.0) + remainderTotal)
            }
        }

        // Corrección final de centavo residual si existiera por coma flotante IEEE 754
        val finalCheckDiff = roundToCents(totalAmount - steppedShares.values.sum())
        if (abs(finalCheckDiff) >= 0.01) {
            steppedShares[targetRecipientId] = roundToCents((steppedShares[targetRecipientId] ?: 0.0) + finalCheckDiff)
        }

        // Paso 4: Validar límites de confianza crediticia (creditTrustLimit) por participante
        for (b in rawBreakdowns) {
            val p = b.participant
            val finalShare = steppedShares[p.participantId] ?: 0.0
            val netForThisExpense = roundToCents(p.paidAmountAtRegister - finalShare)
            if (!p.isSelf && netForThisExpense < 0.0 && p.creditTrustLimit != null) {
                val newDebtFromThisExpense = abs(netForThisExpense)
                val projectedTotalDebt = p.currentUnpaidDebtWithUser + newDebtFromThisExpense
                if (projectedTotalDebt > p.creditTrustLimit + 0.01) {
                    return Outcome.Failure(
                        DomainError.SharedExpense.CreditTrustLimitExceeded(
                            participantName = p.displayName,
                            currentOwed = p.currentUnpaidDebtWithUser,
                            newShare = newDebtFromThisExpense,
                            trustLimit = p.creditTrustLimit
                        )
                    )
                }
            }
        }

        // Paso 5: Construir la salida completa incluyendo participantes excluidos (con cuota $0)
        val rawMap = rawBreakdowns.associateBy { it.participant.participantId }
        val computedList = request.participants.map { p ->
            val breakdown = rawMap[p.participantId]
            if (breakdown == null || !p.isIncluded) {
                val net = roundToCents(p.paidAmountAtRegister)
                ComputedParticipantSplit(
                    participantId = p.participantId,
                    displayName = p.displayName,
                    isSelf = p.isSelf,
                    isIncluded = false,
                    rawWeightOrPercentage = 0.0,
                    baseSubtotalShare = 0.0,
                    allocatedTaxAmount = 0.0,
                    allocatedTipAmount = 0.0,
                    allocatedDiscountAmount = 0.0,
                    allocatedGmfAndFinancialAmount = 0.0,
                    unroundedExactShare = 0.0,
                    roundingAdjustment = 0.0,
                    totalOwedShare = 0.0,
                    paidByThisParticipant = roundToCents(p.paidAmountAtRegister),
                    netBalanceForExpense = net,
                    remainingPendingAmount = abs(net),
                    initialSettlementStatus = if (abs(net) < 0.01) SplitSettlementStatus.NOT_APPLICABLE else SplitSettlementStatus.PENDING
                )
            } else {
                val finalShare = steppedShares[p.participantId] ?: 0.0
                val roundingAdj = roundToCents(finalShare - breakdown.unroundedTotal)
                val net = roundToCents(p.paidAmountAtRegister - finalShare)
                val status = when {
                    abs(net) < 0.01 -> SplitSettlementStatus.SETTLED
                    net > 0.0 -> SplitSettlementStatus.NOT_APPLICABLE // Es acreedor neto en este gasto
                    else -> SplitSettlementStatus.PENDING
                }
                val rawWeight = when (request.splitMethod) {
                    SplitCalculationMethod.PERCENTAGE -> p.percentageInput
                    SplitCalculationMethod.SHARES -> p.sharesUnitsInput
                    SplitCalculationMethod.ADJUSTMENT -> p.adjustmentAmountInput
                    else -> roundToCents((finalShare / totalAmount) * 100.0)
                }
                ComputedParticipantSplit(
                    participantId = p.participantId,
                    displayName = p.displayName,
                    isSelf = p.isSelf,
                    isIncluded = true,
                    rawWeightOrPercentage = rawWeight,
                    baseSubtotalShare = breakdown.baseSubtotal,
                    allocatedTaxAmount = breakdown.taxShare,
                    allocatedTipAmount = breakdown.tipShare,
                    allocatedDiscountAmount = breakdown.discountShare,
                    allocatedGmfAndFinancialAmount = breakdown.extraFinancialShare,
                    unroundedExactShare = roundToCents(breakdown.unroundedTotal),
                    roundingAdjustment = roundingAdj,
                    totalOwedShare = finalShare,
                    paidByThisParticipant = roundToCents(p.paidAmountAtRegister),
                    netBalanceForExpense = net,
                    remainingPendingAmount = if (net < 0.0) abs(net) else 0.0,
                    initialSettlementStatus = status
                )
            }
        }

        return Outcome.Success(
            SplitCalculationSummary(
                effectiveTotalAmount = totalAmount,
                subtotalAmount = roundToCents(request.subtotalAmount),
                totalTaxesAndTip = roundToCents(totalTaxes + request.tipAmount),
                totalRoundingDeltaApplied = remainderTotal,
                remainderAssignedToParticipantId = targetRecipientId,
                participantSplits = computedList
            )
        )
    }

    private fun resolveRemainderRecipient(
        targetRule: RoundingRemainderTarget,
        rawBreakdowns: List<Pair<String, Double>>,
        primaryPayerId: String,
        consecutiveNumber: Long
    ): String {
        if (rawBreakdowns.isEmpty()) return primaryPayerId
        return when (targetRule) {
            RoundingRemainderTarget.PAYER ->
                rawBreakdowns.find { it.first == primaryPayerId }?.first ?: rawBreakdowns.first().first
            RoundingRemainderTarget.SELF ->
                rawBreakdowns.find { it.first == "SELF_USER" }?.first ?: rawBreakdowns.first().first
            RoundingRemainderTarget.LARGEST_SHARE ->
                rawBreakdowns.maxByOrNull { it.second }?.first ?: rawBreakdowns.first().first
            RoundingRemainderTarget.ROTATING -> {
                val idx = (abs(consecutiveNumber) % rawBreakdowns.size).toInt()
                rawBreakdowns[idx].first
            }
        }
    }

    fun roundToCents(value: Double): Double =
        (value * 100.0).roundToLong() / 100.0
}
