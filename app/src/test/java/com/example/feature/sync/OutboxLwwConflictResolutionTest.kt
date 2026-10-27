package com.example.feature.sync

import com.example.feature.sync.model.SyncConflictPolicy
import com.example.feature.sync.model.SyncConflictPolicy.ConflictDecision
import com.example.feature.sync.model.SyncConflictPolicy.VersionedItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Isolated Enterprise Unit Test Suite for Last-Write-Wins (LWW) Conflict Resolution.
 *
 * Requirements:
 * 1. Chronological order based on timestamps determines the winning mutation.
 * 2. If remote timestamp > local timestamp -> USE_REMOTE.
 * 3. If local timestamp > remote timestamp -> KEEP_LOCAL.
 * 4. Ties (identical timestamps) -> KEEP_LOCAL to prevent overwriting user modifications.
 * 5. Handle null states and tombstone deletions cleanly.
 */
class OutboxLwwConflictResolutionTest {

    @Test
    fun evaluateTimestamps_remoteNewer_wins() {
        val localTs = 1730000000000L
        val remoteTs = 1730000005000L

        val decision = SyncConflictPolicy.evaluateTimestamps(localTs, remoteTs)
        assertEquals(ConflictDecision.USE_REMOTE, decision)
    }

    @Test
    fun evaluateTimestamps_localNewer_wins() {
        val localTs = 1730000009000L
        val remoteTs = 1730000001000L

        val decision = SyncConflictPolicy.evaluateTimestamps(localTs, remoteTs)
        assertEquals(ConflictDecision.KEEP_LOCAL, decision)
    }

    @Test
    fun evaluateTimestamps_identicalTimestamps_tieBreakerKeepsLocal() {
        val identicalTs = 1730000000000L

        val decision = SyncConflictPolicy.evaluateTimestamps(identicalTs, identicalTs)
        assertEquals(ConflictDecision.KEEP_LOCAL, decision)
    }

    @Test
    fun resolveConflict_whenLocalIsNullAndRemoteExists_usesRemote() {
        val remote = VersionedItem(
            id = "tx_remote_1",
            item = "Remote Transaction Payload",
            updatedAtTimestamp = 1730000000000L
        )

        val resolution = SyncConflictPolicy.resolveConflict(local = null, remote = remote)

        assertNotNull(resolution)
        assertEquals(ConflictDecision.USE_REMOTE, resolution!!.decision)
        assertEquals("Remote Transaction Payload", resolution.winner)
        assertEquals(1730000000000L, resolution.effectiveTimestamp)
    }

    @Test
    fun resolveConflict_whenRemoteIsNullAndLocalExists_keepsLocal() {
        val local = VersionedItem(
            id = "tx_local_1",
            item = "Local Transaction Payload",
            updatedAtTimestamp = 1730000000000L
        )

        val resolution = SyncConflictPolicy.resolveConflict(local = local, remote = null)

        assertNotNull(resolution)
        assertEquals(ConflictDecision.KEEP_LOCAL, resolution!!.decision)
        assertEquals("Local Transaction Payload", resolution.winner)
    }

    @Test
    fun resolveConflict_remoteIsNewer_overwritesLocalPayload() {
        val local = VersionedItem(
            id = "acc_nu",
            item = "Balance: 6,325,600 COP",
            updatedAtTimestamp = 1730000000000L
        )
        val remote = VersionedItem(
            id = "acc_nu",
            item = "Balance: 6,225,600 COP (After Expense on Tablet)",
            updatedAtTimestamp = 1730000002000L
        )

        val resolution = SyncConflictPolicy.resolveConflict(local, remote)

        assertNotNull(resolution)
        assertEquals(ConflictDecision.USE_REMOTE, resolution!!.decision)
        assertEquals("Balance: 6,225,600 COP (After Expense on Tablet)", resolution.winner)
        assertEquals(1730000002000L, resolution.effectiveTimestamp)
    }

    @Test
    fun resolveConflict_remoteTombstoneIsNewer_marksAsDeleted() {
        val local = VersionedItem(
            id = "asset_car",
            item = "Mazda 3",
            updatedAtTimestamp = 1730000000000L,
            isDeleted = false
        )
        val remoteTombstone = VersionedItem(
            id = "asset_car",
            item = "Mazda 3 (Deleted on Phone)",
            updatedAtTimestamp = 1730000005000L,
            isDeleted = true
        )

        val resolution = SyncConflictPolicy.resolveConflict(local, remoteTombstone)

        assertNotNull(resolution)
        assertEquals(ConflictDecision.USE_REMOTE, resolution!!.decision)
        assertTrue("El tombstone remoto más reciente debe ganar y marcar como eliminado", resolution.isTombstone)
    }
}
