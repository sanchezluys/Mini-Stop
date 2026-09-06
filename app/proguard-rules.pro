# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# Keep data models and Room entities for JSON serialization and local persistence
-keep class com.example.model.** { *; }
-keep class com.example.data.local.** { *; }

# Keep Network packet types and enums for socket JSON communication
-keep class com.example.network.** { *; }
-keepclassmembers enum com.example.network.PacketType { *; }

# Coil image loading library
-keep class coil.** { *; }
