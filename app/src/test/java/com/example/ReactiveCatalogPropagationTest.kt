package com.example

import com.example.core.id.DeterministicIdGenerator
import com.example.domain.model.CategoryType
import com.example.domain.usecase.CreateCategoryUseCase
import com.example.domain.usecase.ObserveCategoriesUseCase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Acceptance Criterion 1:
 * Creating an entity propagates immediately to multiple active collectors without refresh or navigation.
 */
@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class ReactiveCatalogPropagationTest {

    @Test
    fun testReactiveCategoryPropagationAcrossMultipleCollectors() = runTest {
        val fakeRepo = FakeFinancialRepository()
        val observeUseCase = ObserveCategoriesUseCase(fakeRepo)
        val idGen = DeterministicIdGenerator("cat_gen")
        val createUseCase = CreateCategoryUseCase(fakeRepo, idGen)

        // Collector A (e.g. Screen 1: CreateExpense)
        val emittedListA = mutableListOf<List<com.example.domain.model.Category>>()
        val jobA = launch {
            observeUseCase(CategoryType.EXPENSE).collect {
                emittedListA.add(it)
            }
        }

        // Collector B (e.g. Screen 3: CreateBudget)
        val emittedListB = mutableListOf<List<com.example.domain.model.Category>>()
        val jobB = launch {
            observeUseCase(CategoryType.EXPENSE).collect {
                emittedListB.add(it)
            }
        }

        advanceUntilIdle()

        // Initially empty
        assertEquals(1, emittedListA.size)
        assertEquals(0, emittedListA.last().size)
        assertEquals(1, emittedListB.size)
        assertEquals(0, emittedListB.last().size)

        // Screen 1 creates a new category "Gimnasio & Deporte"
        val outcome = createUseCase(
            name = "Gimnasio & Deporte",
            colorHex = "#8B5CF6",
            icon = "fitness_center",
            type = CategoryType.EXPENSE
        )

        assertTrue(outcome.isSuccess)
        val created = outcome.getOrNull()!!
        assertEquals("Gimnasio & Deporte", created.name)

        advanceUntilIdle()

        // Both collectors immediately received the new category
        assertEquals(2, emittedListA.size)
        assertEquals(2, emittedListB.size)
        assertTrue(emittedListA.last().any { it.name == "Gimnasio & Deporte" })
        assertTrue(emittedListB.last().any { it.name == "Gimnasio & Deporte" })

        jobA.cancel()
        jobB.cancel()
    }
}
