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

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep SkySharedPref and SharedPrefStructure classes from being obfuscated
-keep class com.skylake.skytv.jgorunner.data.SkySharedPref { *; }
-keep class com.skylake.skytv.jgorunner.data.SkySharedPref$SharedPrefStructure { *; }

# Keep properties in SharedPrefStructure that are annotated with SharedPrefKey
-keepclassmembers class com.skylake.skytv.jgorunner.data.SkySharedPref$SharedPrefStructure {
    @com.skylake.skytv.jgorunner.data.SkySharedPref$SharedPrefKey <fields>;
}

# Keep the SharedPrefKey annotation
-keep @interface com.skylake.skytv.jgorunner.data.SkySharedPref$SharedPrefKey


# Keep the no-args constructor of the deserialized class
-keepclassmembers class com.skylake.skytv.jgorunner.ui.tvhome.Channel {
  <init>(...);
}

-keepclassmembers class com.skylake.skytv.jgorunner.ui.tvhome.ChannelResponse {
  <init>(...);
}

-keepclassmembers class com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram {
  <init>(...);
}

-keepclassmembers class com.skylake.skytv.jgorunner.ui.tvhome.EpgResponse {
  <init>(...);
}

-keepclassmembers class com.skylake.skytv.jgorunner.activities.ChannelInfo {
  <init>(...);
}

-keep class com.skylake.skytv.jgorunner.ui.tvhome.Channel
-keep class com.skylake.skytv.jgorunner.ui.tvhome.ChannelResponse
-keep class com.skylake.skytv.jgorunner.ui.tvhome.EpgProgram
-keep class com.skylake.skytv.jgorunner.ui.tvhome.EpgResponse
-keep class com.skylake.skytv.jgorunner.activities.ChannelInfo


# Keep Kotlin metadata for reflection
-keep class kotlin.Metadata { *; }

# Keep runtime annotations and property signatures
-keepattributes Signature,RuntimeVisibleAnnotations,RuntimeVisibleParameterAnnotations,RuntimeInvisibleAnnotations,RuntimeInvisibleParameterAnnotations,EnclosingMethod,InnerClasses,AnnotationDefault,SourceFile,LineNumberTable,LocalVariableTable,LocalVariableTypeTable

# Keep all fields in SharedPrefStructure (for reflection)
-keepclassmembers class com.skylake.skytv.jgorunner.data.SkySharedPref$SharedPrefStructure {
    <fields>;
}


-keep class com.skylake.skytv.jgorunner.data.** { *; }





