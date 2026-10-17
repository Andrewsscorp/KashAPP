package com.example.feature.gastoscompartidos

import com.example.core.error.DomainError
import com.example.core.result.Outcome
import com.example.domain.engine.ConsecutiveSequenceEngine
import com.example.domain.models.shared.ConsecutiveMode
import com.example.domain.models.shared.SequenceDocumentType
import com.example.domain.models.shared.SequenceResetPolicy
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

class ConsecutiveSequenceEngineTest {

    @Test
    fun `generateNextAutomatic interpolates standard yearly pattern and increments counter`() {
        val snapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = SequenceDocumentType.SHARED_EXPENSE,
            prefix = "GC",
            patternTemplate = "{PREFIX}-{YYYY}-{SEQ:4}",
            currentValue = 41L,
            resetPolicy = SequenceResetPolicy.YEARLY,
            lastResetPeriodKey = "2026"
        )

        val result = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = snapshot,
            referenceDate = LocalDate.of(2026, 10, 15)
        )

        assertEquals("GC-2026-0042", result.formattedCode)
        assertEquals(42L, result.nextSequenceValue)
        assertFalse(result.wasCounterReset)
    }

    @Test
    fun `generateNextAutomatic resets counter to 1 when year changes under YEARLY policy`() {
        val snapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = SequenceDocumentType.SHARED_EXPENSE,
            prefix = "GC",
            patternTemplate = "{PREFIX}-{YYYY}-{SEQ:4}",
            currentValue = 128L,
            resetPolicy = SequenceResetPolicy.YEARLY,
            lastResetPeriodKey = "2026"
        )

        val result = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = snapshot,
            referenceDate = LocalDate.of(2027, 1, 1)
        )

        assertEquals("GC-2027-0001", result.formattedCode)
        assertEquals(1L, result.nextSequenceValue)
        assertEquals("2027", result.resolvedPeriodKey)
        assertTrue(result.wasCounterReset)
    }

    @Test
    fun `generateNextAutomatic interpolates group code and user initials tokens`() {
        val snapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = SequenceDocumentType.RECEIPT_TICKET,
            scopeGroupCode = "APTO",
            prefix = "TK",
            patternTemplate = "{PREFIX}-{GROUP}-{USER}-{YY}{MM}-{SEQ:3}",
            currentValue = 7L,
            resetPolicy = SequenceResetPolicy.MONTHLY,
            lastResetPeriodKey = "2026-10"
        )

        val result = ConsecutiveSequenceEngine.generateNextAutomatic(
            snapshot = snapshot,
            referenceDate = LocalDate.of(2026, 10, 15),
            groupCode = "APTO",
            userInitials = "CS"
        )

        assertEquals("TK-APTO-CS-2610-008", result.formattedCode)
        assertEquals(8L, result.nextSequenceValue)
    }

    @Test
    fun `validateAndResolveManualOrOcrCode rejects duplicate and suggests next automatic code`() {
        val snapshot = ConsecutiveSequenceEngine.SequenceSnapshot(
            sequenceType = SequenceDocumentType.SHARED_EXPENSE,
            prefix = "GC",
            patternTemplate = "{PREFIX}-{YYYY}-{SEQ:4}",
            currentValue = 14L,
            mode = ConsecutiveMode.HYBRID_ALLOW_OVERRIDE,
            resetPolicy = SequenceResetPolicy.YEARLY,
            lastResetPeriodKey = "2026"
        )

        val outcome = ConsecutiveSequenceEngine.validateAndResolveManualOrOcrCode(
            rawManualInput = "gc-2026-0014",
            snapshot = snapshot,
            alreadyExistsInDb = true,
            referenceDate = LocalDate.of(2026, 10, 15)
        )

        assertTrue(outcome is Outcome.Failure)
        val error = (outcome as Outcome.Failure).error as DomainError.SharedExpense.DuplicateConsecutiveCode
        assertEquals("GC-2026-0014", error.consecutiveCode)
        assertEquals("GC-2026-0015", error.suggestedNextAvailable)
    }

    @Test
    fun `computeTicketIntegrityHash produces deterministic 16-char uppercase SHA-256 signature`() {
        val hash1 = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = "GC-2026-0042",
            receiptTicketCode = "TK-2026-0042",
            totalAmount = 200000.0,
            currency = "COP",
            expenseDate = "2026-10-15",
            participantSharesSignature = "SELF_USER:50000.00;part_1:50000.00;part_2:50000.00;part_3:50000.00"
        )
        val hash2 = ConsecutiveSequenceEngine.computeTicketIntegrityHash(
            consecutiveCode = "gc-2026-0042",
            receiptTicketCode = "tk-2026-0042",
            totalAmount = 200000.0,
            currency = "cop",
            expenseDate = "2026-10-15",
            participantSharesSignature = "SELF_USER:50000.00;part_1:50000.00;part_2:50000.00;part_3:50000.00"
        )

        assertEquals(16, hash1.length)
        assertEquals(hash1, hash2)
    }
}
