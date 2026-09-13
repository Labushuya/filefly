# FileFly — ProGuard/R8-Regeln (Release, isMinifyEnabled=true).

# kotlinx.serialization: @Serializable-Klassen + generierte Serializer behalten.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class de.filefly.**$$serializer { *; }
-keepclassmembers class de.filefly.** {
    *** Companion;
}
-keepclasseswithmembers class de.filefly.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**

# Room
-keep class * extends androidx.room.RoomDatabase { <init>(); }
