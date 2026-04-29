// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.responses.sendFile
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first

fun Route.getFile(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    context: Context
) {
    get("/{token}/file/{file}") {

        val download = (call.queryParameters["download"] != null)
        val requestedFile = call.parameters["file"]
        val clientIp = call.request.local.remoteHost

        //  1. validate
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        if (fileList.isEmpty()) {
            return@get call.respondText("No File Selected\n", status = HttpStatusCode.NotFound)
        }

        //  2. single file or zip

        //  2.1
            val fileInfo = if (requestedFile != null) fileList.find { it.id == requestedFile } else fileList[0]
        if (fileInfo == null) {
            return@get call.respondText("File not found.", status = HttpStatusCode.NotFound)
        }

        return@get call.sendFile(
            fileInfo = fileInfo,
            isStream = !download,
            securityHandler = securityHandler,
            onFileNotAvailable = { fileInfo -> serviceRepository.removeFile(fileInfo.uri) },
            context = context
        )
    }
}
