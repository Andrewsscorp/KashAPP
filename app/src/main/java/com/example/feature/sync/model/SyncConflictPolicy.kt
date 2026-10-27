package com.example.feature.sync.model

/**
 * Chronological Last-Write-Wins (LWW) Conflict Resolution Policy.
 *
 * Implements deterministic timestamp-based ordering:
 * - If remote timestamp > local timestamp: Remote wins (USE_REMOTE).
 * - If local timestamp >= remote timestamp: Local wins (KEEP_LOCAL).
 * - If timestamps are identical, deterministic tie-breaking favors local data to prevent data loss.
 */
object SyncConflictPolicy {

    enum class ConflictDecision {
        USE_REMOTE,
        KEEP_LOCAL,
        NO_CONFLICT
    }

    data class VersionedItem<T>(
        val id: String,
        val item: T,
        val updatedAtTimestamp: Long,
        val isDeleted: Boolean = false
    )

    data class ResolutionResult<T>(
        val winner: T,
        val decision: ConflictDecision,
        val effectiveTimestamp: Long,
        val isTombstone: Boolean = false
    )

    /**
     * Resolves conflict between a local version and a remote version using chronological timestamps (LWW).
     */
    fun <T> resolveConflict(
        local: VersionedItem<T>?,
        remote: VersionedItem<T>?
    ): ResolutionResult<T>? {
        if (local == null && remote == null) return null

        if (local == null && remote != null) {
            return ResolutionResult(
                winner = remote.item,
                decision = ConflictDecision.USE_REMOTE,
                effectiveTimestamp = remote.updatedAtTimestamp,
                isTombstone = remote.isDeleted
            )
        }

        if (local != null && remote == null) {
            return ResolutionResult(
                winner = local.item,
                decision = ConflictDecision.KEEP_LOCAL,
                effectiveTimestamp = local.updatedAtTimestamp,
                isTombstone = local.isDeleted
            )
        }

        val nonNullLocal = local!!
        val nonNullRemote = remote!!

        return if (nonNullRemote.updatedAtTimestamp > nonNullLocal.updatedAtTimestamp) {
            ResolutionResult(
                winner = nonNullRemote.item,
                decision = ConflictDecision.USE_REMOTE,
                effectiveTimestamp = nonNullRemote.updatedAtTimestamp,
                isTombstone = nonNullRemote.isDeleted
            )
        } else {
            ResolutionResult(
                winner = nonNullLocal.item,
                decision = ConflictDecision.KEEP_LOCAL,
                effectiveTimestamp = nonNullLocal.updatedAtTimestamp,
                isTombstone = nonNullLocal.isDeleted
            )
        }
    }

    /**
     * Evaluates chronological decision strictly based on timestamps.
     */
    fun evaluateTimestamps(localTimestamp: Long, remoteTimestamp: Long): ConflictDecision {
        return if (remoteTimestamp > localTimestamp) {
            ConflictDecision.USE_REMOTE
        } else {
            ConflictDecision.KEEP_LOCAL
        }
    }
}
