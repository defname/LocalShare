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
import io.ktor.http.encodeURLPathPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.html.respondHtml
import io.ktor.server.netty.Netty
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondFile
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.close
import io.ktor.utils.io.copyTo
import io.ktor.utils.io.jvm.javaio.toByteReadChannel
import io.ktor.utils.io.jvm.javaio.toOutputStream
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.html.a
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.h1
import kotlinx.html.head
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


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
        val fileName = fileInfo.name

        val mimeTypeString = contentResolver.getType(fileUri) ?: "application/octet-stream"
        val contentType = ContentType.parse(mimeTypeString)

        // 2. filesize
        val fileSize = fileInfo.size

        // 4. send file
        val inputStream = contentResolver.openInputStream(fileUri) ?: return call.respond(HttpStatusCode.InternalServerError)

        try {
            call.response.header( HttpHeaders.ContentDisposition, "${if (stream) "inline" else "attachment"}; filename=${fileName}" )
            call.respond(object : OutgoingContent.ReadChannelContent() {
                override val contentType = contentType
                override val contentLength = fileSize

                override fun readFrom(): ByteReadChannel {
                    return call.writer(Dispatchers.IO) {
                        // Wir öffnen den InputStream innerhalb des Writers
                        contentResolver.openInputStream(fileUri)?.use { input ->
                            ServerRepository.onClientConnected(clientIp)
                            try {
                                val buffer = ByteArray(8192) // 8KB Buffer
                                var bytesRead: Int

                                while (input.read(buffer).also { bytesRead = it } != -1) {
                                    // WICHTIG: Hier prüfen wir bei jedem Buffer-Durchgang den Ban-Status
                                    if (ServerRepository.state.value.bannedIps.contains(clientIp)) {
                                        Log.d("FileServerService", "Abort download: Client $clientIp was banned.")
                                        // Wir werfen eine Exception, um den Ktor-Channel hart zu schließen
                                        throw CancellationException("Client banned")
                                    }

                                    // Daten in den Ktor-Channel schreiben
                                    channel.writeFully(buffer, 0, bytesRead)
                                }
                            } catch (e: Exception) {
                                if (e !is CancellationException) {
                                    Log.e("FileServerService", "Transfer error", e)
                                }
                                throw e
                            } finally {
                                ServerRepository.onClientDisconnected(clientIp)
                            }
                        } ?: throw Exception("Could not open InputStream")
                    }.channel
                }
            })
        } catch (e: Exception) {
            Log.e("FileServerService", "Error during file transfer", e)
            inputStream.close()
        }
    }

    private suspend fun sendZip(
        call: ApplicationCall,
        files: List<FileInfo>,
        clientIp: String
    ) {
        val state = ServerRepository.state.value

        call.response.header(
            HttpHeaders.ContentDisposition,
            "attachment; filename=\"files.zip\""
        )

        call.respondBytesWriter(
            contentType = ContentType.Application.Zip
        ) {
            ServerRepository.onClientConnected(clientIp)

            try {
                // ZipOutputStream auf Ktor Channel
                val zip = ZipOutputStream(this.toOutputStream())

                for (fileInfo in files) {
                    if (ServerRepository.state.value.bannedIps.contains(clientIp)) {
                        Log.d("FileServerService", "Stopping ZIP: Client $clientIp was banned.")
                        break // Schleife abbrechen
                    }
                    try {
                        val uri = fileInfo.uri
                        val inputStream = contentResolver.openInputStream(uri) ?: continue

                        val entryName = fileInfo.name

                        zip.putNextEntry(ZipEntry(entryName))

                        inputStream.use { input ->
                            // 2. Manueller Buffer-Loop statt copyTo
                            val buffer = ByteArray(8192)
                            var read: Int
                            while (input.read(buffer).also { read = it } != -1) {
                                // 3. Check wÄhrend des Kopierens der aktuellen Datei
                                if (ServerRepository.state.value.bannedIps.contains(clientIp)) {
                                    throw CancellationException("Client banned during file streaming")
                                }
                                zip.write(buffer, 0, read)
                            }
                        }

                        zip.closeEntry()

                    } catch (e: Exception) {
                        Log.d("FileServerService", "Error adding file to zip: ${e.message}")
                    }
                }

                zip.finish()
                zip.flush()
                zip.close()

            } catch (e: Exception) {
                Log.e("FileServerService", "ZIP streaming error", e)
            } finally {
                ServerRepository.onClientDisconnected(clientIp)
            }
        }
    }

    private suspend fun sendFileListing(
        call: ApplicationCall,
        files: List<FileInfo>,
        clientIp: String,
        token: String
    ) {
        return call.respondHtml {
            head {
                title { "File Listing" }
                style {
                    """
                body { font-family: sans-serif; padding: 20px; }
                .file { margin-bottom: 15px; }
                button { margin-left: 10px; }
                """.trimIndent()
                }
            }
            body {
                h1 { +"File Listing" }
                files.forEach { file ->
                    val filename = file.name.encodeURLPathPart()
                    div ("file") {
                        span { +file.name }

                        a(href = "/$token/download/${filename}") {
                            button { +"Download" }
                        }

                        a(href = "/$token/stream/${filename}") {
                            button { +"Stream" }
                        }
                    }
                }
                a(href = "/$token/download") {
                    button { +"Download All" }
                }
            }
        }
    }

    private fun startHttpServer() {
        if (server == null) {
            server = embeddedServer(Netty, port = 8080) {
                install(io.ktor.server.plugins.partialcontent.PartialContent)
                routing {
                    get("/{token}/{action}/{filename?}") {
                        val state = ServerRepository.state.value

                        val token = call.parameters["token"]
                        val action = call.parameters["action"]
                        val requestedFilename = call.parameters["filename"]
                        val clientIp = call.request.local.remoteHost

                        //  1. validate
                        if (state.bannedIps.contains(clientIp)) {
                            return@get call.respondText("Your IP is banned.", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }
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
                        if (action == "download") {
                            return@get sendZip(call, state.fileList, clientIp)
                        }
                        else {
                            return@get sendFileListing(call, state.fileList, clientIp, token)
                        }
                    }
                    get("/{token}") {
                        val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest)
                        call.respondRedirect("/$token/stream")
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