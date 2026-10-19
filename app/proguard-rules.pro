# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-renamesourcefileattribute SourceFile
-keepattributes LineNumberTable
-keepattributes *Annotation*

# Room Database Rules (Adaptadores generados en compilación por KSP; no se preservan entidades para permitir ofuscación)
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# Crypto
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# KashApp Domain & Presentation Models
-keep class com.example.domain.model.** { *; }
-keep class com.example.presentation.dashboard.model.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    <init>(...);
}

# Despojar logs de depuración e información en binarios de producción (OWASP MASVS-CODE)
-assumenosideeffects class android.util.Log {
    public static int d(...);
    public static int v(...);
    public static int i(...);
}
