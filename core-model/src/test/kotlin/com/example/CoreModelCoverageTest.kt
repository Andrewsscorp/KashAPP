package com.example

import com.example.core.error.DomainError
import com.example.core.id.DeterministicIdGenerator
import com.example.core.id.SequentialIdGenerator
import com.example.core.result.Outcome
import com.example.core.result.flatMap
import com.example.core.result.map
import com.example.core.result.onFailure
import com.example.core.result.onSuccess
import com.example.core.result.suspendRunOutcome
import com.example.core.text.TextNormalizer
import com.example.domain.models.FinancialRubroType
import com.example.domain.models.SessionState
import com.example.domain.models.UserSession
import com.example.util.MoneyFormatter
import java.math.BigDecimal
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class CoreModelCoverageTest {

    @Test
    fun deterministicAndSequentialIds_areStableAndWellFormed() {
        val deterministic = DeterministicIdGenerator("fixed")
        assertEquals("fixed_1", deterministic.generateId())
        assertEquals("account_2", deterministic.generateId("account"))
        deterministic.reset()
        assertEquals("fixed_1", deterministic.generateId())

        val sequential = SequentialIdGenerator()
        val id = sequential.generateId(" account_")
        assertTrue(id.startsWith("account_"))
        val segments = id.removePrefix("account_").split("_")
        assertEquals(2, segments.size)
        assertTrue(segments[0].all { it.isDigit() })
        assertTrue(segments[1].isNotBlank())
    }

    @Test
    fun outcome_operations_preserveSuccessAndFailureSemantics() {
        val success: Outcome<Int, String> = Outcome.Success(2)
        val failure: Outcome<Int, String> = Outcome.Failure("bad")
        assertTrue(success.isSuccess)
        assertFalse(success.isFailure)
        assertEquals(2, success.getOrNull())
        assertNull(success.errorOrNull())
        assertTrue(failure.isFailure)
        assertNull(failure.getOrNull())
        assertEquals("bad", failure.errorOrNull())
        assertEquals(4, success.map { it * 2 }.getOrNull())
        assertSame(failure, failure.map { it * 2 })
        assertEquals("ok", success.flatMap { Outcome.Success("ok") }.getOrNull())
        assertSame(failure, failure.flatMap { Outcome.Success(it) })

        val successCalls = AtomicInteger(0)
        val failureCalls = AtomicInteger(0)
        success.onSuccess { successCalls.incrementAndGet() }.onFailure { failureCalls.incrementAndGet() }
        failure.onSuccess { successCalls.incrementAndGet() }.onFailure { failureCalls.incrementAndGet() }
        assertEquals(1, successCalls.get())
        assertEquals(1, failureCalls.get())
    }

    @Test
    fun outcomeRunner_mapsErrorsAndRethrowsCancellation() {
        val mapped = suspendRunOutcome<String, String>({ it.message ?: "unknown" }) { error("boom") }
        assertEquals("boom", mapped.errorOrNull())

        var cancellationThrown = false
        try {
            suspendRunOutcome<String, String>({ "mapped" }) { throw CancellationException("cancel") }
        } catch (_: CancellationException) {
            cancellationThrown = true
        }
        assertTrue(cancellationThrown)
    }

    @Test
    fun textNormalizer_handlesNullWhitespaceAccentsAndDisplayText() {
        assertEquals("", TextNormalizer.normalize(null))
        assertEquals("", TextNormalizer.normalize("   "))
        assertEquals("nino jose", TextNormalizer.normalize("  Niño   José "))
        assertEquals("straße", TextNormalizer.normalize(" Straße "))
        assertEquals("Álvaro Núñez", TextNormalizer.sanitizeDisplayName("  Álvaro   Núñez "))
        assertEquals("", TextNormalizer.sanitizeDisplayName(null))
    }

    @Test
    fun moneyFormatter_handlesCurrenciesPrivacyNullAndCompactValues() {
        assertEquals("$ 1.234.567", MoneyFormatter.format(BigDecimal("1234567"), currencyCode = "COP"))
        assertEquals("$ 1.234,56 USD", MoneyFormatter.format(BigDecimal("1234.56"), currencyCode = "usd"))
        assertEquals("€ 12,34 EUR", MoneyFormatter.format(BigDecimal("12.34"), currencyCode = "EUR"))
        assertEquals("¥ 1.234 JPY", MoneyFormatter.format(BigDecimal("1234"), currencyCode = "JPY"))
        assertEquals("$ 0", MoneyFormatter.format(null))
        assertEquals("••••••", MoneyFormatter.format(BigDecimal("100"), isPrivacyMode = true))
        assertEquals("$1.5M", MoneyFormatter.formatCompact(BigDecimal("1500000")))
        assertEquals("€2.5k EUR", MoneyFormatter.formatCompact(BigDecimal("2500"), currencyCode = "EUR"))
        assertEquals("••••", MoneyFormatter.formatCompact(BigDecimal("100"), isPrivacyMode = true))
    }

    @Test
    fun domainErrors_retainTypedContext() {
        val errors: List<DomainError> = listOf(
            DomainError.Validation.Blank,
            DomainError.Validation.TooShort(6, 2),
            DomainError.Validation.TooLong(20, 21),
            DomainError.Validation.InvalidCharacters("emoji"),
            DomainError.Validation.NegativeOrZeroAmount(-1.0),
            DomainError.Validation.InsufficientFunds(5.0, 10.0),
            DomainError.Conflict.DuplicateName("Cuenta", "Cuenta bancaria"),
            DomainError.Conflict.ConcurrentModification(2, 1),
            DomainError.NotFound.EntityNotFound("Cuenta", "id-1"),
            DomainError.Integrity.InUse("Moneda", "COP", 2),
            DomainError.Integrity.SelfTransferNotAllowed("account-1"),
            DomainError.Storage.DiskFull(null),
            DomainError.Storage.DatabaseCorrupted(null),
            DomainError.Storage.DatabaseLocked(null),
            DomainError.Unexpected("unexpected")
        )
        assertEquals(15, errors.size)
        assertEquals("Cuenta bancaria", (errors[6] as DomainError.Conflict.DuplicateName).entityType)
        assertEquals(2, (errors[9] as DomainError.Integrity.InUse).referenceCount)
    }

    @Test
    fun sessionStates_andFinancialRubroTypes_exposeExpectedValues() {
        val session = UserSession("u1", "a@b.com", "Ana", null, "pin")
        val authenticated = SessionState.Authenticated(session)
        assertEquals(session, authenticated.session)
        assertTrue(SessionState.Initializing is SessionState)
        assertTrue(SessionState.Unauthenticated is SessionState)
        assertEquals(4, FinancialRubroType.entries.size)
        assertEquals("Dinero Disponible", FinancialRubroType.AVAILABLE_CASH.title)
        assertEquals("#EF4444", FinancialRubroType.DEBTS.primaryColorHex)
    }
}
