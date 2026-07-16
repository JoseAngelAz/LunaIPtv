# LunaIPtv Phone ProGuard/R8 Rules

# ── Room ──────────────────────────────────────────────
-keep class com.lunaiptv.core.database.entity.** { *; }
-keep class com.lunaiptv.core.database.LunaIPtvDatabase { *; }
-keep class com.lunaiptv.core.database.dao.** { *; }

# ── Koin ──────────────────────────────────────────────
-keep class org.koin.** { *; }
-keepclassmembers class * {
    @org.koin.* <fields>;
}

# ── Media3 / ExoPlayer ───────────────────────────────
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# ── OkHttp ────────────────────────────────────────────
-dontwarn okhttp3.**
-dontwarn okio.**
-keepnames class okhttp3.internal.publicsuffix.PublicSuffixDatabase

# ── Coil ──────────────────────────────────────────────
-dontwarn coil3.**

# ── DataStore ─────────────────────────────────────────
-keep class androidx.datastore.** { *; }

# ── Kotlin Coroutines ─────────────────────────────────
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepclassmembers class kotlinx.coroutines.** {
    volatile <fields>;
}

# ── Enums ─────────────────────────────────────────────
-keepclassmembers enum * {
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

# ── Parcelable ────────────────────────────────────────
-keepclassmembers class * implements android.os.Parcelable {
    public static final ** CREATOR;
}

# ── libmpv / is.your.mpv ─────────────────────────────
-keep class is.your.mpv.** { *; }
-dontwarn is.your.mpv.**

# ── Compose (keep @Composable) ───────────────────────
-dontwarn androidx.compose.**
