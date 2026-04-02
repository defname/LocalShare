package com.defname.sendfile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.toBitmap
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
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondBytesWriter
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.RoutingCall
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
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
import kotlinx.html.img
import kotlinx.html.link
import kotlinx.html.span
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class FileServerService : Service() {
    var server: EmbeddedServer<*,*>? = null

    override fun onCreate() {
        super.onCreate()

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
        // 1. MIME Type
        val fileUri = fileInfo.uri
        val fileName = fileInfo.name

        val mimeTypeString = fileInfo.mimeType
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
        token: String
    ) {
        return call.respondHtml {
            head {
                title { +"File Listing" }
                link (rel = "icon", type = "image/png", href = "/$token/favicon")
                style {
                    unsafe {
                        +"""
                        body { font-family: sans-serif; padding: 20px; }
                        button { margin-left: 10px; }
                
                        .container {
                          display: flex;
                          gap: 10px;
                          flex-wrap: wrap;
                        }
                
                        .file {
                          flex: none;
                          background-color: #f0f0f0;
                          padding: 20px;
                          border: 1px solid #ccc;
                          text-align: center;
                          box-sizing: border-box;
                          width: 250px;
                          height: 250px;
                          min-width: 0;
                
                          display: flex;
                          flex-direction: column;
                          gap: 10px;
                        }
                
                        .file img {
                          width: 100%;
                          object-fit: cover;  // contain
                          flex-grow: 1;
                          flex-shrink: 1;
                          min-height: 0;
                          border-radius: 16px;
                          max-height: 132px;
                          display: block;
                          margin: auto;
                        }
                        .file img.icon {
                          object-fit: contain;
                          max-height: 100px;
                        }
                
                        .image-wrapper {
                          flex-grow: 1;      /* Nimmt den gesamten Platz über dem Text ein */
                          display: flex;
                          align-items: center;    /* Zentriert vertikal */
                          justify-content: center; /* Zentriert horizontal */
                          overflow: hidden;
                          margin-bottom: 10px;
                        }
                
                        .file span.filename {
                          margin-top: auto;
                          font-weight: bold;
                        }
                
                        .file span {
                          display: block;
                          font-size: 0.8rem;
                          width: 100%;
                          white-space: nowrap;
                          overflow: hidden;
                          text-overflow: ellipsis;
                          flex-grow: 0;
                          flex-shrink: 0;
                        }
                        """.trimIndent()
                    }
                }
            }

            body {
                h1 { +"File Listing" }
                a(href = "/$token/download") {
                    button { +"Download All (as ZIP)" }
                }
                div ("container") {
                    files.forEach { file ->
                        val fileId = file.id.encodeURLPathPart()
                        div("file") {
                            div("image-wrapper") {
                                val hasThumbnail = file.filePreview != null
                                val imgSrc = if (file.filePreview != null) "/$token/thumbnail/$fileId"
                                    else "/$token/icon/${file.iconFile.encodeURLPathPart()}"
                                img(
                                    src = imgSrc,
                                    classes = if (!hasThumbnail) "icon" else ""
                                )
                            }
                            span("filename") { +file.name }

                            span {
                                a(href = "/$token/download/${fileId}") {
                                    button { +"Download" }
                                }

                                a(href = "/$token/stream/${fileId}") {
                                    button { +"Open" }
                                }
                            }
                        }
                    }
                }

            }
        }
    }

    private fun verifyRequest(call: RoutingCall): Boolean {
        val state = ServerRepository.state.value
        val token = call.parameters["token"]
        val clientIp = call.request.local.remoteHost

        if (state.bannedIps.contains(clientIp)) {
            return false
        }
        if (token != state.token) {
            Log.d("FileServerService", "Invalid token: ${call.parameters["token"]}")
            return false
        }
        return true
    }

    private fun startHttpServer() {
        if (server == null) {
            server = embeddedServer(Netty, port = 8080, ServerRepository.state.value.selectedIp) {
                install(io.ktor.server.plugins.partialcontent.PartialContent)
                routing {
                    get("/{token}/favicon") {
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }
                        try {
                            // App-Icon laden und direkt als PNG in den Stream schreiben
                            val drawable = packageManager.getApplicationIcon(packageName)
                            call.respondOutputStream(ContentType.Image.PNG) {
                                // toBitmap(128, 128) sorgt für eine feste, browserfreundliche Größe
                                drawable.toBitmap(128, 128).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                    get("/{token}/thumbnail/{file}") {
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }

                        val state = ServerRepository.state.value
                        val requestedFile = call.parameters["file"]

                        val fileInfo = state.fileList.find { it.id == requestedFile }
                        if (fileInfo == null) {
                            return@get call.respondText("File not found.\n", status = io.ktor.http.HttpStatusCode.NotFound)
                        }

                        val thumbnail = fileInfo.filePreview
                        if (thumbnail == null) {
                            return@get call.respondText(
                                "No thumbnail found.\n",
                                status = io.ktor.http.HttpStatusCode.NotFound
                            )
                        }
                        val stream = java.io.ByteArrayOutputStream()
                        thumbnail.compress(android.graphics.Bitmap.CompressFormat.PNG, 100, stream)
                        call.respondBytes(stream.toByteArray(), ContentType.Image.PNG)
                    }

                    get("/{token}/icon/{icon}") {
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
                        }

                        val iconName = call.parameters["icon"]
                        try {
                            val assetStream = this@FileServerService.assets.open("fileicons/$iconName")

                            call.respondBytes(
                                contentType = ContentType.parse("image/svg+xml")
                            ) {
                                assetStream.use { it.readBytes() }
                            }
                        } catch (e: Exception) {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get("/{token}/{action}/{file?}") {
                        val state = ServerRepository.state.value

                        val token = call.parameters["token"]
                        val action = call.parameters["action"]
                        val requestedFile = call.parameters["file"]
                        val clientIp = call.request.local.remoteHost

                        //  1. validate
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }

                        if (action != "download" && action != "stream") {
                            return@get call.respondText("No Access\n", status = io.ktor.http.HttpStatusCode.Forbidden)
                        }

                        if (state.fileList.isEmpty()) {
                            return@get call.respondText("No File Selected\n", status = io.ktor.http.HttpStatusCode.NotFound)
                        }

                        //  2. single file or zip

                        //  2.1
                        if (requestedFile != null || state.fileList.size == 1) {
                            val fileInfo = if (requestedFile != null) state.fileList.find { it.id == requestedFile } else state.fileList[0]
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
                            val token = call.parameters["token"]!!
                            return@get sendFileListing(call, state.fileList, token)
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
        stopHttpServer()
        super.onDestroy()
    }
}