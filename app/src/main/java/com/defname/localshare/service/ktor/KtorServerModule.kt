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

package com.defname.localshare.service.ktor

import android.content.Context
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.routes.getAssets
import com.defname.localshare.service.ktor.routes.getEvents
import com.defname.localshare.service.ktor.routes.getFavIcon
import com.defname.localshare.service.ktor.routes.getFile
import com.defname.localshare.service.ktor.routes.getFileIcon
import com.defname.localshare.service.ktor.routes.getLanding
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
import io.ktor.server.response.respondText
import io.ktor.server.routing.IgnoreTrailingSlash
import io.ktor.server.routing.get
import io.ktor.server.routing.routing

fun Application.configureServerModule(
    serviceRepository: ServiceRepository,
    connectionLogsRepository: ConnectionLogsRepository,
    settingsRepository: SettingsRepository,
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    install(PartialContent)
    install(IgnoreTrailingSlash)

    intercept(ApplicationCallPipeline.Monitoring) {
        val connectionId = connectionLogsRepository.clientConnected(
            method = call.request.httpMethod.value,
            path = call.request.uri,
            clientIp = call.request.local.remoteHost
        )

        call.attributes.put(CallAttributes.connectionId, connectionId)

        try {
            proceed()
        } finally {
            val discconnectReason: DisconnectReason = call.response.status()?.let {
                DisconnectReason.Expected(it.value)
            } ?: DisconnectReason.Unexpected.Unknown

            connectionLogsRepository.clientDisconnected(
                connectionId,
                discconnectReason
            )
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

        getAssets(securityHandler, context)

        getEvents(securityHandler, serviceRepository, settingsRepository, connectionLogsRepository, context)

        getFile(securityHandler, serviceRepository, context)

        getLanding(securityHandler, serviceRepository, context)

        get("{...}") {
            call.respondText("No Access\n", status = HttpStatusCode.Forbidden)
        }
    }
}