# Add project specific ProGuard rules here.
-keepattributes *Annotation*
-keepattributes Signature
-keepattributes SourceFile,LineNumberTable

# GeckoView
-keep class org.mozilla.geckoview.** { *; }
-dontwarn org.mozilla.geckoview.**

# Mozilla Components
-keep class org.mozilla.components.** { *; }
-dontwarn org.mozilla.components.**

# Room
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**

# Moshi
-keep class com.squareup.moshi.** { *; }
-keepclasseswithmembers class * {
    @com.squareup.moshi.* <fields>;
}

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }

# ML Kit
-keep class com.google.mlkit.** { *; }
-keep class com.google.android.gms.** { *; }

# Tink
-keep class com.google.crypto.tink.** { *; }

# Chu Browser models
-keep class com.chubrowser.app.core.** { *; }
-keep class com.chubrowser.app.password.** { *; }
-keep class com.chubrowser.app.bookmark.** { *; }
-keep class com.chubrowser.app.download.** { *; }
