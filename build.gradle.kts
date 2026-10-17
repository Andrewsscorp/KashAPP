import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoReport

// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
  alias(libs.plugins.android.application) apply false
  alias(libs.plugins.android.library) apply false
  alias(libs.plugins.kotlin.jvm) apply false
  alias(libs.plugins.kotlin.compose) apply false
  alias(libs.plugins.google.devtools.ksp) apply false
  alias(libs.plugins.secrets) apply false
  alias(libs.plugins.google.services) apply false
}

subprojects {
  apply(plugin = "jacoco")

  extensions.configure<JacocoPluginExtension> {
    toolVersion = "0.8.13"
  }

  afterEvaluate {
    val unitTestTaskName = if (plugins.hasPlugin("org.jetbrains.kotlin.jvm")) {
      "test"
    } else {
      "testDebugUnitTest"
    }

    tasks.register<JacocoReport>("jacocoUnitTestReport") {
        dependsOn(unitTestTaskName)
        group = "verification"
        description = "Genera cobertura JaCoCo de tests unitarios para $name."

        reports {
          html.required.set(true)
          xml.required.set(true)
          csv.required.set(true)
        }

        val javaClasses = fileTree(layout.buildDirectory) {
          include(
            "classes/**/main/**/*.class",
            "tmp/kotlin-classes/**/main/**/*.class",
            "intermediates/built_in_kotlinc/**/classes/**/*.class",
            "intermediates/javac/**/classes/**/*.class",
            "intermediates/classes/**/**/*.class"
          )
          exclude(
            "**/R.class",
            "**/R$*.class",
            "**/BuildConfig.*",
            "**/Manifest*.*"
          )
        }
        classDirectories.setFrom(javaClasses)
        sourceDirectories.setFrom(files("src/main/java", "src/main/kotlin"))
        additionalSourceDirs.setFrom(files("src/main/java", "src/main/kotlin"))
        executionData.setFrom(fileTree(layout.buildDirectory) {
          include(
            "jacoco/*.exec",
            "outputs/unit_test_code_coverage/**/*.exec",
            "outputs/unit_test_code_coverage/**/*.ec"
          )
        })
    }
  }
}

tasks.register<JacocoReport>("jacocoAllUnitTestReport") {
  group = "verification"
  description = "Genera cobertura JaCoCo agregada de todos los módulos."
  jacocoClasspath = files(subprojects.map { project ->
    project.configurations.getByName("jacocoAnt")
  })

  reports {
    html.required.set(true)
    html.outputLocation.set(layout.buildDirectory.dir("reports/jacoco/jacocoAllUnitTestReport/html"))
    xml.required.set(true)
    xml.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoAllUnitTestReport/jacocoAllUnitTestReport.xml"))
    csv.required.set(true)
    csv.outputLocation.set(layout.buildDirectory.file("reports/jacoco/jacocoAllUnitTestReport/jacocoAllUnitTestReport.csv"))
  }

  classDirectories.setFrom(subprojects.map { project ->
    project.fileTree(project.layout.buildDirectory) {
      include(
        "classes/**/main/**/*.class",
        "tmp/kotlin-classes/**/main/**/*.class",
        "intermediates/built_in_kotlinc/**/classes/**/*.class",
        "intermediates/javac/**/classes/**/*.class",
        "intermediates/classes/**/**/*.class"
      )
      exclude("**/R.class", "**/R$*.class", "**/BuildConfig.*", "**/Manifest*.*")
    }
  })
  sourceDirectories.setFrom(subprojects.flatMap { project ->
    listOf(project.file("src/main/java"), project.file("src/main/kotlin"))
  })
  additionalSourceDirs.setFrom(sourceDirectories)
  executionData.setFrom(subprojects.map { project ->
    project.fileTree(project.layout.buildDirectory) {
      include(
        "jacoco/*.exec",
        "outputs/unit_test_code_coverage/**/*.exec",
        "outputs/unit_test_code_coverage/**/*.ec"
      )
    }
  })
}

gradle.projectsEvaluated {
  tasks.named<JacocoReport>("jacocoAllUnitTestReport") {
    dependsOn(subprojects.map { project ->
      project.tasks.named("jacocoUnitTestReport")
    })
  }
}
