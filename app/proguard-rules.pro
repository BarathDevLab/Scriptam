# Scriptam ProGuard Rules

# QuickJS native library
-keep class io.github.dokar3.quickjs.** { *; }

# Room
-keep class * extends androidx.room.RoomDatabase { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
