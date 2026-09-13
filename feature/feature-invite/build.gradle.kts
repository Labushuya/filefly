// :feature-invite — Admin: Invites erstellen (Code/QR/Deep-Link) + Liste/Revoke.
plugins {
    alias(libs.plugins.filefly.android.feature)
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))
    implementation(project(":core:core-data"))
    implementation(project(":core:core-ui"))
    implementation(libs.kotlinx.coroutines.android)

    // QR-Code erzeugen (reines Encoding, kein Play-Services).
    implementation(libs.zxing.core)
}
