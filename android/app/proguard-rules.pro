# Proguard configuration for Tower Defense RPG
# Protect WebKit JavaScript Interface bridge methods
-keepattributes JavascriptInterface
-keepclassmembers class * {
    @android.webkit.JavascriptInterface <methods>;
}

-keepclassmembers class com.rpg.towerdefense.MainActivity$WebAppInterface {
    public *;
}

# Keep AppCompat and Material Components
-keep class androidx.appcompat.** { *; }
-keep interface androidx.appcompat.** { *; }
-dontwarn androidx.appcompat.**
