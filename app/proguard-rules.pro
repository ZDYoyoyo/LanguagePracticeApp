# kotlinx.serialization：保留 @Serializable 類別的 serializer
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keepclassmembers @kotlinx.serialization.Serializable class ** {
    *** Companion;
    kotlinx.serialization.KSerializer serializer(...);
}
-keepclasseswithmembers class **$$serializer { *; }

# Credential Manager（Google 登入）：官方建議保留 play-services 實作，避免 release 版找不到 provider
-if class androidx.credentials.CredentialManager
-keep class androidx.credentials.playservices.** { *; }
