package com.example.feature.gastoscompartidos.domain.engine

import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToLong

object DebtSimplificationGraphEngine {

    data class RawDirectedDebtEdge(
        val fromDebtorId: String,
        val fromDebtorName: String,
        val toCreditorId: String,
        val toCreditorName: String,
        val amount: Double,
        val sourceConsecutiveCode: String
    )

    data class SimplifiedTransferInstruction(
        val stepOrder: Int,
        val fromParticipantId: String,
        val fromParticipantName: String,
        val toParticipantId: String,
        val toParticipantName: String,
        val amount: Double,
        val explanationNote: String
    )

    data class GraphSimplificationReport(
        val originalTransferCount: Int,
        val simplifiedTransferCount: Int,
        val transactionsSavedCount: Int,
        val totalVolumeBeforeSimplification: Double,
        val totalVolumeAfterSimplification: Double,
        val netBalancesByParticipant: Map<String, Double>,
        val optimizedInstructions: List<SimplifiedTransferInstruction>,
        val stepByStepMathematicalTrace: List<String>
    )

    /**
     * Reduce un conjunto de deudas dirigidas entre miembros de un grupo al mínimo número de pagos posibles.
     */
    fun simplifyGroupDebts(
        rawEdges: List<RawDirectedDebtEdge>
    ): GraphSimplificationReport {
        val activeEdges = rawEdges.filter { it.amount >= 0.01 && it.fromDebtorId != it.toCreditorId }
        if (activeEdges.isEmpty()) {
            return GraphSimplificationReport(
                originalTransferCount = 0,
                simplifiedTransferCount = 0,
                transactionsSavedCount = 0,
                totalVolumeBeforeSimplification = 0.0,
                totalVolumeAfterSimplification = 0.0,
                netBalancesByParticipant = emptyMap(),
                optimizedInstructions = emptyList(),
                stepByStepMathematicalTrace = listOf("No existen deudas pendientes por simplificar en este grupo.")
            )
        }

        val namesMap = mutableMapOf<String, String>()
        val netBalanceMap = mutableMapOf<String, Double>()

        // 1. Consolidar aristas paralelas entre el mismo par (A -> B) para contar transferencias brutas reales
        val pairwiseRaw = mutableMapOf<Pair<String, String>, Double>()
        for (edge in activeEdges) {
            namesMap[edge.fromDebtorId] = edge.fromDebtorName
            namesMap[edge.toCreditorId] = edge.toCreditorName
            netBalanceMap[edge.fromDebtorId] = (netBalanceMap[edge.fromDebtorId] ?: 0.0) - edge.amount
            netBalanceMap[edge.toCreditorId] = (netBalanceMap[edge.toCreditorId] ?: 0.0) + edge.amount

            val key = edge.fromDebtorId to edge.toCreditorId
            pairwiseRaw[key] = (pairwiseRaw[key] ?: 0.0) + edge.amount
        }

        val roundedNetBalances = netBalanceMap.mapValues { roundCents(it.value) }
        val totalRawVolume = roundCents(activeEdges.sumOf { it.amount })

        val traceLogs = mutableListOf<String>()
        traceLogs.add(
            "1. Posición Neta Inicial por Participante: " +
                roundedNetBalances.entries.joinToString(", ") { (id, bal) ->
                    val sign = if (bal >= 0) "+" else ""
                    "${namesMap[id] ?: id}: $sign$${bal.toLong()}"
                }
        )

        // 2. Separar en colas de Deudores (balance < 0) y Acreedores (balance > 0)
        data class MutableNode(val id: String, val name: String, var balance: Double)

        val debtors = roundedNetBalances
            .filter { it.value < -0.01 }
            .map { MutableNode(it.key, namesMap[it.key] ?: it.key, abs(it.value)) }
            .toMutableList()

        val creditors = roundedNetBalances
            .filter { it.value > 0.01 }
            .map { MutableNode(it.key, namesMap[it.key] ?: it.key, it.value) }
            .toMutableList()

        val instructions = mutableListOf<SimplifiedTransferInstruction>()
        var step = 1

        // 3. Emparejamiento voraz ordenado (Greedy Max-Debtor to Max-Creditor Min-Cash-Flow)
        while (debtors.isNotEmpty() && creditors.isNotEmpty()) {
            debtors.sortByDescending { it.balance }
            creditors.sortByDescending { it.balance }

            val maxDebtor = debtors.first()
            val maxCreditor = creditors.first()

            val transferAmount = roundCents(min(maxDebtor.balance, maxCreditor.balance))
            if (transferAmount >= 0.01) {
                val note = "${maxDebtor.name} transfiere $${transferAmount.toLong()} a ${maxCreditor.name} " +
                    "(salda posición neta cruzada)."
                instructions.add(
                    SimplifiedTransferInstruction(
                        stepOrder = step,
                        fromParticipantId = maxDebtor.id,
                        fromParticipantName = maxDebtor.name,
                        toParticipantId = maxCreditor.id,
                        toParticipantName = maxCreditor.name,
                        amount = transferAmount,
                        explanationNote = note
                    )
                )
                traceLogs.add("Paso ${step + 1}: $note")
                step++
            }

            maxDebtor.balance = roundCents(maxDebtor.balance - transferAmount)
            maxCreditor.balance = roundCents(maxCreditor.balance - transferAmount)

            if (maxDebtor.balance < 0.01) debtors.removeAt(0)
            if (maxCreditor.balance < 0.01) creditors.removeAt(0)
        }

        val simplifiedVolume = roundCents(instructions.sumOf { it.amount })
        val originalPairCount = pairwiseRaw.count { it.value >= 0.01 }

        return GraphSimplificationReport(
            originalTransferCount = originalPairCount,
            simplifiedTransferCount = instructions.size,
            transactionsSavedCount = (originalPairCount - instructions.size).coerceAtLeast(0),
            totalVolumeBeforeSimplification = totalRawVolume,
            totalVolumeAfterSimplification = simplifiedVolume,
            netBalancesByParticipant = roundedNetBalances,
            optimizedInstructions = instructions,
            stepByStepMathematicalTrace = traceLogs
        )
    }

