package com.example.domain.usecase

import com.example.domain.repository.FinancialRepository
import java.math.BigDecimal

/**
 * Data Transfer Object for creating an inter-account transfer.
 */
data class TransferDTO(
    val sourceAccountId: String,
    val destinationAccountId: String,
    val amount: BigDecimal,
    val userId: String = "user_default",
    val sourceAccountName: String = "Cuenta origen",
    val destinationAccountName: String = "Cuenta destino",
    val currency: String = "COP",
    val dateFormatted: String = "2026-09-26",
    val concept: String? = null,
    val note: String? = null
)

/**
 * Clean Architecture Use Case to validate and execute transfers between accounts.
 *
 * Enforces:
 * 1. Transfer amount must be > 0.
 * 2. Source and Destination accounts cannot be the same.
 * 3. Source account balance must be >= transfer amount (no overdraft without coverage).
 * 4. Atomic balance decrement on source and increment on destination.
 */
class CreateTransferUseCase(
    private val financialRepository: FinancialRepository
) {
    suspend fun execute(dto: TransferDTO): Boolean {
        if (dto.amount <= BigDecimal.ZERO) {
            throw IllegalArgumentException("El monto de transferencia debe ser mayor a cero.")
        }

        if (dto.sourceAccountId == dto.destinationAccountId) {
            throw IllegalArgumentException("No se puede transferir a la misma cuenta.")
        }

        val sourceAccount = financialRepository.getAccountById(dto.sourceAccountId)
            ?: throw IllegalArgumentException("Cuenta de origen no encontrada.")

        val sourceBalance = BigDecimal.valueOf(sourceAccount.balance)
        if (sourceBalance < dto.amount) {
            throw IllegalArgumentException("Fondos insuficientes para realizar la transferencia.")
        }

        val result = financialRepository.recordTransfer(
            userId = dto.userId,
            sourceAccountId = dto.sourceAccountId,
            sourceAccountName = dto.sourceAccountName,
            destinationAccountId = dto.destinationAccountId,
            destinationAccountName = dto.destinationAccountName,
            amount = dto.amount.toDouble(),
            currency = dto.currency,
            dateFormatted = dto.dateFormatted,
            concept = dto.concept,
            note = dto.note,
            receiptUrl = null,
            saveAsTemplate = false,
            tags = listOf("Transferencia")
        )

        return result.isSuccess
    }
}
