package com.example.feature.versiongate.domain

import com.example.feature.versiongate.data.VersionDataSource
import com.example.feature.versiongate.model.DeviceIngressTelemetry

/**
 * Domain use case to register active session ingress telemetry in Firebase Firestore.
 */
class RecordIngressTelemetryUseCase(
    private val dataSource: VersionDataSource
) {
    suspend operator fun invoke(telemetry: DeviceIngressTelemetry): Result<Unit> {
        return dataSource.recordIngressTelemetry(telemetry)
    }
}
