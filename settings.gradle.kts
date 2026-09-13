// FileFly — Modul-Includes. build-logic ist ein eigenständiger Composite-Build
// (liefert die Convention-Plugins), damit jedes Modul nur ein Plugin anwendet
// statt Build-Config zu duplizieren.

pluginManagement {
    includeBuild("build-logic")
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

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "filefly"

include(":app")

include(":core:core-common")
include(":core:core-network")
include(":core:core-data")
include(":core:core-ui")

include(":feature:feature-onboarding")
include(":feature:feature-upload")
include(":feature:feature-history")
include(":feature:feature-settings")
include(":feature:feature-updater")
