package com.example

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * TASK-025 (Fase 5 Prevención Continua - Guardián Arquitectónico):
 * Test estático que valida el cumplimiento estricto de las directrices
 * de diseño adaptativo y responsividad de KashApp (ADR-0008).
 *
 * Protege permanentemente el repositorio contra:
 * 1. OutlinedTextField con altura fija menor a 56dp (recorte de letras descendentes).
 * 2. Botones compactos con altura rígida menor a 40dp sin elasticidad.
 * 3. Banderas de navegación de nivel superior con remember en lugar de rememberSaveable.
 * 4. Adopción de CurrencyText en pantallas financieras core.
 */
class ArchitectureResponsivenessGuardTest {

    private fun getProjectRoot(): File {
        val currentDir = File(".").canonicalFile
        return if (currentDir.name == "app") currentDir.parentFile ?: currentDir else currentDir
    }

    private fun getAllSourceFiles(): List<File> {
        val root = getProjectRoot()
        val sourceDirs = listOf(
            File(root, "app/src/main/java"),
            File(root, "core-ui/src/main/java"),
            File(root, "feature-activos/src/main/java"),
            File(root, "feature-gastos-compartidos/src/main/java"),
            File(root, "feature-informes/src/main/java"),
            File(root, "feature-objetivos/src/main/java"),
            File(root, "feature-ocr/src/main/java")
        )
        return sourceDirs.filter { it.exists() }.flatMap { dir ->
            dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
        }
    }

    @Test
    fun guard_noOutlinedTextFieldWithRigidHeightBelow56dp() {
        val files = getAllSourceFiles()
        val violations = mutableListOf<String>()

        val regex = Regex("""OutlinedTextField[\s\S]{0,150}?\.height\(\s*([1-4][0-9]|5[0-5])\.dp\s*\)""")

        for (file in files) {
            val content = file.readText()
            if (content.contains("OutlinedTextField") && content.contains(".height(")) {
                val matches = regex.findAll(content)
                for (match in matches) {
                    violations.add("${file.name}: ${match.value.take(60)}")
                }
            }
        }

        assertTrue(
            "Se detectaron OutlinedTextField con .height(< 56.dp) que provocan recorte de fuentes: \n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun guard_noButtonsWithRigidHeightBelow40dp() {
        val files = getAllSourceFiles()
        val violations = mutableListOf<String>()

        val rigidHeightRegex = Regex("""\b(Button|OutlinedButton)[\s\S]{0,150}?Modifier[\s\S]{0,150}?\.height\(\s*([1-3][0-9])\.dp\s*\)""")

        for (file in files) {
            val content = file.readText()
            if ((content.contains("Button(") || content.contains("OutlinedButton(")) && content.contains(".height(")) {
                val matches = rigidHeightRegex.findAll(content)
                for (match in matches) {
                    violations.add("${file.name}: ${match.value.take(60)}")
                }
            }
        }

        assertTrue(
            "Se detectaron botones con .height(< 40.dp) rígido sin usar KashCompactButton o heightIn(min = ...): \n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun guard_dashboardNavigationUsesRememberSaveable() {
        val root = getProjectRoot()
        val dashboardFile = File(root, "app/src/main/java/com/example/presentation/dashboard/DashboardScreen.kt")
        assertTrue("DashboardScreen.kt debe existir", dashboardFile.exists())

        val content = dashboardFile.readText()
        val violations = mutableListOf<String>()

        val navFlagRegex = Regex("""var\s+(show[A-Z][a-zA-Z0-9]+)\s+by\s+remember\s*\{""")
        val matches = navFlagRegex.findAll(content)
        for (match in matches) {
            violations.add("Variable ${match.groupValues[1]} usa 'remember' en lugar de 'rememberSaveable'")
        }

        assertTrue(
            "Las banderas de navegación de nivel superior deben persistir con rememberSaveable: \n${violations.joinToString("\n")}",
            violations.isEmpty()
        )
    }

    @Test
    fun guard_currencyTextUsedInFinancialModules() {
        val root = getProjectRoot()
        val budgetsFile = File(root, "app/src/main/java/com/example/presentation/budget/BudgetsScreen.kt")
        assertTrue(budgetsFile.exists())
        val content = budgetsFile.readText()
        assertTrue(
            "BudgetsScreen debe utilizar CurrencyText para cifras monetarias",
            content.contains("CurrencyText")
        )
    }
}
