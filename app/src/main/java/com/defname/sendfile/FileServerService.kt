package com.defname.sendfile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import java.io.File


class FileServerService : Service() {
    var server: EmbeddedServer<*,*>? = null
    private lateinit var connectivityManager: ConnectivityManager

    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onAvailable(network: Network) {
            ServerRepository.updateLocalIpAddresses()
        }

        override fun onLost(network: Network) {
            ServerRepository.updateLocalIpAddresses()
        }

        override fun onCapabilitiesChanged(
            network: Network,
            networkCapabilities: NetworkCapabilities
        ) {
            ServerRepository.updateLocalIpAddresses()
        }

    }

    override fun onCreate() {
        super.onCreate()
        connectivityManager = getSystemService(ConnectivityManager::class.java)
        connectivityManager.registerDefaultNetworkCallback(networkCallback)

        createNotificationChannel()
        ServerRepository.updateLocalIpAddresses()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "my_channel_id",
                "File Transfer Service",
                NotificationManager.IMPORTANCE_HIGH
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == "START_SERVER") {
            val stopIntent = Intent(this, FileServerService::class.java).apply {
                action = "STOP_SERVER"
            }

            val deletePendingIntent = android.app.PendingIntent.getService(
                this,
                1,
                stopIntent,
                android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val state = ServerRepository.state.value

            // Notification bauen
            val notification = NotificationCompat.Builder(this, "my_channel_id")
                .setContentTitle("Sharing Files...")
                .setContentText("Server is running.")
                .setSmallIcon(R.drawable.ic_launcher_foreground) // Teste mal ein Standard-Icon
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setOngoing(true) // Verhindert das Wegwischen durch den User
                .setDeleteIntent(deletePendingIntent)
                .addAction(
                    android.R.drawable.ic_menu_close_clear_cancel,
                    "Stop Server",
                    deletePendingIntent
                )
                .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
                .build()

            startForeground(1, notification)

            startHttpServer()
        } else if (intent?.action == "STOP_SERVER") {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
        return START_NOT_STICKY
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        stopSelf()
    }


    override fun onBind(intent: Intent?): IBinder? = null

    private suspend fun sendFile(call: ApplicationCall, fileInfo: FileInfo, clientIp: String, stream: Boolean) {
        val state = ServerRepository.state.value

        // 1. MIME Type
        val fileUri = fileInfo.uri

        val mimeTypeString = contentResolver.getType(fileUri) ?: "application/octet-stream"
        val contentType = ContentType.parse(mimeTypeString)

        // 2. filesize
        val fileSize = fileInfo.size

        // 3. client ip
        if (state.bannedIps.contains(clientIp)) {
            return call.respondText("Your IP is banned.", status = HttpStatusCode.Forbidden)
        }

        // 4. send file
        Log.d("FileServerService", "$fileUri \n ${fileUri.path}")

        val file = File(fileUri.path!!)
        return call.respondFile(file)

        // 4. send file
        val inputStream = contentResolver.openInputStream(fileUri) ?: return call.respond(HttpStatusCode.InternalServerError)

        try {
            // respondBytesWriter ist ideal für Streams und arbeitet nahtlos mit dem PartialContent-Plugin
            call.respondBytesWriter (
                contentType = contentType,
                contentLength = fileInfo.size
            ) {
                inputStream.use { input ->
                    try {
                        ServerRepository.onClientConnected(clientIp)
                        // Stream effizient in den Netzwerk-Channel kopieren
                        input.toByteReadChannel(context = Dispatchers.IO).copyTo(this)
                    } catch (e: Exception) {
                        Log.d("FileServerService", "Transfer to $clientIp interrupted: ${e.message}")
                    } finally {
                        ServerRepository.onClientDisconnected(clientIp)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("FileServerService", "Error during file transfer", e)
            inputStream.close()
        }
    }

    private suspend fun sendZip(call: ApplicationCall, files: List<FileInfo>, clientIp: String) {

    }

    private fun startHttpServer() {
        if (server == null) {
            server = embeddedServer(Netty, port = 8080) {
                install(io.ktor.server.plugins.partialcontent.PartialContent)
                routing {
                    val state = ServerRepository.state.value

                    get("/{token}/{action}/{filename?}") {
                        val token = call.parameters["token"]
                        val action = call.parameters["action"]
                        val requestedFilename = call.parameters["filename"]
                        val clientIp = call.request.local.remoteHost

                        //  1. validate
                        if (token != ServerRepository.getToken()) {
                            Log.d("FileServerService", "Invalid token: ${call.parameters["token"]}")
                            return@get call.respondText("No Access\n", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }

                        if (action != "download" && action != "stream") {
                            return@get call.respondText("No Access\n", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }

                        if (state.fileList.isEmpty()) {
                            return@get call.respondText("No File Selected\n", status = io.ktor.http.HttpStatusCode.NotFound)
                        }

                        //  2. single file or zip

                        //  2.1
                        if (requestedFilename != null || state.fileList.size == 1) {
                            val fileInfo = if (requestedFilename != null) state.fileList.find { it.name == requestedFilename } else state.fileList[0]
                            if (fileInfo == null) {
                                return@get call.respondText("File not found.", status = io.ktor.http.HttpStatusCode.NotFound)
                            }

                            return@get sendFile(call, fileInfo, clientIp, action == "stream")
                        }

                        // 2.2 multiple files
                        return@get call.respondText("Multiple files not supported yet.", status = io.ktor.http.HttpStatusCode.NotImplemented)

                    }
                    get("{...}") {
                        call.respondText("No Access\n", status = io.ktor.http.HttpStatusCode.Forbidden)
                    }
                }
            }.start(wait = false)
            ServerRepository.onServerStarted()
        }
    }

    private fun stopHttpServer() {
        server?.stop(1000, 2000)
        server = null
        ServerRepository.onServerStopped()
    }

    override fun onDestroy() {
        connectivityManager.unregisterNetworkCallback(networkCallback)
        stopHttpServer()
        super.onDestroy()
    }
}