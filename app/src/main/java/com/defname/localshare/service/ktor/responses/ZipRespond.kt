// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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

    respondBytesWriter(contentType = ContentType.Application.Zip) {
        try {
            val zip = ZipOutputStream(this.toOutputStream())

            for (fileInfo in files) {
                if (!securityHandler.mayContinueDownload(this@sendZip.request)) {
                    val clientIp = this@sendZip.request.local.remoteHost
                    Log.d("FileServerService", "Stopping ZIP: Client $clientIp was banned.")
                    break
                }
                try {
                    val inputStream = context.contentResolver.openInputStream(fileInfo.uri) ?: continue
                    zip.putNextEntry(ZipEntry(fileInfo.name))

                    inputStream.use { input ->
                        val buffer = ByteArray(8192)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            if (!securityHandler.mayContinueDownload(this@sendZip.request)) {
                                // Re-throw as a real exception so the outer loop breaks
                                throw IllegalStateException("Client banned during zip streaming")
                            }
                            zip.write(buffer, 0, read)
                        }
                    }

                    zip.closeEntry()
                } catch (e: IllegalStateException) {
                    // Ban detected mid-file — stop the whole ZIP
                    Log.d("FileServerService", "ZIP aborted: ${e.message}")
                    break
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
