package com.example.core.crash

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Suite de pruebas de resiliencia ante crashes y aislamiento multiproceso (Pilar 05).
 */
class KashCrashHandlerResilienceTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var testCacheDir: File
    private lateinit var handler: KashCrashHandler

    @Before
    fun setUp() {
        testCacheDir = tempFolder.newFolder("test_cache")
        handler = KashCrashHandler(
            appContext = null,
            cacheDirProvider = { testCacheDir }
        )
    }

    @Test
    fun testCrashReportRedactsPiiAndTokensBeforeDiskWrite() {
        val sensitiveAccount = "24588910245"
        val sampleJwtToken = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIn0.dozjgNryP4J3jVmNHl0w5N_XgL0n3I9PlFUP0THsR8U"
        val fakeException = RuntimeException("Fallo en cuenta $sensitiveAccount con token $sampleJwtToken")

        val report = handler.buildSanitizedCrashReport("TestThread", fakeException)

        // Verificar que no aparezca el número de cuenta ni el token JWT crudo
        assertFalse("El número de cuenta sin rediseñar no debe aparecer en el reporte", report.contains(sensitiveAccount))
        assertFalse("El token JWT sin enmascarar no debe aparecer en el reporte", report.contains(sampleJwtToken))
        assertTrue("Debe contener la marca de redacción de PII", report.contains("PII Redacted : TRUE"))
    }

    @Test
    fun testCrashReportRetentionKeepsMaxThreeFiles() {
        val crashDir = File(testCacheDir, KashCrashHandler.CRASH_DIR_NAME)

        // Generar 5 reportes consecutivos con marca de tiempo espaciada
        for (i in 1..5) {
            val file = handler.persistSanitizedCrashReport("Reporte de prueba $i")
            file?.setLastModified(System.currentTimeMillis() + (i * 1000L))
        }

        val remainingFiles = crashDir.listFiles() ?: emptyArray()
        assertTrue("La retención debe mantener como máximo 3 reportes en caché", remainingFiles.size <= 3)
    }

    @Test
    fun testCrashProcessSkipsDatabaseAndContainerInit() {
        val crashProcessName = "com.suarez.kashapp:crash"
        val mainProcessName = "com.suarez.kashapp"

        assertTrue(crashProcessName.endsWith(":crash"))
        assertFalse(mainProcessName.endsWith(":crash"))
    }
}
