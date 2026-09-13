// :core-network — HTTP-Client (OkHttp + kotlinx.serialization) für den filefly-server.
// Chunked Upload, Auth (JWT), Datei-Operationen. Kernlogik JVM-testbar (MockWebServer).
plugins {
    alias(libs.plugins.filefly.android.library)
    alias(libs.plugins.kotlin.serialization)
}

dependencies {
    implementation(project(":core:core-common"))

    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.mockwebserver)
    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
