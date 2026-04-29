// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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