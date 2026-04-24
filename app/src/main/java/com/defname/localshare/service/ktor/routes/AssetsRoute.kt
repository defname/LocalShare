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
