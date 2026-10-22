# kotlinx.serialization: згенеровані серіалізатори моделей даних
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keep,includedescriptorclasses class ua.x44to.varta.**$$serializer { *; }
-keepclassmembers class ua.x44to.varta.** { *** Companion; }
-keepclasseswithmembers class ua.x44to.varta.** { kotlinx.serialization.KSerializer serializer(...); }
