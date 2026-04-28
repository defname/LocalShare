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
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.getFileIcon(
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    get("/{token}/icon/{icon}") {
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val iconName = call.parameters["icon"]
        try {
            val assetStream = context.assets.open("fileicons/$iconName")

            call.respondBytes(
                contentType = ContentType.parse("image/svg+xml")
            ) {
                assetStream.use { it.readBytes() }
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}