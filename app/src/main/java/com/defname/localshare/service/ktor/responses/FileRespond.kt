package com.defname.localshare.service.ktor.responses

import android.content.Context
import android.util.Log
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.content.OutgoingContent
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.utils.io.ByteReadChannel
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.writeFully
import io.ktor.utils.io.writer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun ApplicationCall.sendFile(
    fileInfo: FileInfo,
    isStream: Boolean,
    securityHandler: ServerSecurityHandler,
    onFileNotAvailable: (FileInfo) -> Unit,
    context: Context
) {
    val fileUri = fileInfo.uri
    val fileName = fileInfo.name

    val mimeTypeString = fileInfo.mimeType
    val contentType = ContentType.parse(mimeTypeString)

    // 2. filesize
    val fileSize = fileInfo.size

    // 4. send file
    val inputStream = try {
        context.contentResolver.openInputStream(fileUri)
    } catch (e: Exception) {
        null
    }

    if (inputStream == null) {
        Log.d("FileServerService", "File not accessible anymore: $fileUri")
        onFileNotAvailable(fileInfo)
        return respond(HttpStatusCode.Gone, "File no longer available")
    }

    try {
        response.header( HttpHeaders.ContentDisposition, "${if (isStream) "inline" else "attachment"}; filename=${fileName}" )
        respond(object : OutgoingContent.ReadChannelContent() {
            override val contentType = contentType
            override val contentLength = fileSize

            override fun readFrom(): ByteReadChannel {
                return writer(Dispatchers.IO) {
                    // Wir öffnen den InputStream innerhalb des Writers
                    inputStream.use { input ->
                        try {
                            val buffer = ByteArray(8192) // 8KB Buffer
                            var bytesRead: Int

                            while (input.read(buffer).also { bytesRead = it } != -1) {
                                // WICHTIG: Hier prüfen wir bei jedem Buffer-Durchgang den Ban-Status
                                if (!securityHandler.mayContinueDownload(this@sendFile.request)) {
                                    val clientIp = this@sendFile.request.local.remoteHost
                                    Log.d("FileServerService", "Abort download: Client $clientIp was banned.")
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
                        }
                    } ?: throw Exception("Could not open InputStream")
                }.channel
            }
        })
    } catch (e: Exception) {
        Log.e("FileServerService", "Error during file transfer", e)
        withContext(Dispatchers.IO) {
            inputStream.close()
        }
    }
}
