# kotlinx.serialization – zapis gry
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class pl.monter.core.**$$serializer { *; }
-keepclassmembers class pl.monter.core.** {
    *** Companion;
}
-keepclasseswithmembers class pl.monter.core.** {
    kotlinx.serialization.KSerializer serializer(...);
}
