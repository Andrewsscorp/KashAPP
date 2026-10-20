package com.example.feature.capturaautomatica.parser

import com.example.feature.capturaautomatica.domain.parser.OtpSafetyFilter
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OtpSafetyFilterTest {

    @Test
    fun testOtpSafetyFilter_detectsClaveDinamicaAndOtpMessages() {
        // TEST-AC-05: Bloqueo estricto de Clave Dinámica / OTP
        val m1 = "Bancolombia: Tu clave dinamica es 839201. No la compartas con nadie."
        assertTrue("Debe detectar clave dinamica", OtpSafetyFilter.isSensitiveAuthMessage("Bancolombia", m1))

        val m2 = "Nequi: Usa el código 748291 para autorizar tu inicio de sesión."
        assertTrue("Debe detectar código de autorización", OtpSafetyFilter.isSensitiveAuthMessage("Nequi", m2))

        val m3 = "DaviPlata: Tu token de seguridad para transferir es 193820."
        assertTrue("Debe detectar token de seguridad", OtpSafetyFilter.isSensitiveAuthMessage("DaviPlata", m3))

        val m4 = "BBVA: Código de verificación 492019 para cambio de clave."
        assertTrue("Debe detectar código de verificación", OtpSafetyFilter.isSensitiveAuthMessage("BBVA", m4))

        val m5 = "Nu: Tu código OTP de seguridad es 849201."
        assertTrue("Debe detectar OTP", OtpSafetyFilter.isSensitiveAuthMessage("Nu", m5))
    }

    @Test
    fun testOtpSafetyFilter_allowsNormalFinancialTransactionMessages() {
        val t1 = "Bancolombia le informa Compra por $48.500,00 en D1 TUNJA CENTRO T.Deb *4821"
        assertFalse("No debe bloquear compra legítima", OtpSafetyFilter.isSensitiveAuthMessage("Bancolombia", t1))

        val t2 = "Pagaste $22.000,00 en CREPES Y WAFFLES con tu Nequi"
        assertFalse("No debe bloquear pago legítimo en Nequi", OtpSafetyFilter.isSensitiveAuthMessage("Nequi", t2))

        val t3 = "Compra aprobada por $ 64.900,00 en UBER TRIP con tu tarjeta ...7712"
        assertFalse("No debe bloquear compra de Nu", OtpSafetyFilter.isSensitiveAuthMessage("Nu", t3))
    }
}
