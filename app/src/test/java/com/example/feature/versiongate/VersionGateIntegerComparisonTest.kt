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
 * Isolated Unit Test: Integer Version Code Comparison Engine.
 * Verifies that client version is rigorously compared against minimum supported build.
 */
class VersionGateIntegerComparisonTest {

    private class FakeVersionDataSource(val policy: VersionGatePolicy) : VersionDataSource {
        override suspend fun fetchRemotePolicy(): VersionGatePolicy = policy
        override suspend fun recordIngressTelemetry(telemetry: DeviceIngressTelemetry): Result<Unit> = Result.success(Unit)
        override fun getCachedPolicy(): VersionGatePolicy? = policy
        override fun saveCachedPolicy(policy: VersionGatePolicy) {}
    }

    @Test
    fun evaluate_whenCurrentVersionIsLessThanMinimum_returnsBlockedStatus() = runBlocking {
        val policy = VersionGatePolicy(
            minSupportedVersionCode = 940,
            latestVersionCode = 945,
            latestVersionName = "9.4.5"
        )
        val useCase = CheckAppVersionUseCase(FakeVersionDataSource(policy))

        val result = useCase(currentVersionCode = 935, currentVersionName = "9.3.5")

        assertTrue(result is VersionGateStatus.Blocked)
        val blocked = result as VersionGateStatus.Blocked
        assertEquals(935, blocked.currentVersionCode)
        assertEquals("9.3.5", blocked.currentVersionName)
        assertEquals(940, blocked.policy.minSupportedVersionCode)
    }

    @Test
    fun evaluate_whenCurrentVersionEqualsMinimum_returnsUpToDate() = runBlocking {
        val policy = VersionGatePolicy(
            minSupportedVersionCode = 935,
            latestVersionCode = 935,
            latestVersionName = "9.3.5"
        )
        val useCase = CheckAppVersionUseCase(FakeVersionDataSource(policy))

        val result = useCase(currentVersionCode = 935, currentVersionName = "9.3.5")

        assertEquals(VersionGateStatus.UpToDate, result)
    }

    @Test
    fun evaluate_whenCurrentVersionIsHigherThanMinimum_returnsUpToDate() = runBlocking {
        val policy = VersionGatePolicy(
            minSupportedVersionCode = 900,
            latestVersionCode = 935,
            latestVersionName = "9.3.5"
        )
        val useCase = CheckAppVersionUseCase(FakeVersionDataSource(policy))

        val result = useCase(currentVersionCode = 935, currentVersionName = "9.3.5")

        assertEquals(VersionGateStatus.UpToDate, result)
    }
}
