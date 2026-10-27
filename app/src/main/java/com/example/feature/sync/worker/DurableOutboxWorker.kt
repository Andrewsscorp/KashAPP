package com.example.feature.sync.worker

import android.content.Context
import android.util.Log
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.example.KashAppApplication
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

/**
 * Enterprise WorkManager CoroutineWorker for durable background sync.
 * Guarantees execution even if the app process is killed or device reboots.
 */
class DurableOutboxWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting DurableOutboxWorker synchronization cycle...")
        return@withContext try {
            val app = appContext.applicationContext as? KashAppApplication
            val appContainer = app?.appContainer
            if (appContainer == null) {
                Log.w(TAG, "AppContainer unavailable, retrying later.")
                return@withContext Result.retry()
            }

            val syncEngine = appContainer.outboxSyncEngine
            val currentUserId = appContainer.firebaseDataSource.getCurrentUserId().orEmpty()

            val result = syncEngine.syncBidirectional(currentUserId)
            if (result.isSuccess) {
                val stats = result.getOrDefault(com.example.feature.sync.model.SyncMergeStats())
                Log.i(TAG, "DurableOutboxWorker completed: pushed=${stats.pushedCount}, pulled=${stats.pulledCount}, purged=${stats.purgedDeliveredCount}")
                Result.success()
            } else {
                Log.w(TAG, "DurableOutboxWorker encountered error: ${result.exceptionOrNull()?.message}, retrying.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "DurableOutboxWorker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "DurableOutboxWorker"
        const val PERIODIC_WORK_NAME = "kashapp_durable_outbox_sync"
        const val IMMEDIATE_WORK_NAME = "kashapp_durable_sync_now"

        fun schedulePeriodic(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<DurableOutboxWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Periodic durable outbox sync scheduled every 15 minutes.")
        }

        fun enqueueImmediate(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<DurableOutboxWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                IMMEDIATE_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.i(TAG, "Immediate durable outbox sync enqueued.")
        }
    }
}
