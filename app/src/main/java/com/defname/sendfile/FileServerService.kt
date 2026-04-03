package com.defname.sendfile

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.http.encodeURLPathPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


class FileServerService : Service() {
    private var server: EmbeddedServer<*,*>? = null
    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var idleTimeoutJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null


    override fun onCreate() {
        super.onCreate()

        createNotificationChannel()

        serviceScope.launch {
            ServerRepository.state.collect { state ->
                updateWakeLock(state.keepScreenOn, state.isRunning)

                if (state.isRunning) {

                    if (state.activeClients.isEmpty()) {
                        resetIdleTimer()
                    } else {
                        idleTimeoutJob?.cancel()
                        idleTimeoutJob = null
                    }
                }
            }
        }
    }

    private fun resetIdleTimer() {
        idleTimeoutJob?.cancel()
        val state = ServerRepository.state.value
        val timeoutMillis = state.idleTimeoutSeconds * 1000L

        if (timeoutMillis <= 0L || !state.isRunning) {
            return
        }

        idleTimeoutJob = serviceScope.launch {
            delay(timeoutMillis)
            if (ServerRepository.state.value.activeClients.isEmpty()) {
                Log.d("FileServerService", "Stopping server due to idle timeout")
                stopHttpServer()
                stopSelf()
            }
        }
    }

