package com.defname.localshare.service.ktor.responses

import android.content.Context
import android.util.Log
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytesWriter
import io.ktor.utils.io.CancellationException
import io.ktor.utils.io.jvm.javaio.toOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream


suspend fun ApplicationCall.sendZip(
    files: List<FileInfo>,
    securityHandler: ServerSecurityHandler,
    context: Context,
    filename: String = "files.zip"
) {
    if (files.isEmpty()) {
        respond(HttpStatusCode.NotFound, "No files available for download")
        return
    }

    response.header(
        HttpHeaders.ContentDisposition,
        "attachment; filename=\"$filename\""
    )

    respondBytesWriter(
        contentType = ContentType.Application.Zip
    ) {
        try {
            val zip = ZipOutputStream(this.toOutputStream())

            for (fileInfo in files) {
                if (!securityHandler.mayContinueDownload(this@sendZip.request)) {
                    val clientIp = this@sendZip.request.local.remoteHost
                    Log.d("FileServerService", "Stopping ZIP: Client $clientIp was banned.")
                    break // Schleife abbrechen
                }
                try {
                    val uri = fileInfo.uri
                    val inputStream = context.contentResolver.openInputStream(uri) ?: continue

                    val entryName = fileInfo.name

                    zip.putNextEntry(ZipEntry(entryName))

                    inputStream.use { input ->
                        // 2. Manueller Buffer-Loop statt copyTo
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            // 3. Check während des Kopierens der aktuellen Datei
                            if (!securityHandler.mayContinueDownload(this@sendZip.request)) {
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
        }
    }
}