package com.example.feature.versiongate

import com.example.feature.versiongate.model.DeviceIngressTelemetry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Isolated Unit Test: Device Ingress Telemetry Serialization.
 * Verifies that the access audit telemetry document matches the Firestore schema contract.
 */
class DeviceIngressTelemetryPayloadTest {

    @Test
    fun toMap_producesValidFirestoreIngressPayload() {
        val now = 1730000000000L
        val telemetry = DeviceIngressTelemetry(
            userId = "user_andrew_01",
            deviceId = "sha256_mock_device_abc123",
            appVersionCode = 935,
            appVersionName = "9.3.5",
            osVersion = 34,
            deviceModel = "Samsung SM-X200",
            platform = "Android",
            ingressTimestamp = now
        )

        val map = telemetry.toMap()

        assertEquals("user_andrew_01", map["userId"])
        assertEquals("sha256_mock_device_abc123", map["deviceId"])
        assertEquals(935, map["appVersionCode"])
        assertEquals("9.3.5", map["appVersionName"])
        assertEquals(34, map["osVersion"])
        assertEquals("Samsung SM-X200", map["deviceModel"])
        assertEquals("Android", map["platform"])
        assertEquals(now, map["ingressTimestamp"])
        assertNotNull(map["deviceId"])
    }
}
