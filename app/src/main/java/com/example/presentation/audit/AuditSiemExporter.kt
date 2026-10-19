package com.example.presentation.audit

import android.content.Context
import androidx.core.content.FileProvider
import com.example.util.FinancialReportExporter.ExportResult
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Enterprise SIEM / SOC-2 JSON Exporter for KashApp Audit Vault.
 * Produces structured JSON log files ready for ingestion into enterprise
 * security information and event management (SIEM) systems (Splunk, Elastic, Datadog).
 */
object AuditSiemExporter {

    fun exportToSiemJson(
        context: Context,
        logs: List<AuditUiEntry>,
        userName: String = "Usuario KashApp",
        filterDesc: String = "Todos los Eventos"
    ): ExportResult {
        if (logs.isEmpty()) {
            return ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "No hay registros de auditoría para exportar"
            )
        }

        return try {
            val root = JSONObject()
            val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US)
            val nowStr = isoFormat.format(Date())

            // Header metadata
            val metadata = JSONObject().apply {
                put("generator", "KashApp Enterprise Forensic Engine")
                put("version", com.example.BuildConfig.VERSION_NAME)
                put("app_version", com.example.BuildConfig.VERSION_NAME)
                put("generated_at", nowStr)
                put("operator_user", userName)
                put("filter_applied", filterDesc)
                put("total_events", logs.size)
                put("security_standard", "SOC-2 Type II / ISO 27001 / NIIF NIC 16")
                put("storage_engine", "SQLCipher AES-256 (Hardware Keystore backed)")
                put("immutability_guarantee", "Append-Only Cryptographic Chain")
            }
            root.put("metadata", metadata)

            val eventsArray = JSONArray()
            logs.forEachIndexed { index, entry ->
                val eventObj = JSONObject().apply {
                    put("sequence_id", index + 1)
                    put("log_id", entry.id)
                    put("timestamp_epoch_ms", entry.timestamp)
                    put("timestamp_iso", isoFormat.format(Date(entry.timestamp)))
                    put("formatted_datetime", entry.formattedDateTime)
                    put("category", entry.category.name)
                    put("category_label", entry.category.label)
                    put("severity", entry.severity.name)
                    put("event_title", entry.eventTitle)
                    put("details", entry.details)
                    put("auth_origin", entry.authMethodOrOrigin)
                    put("sha256_digest", entry.hashSha256)
                    put("immutable", entry.isImmutable)
                }
                eventsArray.put(eventObj)
            }
            root.put("events", eventsArray)

            val fileName = "KashApp_SIEM_Auditoria_${System.currentTimeMillis()}.json"
            val file = File(com.example.util.SharedExportSecurityUtil.getExportsDir(context), fileName)
            FileOutputStream(file).use { out ->
                out.write(root.toString(2).toByteArray(Charsets.UTF_8))
            }

            val uri = try {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", file)
            } catch (_: Throwable) {
                android.net.Uri.fromFile(file)
            }

            ExportResult(
                success = true,
                filePath = file.absolutePath,
                fileUri = uri,
                summaryText = "Archivo SIEM JSON exportado exitosamente ($fileName)",
                message = "Archivo SIEM JSON generado correctamente (${logs.size} eventos)"
            )
        } catch (e: Exception) {
            ExportResult(
                success = false,
                filePath = null,
                fileUri = null,
                summaryText = "",
                message = "Error al exportar archivo SIEM: ${e.message}"
            )
        }
    }
}
