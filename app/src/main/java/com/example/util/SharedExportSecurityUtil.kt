package com.example.util

import android.content.Context
import java.io.File

/**
 * Utilidad de seguridad para gestionar el directorio acotado 'shared_exports' del FileProvider.
 * Cumple con REM-P1-04 (HALL-07 / GATE-07), evitando la retención de artefactos financieros en texto plano.
 */
object SharedExportSecurityUtil {
    const val EXPORTS_DIR_NAME = "shared_exports"
    const val STALE_THRESHOLD_MS = 5 * 60 * 1000L // 5 minutos

    /**
     * Obtiene el subdirectorio exclusivo de exportaciones compartidas y purga archivos obsoletos.
     */
    fun getExportsDir(context: Context): File {
        val dir = File(context.cacheDir, EXPORTS_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
        purgeStaleExports(context, STALE_THRESHOLD_MS)
        return dir
    }

    /**
     * Purga archivos en shared_exports con antigüedad mayor a [maxAgeMs].
     */
    fun purgeStaleExports(context: Context, maxAgeMs: Long = STALE_THRESHOLD_MS) {
        try {
            val dir = File(context.cacheDir, EXPORTS_DIR_NAME)
            if (dir.exists() && dir.isDirectory) {
                val now = System.currentTimeMillis()
                dir.listFiles()?.forEach { file ->
                    if (file.isFile && (now - file.lastModified() > maxAgeMs)) {
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {
            // Limpieza best-effort
        }
    }

    /**
     * Purga todos los archivos en shared_exports sin importar su antigüedad.
     */
    fun purgeAllExports(context: Context) {
        try {
            val dir = File(context.cacheDir, EXPORTS_DIR_NAME)
            if (dir.exists() && dir.isDirectory) {
                dir.listFiles()?.forEach { file ->
                    if (file.isFile) {
                        file.delete()
                    }
                }
            }
        } catch (_: Throwable) {
            // Limpieza best-effort
        }
    }
}
