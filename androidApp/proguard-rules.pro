# kotlinx.serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.mafia.**$$serializer { *; }
-keepclassmembers class com.mafia.** {
    *** Companion;
}
-keepclasseswithmembers class com.mafia.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# SLF4J / Logback (server-side logging pulled in transitively — not used on Android)
-dontwarn org.slf4j.**
-dontwarn ch.qos.logback.**

# Ktor
-keep class io.ktor.** { *; }
-keep class kotlinx.coroutines.** { *; }
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.**

# Koin
-keep class org.koin.** { *; }
-dontwarn org.koin.**

# Keep model classes used in serialization
-keep class com.mafia.shared.model.** { *; }
-keep class com.mafia.shared.network.messages.** { *; }
