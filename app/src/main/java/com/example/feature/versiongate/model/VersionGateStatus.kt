package com.example.feature.versiongate.model

/**
 * Access resolution evaluated by the Version Gate.
 */
sealed interface VersionGateStatus {
    object UpToDate : VersionGateStatus

    data class Blocked(
        val policy: VersionGatePolicy,
        val currentVersionCode: Int,
        val currentVersionName: String
    ) : VersionGateStatus

    data class Maintenance(
        val message: String
    ) : VersionGateStatus

    data class OptionalUpdate(
        val policy: VersionGatePolicy,
        val currentVersionCode: Int,
        val currentVersionName: String
    ) : VersionGateStatus
}
