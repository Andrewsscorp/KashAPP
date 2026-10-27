package com.example.feature.versiongate.presentation

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.feature.versiongate.domain.CheckAppVersionUseCase
import com.example.feature.versiongate.domain.RecordIngressTelemetryUseCase
import com.example.feature.versiongate.model.DeviceIngressTelemetry
import com.example.feature.versiongate.model.VersionGateStatus
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.security.MessageDigest

/**
 * ViewModel managing the Version Gate lifecycle, remote verification, and device ingress telemetry.
 */
class VersionGateViewModel(
    private val checkAppVersionUseCase: CheckAppVersionUseCase,
    private val recordIngressTelemetryUseCase: RecordIngressTelemetryUseCase
) : ViewModel() {

    private val _gateStatus = MutableStateFlow<VersionGateStatus>(VersionGateStatus.UpToDate)
    val gateStatus: StateFlow<VersionGateStatus> = _gateStatus.asStateFlow()

    fun verifyVersionAndRecordIngress(context: Context, userId: String) {
        val currentVersionCode = BuildConfig.VERSION_CODE
        val currentVersionName = BuildConfig.VERSION_NAME

        // Observe real-time changes from Firebase Firestore
        viewModelScope.launch {
            try {
                checkAppVersionUseCase.observeStatus(currentVersionCode, currentVersionName).collect { status ->
                    _gateStatus.value = status
                }
            } catch (e: Exception) {
                Log.w("VersionGateViewModel", "Notice observing real-time version status: ${e.message}")
            }
        }

        // Record ingress telemetry
        viewModelScope.launch {
            try {
                val rawAndroidId = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID).orEmpty()
                val secureDeviceId = hashSha256(rawAndroidId.ifBlank { "kashapp_device_${Build.SERIAL}" })

                val telemetry = DeviceIngressTelemetry(
                    userId = userId.ifBlank { "anonymous" },
                    deviceId = secureDeviceId,
                    appVersionCode = currentVersionCode,
                    appVersionName = currentVersionName,
                    osVersion = Build.VERSION.SDK_INT,
                    deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}".trim(),
                    platform = "Android",
                    ingressTimestamp = System.currentTimeMillis()
                )

                recordIngressTelemetryUseCase(telemetry)
            } catch (e: Exception) {
                Log.w("VersionGateViewModel", "Notice recording ingress telemetry: ${e.message}")
            }
        }
    }

    private fun hashSha256(input: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }.take(32)
        } catch (_: Exception) {
            "fallback_${input.hashCode()}"
        }
    }
}
