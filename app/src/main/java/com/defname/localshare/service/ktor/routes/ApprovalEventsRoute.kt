// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.SessionState
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch

fun Route.getApprovalEvents(
    securityHandler: ServerSecurityHandler
) {
    get("/{token}/approval-events/{sessionId}") {
        val token = call.parameters["token"]
            ?: return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)

        if (!securityHandler.checkToken(token)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val sessionId = call.parameters["sessionId"]
            ?: return@get call.respondText("Missing session.", status = HttpStatusCode.BadRequest)

        val session = securityHandler.getSession(sessionId)
            ?: return@get call.respondText("Session not found.", status = HttpStatusCode.NotFound)

        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Private))
        call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
        call.response.header(HttpHeaders.Connection, "keep-alive")
        call.response.header("X-Accel-Buffering", "no")

        call.respondOutputStream {
            val writer = bufferedWriter()

            // If already decided, send state immediately and close
            if (session.state != SessionState.PENDING) {
                writer.writeEvent("status", session.state.name.lowercase())
                return@respondOutputStream
            }

            try {
                coroutineScope {
                    // Heartbeat to keep connection alive
                    launch {
                        while (true) {
                            delay(15_000)
                            writer.writeHeartbeat()
                        }
                    }

                    // Wait for this session's state to change
                    launch {
                        securityHandler.sessionEvents
                            .filter { it.first == sessionId }
                            .collect { (_, newState) ->
                                writer.writeEvent("status", newState.name.lowercase())
                                this@coroutineScope.cancel("Session resolved")
                            }
                    }
                }
            } catch (e: Exception) {
                // Client disconnected or session resolved — normal shutdown
            }
        }
    }
}
