package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.responses.sendFileListing
import com.defname.localshare.service.ktor.responses.sendZip
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first

fun Route.getLanding(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    context: Context
) {
    get("/{token}/") {
        val download = (call.queryParameters["download"] != null)
        val requestedFile = call.parameters["file"]
        val clientIp = call.request.local.remoteHost

        //  1. validate
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        // 2.2 multiple files
        if (download) {
            return@get call.sendZip(
                files = fileList,
                securityHandler = securityHandler,
                context = context
            )
        } else {
            val token = call.parameters["token"]!!
            return@get call.sendFileListing(
                files = fileList,
                token = token,
                context = context
            )
        }
    }
}