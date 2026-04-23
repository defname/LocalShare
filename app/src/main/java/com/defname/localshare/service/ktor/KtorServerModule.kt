package com.defname.localshare.service.ktor

import android.content.Context
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.LogEntry
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.routes.getEvents
import com.defname.localshare.service.ktor.routes.getFavIcon
import com.defname.localshare.service.ktor.routes.getFile
import com.defname.localshare.service.ktor.routes.getFileIcon
import com.defname.localshare.service.ktor.routes.getThumbnail
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.plugins.partialcontent.PartialContent
import io.ktor.server.request.httpMethod
import io.ktor.server.request.uri
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureServerModule(
    serviceRepository: ServiceRepository,
    logsRepository: LogsRepository,
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    install(PartialContent)

    intercept(ApplicationCallPipeline.Monitoring) {
        val entry = LogEntry(
            method = call.request.httpMethod.value,
            path = call.request.uri,
            clientIp = call.request.local.remoteHost
        )
        val logEntryId = entry.id
        logsRepository.addLog(entry)
        val connectionId = serviceRepository.clientConnected(
            ip = call.request.local.remoteHost,
            requestedUri = call.request.uri
        )

        call.attributes.put(CallAttributes.connectionId, connectionId)

        try {
            proceed()
        } finally {
            logsRepository.updateLogStatus(
                logEntryId,
                call.response.status()?.value ?: -1
            )
            serviceRepository.clientDisconnected(connectionId)
        }

    }

    // 2. Routing definieren
    routing {
        get("/favicon.ico") {
            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            return@get call.respond(HttpStatusCode.NoContent)
        }

        getFavIcon(securityHandler, context)

        getThumbnail(securityHandler, serviceRepository, context)

        getFileIcon(securityHandler, context)

        getEvents(securityHandler, serviceRepository, context)

        getFile(securityHandler, serviceRepository, context)

        get("/{token}") {
            val token = call.parameters["token"] ?: return@get call.respond(HttpStatusCode.BadRequest)
            call.respondRedirect("/$token/stream")
        }
        get("{...}") {
            call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
        }
    }
}