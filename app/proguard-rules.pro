# Methods annotated with @JavascriptInterface are called by name from JavaScript,
# so R8 must not rename or remove them.
-keepattributes JavascriptInterface
-keepclassmembers class io.github.hanxmoneai.sunmibridge.bridge.PosBridge {
    @android.webkit.JavascriptInterface <methods>;
}

# Sunmi printer SDK talks to the system print service through reflection/AIDL.
-keep class com.sunmi.printerx.** { *; }
-dontwarn com.sunmi.printerx.**
