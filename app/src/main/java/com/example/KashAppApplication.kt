package com.example

import android.app.Application
import android.util.Log
import com.example.di.AppContainer

class KashAppApplication : Application() {

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Install graceful uncaught exception handler to log fatal issues cleanly
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e("KashAppFatal", "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            defaultHandler?.uncaughtException(thread, throwable)
        }

        try {
            // Pre-warm container
            appContainer
        } catch (t: Throwable) {
            Log.e("KashAppApplication", "Fatal error initializing AppContainer: ${t.message}", t)
        }

        try {
            com.example.util.DebtReminderNotificationManager.createNotificationChannel(this)
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "Notification channel creation notice: ${t.message}")
        }

        try {
            com.example.data.worker.OutboxSyncWorker.schedulePeriodicSync(this)
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "OutboxSyncWorker schedule notice: ${t.message}")
        }
    }
}
