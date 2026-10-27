package com.example.feature.versiongate

import com.example.feature.versiongate.data.VersionDataSource
import com.example.feature.versiongate.domain.CheckAppVersionUseCase
import com.example.feature.versiongate.model.DeviceIngressTelemetry
import com.example.feature.versiongate.model.VersionGatePolicy
import com.example.feature.versiongate.model.VersionGateStatus
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated Unit Test: Optional Update Notification Evaluation.
 * Verifies that when version meets minimum requirement but a newer build is released,
 * the policy triggers an OptionalUpdate status without blocking the user.
 */
class VersionGateOptionalUpdateTest {

    private class FakeVersionDataSource(val policy: VersionGatePolicy) : VersionDataSource {
        override suspend fun fetchRemotePolicy(): VersionGatePolicy = policy
        override fun observeRemotePolicy(): kotlinx.coroutines.flow.Flow<VersionGatePolicy> = kotlinx.coroutines.flow.flowOf(policy)
        override suspend fun recordIngressTelemetry(telemetry: DeviceIngressTelemetry): Result<Unit> = Result.success(Unit)
        override fun getCachedPolicy(): VersionGatePolicy? = policy
        override fun saveCachedPolicy(policy: VersionGatePolicy) {}
    }

    @Test
    fun evaluate_whenVersionBetweenMinimumAndLatest_returnsOptionalUpdate() = runBlocking {
        val policy = VersionGatePolicy(
            minSupportedVersionCode = 930,
            latestVersionCode = 940,
            latestVersionName = "9.4.0",
            isMaintenanceMode = false
        )
        val useCase = CheckAppVersionUseCase(FakeVersionDataSource(policy))

        val result = useCase(currentVersionCode = 935, currentVersionName = "9.3.5")

        assertTrue(result is VersionGateStatus.OptionalUpdate)
        val optional = result as VersionGateStatus.OptionalUpdate
        assertEquals(935, optional.currentVersionCode)
        assertEquals(940, optional.policy.latestVersionCode)
    }
}
