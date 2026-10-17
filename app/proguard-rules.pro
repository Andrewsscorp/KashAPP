# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

-keepattributes SourceFile,LineNumberTable
-keepattributes *Annotation*

# Room Database Rules
-keepclassmembers class * extends androidx.room.RoomDatabase {
    <init>();
}
-keep class * extends androidx.room.RoomDatabase
-keep @androidx.room.Entity class * { *; }
-keep @androidx.room.Dao interface * { *; }
-dontwarn androidx.room.paging.**

# SQLCipher
-keep class net.zetetic.database.** { *; }
-keep class net.zetetic.database.sqlcipher.** { *; }
-dontwarn net.zetetic.database.**

# Crypto
-keep class org.bouncycastle.** { *; }
-dontwarn org.bouncycastle.**

# KashApp Entities and Domain Models
-keep class com.example.data.local.entity.** { *; }
-keep class com.example.domain.model.** { *; }
-keep class com.example.presentation.dashboard.model.** { *; }

# WorkManager
-keep class androidx.work.** { *; }
-keep class * extends androidx.work.ListenableWorker {
    <init>(...);
}
