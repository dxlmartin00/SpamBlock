# SpamBlock ProGuard / R8 Rules

# Keep application components defined in AndroidManifest.xml
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Application
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver
-keep public class * extends android.content.ContentProvider
-keep public class * extends android.telecom.CallScreeningService

# Keep data models and receivers
-keep class com.spamblock.data.** { *; }
-keep class com.spamblock.util.** { *; }
-keep class com.spamblock.receiver.** { *; }

# Compose rules
-keepclassmembers class * {
    @androidx.compose.runtime.Composable *;
}

# Coroutines & reflection optimization
-dontwarn kotlinx.coroutines.**
