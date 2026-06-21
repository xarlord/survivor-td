# Add project specific ProGuard rules here.

# Fleks ECS
-keep class com.github.quillraven.fleks.** { *; }

# Kotlinx Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# dyn4j
-keep class org.dyn4j.** { *; }
