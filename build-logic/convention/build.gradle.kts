// build-logic — eigenständiger Composite-Build, der die FileFly-Convention-Plugins
// bereitstellt. Kapselt AGP/Kotlin/Compose/Hilt-Setup, damit Module nicht duplizieren.

plugins {
    `kotlin-dsl`
}

group = "de.filefly.buildlogic"

// Toolchain auf JDK 21 (matcht CI setup-java 21).
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
}

// Registriert die Convention-Plugins unter stabilen IDs (siehe libs.versions.toml [plugins]).
gradlePlugin {
    plugins {
        register("androidApplication") {
            id = "filefly.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidLibrary") {
            id = "filefly.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidCompose") {
            id = "filefly.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidFeature") {
            id = "filefly.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
        register("jvmLibrary") {
            id = "filefly.jvm.library"
            implementationClass = "JvmLibraryConventionPlugin"
        }
        register("androidHilt") {
            id = "filefly.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
    }
}
