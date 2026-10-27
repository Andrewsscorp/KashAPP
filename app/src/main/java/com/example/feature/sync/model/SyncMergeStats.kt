package com.example.feature.sync.model

/**
 * Metric tracking and statistics for delta synchronizations and outbox queue drains.
 */
data class SyncMergeStats(
    val pushedCount: Int = 0,
    val pulledCount: Int = 0,
    val localWinsCount: Int = 0,
    val remoteWinsCount: Int = 0,
    val purgedDeliveredCount: Int = 0,
    val failedCount: Int = 0,
    val durationMillis: Long = 0L,
    val errorMessage: String? = null
) {
    val isSuccess: Boolean get() = failedCount == 0 && errorMessage == null
    val totalProcessed: Int get() = pushedCount + pulledCount
}
