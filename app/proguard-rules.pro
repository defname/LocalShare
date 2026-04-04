# Netty Regeln
-keep class io.netty.** { *; }
-dontwarn io.netty.**

# Ktor Regeln
-keep class io.ktor.** { *; }
-dontwarn io.ktor.**

# Da der Fehler spezifisch bei den Channels auftritt:
-keepnames class io.netty.channel.socket.nio.NioServerSocketChannel
-keepnames class io.netty.channel.socket.nio.NioSocketChannel

# Falls du Kotlin Coroutines nutzt (was Ktor tut):
-keepnames class kotlinx.coroutines.internal.MainDispatcherFactory {}
-keepnames class kotlinx.coroutines.CoroutineExceptionHandler {}
-keepnames class kotlinx.coroutines.android.AndroidExceptionPreHandler {}
-keepnames class kotlinx.coroutines.android.AndroidDispatcherFactory {}