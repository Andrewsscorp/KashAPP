package com.example.feature.versiongate.model

/**
 * Payload sent to Firebase Firestore on each ingress / login session.
 */
data class DeviceIngressTelemetry(
    val userId: String,
    val deviceId: String,
    val appVersionCode: Int,
    val appVersionName: String,
    val osVersion: Int,
    val deviceModel: String,
    val platform: String = "Android",
    val ingressTimestamp: Long = System.currentTimeMillis()
) {
    fun toMap(): Map<String, Any> = mapOf(
        "userId" to userId,
        "deviceId" to deviceId,
        "appVersionCode" to appVersionCode,
        "appVersionName" to appVersionName,
        "osVersion" to osVersion,
        "deviceModel" to deviceModel,
        "platform" to platform,
        "ingressTimestamp" to ingressTimestamp
    )
}
