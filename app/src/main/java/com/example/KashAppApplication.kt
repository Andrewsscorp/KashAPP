package com.example

import android.app.Application
import android.util.Log
import com.example.data.worker.OutboxSyncWorker
import com.example.data.worker.SharedExpenseRecurrenceWorker
import com.example.di.AppContainer
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.util.DebtReminderNotificationManager

class KashAppApplication : Application() {

    val appContainer: AppContainer by lazy {
        AppContainer(this)
    }

    override fun onCreate() {
        super.onCreate()

        // Si el proceso actual es el proceso aislado :crash, omitir inicialización de base de datos y contenedores
        val isCrashProcess = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
            getProcessName().endsWith(":crash")
        } else {
            val pid = android.os.Process.myPid()
            val am = getSystemService(ACTIVITY_SERVICE) as? android.app.ActivityManager
            am?.runningAppProcesses?.firstOrNull { it.pid == pid }?.processName?.endsWith(":crash") == true
        }
        if (isCrashProcess) return

        // Instalar manejador global de excepciones fatales con aislamiento en proceso :crash y sanitización de PII
        com.example.core.crash.KashCrashHandler.install(this) {
            try {
                appContainer.securityDatabase.close()
            } catch (_: Throwable) {}
        }

        // Registrar coordinador de cerrojo de ciclo de vida con reloj monotónico
        registerActivityLifecycleCallbacks(appContainer.appLockLifecycleCoordinator)

        // 1. Inicializar Guardián Zero-Trust Firebase App Check para proteger llamadas a Firebase AI y Firestore
        try {
            FirebaseApiProtectionGuard.installAppCheckIfNeeded(
                context = this,
                isDebugBuild = BuildConfig.DEBUG
            )
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "FirebaseAppCheck guard notice: ${t.message}")
        }

        // 2. Pre-calentar contenedor de dependencias, repositorio de captura automática y re-enlace del servicio
        try {
            appContainer.autoCaptureRepository
            com.example.feature.capturaautomatica.service.BankNotificationListenerService.ensureServiceRebind(this)
        } catch (t: Throwable) {
            Log.e("KashAppApplication", "Fatal error initializing AppContainer: ${t.message}", t)
        }

        // 3. Crear canales de notificación de deudas y gastos compartidos
        try {
            DebtReminderNotificationManager.createNotificationChannel(this)
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "Notification channel creation notice: ${t.message}")
        }

        // 4. Programar sincronización Outbox y Worker de Gastos Compartidos Recurrentes + Quincenas (15 y 30)
        try {
            OutboxSyncWorker.schedulePeriodicSync(this)
            com.example.feature.sync.worker.DurableOutboxWorker.schedulePeriodic(this)
            appContainer.networkConnectivityMonitor
            SharedExpenseRecurrenceWorker.schedulePeriodicRecurrenceAndReminders(this)
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "Workers schedule notice: ${t.message}")
        }

        try {
            com.example.presentation.payments.notification.UpcomingPaymentsNotificationHelper.createNotificationChannel(this)
            com.example.data.worker.UpcomingPaymentsWorkScheduler.schedulePeriodicAlerts(this)
        } catch (t: Throwable) {
            Log.w("KashAppApplication", "UpcomingPaymentsWorkScheduler init notice: ${t.message}")
        }
    }
}
