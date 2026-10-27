package com.example.feature.versiongate.data

import android.content.Context
import android.util.Log
import com.example.feature.versiongate.model.DeviceIngressTelemetry
import com.example.feature.versiongate.model.VersionGatePolicy
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await

/**
 * Production implementation of VersionDataSource using Firebase Firestore.
 * Interacts with 'app_config/version_control' for version enforcement
 * and 'user_devices/{userId}_{deviceId}' for access audit telemetry.
 */
class FirebaseVersionDataSource(
    private val context: Context,
    private val firestore: FirebaseFirestore = FirebaseFirestore.getInstance()
) : VersionDataSource {

    private val prefs = context.getSharedPreferences("kashapp_version_gate_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val TAG = "FirebaseVersionGate"
        private const val CONFIG_COLLECTION = "app_config"
        private const val CONFIG_DOC = "version_control"
        private const val DEVICES_COLLECTION = "user_devices"

        private const val KEY_MIN_VERSION = "cached_min_version"
        private const val KEY_LATEST_VERSION = "cached_latest_version"
        private const val KEY_LATEST_NAME = "cached_latest_name"
        private const val KEY_UPDATE_TITLE = "cached_update_title"
        private const val KEY_UPDATE_MESSAGE = "cached_update_message"
        private const val KEY_UPDATE_URL = "cached_update_url"
        private const val KEY_MAINTENANCE = "cached_maintenance"
    }

    override suspend fun fetchRemotePolicy(): VersionGatePolicy {
        return try {
            val snapshot = firestore.collection(CONFIG_COLLECTION)
                .document(CONFIG_DOC)
                .get()
                .await()

            if (snapshot.exists()) {
                val policy = VersionGatePolicy(
                    minSupportedVersionCode = snapshot.getLong("minSupportedVersionCode")?.toInt() ?: 1,
                    latestVersionCode = snapshot.getLong("latestVersionCode")?.toInt() ?: 1,
                    latestVersionName = snapshot.getString("latestVersionName") ?: "1.0.0",
                    forceUpdateTitle = snapshot.getString("forceUpdateTitle") ?: "Actualización Obligatoria de Seguridad",
                    forceUpdateMessage = snapshot.getString("forceUpdateMessage") ?: "Tu versión actual ya no es compatible. Por favor actualiza la aplicación.",
                    updateUrl = snapshot.getString("updateUrl") ?: "https://github.com/Andrewsscorp/KashAPP/releases",
                    isMaintenanceMode = snapshot.getBoolean("isMaintenanceMode") ?: false,
                    maintenanceMessage = snapshot.getString("maintenanceMessage") ?: "El sistema se encuentra en mantenimiento preventivo."
                )
                saveCachedPolicy(policy)
                policy
            } else {
                val fallback = getCachedPolicy() ?: VersionGatePolicy()
                try {
                    val initialMap = mapOf(
                        "minSupportedVersionCode" to fallback.minSupportedVersionCode,
                        "latestVersionCode" to fallback.latestVersionCode,
                        "latestVersionName" to fallback.latestVersionName,
                        "forceUpdateTitle" to fallback.forceUpdateTitle,
                        "forceUpdateMessage" to fallback.forceUpdateMessage,
                        "updateUrl" to fallback.updateUrl,
                        "isMaintenanceMode" to fallback.isMaintenanceMode,
                        "maintenanceMessage" to fallback.maintenanceMessage
                    )
                    firestore.collection(CONFIG_COLLECTION).document(CONFIG_DOC).set(initialMap).await()
                } catch (e: Exception) {
                    Log.w(TAG, "Could not initialize default remote version doc: ${e.message}")
                }
                fallback
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching remote version policy, using cached: ${e.message}")
            getCachedPolicy() ?: VersionGatePolicy()
        }
    }

    override suspend fun recordIngressTelemetry(telemetry: DeviceIngressTelemetry): Result<Unit> {
        return try {
            val docId = "${telemetry.userId}_${telemetry.deviceId}".take(120)
            firestore.collection(DEVICES_COLLECTION)
                .document(docId)
                .set(telemetry.toMap(), SetOptions.merge())
                .await()
            Log.i(TAG, "Recorded ingress telemetry for device: ${telemetry.deviceId} (version ${telemetry.appVersionCode})")
            Result.success(Unit)
        } catch (e: Exception) {
            Log.w(TAG, "Notice recording ingress telemetry: ${e.message}")
            Result.failure(e)
        }
    }

    override fun getCachedPolicy(): VersionGatePolicy? {
        if (!prefs.contains(KEY_MIN_VERSION)) return null
        return VersionGatePolicy(
            minSupportedVersionCode = prefs.getInt(KEY_MIN_VERSION, 1),
            latestVersionCode = prefs.getInt(KEY_LATEST_VERSION, 1),
            latestVersionName = prefs.getString(KEY_LATEST_NAME, "1.0.0") ?: "1.0.0",
            forceUpdateTitle = prefs.getString(KEY_UPDATE_TITLE, "Actualización Obligatoria de Seguridad") ?: "Actualización Obligatoria de Seguridad",
            forceUpdateMessage = prefs.getString(KEY_UPDATE_MESSAGE, "Por favor actualiza la aplicación.") ?: "Por favor actualiza la aplicación.",
            updateUrl = prefs.getString(KEY_UPDATE_URL, "https://github.com/Andrewsscorp/KashAPP/releases") ?: "https://github.com/Andrewsscorp/KashAPP/releases",
            isMaintenanceMode = prefs.getBoolean(KEY_MAINTENANCE, false)
        )
    }

    override fun saveCachedPolicy(policy: VersionGatePolicy) {
        prefs.edit()
            .putInt(KEY_MIN_VERSION, policy.minSupportedVersionCode)
            .putInt(KEY_LATEST_VERSION, policy.latestVersionCode)
            .putString(KEY_LATEST_NAME, policy.latestVersionName)
            .putString(KEY_UPDATE_TITLE, policy.forceUpdateTitle)
            .putString(KEY_UPDATE_MESSAGE, policy.forceUpdateMessage)
            .putString(KEY_UPDATE_URL, policy.updateUrl)
            .putBoolean(KEY_MAINTENANCE, policy.isMaintenanceMode)
            .apply()
    }
}
