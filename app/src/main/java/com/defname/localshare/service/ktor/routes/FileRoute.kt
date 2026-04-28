/*
 * LocalShare - Share files locally
 * Copyright (C) 2026 defname
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

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
