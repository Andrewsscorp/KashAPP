package com.example.security

import android.content.Context
import android.view.WindowManager
import androidx.test.core.app.ApplicationProvider
import com.example.MainActivity
import com.example.util.SharedExportSecurityUtil
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Suite de Pruebas de Seguridad para REM-P1-04 (HALL-07 / GATE-07).
 * Valida:
 * 1. Activación mandatoria de FLAG_SECURE en MainActivity contra capturas y previews.
 * 2. Restricción estricta de FileProvider a 'shared_exports/' (erradicación de path=".").
 * 3. Corrección de autoridad FileProvider (${applicationId}.provider vs .fileprovider).
 * 4. Aislamiento y purga automática de archivos temporales en cacheDir/shared_exports.
 */
@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class FileProviderAndFlagSecureSecurityTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        SharedExportSecurityUtil.purgeAllExports(context)
    }

    @Test
    fun mainActivity_enforcesFlagSecure_toPreventScreenCapturesAndLeakage() {
        // Ejecutamos el ciclo de vida de MainActivity en Robolectric
        val controller = Robolectric.buildActivity(MainActivity::class.java)
        val activity = controller.create().get()

        val windowFlags = activity.window.attributes.flags
        val hasFlagSecure = (windowFlags and WindowManager.LayoutParams.FLAG_SECURE) != 0

        assertTrue(
            "MainActivity MUST have FLAG_SECURE enabled to prevent screen captures and recent task previews",
            hasFlagSecure
        )
    }

    @Test
    fun filePathsXml_restrictsCachePathToSharedExports_andEliminatesRootCacheExposure() {
        // Buscamos el archivo físico res/xml/file_paths.xml
        val possibleLocations = listOf(
            File("src/main/res/xml/file_paths.xml"),
            File("app/src/main/res/xml/file_paths.xml"),
            File("../app/src/main/res/xml/file_paths.xml")
        )
        val filePathsXml = possibleLocations.firstOrNull { it.exists() }
        assertNotNull("file_paths.xml must exist in the project repository", filePathsXml)

        val xmlContent = filePathsXml!!.readText()

        // 1. Debe contener la ruta restringida shared_exports/
        assertTrue(
            "file_paths.xml MUST declare cache-path name='shared_exports' path='shared_exports/'",
            xmlContent.contains("""<cache-path name="shared_exports" path="shared_exports/" />""")
        )

        // 2. NO debe exponer la raíz completa de cacheDir con path="."
        assertFalse(
            "VULNERABILITY: file_paths.xml MUST NOT expose the entire cache root with path='.'",
            xmlContent.contains("""path="."""")
        )

        // 3. No debe haber ningún path="." en toda la definición
        assertFalse(
            "VULNERABILITY: No path in file_paths.xml should be '.'",
            xmlContent.contains("""path="."""")
        )
    }

    @Test
    fun sharedExportSecurityUtil_purgesStaleExports_retainingRecentFiles() {
        val exportsDir = SharedExportSecurityUtil.getExportsDir(context)
        assertTrue(exportsDir.exists())
        assertEquals("shared_exports", exportsDir.name)

        // Creamos un archivo viejo (modificado hace 10 minutos)
        val oldFile = File(exportsDir, "ticket_old_export.png").apply {
            writeText("dummy thermal ticket")
            setLastModified(System.currentTimeMillis() - (10 * 60 * 1000L))
        }

        // Creamos un archivo reciente (modificado hace 30 segundos)
        val recentFile = File(exportsDir, "ticket_recent_export.pdf").apply {
            writeText("dummy pdf ticket")
            setLastModified(System.currentTimeMillis() - 30_000L)
        }

        assertTrue(oldFile.exists())
        assertTrue(recentFile.exists())

        // Purgamos con umbral de 5 minutos
        SharedExportSecurityUtil.purgeStaleExports(context, maxAgeMs = 5 * 60 * 1000L)

        // El archivo viejo debe haber sido eliminado
        assertFalse("Old exported ticket (> 5 min) must be purged automatically", oldFile.exists())

        // El archivo reciente debe mantenerse para permitir que la app receptora lo lea
        assertTrue("Recent exported ticket (< 5 min) must be retained during active share", recentFile.exists())
    }

    @Test
    fun sharedExportSecurityUtil_purgeAllExports_eradicatesAllArtifactsOnAppExit() {
        val exportsDir = SharedExportSecurityUtil.getExportsDir(context)
        File(exportsDir, "file1.pdf").writeText("content1")
        File(exportsDir, "file2.csv").writeText("content2")
        File(exportsDir, "file3.json").writeText("content3")

        assertEquals(3, exportsDir.listFiles()?.size ?: 0)

        // Purga total (como en onDestroy)
        SharedExportSecurityUtil.purgeAllExports(context)

        assertEquals(0, exportsDir.listFiles()?.size ?: 0)
    }

    @Test
    fun noExporterInCodebase_usesInvalidFileproviderAuthority() {
        // Verificamos que no exista ningún uso residual de .fileprovider
        val sourceDirs = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("../feature-gastos-compartidos/src/main/java"),
            File("feature-gastos-compartidos/src/main/java")
        )

        var foundInvalidAuthority = false
        var invalidLocation = ""

        sourceDirs.filter { it.exists() }.forEach { dir ->
            dir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                val content = file.readText()
                if (content.contains(".fileprovider\"")) {
                    foundInvalidAuthority = true
                    invalidLocation = file.absolutePath
                }
            }
        }

        assertFalse(
            "Found invalid '.fileprovider' authority in $invalidLocation. All components MUST use '.provider'",
            foundInvalidAuthority
        )
    }
}
