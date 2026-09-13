// :feature-onboarding — Server-URL eingeben + Invite-Code einlösen.
plugins {
    alias(libs.plugins.filefly.android.feature)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-ui"))
    implementation(libs.kotlinx.coroutines.android)
}
