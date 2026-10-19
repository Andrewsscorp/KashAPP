package com.example.security

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.SecurityDatabase
import com.example.domain.security.FirebaseApiProtectionGuard
import com.example.features.settings.data.repository.SettingsRepositoryImpl
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Suite de pruebas unitarias para REM-P3-02 (Fase 10 / VER-F10-01):
 * 1. Verificación del flujo de consentimiento informado Ley 1581 de 2012 / Decreto 1377 de 2013 en SettingsRepositoryImpl.
 * 2. Registro inmutable en AuditSecurityLogDao de eventos PRIVACY_CONSENT_GRANTED y PRIVACY_CONSENT_REVOKED.
 * 3. Sincronización en tiempo de ejecución con FirebaseApiProtectionGuard.
 * 4. Bloqueo estricto de llamadas de IA en la nube ante falta de consentimiento.
 * 5. Sanitización de PII (PAN, Cédula, Teléfono, Correo) preservando importes monetarios en pesos colombianos.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class GeminiAiPiiSanitizationAndConsentTest {

    private lateinit var context: Context
    private lateinit var database: SecurityDatabase
    private lateinit var settingsRepository: SettingsRepositoryImpl
    private val testUserId = "user_consent_test_01"

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        database = Room.inMemoryDatabaseBuilder(context, SecurityDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        settingsRepository = SettingsRepositoryImpl(
            context = context,
            auditSecurityLogDao = database.auditSecurityLogDao(),
            fixedAssetDao = database.fixedAssetDao()
        )

        FirebaseApiProtectionGuard.setPrivacyConsent(false)
    }

    @After
    fun tearDown() {
        database.close()
        FirebaseApiProtectionGuard.setPrivacyConsent(false)
    }

    @Test
    fun testPrivacyPolicyConsentFlow_recordsAuditLogAndUnlocksCloudAi() = runTest {
        val auditLogDao = database.auditSecurityLogDao()

        // 1. Inicialmente no hay consentimiento aceptado
        assertFalse("El consentimiento inicial debe ser falso", settingsRepository.isPrivacyPolicyAccepted(testUserId))
        assertFalse("Guardia de IA debe reportar consentimiento inactivo", FirebaseApiProtectionGuard.isPrivacyConsentGranted())

        val blockedVerdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = "Gasto almuerzo $ 35.000",
            requireInformedConsent = true
        )
        assertTrue("Debe bloquearse la llamada sin consentimiento Ley 1581", blockedVerdict is FirebaseApiProtectionGuard.GuardVerdict.Blocked)
        assertEquals("CONSENT_REQUIRED_LEY_1581", (blockedVerdict as FirebaseApiProtectionGuard.GuardVerdict.Blocked).reasonCode)

        // 2. Otorgar consentimiento informado según Ley 1581 de 2012
        val consentVersion = "1.0-LEY-1581-2012"
        val grantResult = settingsRepository.setPrivacyPolicyConsent(testUserId, true, consentVersion)
        assertTrue("El guardado del consentimiento debe ser exitoso", grantResult.isSuccess)

        // 3. Verificar estado en repositorio y en el Guardián de IA
        assertTrue("El repositorio debe reflejar consentimiento aceptado", settingsRepository.isPrivacyPolicyAccepted(testUserId))
        assertTrue("El Guardián de IA debe estar habilitado", FirebaseApiProtectionGuard.isPrivacyConsentGranted())
        assertEquals(consentVersion, FirebaseApiProtectionGuard.getPrivacyConsentVersion())

        // 4. Verificar inserción en audit_security_logs
        val logs = auditLogDao.getAllLogsList()
        val grantLog = logs.find { it.eventType == "PRIVACY_CONSENT_GRANTED" }
        assertNotNull("Debe existir log de PRIVACY_CONSENT_GRANTED", grantLog)
        assertEquals("LEY_1581_CONSENT", grantLog!!.authMethod)
        assertTrue("El detalle debe registrar la versión", grantLog.details.contains(consentVersion))
        assertTrue("El detalle debe registrar OTORGADA", grantLog.details.contains("OTORGADA"))

        // 5. Verificar que con consentimiento otorgado la llamada a la IA es permitida y sanitizada
        val rawPromptWithPii = """
            Factura Restaurante Crepes
            Cédula: 1020304050
            Tarjeta Visa 4532 1234 5678 9012
            Total: $ 125.000 COP
            Contacto: cliente@empresa.com Tel: 300 123 4567
        """.trimIndent()

        val allowedVerdict = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = rawPromptWithPii,
            requireInformedConsent = true
        )

        assertTrue("Llamada con consentimiento otorgado debe ser permitida", allowedVerdict is FirebaseApiProtectionGuard.GuardVerdict.Allowed)
        val allowed = allowedVerdict as FirebaseApiProtectionGuard.GuardVerdict.Allowed

        // Verificación de sanitización
        assertTrue("Debe contener [REDACTED_PAN]", allowed.sanitizedPrompt.contains("[REDACTED_PAN]"))
        assertFalse("No debe exponer la tarjeta", allowed.sanitizedPrompt.contains("4532 1234 5678 9012"))

        assertTrue("Debe contener [REDACTED_ID]", allowed.sanitizedPrompt.contains("[REDACTED_ID]"))
        assertFalse("No debe exponer la cédula", allowed.sanitizedPrompt.contains("1020304050"))

        assertTrue("Debe contener [REDACTED_EMAIL]", allowed.sanitizedPrompt.contains("[REDACTED_EMAIL]"))
        assertFalse("No debe exponer el email", allowed.sanitizedPrompt.contains("cliente@empresa.com"))

        assertTrue("Debe contener [REDACTED_PHONE]", allowed.sanitizedPrompt.contains("[REDACTED_PHONE]"))
        assertFalse("No debe exponer el teléfono", allowed.sanitizedPrompt.contains("300 123 4567"))

        // Verificación de preservación del monto en pesos
        assertTrue("Debe preservar el importe $ 125.000 COP", allowed.sanitizedPrompt.contains("$ 125.000 COP"))

        // 6. Revocar consentimiento y verificar bloqueo inmediato
        val revokeResult = settingsRepository.setPrivacyPolicyConsent(testUserId, false, consentVersion)
        assertTrue("Revocación debe ser exitosa", revokeResult.isSuccess)
        assertFalse("El repositorio debe reflejar consentimiento revocado", settingsRepository.isPrivacyPolicyAccepted(testUserId))
        assertFalse("El Guardián debe bloquear de nuevo", FirebaseApiProtectionGuard.isPrivacyConsentGranted())

        val postRevokeLogs = auditLogDao.getAllLogsList()
        val revokeLog = postRevokeLogs.find { it.eventType == "PRIVACY_CONSENT_REVOKED" }
        assertNotNull("Debe existir log de PRIVACY_CONSENT_REVOKED", revokeLog)
        assertTrue(revokeLog!!.details.contains("REVOCADA"))

        val blockedAfterRevocation = FirebaseApiProtectionGuard.authorizeAndSanitizeRequest(
            rawPrompt = "Nuevo gasto $ 50.000",
            requireInformedConsent = true
        )
        assertTrue("Llamada tras revocación debe ser bloqueada", blockedAfterRevocation is FirebaseApiProtectionGuard.GuardVerdict.Blocked)
        assertEquals("CONSENT_REQUIRED_LEY_1581", (blockedAfterRevocation as FirebaseApiProtectionGuard.GuardVerdict.Blocked).reasonCode)
    }
}