    private fun updateWakeLock(keepScreenOn: Boolean, isRunning: Boolean) {
        if (keepScreenOn && isRunning) {
            if (wakeLock == null) {
                val powerManager = getSystemService(POWER_SERVICE) as PowerManager
                // SCREEN_BRIGHT_WAKE_LOCK hält das Display an
                // ACQUIRE_CAUSES_WAKEUP schaltet es ggf. sogar ein
                wakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK or PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "SendFile::KeepScreenOn"
                )
            }
            if (wakeLock?.isHeld == false) {
                wakeLock?.acquire()
                Log.d("FileServerService", "WakeLock acquired: Display forced ON")
            }
        } else {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
                Log.d("FileServerService", "WakeLock released")
            }
            wakeLock = null
        }
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
        when (intent?.action) {
            "START_SERVER" -> {
                val stopIntent = Intent(this, FileServerService::class.java).apply {
                    action = "STOP_SERVER"
                }

                val deletePendingIntent = PendingIntent.getService(
                    this,
                    1,
                    stopIntent,
                    PendingIntent.FLAG_IMMUTABLE
                )

                val contentIntent = Intent(this, MainActivity::class.java).apply {
                    // Stellt sicher, dass die App in den Vordergrund kommt, ohne neu zu starten
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
                }

                val contentPendingIntent = PendingIntent.getActivity(
                    this,
                    0,
                    contentIntent,
                    PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                )

                // Notification bauen
                val notification = NotificationCompat.Builder(this, "my_channel_id")
                    .setContentTitle("Sharing Files...")
                    .setContentText("Server is running.")
                    .setSmallIcon(R.drawable.ic_launcher_foreground) // Teste mal ein Standard-Icon
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setOngoing(true) // Verhindert das Wegwischen durch den User
                    .setContentIntent(contentPendingIntent)
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
            }
            "STOP_SERVER" -> {
                stopForeground(STOP_FOREGROUND_REMOVE)
                stopSelf()
            }
            "APPROVE_IP" -> {
                val ip = intent.getStringExtra("client_ip") ?: ""
                ServerRepository.approveRequest(ip, true)
                cancelApprovalNotification(ip)
            }
            "DENY_IP" -> {
                val ip = intent.getStringExtra("client_ip") ?: ""
                ServerRepository.approveRequest(ip, false)
                cancelApprovalNotification(ip)
            }
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
                                    if (ServerRepository.state.value.blacklist.contains(clientIp)) {
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
                    if (ServerRepository.state.value.blacklist.contains(clientIp)) {
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
                                if (ServerRepository.state.value.blacklist.contains(clientIp)) {
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
        try {
            // 1. Template aus Assets laden
            val template = assets.open("listing.html").bufferedReader().use { it.readText() }

            // 2. Den dynamischen Teil für die Dateien bauen (HTML-Schnipsel)
            val fileEntriesHtml = StringBuilder()
            val fileItems = mutableListOf<String>()
            files.forEach { file ->
                val fileId = file.id.encodeURLPathPart()
                val hasThumbnail = file.filePreview != null
                val imgSrc = if (hasThumbnail) "/$token/thumbnail/$fileId"
                else "/$token/icon/${file.iconFile.encodeURLPathPart()}"
                val imgClass = if (!hasThumbnail) "icon" else ""

                fileEntriesHtml.append("""
                <div class="file">
                    <div class="image-wrapper">
                        <img src="$imgSrc" class="$imgClass" />
                    </div>
                    <span class="filename">${file.name}</span>
                    <span>
                        <a href="/$token/download/$fileId"><button>Download</button></a>
                        <a href="/$token/stream/$fileId"><button>Open</button></a>
                    </span>
                </div>
                """.trimIndent())
                fileItems += ("""
                {
                    fileId: "$fileId",
                    filename: "${file.name}",
                    hasThumbnail: $hasThumbnail,
                    icon: "${file.iconFile}",
                    size: ${file.size},
                    mimeType: "${file.mimeType}"
                }    
                """.trimIndent())
            }

            // 3. Platzhalter im Template ersetzen
            val finalHtml = template
                .replace("{{token}}", token)
                .replace("{{noscript}}", fileEntriesHtml.toString())
                .replace("{{appname}}", applicationInfo.loadLabel(packageManager).toString())
                .replace("{{appurl}}", getString(R.string.app_url))
                .replace("{{filelist}}", "[" + fileItems.joinToString(",\n") + "]")


            // 4. Antwort senden
            call.respondText(finalHtml, ContentType.Text.Html)

        } catch (e: Exception) {
            Log.e("FileServerService", "Error loading template", e)
            call.respond(HttpStatusCode.InternalServerError, "Template Error")
        }
    }

    private fun showApprovalNotification(clientIp: String) {
        val approveIntent = Intent(this, FileServerService::class.java).apply {
            action = "APPROVE_IP"
            putExtra("client_ip", clientIp)
        }
        val approvePendingIntent = PendingIntent.getService(
            this,
            clientIp.hashCode(),
            approveIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val denyIntent = Intent(
            this,
            FileServerService::class.java
        ).apply {
            action = "DENY_IP"
            putExtra("client_ip", clientIp)
        }
        val denyPendingIntent = PendingIntent.getService(
            this,
            clientIp.hashCode() + 1,
            denyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(this, "my_channel_id")
            .setContentTitle("Verbindungsanfrage")
            .setContentText("Gerät $clientIp möchte Dateien laden.")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .addAction(android.R.drawable.ic_menu_add, "Erlauben", approvePendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Blockieren", denyPendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(1001 + clientIp.hashCode(), notification)
    }

    private suspend fun verifyRequest(call: RoutingCall): Boolean {
        val state = ServerRepository.state.value
        val token = call.parameters["token"]
        val clientIp = call.request.local.remoteHost

        if (ServerRepository.isBlacklisted(clientIp)) {
            return false
        }
        if (token != state.token) {
            Log.d("FileServerService", "Invalid token: ${call.parameters["token"]}")
            return false
        }
        if (ServerRepository.isWhitelisted(clientIp)) {
            return true
        }
        if (!state.requireApproval) {
            return true
        }
        return waitForApproval(clientIp)
    }

    private fun cancelApprovalNotification(clientIp: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        // WICHTIG: Die ID muss exakt mit der in showApprovalNotification übereinstimmen (1001 + hashCode)
        nm.cancel(1001 + clientIp.hashCode())
    }

    private suspend fun waitForApproval(clientIp: String): Boolean {
        if (ServerRepository.isWhitelisted(clientIp)) return true

        val deferred = ServerRepository.askForPermission(clientIp)

        // Benachrichtigung mit "Zulassen" / "Ablehnen" senden
        showApprovalNotification(clientIp)

        return try {
            // Timeout nach 30 Sekunden, falls der User nicht reagiert
            withTimeout(30000) {
                deferred.await()
            }
        } catch (_: Exception) {
            false // Timeout führt zur Ablehnung
        } finally {
            cancelApprovalNotification(clientIp)
        }
    }

    private fun startHttpServer() {
        if (server == null) {
            server = embeddedServer(Netty, port = ServerRepository.state.value.port, ServerRepository.state.value.selectedIp) {
                install(io.ktor.server.plugins.partialcontent.PartialContent)

                intercept(ApplicationCallPipeline.Monitoring) {
                    proceed()
                    val entry = LogEntry(
                        method = call.request.httpMethod.value,
                        path = call.request.uri,
                        status = call.response.status()?.value ?: 0,
                        clientIp = call.request.local.remoteHost
                    )
                    ServerRepository.addLog(entry)
                }

                routing {
                    get("/favicon.ico") {
                        call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
                        return@get call.respond(HttpStatusCode.NoContent)
                    }
                    get("/{token}/favicon/{size?}") {
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
                        }
                        val size = when (call.parameters["size"]) {
                            "medium" -> 64
                            "large" -> 128
                            "huge" -> 512
                            else -> 32
                        }
                        try {
                            // App-Icon laden und direkt als PNG in den Stream schreiben
                            val drawable = ContextCompat.getDrawable(this@FileServerService, R.mipmap.ic_launcher)
                            if (drawable == null) {
                                return@get call.respond(HttpStatusCode.NotFound)
                            }
                            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
                            call.respondOutputStream(ContentType.Image.PNG) {
                                // toBitmap(128, 128) sorgt für eine feste, browserfreundliche Größe
                                drawable.toBitmap(size, size).compress(android.graphics.Bitmap.CompressFormat.PNG, 100, this)
                            }
                        } catch (_: Exception) {
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                    get("/{token}/thumbnail/{file}") {
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
                        }

                        val state = ServerRepository.state.value
                        val requestedFile = call.parameters["file"]

                        val fileInfo = state.fileList.find { it.id == requestedFile }
                        if (fileInfo == null) {
                            return@get call.respondText("File not found.\n", status = HttpStatusCode.NotFound)
                        }

                        val thumbnail = fileInfo.filePreview
                        if (thumbnail == null) {
                            return@get call.respondText(
                                "No thumbnail found.\n",
                                status = HttpStatusCode.NotFound
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
                        } catch (_: Exception) {
                            call.respond(HttpStatusCode.NotFound)
                        }
                    }

                    get("/{token}/{action}/{file?}") {
                        val state = ServerRepository.state.value

                        val action = call.parameters["action"]
                        val requestedFile = call.parameters["file"]
                        val clientIp = call.request.local.remoteHost

                        //  1. validate
                        if (!verifyRequest(call)) {
                            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
                        }

                        if (action != "download" && action != "stream") {
                            return@get call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
                        }

                        if (state.fileList.isEmpty()) {
                            return@get call.respondText("No File Selected\n", status = HttpStatusCode.NotFound)
                        }

                        //  2. single file or zip

                        //  2.1
                        if (requestedFile != null || state.fileList.size == 1) {
                            val fileInfo = if (requestedFile != null) state.fileList.find { it.id == requestedFile } else state.fileList[0]
                            if (fileInfo == null) {
                                return@get call.respondText("File not found.", status = HttpStatusCode.NotFound)
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
                        call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
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
        serviceScope.cancel()
        stopHttpServer()
        super.onDestroy()
    }
}