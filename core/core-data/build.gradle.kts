// :core-data — Persistenz (DataStore für Settings/Server-Config, Room für Upload-Historie).
plugins {
    alias(libs.plugins.filefly.android.library)
    alias(libs.plugins.ksp)
}

// Room-Schema-Export: JSON-Schemas nach schemas/ — Grundlage für Migrations-Tests.
ksp {
    arg("room.schemaLocation", "$projectDir/schemas")
}

dependencies {
    implementation(project(":core:core-common"))
    implementation(project(":core:core-network"))

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.junit)
}
