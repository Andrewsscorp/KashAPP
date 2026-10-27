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
 * Isolated Unit Test: Global Maintenance Mode Killswitch.
 * Verifies that maintenance mode halts ingress unconditionally.
 */
class VersionGateMaintenanceModeTest {

    private class FakeVersionDataSource(val policy: VersionGatePolicy) : VersionDataSource {
        override suspend fun fetchRemotePolicy(): VersionGatePolicy = policy
        override suspend fun recordIngressTelemetry(telemetry: DeviceIngressTelemetry): Result<Unit> = Result.success(Unit)
        override fun getCachedPolicy(): VersionGatePolicy? = policy
        override fun saveCachedPolicy(policy: VersionGatePolicy) {}
    }

    @Test
    fun evaluate_whenMaintenanceActive_returnsMaintenanceStatusEvenWithLatestVersion() = runBlocking {
        val policy = VersionGatePolicy(
            minSupportedVersionCode = 900,
            latestVersionCode = 935,
            latestVersionName = "9.3.5",
            isMaintenanceMode = true,
            maintenanceMessage = "Mantenimiento por actualización de infraestructura bancaria"
        )
        val useCase = CheckAppVersionUseCase(FakeVersionDataSource(policy))

        val result = useCase(currentVersionCode = 950, currentVersionName = "9.5.0")

        assertTrue(result is VersionGateStatus.Maintenance)
        val maint = result as VersionGateStatus.Maintenance
        assertEquals("Mantenimiento por actualización de infraestructura bancaria", maint.message)
    }
}
