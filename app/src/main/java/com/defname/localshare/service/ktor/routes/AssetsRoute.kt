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
import io.ktor.http.defaultForFilePath
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.getAssets(
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    get("/{token}/static/{path...}") {
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val path = call.parameters.getAll("path")?.joinToString("/")
        if (path.isNullOrBlank()) {
            return@get call.respondText("No Path.", status = HttpStatusCode.NotFound)
        }

        try {
            val assetPath = "web/$path"
            val inputStream = context.assets.open(assetPath)
            val contentType = ContentType.defaultForFilePath(path)

            call.respondOutputStream(contentType, HttpStatusCode.OK) {
                inputStream.copyTo(this)
            }
        } catch (e: Exception) {
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
