// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.graphics.Bitmap
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream

fun Route.getThumbnail(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    fileInfoProvider: FileInfoProvider,
    context: Context
) {
    get("/{token}/thumbnail/{file}") {
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        val requestedFile = call.parameters["file"]

        val fileInfo = fileList.find { it.id == requestedFile }
        if (fileInfo == null) {
            return@get call.respondText("File not found.\n", status = HttpStatusCode.NotFound)
        }

        val thumbnail: Bitmap? = fileInfoProvider.getThumbnail(fileInfo.uri)

        if (thumbnail == null) {
            val iconFilename = fileInfo.iconFile
            return@get try {
                context.assets.open("fileicons/$iconFilename").use { inputStream ->
                    val bytes = inputStream.readBytes()
                    val contentType = if (iconFilename.endsWith(".svg")) {
                        ContentType.parse("image/svg+xml")
                    } else {
                        ContentType.Image.PNG
                    }
                    call.respondBytes(bytes, contentType)
                }
            } catch (e: Exception) {
                call.respondText(
                    "No thumbnail or icon found.\n",
                    status = HttpStatusCode.NotFound
                )
            }
        }
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream)
        call.respondBytes(stream.toByteArray(), ContentType.Image.PNG)
    }
}