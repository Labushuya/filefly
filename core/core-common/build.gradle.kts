// :core-common — reines Kotlin/JVM (SemVer, Result-Typen, Konstanten).
// Keine Android-Deps -> schnellste Tests.
plugins {
    alias(libs.plugins.filefly.jvm.library)
}

dependencies {
    implementation(libs.kotlinx.coroutines.android)
}
