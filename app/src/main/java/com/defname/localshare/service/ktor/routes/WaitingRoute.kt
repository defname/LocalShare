// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.request.cookies
import io.ktor.server.response.respondRedirect
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.getWaiting(
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    get("/{token}/waiting") {
        val token = call.parameters["token"]
            ?: return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)

        if (!securityHandler.checkToken(token)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val sessionId = call.request.cookies()["ls_session"]
            ?: return@get call.respondRedirect("/$token/")

        try {
            val template = context.assets.open("web/waiting.html").bufferedReader().use { it.readText() }
            val html = template
                .replace("{{token}}", token)
                .replace("{{sessionId}}", sessionId)
            call.respondText(html, ContentType.Text.Html)
        } catch (e: Exception) {
            call.respondText(
                "<html><body><p>Waiting for approval...</p></body></html>",
                ContentType.Text.Html
            )
        }
    }
}
