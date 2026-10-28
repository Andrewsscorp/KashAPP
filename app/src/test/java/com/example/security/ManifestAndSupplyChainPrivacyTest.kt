package com.example.security

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Suite de verificación estática de Manifiesto y Supply Chain (OWASP MASVS-PRIVACY).
 * Valida la remoción del Advertising ID (AD_ID), el proceso aislado de crash y la exclusión de metadatos de dependencias.
 */
class ManifestAndSupplyChainPrivacyTest {

    @Test
    fun testAdIdPermissionExplicitlyRemovedInManifest() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        assertTrue("AndroidManifest.xml debe existir", manifestFile.exists())

        val content = manifestFile.readText()
        assertTrue(
            "AndroidManifest.xml debe contener la remoción explícita del permiso AD_ID",
            content.contains("com.google.android.gms.permission.AD_ID") && content.contains("tools:node=\"remove\"")
        )
    }

    @Test
    fun testCrashRecoveryActivityRunsInIsolatedProcess() {
        val manifestFile = File("src/main/AndroidManifest.xml")
        val content = manifestFile.readText()

        assertTrue(
            "CrashRecoveryActivity debe ejecutarse en el proceso aislado :crash",
            content.contains("CrashRecoveryActivity") && content.contains("android:process=\":crash\"")
        )
    }

    @Test
    fun testBundleDependenciesInfoDisabledInBuildGradle() {
        val buildGradleFile = File("build.gradle.kts")
        assertTrue("app/build.gradle.kts debe existir", buildGradleFile.exists())

        val content = buildGradleFile.readText()
        assertTrue(
            "dependenciesInfo debe tener includeInBundle = false",
            content.contains("includeInBundle = false")
        )
    }
}
