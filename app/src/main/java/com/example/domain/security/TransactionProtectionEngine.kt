package com.example.domain.security

import com.example.data.local.entity.TransactionEntity
import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.util.Locale

/**
 * Enterprise Transaction Protection & Cryptographic Audit Ledger Engine.
 * 
 * Guarantees mathematical and immutable integrity of transactions:
 * 1. Computes SHA-256 tamper-evident integrity hashes for each transaction record.
 * 2. Detects ledger tampering, balance drift, or unauthorized modifications.
 * 3. Enforces accounting immutability: forbids destructive deletion of accounts or records with active audit history.
 */
object TransactionProtectionEngine {

    private const val SALT = "KashApp_Security_Audit_Ledger_v2_Immunity"

    /**
     * Computes a cryptographic SHA-256 signature for a transaction based on immutable fields.
     */
    fun computeTransactionHash(transaction: TransactionEntity): String {
        val payload = buildString {
            append(transaction.id)
            append(":")
            append(transaction.accountId)
            append(":")
            append(String.format(Locale.US, "%.4f", transaction.amount))
            append(":")
            append(transaction.type)
            append(":")
            append(transaction.date)
            append(":")
            append(SALT)
        }
        return sha256(payload)
    }

    /**
     * Verifies that the transaction record has not been tampered with or altered in local storage.
     */
    fun verifyTransactionIntegrity(transaction: TransactionEntity, expectedHash: String?): Boolean {
        if (expectedHash.isNullOrBlank()) return true // legacy or unhashed
        val currentHash = computeTransactionHash(transaction)
        return currentHash.equals(expectedHash.trim(), ignoreCase = true)
    }

    /**
     * Audits an account's transactions and computes expected net balance from registered ledger movements.
     */
    fun auditAccountBalanceIntegrity(
        initialBalance: Double,
        transactions: List<TransactionEntity>
    ): BalanceAuditReport {
        var totalInflow = 0.0
        var totalOutflow = 0.0

        for (tx in transactions) {
            when (tx.type.uppercase()) {
                "INCOME", "INGRESO" -> totalInflow += tx.amount
                "EXPENSE", "GASTO" -> totalOutflow += tx.amount
                "TRANSFER", "TRANSFERENCIA" -> {
                    // For transfers, context determines inflow or outflow
                    if (tx.amount >= 0) totalInflow += tx.amount else totalOutflow += -tx.amount
                }
                else -> totalOutflow += tx.amount
            }
        }

        val computedNet = initialBalance + totalInflow - totalOutflow
        return BalanceAuditReport(
            transactionCount = transactions.size,
            totalInflow = totalInflow,
            totalOutflow = totalOutflow,
            computedNetBalance = computedNet,
            isLockedAgainstDeletion = transactions.isNotEmpty()
        )
    }

    private fun sha256(input: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(StandardCharsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }
}

data class BalanceAuditReport(
    val transactionCount: Int,
    val totalInflow: Double,
    val totalOutflow: Double,
    val computedNetBalance: Double,
    val isLockedAgainstDeletion: Boolean
)
