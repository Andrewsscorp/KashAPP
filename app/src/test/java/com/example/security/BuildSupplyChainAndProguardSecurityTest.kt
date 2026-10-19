package com.example.security

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

@RunWith(RobolectricTestRunner::class)
@Config(manifest = Config.NONE)
class BuildSupplyChainAndProguardSecurityTest {

    private fun resolveProjectFile(relativePath: String): File {
        return listOf(
            File(relativePath),
            File("..", relativePath),
            File("../..", relativePath)
        ).firstOrNull { it.exists() } ?: File(relativePath)
    }

    @Test
    fun testCoreUiManifest_isolatesComponentActivityToDebug() {
        val mainManifest = resolveProjectFile("core-ui/src/main/AndroidManifest.xml")
        assertTrue("core-ui main manifest must exist", mainManifest.exists())
        val mainContent = mainManifest.readText(Charsets.UTF_8)
        assertFalse("ComponentActivity must NOT be present in main manifest", mainContent.contains("ComponentActivity"))

        val debugManifest = resolveProjectFile("core-ui/src/debug/AndroidManifest.xml")
        assertTrue("core-ui debug manifest must exist", debugManifest.exists())
        val debugContent = debugManifest.readText(Charsets.UTF_8)
        assertTrue("ComponentActivity must be declared in debug manifest for previews/testing", debugContent.contains("ComponentActivity"))
    }

    @Test
    fun testProguardRules_hardenedAgainstReverseEngineering() {
        val proguardFile = resolveProjectFile("app/proguard-rules.pro")
        assertTrue("proguard-rules.pro must exist", proguardFile.exists())
        val content = proguardFile.readText(Charsets.UTF_8)

        // 1. SourceFile attribute renaming
        assertTrue("Must rename SourceFile attribute", content.contains("-renamesourcefileattribute SourceFile"))

        // 2. No reflection rules keeping local Room entities
        assertFalse("Must NOT keep all entity classes without obfuscation", content.contains("-keep class com.example.data.local.entity.**"))
        assertFalse("Must NOT keep @Entity classes across the board", content.contains("-keep @androidx.room.Entity"))

        // 3. Strip Log.d/v in production
        assertTrue("Must strip Log.d/v in release builds", content.contains("-assumenosideeffects class android.util.Log"))
    }

    @Test
    fun testSigningConfigAndGitignore_protectsKeystoreFiles() {
        val buildGradle = resolveProjectFile("app/build.gradle.kts")
        assertTrue("app/build.gradle.kts must exist", buildGradle.exists())
        val buildContent = buildGradle.readText(Charsets.UTF_8)
        assertFalse("Hardcoded my-upload-key.jks fallback must be removed", buildContent.contains("my-upload-key.jks"))

        val gitignore = resolveProjectFile(".gitignore")
        assertTrue(".gitignore must exist", gitignore.exists())
        val gitignoreContent = gitignore.readText(Charsets.UTF_8)
        assertTrue(".gitignore must ignore *.jks", gitignoreContent.contains("*.jks"))
        assertTrue(".gitignore must ignore *.keystore", gitignoreContent.contains("*.keystore"))
    }

    @Test
    fun testGradleWrapper_hasDistributionSha256Sum() {
        val wrapperProps = resolveProjectFile("gradle/wrapper/gradle-wrapper.properties")
        assertTrue("gradle-wrapper.properties must exist", wrapperProps.exists())
        val content = wrapperProps.readText(Charsets.UTF_8)
        assertTrue("Must specify distributionSha256Sum for supply chain integrity", content.contains("distributionSha256Sum="))
        assertTrue("Must match official Gradle 9.3.1 SHA-256", content.contains("b266d5ff6b90eada6dc3b20cb090e3731302e553a27c5d3e4df1f0d76beaff06"))
    }

    @Test
    fun testDependenciesVersion_okhttpUpdatedTo4120() {
        val tomlFile = resolveProjectFile("gradle/libs.versions.toml")
        assertTrue("libs.versions.toml must exist", tomlFile.exists())
        val content = tomlFile.readText(Charsets.UTF_8)
        assertTrue("okhttp must be updated to 4.12.0", content.contains("okhttp = \"4.12.0\""))
        assertTrue("loggingInterceptor must be updated to 4.12.0", content.contains("loggingInterceptor = \"4.12.0\""))
    }
}
