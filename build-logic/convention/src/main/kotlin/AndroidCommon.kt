import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

// Gemeinsame Android-SDK/Compile-Optionen. Zentral, damit min/target/compileSdk
// an EINER Stelle stehen (compileSdk/targetSdk=36, minSdk=26).
internal object FileflyConfig {
    const val COMPILE_SDK = 36
    const val TARGET_SDK = 36
    const val MIN_SDK = 26
    const val NAMESPACE_PREFIX = "de.filefly"
    val JAVA_VERSION = JavaVersion.VERSION_21
}

internal fun Project.configureAndroidCommon(
    commonExtension: CommonExtension<*, *, *, *, *, *>,
) {
    commonExtension.apply {
        compileSdk = FileflyConfig.COMPILE_SDK

        defaultConfig {
            minSdk = FileflyConfig.MIN_SDK
        }

        compileOptions {
            sourceCompatibility = FileflyConfig.JAVA_VERSION
            targetCompatibility = FileflyConfig.JAVA_VERSION
        }
    }

    // Kotlin JVM-Target 21 (matcht Java-Toolchain + CI).
    extensions.configure(KotlinAndroidProjectExtension::class.java) {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_21)
        }
    }
}
