package com.example.feature.versiongate.domain

import com.example.feature.versiongate.data.VersionDataSource
import com.example.feature.versiongate.model.VersionGatePolicy
import com.example.feature.versiongate.model.VersionGateStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Domain use case to evaluate app version status against remote and cached policies.
 */
class CheckAppVersionUseCase(
    private val dataSource: VersionDataSource
) {
    suspend operator fun invoke(
        currentVersionCode: Int,
        currentVersionName: String
    ): VersionGateStatus {
        val policy = dataSource.fetchRemotePolicy()
        return evaluate(policy, currentVersionCode, currentVersionName)
    }

    fun observeStatus(
        currentVersionCode: Int,
        currentVersionName: String
    ): Flow<VersionGateStatus> {
        return dataSource.observeRemotePolicy().map { policy ->
            evaluate(policy, currentVersionCode, currentVersionName)
        }
    }

    fun evaluate(
        policy: VersionGatePolicy,
        currentVersionCode: Int,
        currentVersionName: String
    ): VersionGateStatus {
        return when {
            policy.isMaintenanceMode -> {
                VersionGateStatus.Maintenance(policy.maintenanceMessage)
            }
            currentVersionCode < policy.minSupportedVersionCode -> {
                VersionGateStatus.Blocked(
                    policy = policy,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
            }
            currentVersionCode < policy.latestVersionCode -> {
                VersionGateStatus.OptionalUpdate(
                    policy = policy,
                    currentVersionCode = currentVersionCode,
                    currentVersionName = currentVersionName
                )
            }
            else -> {
                VersionGateStatus.UpToDate
            }
        }
    }
}
