package com.example.feature.gastoscompartidos

import com.example.domain.analytics.SharedExpenseFirebaseAiEngine
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.feature.gastoscompartidos.domain.ocr.SharedOcrReceiptBridge
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Suite de verificación obligatoria para REM-P3-02 (Fase 10 / VER-F10-01):
 * - Sanitización estricta de PII (PAN de tarjetas, correos, teléfonos y documentos).
 * - Preservación inalterada de importes monetarios en pesos colombianos (COP).
 * - Cumplimiento del régimen de autorización previa e informada (Ley 1581 de 2012 / Dec. 1377 de 2013).
 */
class OcrPiiSanitizerTest {

    @Before
    fun setUp() {
        FirebaseApiProtectionGuard.setPrivacyConsent(false)
    }

    @After
    fun tearDown() {
        FirebaseApiProtectionGuard.setPrivacyConsent(false)
    }

    @Test
    fun testSanitizePii_preservesCopAmountsAndRedactsCardPanAndSensitiveData() {
        val simulatedReceipt = """
            RESTAURANTE EL BUEN SABOR S.A.S.
            NIT: 900.543.210-9
            FACTURA ELECTRONICA: FE-98432
            FECHA: 2026-10-18
            1x BANDEJA PAISA TRADICIONAL $ 38.000
            2x JUGO DE LULO EN AGUA $ 16.000
            1x POSTRE TRES LECHES $ 12.500
            SUBTOTAL: $ 66.500
            PROPINA VOLUNTARIA 10%: $ 6.650
            IMPOCONSUMO 8%: $ 5.320
            TOTAL PAGADO: $ 78.470
            FORMA DE PAGO: TARJETA MASTERCARD **** **** **** 4321
            BANCO: BANCOLOMBIA CUENTA 1029384756
            AUTORIZACION: 839201
            CLIENTE: CÉDULA 1020304050
            TEL: +57 300 123 4567
            EMAIL: comensal.auditoria@empresa.com
        """.trimIndent()

        val sanitized = FirebaseApiProtectionGuard.sanitizePiiBeforeCloudAi(simulatedReceipt)

        // 1. Verificación de Sanitización de PII
        assertTrue("Debe contener token [REDACTED_PAN]", sanitized.contains("[REDACTED_PAN]"))
        assertFalse("No debe exponer la tarjeta enmascarada", sanitized.contains("**** **** **** 4321"))

        assertTrue("Debe contener token [REDACTED_EMAIL]", sanitized.contains("[REDACTED_EMAIL]"))
        assertFalse("No debe exponer el correo electrónico", sanitized.contains("comensal.auditoria@empresa.com"))

        assertTrue("Debe contener token [REDACTED_PHONE]", sanitized.contains("[REDACTED_PHONE]"))
        assertFalse("No debe exponer el número de teléfono celular", sanitized.contains("300 123 4567"))

        assertTrue("Debe contener token [REDACTED_ID]", sanitized.contains("[REDACTED_ID]"))
        assertFalse("No debe exponer la cédula de ciudadanía", sanitized.contains("1020304050"))
        assertFalse("No debe exponer el NIT en el texto transmitido a la IA", sanitized.contains("900.543.210-9"))

        // 2. Verificación de Preservación Intacta de Montos COP (Regla crítica de no-regresión contable)
        assertTrue("Debe conservar el precio del plato principal $ 38.000", sanitized.contains("$ 38.000"))
        assertTrue("Debe conservar el precio de las bebidas $ 16.000", sanitized.contains("$ 16.000"))
        assertTrue("Debe conservar el precio del postre $ 12.500", sanitized.contains("$ 12.500"))
        assertTrue("Debe conservar el subtotal $ 66.500", sanitized.contains("$ 66.500"))
        assertTrue("Debe conservar la propina $ 6.650", sanitized.contains("$ 6.650"))
        assertTrue("Debe conservar el impoconsumo $ 5.320", sanitized.contains("$ 5.320"))
        assertTrue("Debe conservar el total pagado $ 78.470", sanitized.contains("$ 78.470"))
    }

