package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.responses.sendFile
import com.defname.localshare.service.ktor.responses.sendFileListing
import com.defname.localshare.service.ktor.responses.sendZip
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
    get("/{token}/{action}/{file?}") {

        val action = call.parameters["action"]
        val requestedFile = call.parameters["file"]
        val clientIp = call.request.local.remoteHost

        //  1. validate
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        if (action != "download" && action != "stream") {
            return@get call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        if (fileList.isEmpty()) {
            return@get call.respondText("No File Selected\n", status = HttpStatusCode.NotFound)
        }

        //  2. single file or zip

        //  2.1
        if (requestedFile != null || fileList.size == 1) {
            val fileInfo = if (requestedFile != null) fileList.find { it.id == requestedFile } else fileList[0]
            if (fileInfo == null) {
                return@get call.respondText("File not found.", status = HttpStatusCode.NotFound)
            }

            return@get call.sendFile(
                fileInfo = fileInfo,
                isStream = action == "stream",
                securityHandler = securityHandler,
                onFileNotAvailable = { fileInfo -> serviceRepository.removeFile(fileInfo.uri) },
                context = context
            )
        }

        // 2.2 multiple files
        if (action == "download") {
            return@get call.sendZip(
                files = fileList,
                securityHandler = securityHandler,
                context = context
            )
        }
        else {
            val token = call.parameters["token"]!!
            return@get call.sendFileListing(
                files = fileList,
                token = token,
                context = context
            )
        }
    }
}
