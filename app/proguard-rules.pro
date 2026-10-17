# Keep Google Mobile Ads SDK classes (AdMob uses reflection internally).
-keep class com.google.android.gms.ads.** { *; }
-keep class com.google.ads.** { *; }

# Keep the JS bridge methods that the WebView calls by name via @JavascriptInterface.
-keepclassmembers class com.davis.offerwall.web.OfferwallBridge {
    public *;
}

# Kotlin metadata (safe default)
-keep class kotlin.Metadata { *; }
