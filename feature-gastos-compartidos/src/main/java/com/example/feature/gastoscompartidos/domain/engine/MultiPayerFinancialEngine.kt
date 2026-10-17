package com.example.feature.gastoscompartidos.domain.engine

import com.example.data.local.entity.AccountCreditCardEntity
import com.example.data.local.entity.AccountEntity
import kotlin.math.pow
import kotlin.math.roundToLong

object MultiPayerFinancialEngine {

    const val COLOMBIA_GMF_RATE = 0.004 // 4x1000 (0.4%)

    data class PayerContributionInput(
        val participantId: String,
        val paidAmount: Double,
        val kashAccount: AccountEntity? = null,
        val creditCardConfig: AccountCreditCardEntity? = null,
        val creditCardInstallments: Int = 1,
        val passCreditCardInterestToGroup: Boolean = false
    )

    data class EvaluatedPayerContribution(
        val participantId: String,
        val isSelfUser: Boolean,
        val paidPrincipalAmount: Double,
        val kashAccountId: String?,
        val kashAccountName: String?,
        val paymentInstrumentType: String,
        val gmf4x1000Generated: Double,
        val creditCardInstallments: Int,
        val effectiveRateEa: Double,
        val effectiveRateEm: Double,
        val projectedCreditCardInterestTotal: Double,
        val monthlyInstallmentWithInterest: Double,
        val totalBankOrQuotaImpactForUser: Double
    )

    data class MultiPayerEvaluationResult(
        val contributions: List<EvaluatedPayerContribution>,
        val myTotalPrincipalPaid: Double,
        val myTotalGmfGenerated: Double,
        val totalCreditCardInterestSurchargeForGroup: Double,
        val totalPaidByAllParticipants: Double
    )

    /**
     * Evalúa todos los desembolsos de los pagadores, calculando 4x1000 e intereses de cuotas TC.
     */
    fun evaluatePayers(
        inputs: List<PayerContributionInput>,
        prorateGmfAmongDebtors: Boolean
    ): MultiPayerEvaluationResult {
        val evaluated = inputs.map { input ->
            val isSelf = input.participantId == "SELF_USER"
            val account = input.kashAccount
            val instrumentType = account?.type?.uppercase() ?: "CASH"
            val isCreditCard = instrumentType == "CREDIT_CARD" || instrumentType == "CREDIT"

            // 1. Cálculo de 4x1000 (GMF) si es cuenta bancaria/billetera gravada
            val gmf = if (isSelf && account != null && !isCreditCard && instrumentType != "CASH" && !account.isTaxExempt) {
                roundCents(input.paidAmount * COLOMBIA_GMF_RATE)
            } else {
                0.0
            }

            // 2. Cálculo de intereses de Tarjeta de Crédito si se difiere a más de 1 cuota
            val installments = input.creditCardInstallments.coerceIn(1, 72)
            val rateEa = input.creditCardConfig?.purchasesRateEa
                ?: input.creditCardConfig?.interestRateEa
                ?: account?.interestRateEa
                ?: 24.50
            val rateEmPct = if (rateEa > 0.0) {
                ((1.0 + (rateEa / 100.0)).pow(1.0 / 12.0) - 1.0) * 100.0
            } else 0.0
            val emDecimal = rateEmPct / 100.0

            val (monthlyQuota, totalInterest) = if (isSelf && isCreditCard && installments > 1 && emDecimal > 0.0) {
                // Fórmula de anualidad francesa: Cuota = P * [i * (1+i)^n] / [(1+i)^n - 1]
                val factor = (1.0 + emDecimal).pow(installments.toDouble())
                val quota = input.paidAmount * ((emDecimal * factor) / (factor - 1.0))
                val totalToPay = quota * installments
                val interest = (totalToPay - input.paidAmount).coerceAtLeast(0.0)
                roundCents(quota) to roundCents(interest)
            } else {
                // A 1 sola cuota en Latinoamérica hay gracia de intereses corrientes
                roundCents(input.paidAmount) to 0.0
            }

            EvaluatedPayerContribution(
                participantId = input.participantId,
                isSelfUser = isSelf,
                paidPrincipalAmount = roundCents(input.paidAmount),
                kashAccountId = account?.id,
                kashAccountName = account?.name,
                paymentInstrumentType = instrumentType,
                gmf4x1000Generated = gmf,
                creditCardInstallments = installments,
                effectiveRateEa = rateEa,
                effectiveRateEm = roundCents(rateEmPct),
                projectedCreditCardInterestTotal = totalInterest,
                monthlyInstallmentWithInterest = monthlyQuota,
                totalBankOrQuotaImpactForUser = if (isSelf) roundCents(input.paidAmount + gmf) else 0.0
            )
        }

        return MultiPayerEvaluationResult(
            contributions = evaluated,
            myTotalPrincipalPaid = roundCents(evaluated.filter { it.isSelfUser }.sumOf { it.paidPrincipalAmount }),
            myTotalGmfGenerated = roundCents(evaluated.filter { it.isSelfUser }.sumOf { it.gmf4x1000Generated }),
            totalCreditCardInterestSurchargeForGroup = roundCents(
                evaluated.filter { it.isSelfUser }.sumOf { exp ->
                    val sourceInput = inputs.find { it.participantId == exp.participantId && it.kashAccount?.id == exp.kashAccountId }
                    if (sourceInput?.passCreditCardInterestToGroup == true) exp.projectedCreditCardInterestTotal else 0.0
                }
            ),
            totalPaidByAllParticipants = roundCents(evaluated.sumOf { it.paidPrincipalAmount })
        )
    }

    private fun roundCents(v: Double): Double = (v * 100.0).roundToLong() / 100.0
}
