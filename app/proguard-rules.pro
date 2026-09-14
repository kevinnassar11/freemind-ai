# Kotlin serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** { *** Companion; }
-keepclasseswithmembers class kotlinx.serialization.json.** { kotlinx.serialization.KSerializer serializer(...); }
-keep,includedescriptorclasses class com.freemind.ai.**$$serializer { *; }
-keepclassmembers class com.freemind.ai.** { *** Companion; }
-keepclasseswithmembers class com.freemind.ai.** { kotlinx.serialization.KSerializer serializer(...); }
