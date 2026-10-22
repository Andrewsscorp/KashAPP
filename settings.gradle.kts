pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
  }
}

plugins { id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0" }

dependencyResolutionManagement {
  repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
  repositories {
    google()
    mavenCentral()
  }
}

rootProject.name = "KashApp"

include(":app")
include(":core-model")
include(":core-database")
include(":core-ui")
include(":feature-activos")
include(":feature-ocr")
include(":feature-informes")
include(":feature-objetivos")
include(":feature-gastos-compartidos")
include(":feature-captura-automatica")
include(":feature-impuestos-dian")