    @Test
    fun testSanitizePii_raw16DigitCardNumbersRedactedWhilePreservingRawCopAmounts() {
        val rawInput = """
            Pago cena con tarjeta 4532 1234 5678 9012 por valor de 150000 COP.
            Adicional tarjeta visa 4111222233334444 por 35000 pesos.
            Subtotal: 185000.
        """.trimIndent()

        val sanitized = FirebaseApiProtectionGuard.sanitizePiiBeforeCloudAi(rawInput)

        assertTrue("Debe reemplazar tarjetas de 16 dígitos por [REDACTED_PAN]", sanitized.contains("[REDACTED_PAN]"))
        assertFalse("No debe contener tarjeta 4532 1234 5678 9012", sanitized.contains("4532 1234 5678 9012"))
        assertFalse("No debe contener tarjeta 4111222233334444", sanitized.contains("4111222233334444"))

        // Montos deben conservarse
        assertTrue("Debe conservar monto 150000 COP", sanitized.contains("150000 COP"))
        assertTrue("Debe conservar monto 35000 pesos", sanitized.contains("35000 pesos"))
        assertTrue("Debe conservar Subtotal: 185000", sanitized.contains("185000"))
    }

    @Test
    fun testAuthorizeRequest_blocksWhenConsentNotGranted_Ley1581() {
        FirebaseApiProtectionGuard.setPrivacyConsent(false)

        val verdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = "Gasto de almuerzo $ 45.000",
            requireInformedConsent = true
        )

        assertTrue("Llamada sin consentimiento debe ser bloqueada", verdict is FirebaseApiProtectionGuard.GuardVerdict.Blocked)
        val blocked = verdict as FirebaseApiProtectionGuard.GuardVerdict.Blocked
        assertEquals("CONSENT_REQUIRED_LEY_1581", blocked.reasonCode)
        assertTrue(
            "Mensaje debe citar Ley 1581 de 2012",
            blocked.userMessage.contains("Ley 1581 de 2012")
        )
    }

    @Test
    fun testAuthorizeRequest_allowsWhenConsentGranted_Ley1581() {
        val timestamp = 1776510000000L
        FirebaseApiProtectionGuard.setPrivacyConsent(
            granted = true,
            version = "1.0-LEY-1581-2012",
            timestampMs = timestamp
        )

        val verdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = "Gasto de almuerzo $ 45.000 pagado con tarjeta **** **** **** 8888",
            requireInformedConsent = true
        )

        assertTrue("Llamada con consentimiento otorgado debe ser permitida", verdict is FirebaseApiProtectionGuard.GuardVerdict.Allowed)
        val allowed = verdict as FirebaseApiProtectionGuard.GuardVerdict.Allowed
        assertTrue("Prompt sanitizado debe conservar el monto", allowed.sanitizedPrompt.contains("$ 45.000"))
        assertTrue("Prompt sanitizado debe haber enmascarado el PAN", allowed.sanitizedPrompt.contains("[REDACTED_PAN]"))
        assertFalse("Prompt sanitizado no debe tener el número de tarjeta", allowed.sanitizedPrompt.contains("8888"))

        val telemetry = FirebaseApiProtectionGuard.getProtectionTelemetry()
        assertTrue("Telemetría debe reportar consentimiento otorgado", telemetry.isPrivacyConsentGranted)
        assertEquals("1.0-LEY-1581-2012", telemetry.privacyConsentVersion)
    }

    @Test
    fun testSharedOcrReceiptBridge_and_SharedExpenseFirebaseAiEngine_useSanitizer() {
        val testText = "Recibo con tarjeta **** **** **** 9999 y celular 3109876543 por $ 52.000"

        val bridgeSanitized = SharedOcrReceiptBridge.sanitizePiiBeforeCloudAi(testText)
        assertTrue(bridgeSanitized.contains("[REDACTED_PAN]"))
        assertTrue(bridgeSanitized.contains("[REDACTED_PHONE]"))
        assertTrue(bridgeSanitized.contains("$ 52.000"))

        val engine = SharedExpenseFirebaseAiEngine()
        val engineSanitized = engine.sanitizePiiBeforeCloudAi(testText)
        assertEquals(bridgeSanitized, engineSanitized)
    }
}
