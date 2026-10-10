package com.example.data.worker

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
 * Android WorkManager CoroutineWorker that monitors the offline Outbox queue (OutboxDao)
 * and synchronizes pending mutations to cloud storage (Firebase Firestore) as soon as network is connected.
 */
class OutboxSyncWorker(
    private val appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.i(TAG, "Starting OutboxSyncWorker execution...")
        return@withContext try {
            val appContainer = (appContext.applicationContext as? KashAppApplication)?.appContainer
            if (appContainer == null) {
                Log.w(TAG, "AppContainer not available, retrying later.")
                return@withContext Result.retry()
            }

            val processResult = appContainer.financialRepository.processOutboxQueue()
            if (processResult.isSuccess) {
                val processedCount = processResult.getOrDefault(0)
                Log.i(TAG, "OutboxSyncWorker completed successfully: $processedCount operations processed.")
                Result.success()
            } else {
                val err = processResult.exceptionOrNull()
                Log.w(TAG, "OutboxSyncWorker encountered error: ${err?.message}, retrying with backoff.")
                Result.retry()
            }
        } catch (e: Exception) {
            Log.e(TAG, "OutboxSyncWorker failed: ${e.message}", e)
            Result.retry()
        }
    }

    companion object {
        const val TAG = "OutboxSyncWorker"
        const val PERIODIC_WORK_NAME = "kashapp_periodic_outbox_sync"
        const val ONE_TIME_WORK_NAME = "kashapp_immediate_outbox_sync"

        fun schedulePeriodicSync(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = PeriodicWorkRequestBuilder<OutboxSyncWorker>(15, TimeUnit.MINUTES)
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                PERIODIC_WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
            Log.i(TAG, "Scheduled periodic Outbox synchronization every 15 minutes with CONNECTED network constraint.")
        }

        fun syncNow(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val request = OneTimeWorkRequestBuilder<OutboxSyncWorker>()
                .setConstraints(constraints)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                ONE_TIME_WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
            Log.i(TAG, "Enqueued immediate Outbox synchronization.")
        }
    }
}
