package com.example.core.crash

import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.example.core.security.ColombianPiiRedactor
import kotlinx.coroutines.CoroutineExceptionHandler
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

/**
 * Manejador global de excepciones fatales (Hilos JVM + Corrutinas) con aislamiento en proceso ':crash'
 * y sanitización obligatoria de PII (Ley 1581 / OWASP MASVS-RESILIENCE).
 */
class KashCrashHandler internal constructor(
    private val appContext: Context? = null,
    private val onPreKillCleanup: (() -> Unit)? = null,
    private val cacheDirProvider: () -> File = { appContext?.cacheDir ?: File(System.getProperty("java.io.tmpdir")) }
) : Thread.UncaughtExceptionHandler {

    private val defaultHandler: Thread.UncaughtExceptionHandler? =
        Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            handleFatalThrowable(thread.name, throwable)
        } catch (e: Throwable) {
            defaultHandler?.uncaughtException(thread, throwable)
        } finally {
            try {
                onPreKillCleanup?.invoke()
            } catch (_: Throwable) {}
            android.os.Process.killProcess(android.os.Process.myPid())
            exitProcess(10)
        }
    }

    internal fun handleFatalThrowable(threadName: String, throwable: Throwable) {
        val sanitizedReport = buildSanitizedCrashReport(threadName, throwable)
        val reportFile = persistSanitizedCrashReport(sanitizedReport)

        val nonNullContext = appContext
        if (nonNullContext != null) {
            val intent = Intent(nonNullContext, CrashRecoveryActivity::class.java).apply {
                putExtra(EXTRA_CRASH_SUMMARY, sanitizedReport.take(MAX_INTENT_CHARS))
                if (reportFile != null) {
                    putExtra(EXTRA_CRASH_FILE_PATH, reportFile.absolutePath)
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
            }
            nonNullContext.startActivity(intent)
            try {
                Thread.sleep(400L)
            } catch (_: InterruptedException) {}
        }
    }

    internal fun buildSanitizedCrashReport(threadName: String, throwable: Throwable): String {
        val rawStackTrace = StringWriter().also { sw ->
            throwable.printStackTrace(PrintWriter(sw))
        }.toString()

        val redactedStackTrace = ColombianPiiRedactor.redactFinancialText(rawStackTrace)
        val timestampIso = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", Locale.US).format(Date())

        return buildString {
            appendLine("=== KASHAPP SECURE CRASH DIAGNOSTIC REPORT ===")
            appendLine("Timestamp    : $timestampIso")
            appendLine("App Version  : ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Device       : ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine("Android OS   : ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Thread       : $threadName")
            appendLine("PII Redacted : TRUE (ColombianPiiRedactor v1)")
            appendLine("==============================================")
            appendLine(redactedStackTrace)
        }
    }

    internal fun persistSanitizedCrashReport(reportContent: String): File? {
        return try {
            val dir = File(cacheDirProvider(), CRASH_DIR_NAME).apply {
                if (!exists()) mkdirs()
            }
            // Retención máxima: conservar solo los últimos 3 reportes y borrar antiguos
            dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(2)?.forEach { it.delete() }

            val fileName = "kash_crash_${System.currentTimeMillis()}.txt"
            File(dir, fileName).apply {
                writeText(reportContent, Charsets.UTF_8)
            }
        } catch (e: Exception) {
            Log.e(TAG, "No fue posible guardar reporte de crash en caché", e)
            null
        }
    }

    companion object {
        private const val TAG = "KashCrashHandler"
        const val CRASH_DIR_NAME = "kash_crash_reports"
        const val EXTRA_CRASH_SUMMARY = "extra_kash_crash_summary"
        const val EXTRA_CRASH_FILE_PATH = "extra_kash_crash_file_path"
        private const val MAX_INTENT_CHARS = 32_000

        fun install(context: Context, onPreKillCleanup: (() -> Unit)? = null): CoroutineExceptionHandler {
            val handler = KashCrashHandler(context.applicationContext, onPreKillCleanup)
            Thread.setDefaultUncaughtExceptionHandler(handler)
            return CoroutineExceptionHandler { _, throwable ->
                handler.handleFatalThrowable("CoroutineScope", throwable)
            }
        }
    }
}
