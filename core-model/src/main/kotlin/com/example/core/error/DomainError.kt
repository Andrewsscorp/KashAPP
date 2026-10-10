package com.example.core.error

/**
 * Exhaustive sealed hierarchy for all typed domain errors in KashApp.
 */
sealed interface DomainError {

    sealed interface Validation : DomainError {
        data object Blank : Validation
        data class TooShort(val min: Int, val current: Int) : Validation
        data class TooLong(val max: Int, val current: Int) : Validation
        data class InvalidCharacters(val details: String) : Validation
        data class NegativeOrZeroAmount(val amount: Double) : Validation
        data class InsufficientFunds(val available: Double, val required: Double) : Validation
    }

    sealed interface Conflict : DomainError {
        data class DuplicateName(val name: String, val entityType: String = "Entidad") : Conflict
        data class ConcurrentModification(val currentVersion: Long, val expectedVersion: Long) : Conflict
    }

    sealed interface NotFound : DomainError {
        data class EntityNotFound(val entityType: String, val id: String) : NotFound
    }

    sealed interface Integrity : DomainError {
        data class InUse(val entityType: String, val id: String, val referenceCount: Int) : Integrity
        data class SelfTransferNotAllowed(val accountId: String) : Integrity
    }

    sealed interface Storage : DomainError {
        data class DiskFull(val cause: Throwable?) : Storage
        data class DatabaseCorrupted(val cause: Throwable?) : Storage
        data class DatabaseLocked(val cause: Throwable?) : Storage
    }

    data class Unexpected(val message: String, val cause: Throwable? = null) : DomainError
}
