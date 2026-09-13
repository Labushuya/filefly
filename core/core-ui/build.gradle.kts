// :core-ui — geteiltes Material-3-Theme (dynamic color, dark mode), Design-Tokens,
// wiederverwendbare Compose-Komponenten.
plugins {
    alias(libs.plugins.filefly.android.library)
    alias(libs.plugins.filefly.android.compose)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.coil.compose)
}
