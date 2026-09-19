# ProGuard and R8 rules for FastIPTV

# General attributes
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod
-keepattributes RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations
-keepattributes SourceFile,LineNumberTable

# Kotlinx Serialization
-dontnote kotlinx.serialization.SerializationKt
-keepclassmembers class * {
    *** Companion;
}
-keepclasseswithmembers class * {
    kotlinx.serialization.KSerializer serializer(...);
}
-keepnames class kotlinx.serialization.UnknownFieldException { *; }

# FastIPTV Data DTOs - Keep all models, fields, companions, and serializers intact
-keep class com.fastiptv.data.api.dto.** { *; }
-keepclassmembers class com.fastiptv.data.api.dto.** {
    *** Companion;
    *** $serializer;
    <fields>;
    <init>(...);
}

# Room Database & SQLite
-keep class * extends androidx.room.RoomDatabase
-dontwarn androidx.room.paging.**
-keep class com.fastiptv.data.db.** { *; }
-keep class com.fastiptv.data.db.entities.** { *; }
-keep class com.fastiptv.data.db.dao.** { *; }

# Retrofit & OkHttp
-dontwarn retrofit2.**
-keep class retrofit2.** { *; }
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn okhttp3.**
-dontwarn okio.**

# Media3 ExoPlayer (reflection-based decoder & renderer loading)
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.datasource.** { *; }
-keep class androidx.media3.decoder.** { *; }
-keep class androidx.media3.extractor.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Coil 3 (image loading)
-keep class coil3.** { *; }
-dontwarn coil3.**

# Hilt / Dagger
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class * extends android.app.Application { *; }
-keep class * extends android.app.Activity { *; }
-dontwarn dagger.hilt.**

# WorkManager
-keep class * extends androidx.work.ListenableWorker {
    public <init>(android.content.Context, androidx.work.WorkerParameters);
}
-keep class androidx.work.WorkerParameters { *; }

# Compose TV & Foundation
-dontwarn androidx.tv.**
