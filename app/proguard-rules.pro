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

# Ktor / Netty / Coroutines spezifische Regeln
-dontwarn io.netty.**
-dontwarn io.ktor.**
-dontwarn kotlinx.coroutines.debug.**

# Ignoriere fehlende Java-Management & Logging Klassen (die es auf Android nicht gibt)
-dontwarn java.lang.management.**
-dontwarn javax.management.**
-dontwarn org.apache.log4j.**
-dontwarn org.apache.logging.log4j.**
-dontwarn org.slf4j.**
-dontwarn reactor.blockhound.**
-dontwarn jdk.jfr.**

# Falls du R8 erlauben willst, fehlende Klassen komplett zu ignorieren:
# -ignorewarnings
