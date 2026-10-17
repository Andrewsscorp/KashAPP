package com.example

import android.database.sqlite.SQLiteConstraintException
import com.example.core.error.DomainError
import com.example.core.error.PersistenceErrorTranslator
import com.example.core.result.Outcome
import com.example.core.result.map
import com.example.core.result.suspendRunOutcome
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

/**
 * Verifies Acceptance Criterion 10:
 * Typed domain errors, functional transformation and strict preservation of Coroutines CancellationException.
 */
class OutcomeAndDomainErrorTest {

    @Test
    fun testOutcomeSuccessMapping() {
        val outcome: Outcome<Int, DomainError> = Outcome.Success(42)
        val mapped = outcome.map { it * 2 }

        assertTrue(mapped.isSuccess)
        assertEquals(84, mapped.getOrNull())
    }

    @Test
    fun testOutcomeFailurePreservation() {
        val error = DomainError.Validation.Blank
        val outcome: Outcome<Int, DomainError> = Outcome.Failure(error)
        val mapped = outcome.map { it * 2 }

        assertTrue(mapped.isFailure)
        assertEquals(DomainError.Validation.Blank, mapped.errorOrNull())
    }

    @Test
    fun testPersistenceErrorTranslatorForUniqueConstraint() {
        val sqlEx = SQLiteConstraintException("UNIQUE constraint failed: categories.nameNormalized")
        val domainError = PersistenceErrorTranslator.translate(sqlEx, entityType = "Categoría", name = "Alimentación")

        assertTrue(domainError is DomainError.Conflict.DuplicateName)
        assertEquals("Alimentación", (domainError as DomainError.Conflict.DuplicateName).name)
    }

    @Test
    fun testCancellationExceptionNeverSwallowed() = runTest {
        try {
            suspendRunOutcome<String, DomainError>(
                errorMapper = { DomainError.Unexpected(it.message ?: "") }
            ) {
                throw CancellationException("Coroutine cancelled by user")
            }
            fail("Should have re-thrown CancellationException")
        } catch (ce: CancellationException) {
            assertEquals("Coroutine cancelled by user", ce.message)
        }
    }
}