    /**
     * Cascada de Amortización FIFO para Abonos Parciales:
     * Distribuye un pago parcial sobre las deudas más antiguas primero.
     */
    data class PendingSplitCandidate(
        val splitId: String,
        val sharedExpenseId: String,
        val consecutiveCode: String,
        val expenseDate: String,
        val createdAt: Long,
        val currentRemainingAmount: Double
    )

    data class FifoAllocationSlice(
        val splitId: String,
        val sharedExpenseId: String,
        val consecutiveCode: String,
        val amountApplied: Double,
        val newRemainingAmount: Double,
        val newStatus: String // "PARTIAL" | "SETTLED"
    )

    fun allocatePartialPaymentFifo(
        paymentAmount: Double,
        pendingSplits: List<PendingSplitCandidate>
    ): List<FifoAllocationSlice> {
        var remainingToApply = roundCents(paymentAmount.coerceAtLeast(0.0))
        val sortedOldestFirst = pendingSplits
            .filter { it.currentRemainingAmount >= 0.01 }
            .sortedWith(compareBy<PendingSplitCandidate> { it.expenseDate }.thenBy { it.createdAt })

        val allocations = mutableListOf<FifoAllocationSlice>()
        for (candidate in sortedOldestFirst) {
            if (remainingToApply < 0.01) break
            val applied = roundCents(min(remainingToApply, candidate.currentRemainingAmount))
            val newRem = roundCents((candidate.currentRemainingAmount - applied).coerceAtLeast(0.0))
            val status = if (newRem < 0.01) "SETTLED" else "PARTIAL"

            allocations.add(
                FifoAllocationSlice(
                    splitId = candidate.splitId,
                    sharedExpenseId = candidate.sharedExpenseId,
                    consecutiveCode = candidate.consecutiveCode,
                    amountApplied = applied,
                    newRemainingAmount = newRem,
                    newStatus = status
                )
            )
            remainingToApply = roundCents(remainingToApply - applied)
        }
        return allocations
    }

    private fun roundCents(v: Double): Double = (v * 100.0).roundToLong() / 100.0
}
