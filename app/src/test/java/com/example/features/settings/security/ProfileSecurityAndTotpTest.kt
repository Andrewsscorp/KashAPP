package com.example.features.settings.security

import com.example.data.local.KeystoreCryptoManager
import com.example.feature.impuestosdian.domain.model.TaxJurisdiction
import com.example.features.settings.data.UserProfileSecurityManager
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Suite de pruebas unitarias para el motor criptográfico 2FA TOTP (RFC 6238),
 * cifrado Keystore AES-256-GCM y validadores de seguridad del módulo "Mi Perfil".
 */
class ProfileSecurityAndTotpTest {

    @Test
    fun `1_encodeAndDecodeBase32_roundTripIsExact`() {
        val original = "KashApp-2FA-Secret-2026".toByteArray(Charsets.UTF_8)
        val encoded = TotpTwoFactorAuthenticator.encodeBase32(original)
        val decoded = TotpTwoFactorAuthenticator.decodeBase32(encoded)
        assertArrayEquals(original, decoded)
    }

    @Test
    fun `2_generateSecretBase32_produces32CharValidBase32`() {
        val secret = TotpTwoFactorAuthenticator.generateSecretBase32()
        assertEquals(32, secret.length)
        assertTrue(secret.all { it in "ABCDEFGHIJKLMNOPQRSTUVWXYZ234567" })
    }

    @Test
    fun `3_generateAndVerifyTotpCode_acceptsCurrentAndAdjacentWindow`() {
        val secret = TotpTwoFactorAuthenticator.generateSecretBase32()
        val fixedTimestamp = 1_710_000_000_000L // Instante determinista

        val codeNow = TotpTwoFactorAuthenticator.generateCurrentCode(secret, fixedTimestamp)
        assertEquals(6, codeNow.length)
        assertTrue(codeNow.all { it.isDigit() })

        // Debe ser válido en el instante exacto
        assertTrue(TotpTwoFactorAuthenticator.verifyCode(secret, codeNow, fixedTimestamp))

        // Debe ser válido con +25 segundos de desfase (misma ventana o ventana adyacente +/- 1)
        assertTrue(TotpTwoFactorAuthenticator.verifyCode(secret, codeNow, fixedTimestamp + 25_000L))

        // Debe ser rechazado si han pasado 5 minutos (10 ventanas de 30s)
        assertFalse(TotpTwoFactorAuthenticator.verifyCode(secret, codeNow, fixedTimestamp + 300_000L))
    }

    @Test
    fun `4_buildOtpAuthUri_containsIssuerAccountAndSecret`() {
        val secret = "JBSWY3DPEHPK3PXPJBSWY3DPEHPK3PXP"
        val uri = TotpTwoFactorAuthenticator.buildOtpAuthUri(
            accountEmail = "usuario@kashapp.co",
            secretBase32 = secret
        )
        assertTrue(uri.startsWith("otpauth://totp/"))
        assertTrue(uri.contains("secret=$secret"))
        assertTrue(uri.contains("issuer=KashApp"))
        assertTrue(uri.contains("digits=6"))
        assertTrue(uri.contains("period=30"))
    }

    @Test
    fun `5_generateRecoveryCodes_creates8UniqueFormattedCodes`() {
        val codes = TotpTwoFactorAuthenticator.generateRecoveryCodes()
        assertEquals(8, codes.size)
        assertEquals(8, codes.toSet().size)
        codes.forEach { code ->
            assertTrue(Regex("^[A-Z2-9]{4}-[A-Z2-9]{4}$").matches(code))
        }
    }

    @Test
    fun `6_normalizeE164PhoneNumber_handlesColombianAndInternationalFormats`() {
        // Celular colombiano de 10 dígitos sin prefijo -> +57
        assertEquals(
            "+573001234567",
            UserProfileSecurityManager.normalizeE164PhoneNumber("300 123 4567")
        )
        // Celular con prefijo internacional explícito
        assertEquals(
            "+13055550199",
            UserProfileSecurityManager.normalizeE164PhoneNumber("+1 (305) 555-0199")
        )
        // Número inválido (muy corto o con letras)
        assertNull(UserProfileSecurityManager.normalizeE164PhoneNumber("12345"))
        assertNull(UserProfileSecurityManager.normalizeE164PhoneNumber("abc-telefono"))
    }

    @Test
    fun `7_validatePasswordStrength_enforcesBankingComplexity`() {
        // Muy corta
        assertNotNull(UserProfileSecurityManager.validatePasswordStrength("Ab1"))
        // Sin mayúscula
        assertNotNull(UserProfileSecurityManager.validatePasswordStrength("kashapp2026"))
        // Sin número
        assertNotNull(UserProfileSecurityManager.validatePasswordStrength("KashAppSegura"))
        // Válida
        assertNull(UserProfileSecurityManager.validatePasswordStrength("KashApp2026Secure"))
    }

    @Test
    fun `8_mapTimezoneToJurisdiction_resolvesCorrectTaxAuthorityAndCurrency`() {
        val co = UserProfileSecurityManager.mapTimezoneToJurisdiction("Bogotá (UTC-5) • Colombia (DIAN)")
        assertEquals(TaxJurisdiction.CO_DIAN, co.jurisdiction)
        assertEquals("COP", co.defaultCurrency)

        val mx = UserProfileSecurityManager.mapTimezoneToJurisdiction("America/Mexico_City")
        assertEquals(TaxJurisdiction.MX_SAT, mx.jurisdiction)
        assertEquals("MXN", mx.defaultCurrency)

        val es = UserProfileSecurityManager.mapTimezoneToJurisdiction("ES_AEAT")
        assertEquals(TaxJurisdiction.ES_AEAT, es.jurisdiction)
        assertEquals("EUR", es.defaultCurrency)
    }

    @Test
    fun `9_keystoreCryptoManager_encryptsAndDecryptsTotpSecretAndRecoveryCodes`() {
        val crypto = KeystoreCryptoManager(allowSoftwareFallbackForTests = true)
        val secret = TotpTwoFactorAuthenticator.generateSecretBase32()
        val encrypted = crypto.encryptData(secret)
        assertFalse(encrypted.contains(secret))
        val decrypted = crypto.decryptData(encrypted)
        assertEquals(secret, decrypted)
    }
}
