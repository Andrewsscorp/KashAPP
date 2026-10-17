package com.example.core.error

import android.database.sqlite.SQLiteConstraintException
import android.database.sqlite.SQLiteDatabaseCorruptException
import android.database.sqlite.SQLiteFullException
import android.database.sqlite.SQLiteTableLockedException
import kotlin.coroutines.cancellation.CancellationException

/**
 * Centralized translator mapping SQLite and Room database exceptions into strongly-typed DomainErrors.
 */
object PersistenceErrorTranslator {

    fun translate(throwable: Throwable, entityType: String = "Entidad", name: String = ""): DomainError {
        if (throwable is CancellationException) {
            throw throwable
        }

        return when (throwable) {
            is SQLiteConstraintException -> {
                val msg = throwable.message?.lowercase() ?: ""
                if (msg.contains("unique") || msg.contains("primary key")) {
                    DomainError.Conflict.DuplicateName(name = name, entityType = entityType)
                } else if (msg.contains("foreign key")) {
                    DomainError.Integrity.InUse(entityType = entityType, id = name, referenceCount = 1)
                } else {
                    DomainError.Conflict.DuplicateName(name = name, entityType = entityType)
                }
            }
            is SQLiteFullException -> DomainError.Storage.DiskFull(throwable)
            is SQLiteDatabaseCorruptException -> DomainError.Storage.DatabaseCorrupted(throwable)
            is SQLiteTableLockedException -> DomainError.Storage.DatabaseLocked(throwable)
            else -> DomainError.Unexpected(message = throwable.message ?: "Error desconocido en persistencia", cause = throwable)
        }
    }
}
