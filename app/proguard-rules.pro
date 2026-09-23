-keep class com.gymcompanion.app.data.model.** { *; }
-keepclassmembers class * {
    @androidx.room.* <methods>;
}
-keep class com.gymcompanion.app.viewmodel.** { *; }
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**
-dontwarn okio.**
