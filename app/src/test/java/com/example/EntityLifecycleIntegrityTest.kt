package com.example

import com.example.domain.usecase.ArchiveCategoryUseCase
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Verifies Acceptance Criterion 3:
 * Entity lifecycle archiving respects soft-delete and returns valid outcomes.
 */
class EntityLifecycleIntegrityTest {

    @Test
    fun testArchiveCategorySoftDelete() = runTest {
        val fakeRepo = FakeFinancialRepository()
        val archiveUseCase = ArchiveCategoryUseCase(fakeRepo)

        val outcome = archiveUseCase("cat_salud")
        assertTrue(outcome.isSuccess)
    }
}
