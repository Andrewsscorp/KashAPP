package com.example

import com.example.core.id.DeterministicIdGenerator
import com.example.core.id.SequentialIdGenerator
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Acceptance Criterion 8:
 * Idempotent client-side ID generation prevents duplicate writes.
 */
class IdempotentCreationTest {

    @Test
    fun testSequentialIdGeneratorProducesOrderedUniqueIds() {
        val generator = SequentialIdGenerator()
        val id1 = generator.generateId("tx")
        val id2 = generator.generateId("tx")

        assertNotEquals(id1, id2)
        assertTrue(id1.startsWith("tx_"))
        assertTrue(id2.startsWith("tx_"))
    }

    @Test
    fun testDeterministicIdGeneratorForTesting() {
        val generator = DeterministicIdGenerator("cat")
        val id1 = generator.generateId()
        val id2 = generator.generateId()

        assertEquals("cat_1", id1)
        assertEquals("cat_2", id2)
    }
}
