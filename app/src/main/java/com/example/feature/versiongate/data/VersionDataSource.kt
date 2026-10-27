package com.example.feature.versiongate.data

import com.example.feature.versiongate.model.DeviceIngressTelemetry
import com.example.feature.versiongate.model.VersionGatePolicy
import kotlinx.coroutines.flow.Flow

/**
 * Data source interface for remote version configuration and device telemetry.
 */
interface VersionDataSource {
    suspend fun fetchRemotePolicy(): VersionGatePolicy
    fun observeRemotePolicy(): Flow<VersionGatePolicy>
    suspend fun recordIngressTelemetry(telemetry: DeviceIngressTelemetry): Result<Unit>
    fun getCachedPolicy(): VersionGatePolicy?
    fun saveCachedPolicy(policy: VersionGatePolicy)
}
