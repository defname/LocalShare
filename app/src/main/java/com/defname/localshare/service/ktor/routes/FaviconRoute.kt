// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.graphics.Bitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import com.defname.localshare.R
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.header
import io.ktor.server.response.respond
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get

fun Route.getFavIcon(
    securityHandler: ServerSecurityHandler,
    context: Context
) {
    get("/{token}/favicon/{size?}") {
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }
        val size = when (call.parameters["size"]) {
            "medium" -> 64
            "large" -> 128
            "huge" -> 512
            else -> 32
        }
        try {
            val drawable = ContextCompat.getDrawable(context, R.mipmap.ic_launcher)
            if (drawable == null) {
                return@get call.respond(HttpStatusCode.NotFound)
            }
            call.response.header(HttpHeaders.CacheControl, "public, max-age=31536000, immutable")
            call.respondOutputStream(ContentType.Image.PNG) {
                // toBitmap(128, 128) sorgt für eine feste, browserfreundliche Größe
                drawable.toBitmap(size, size).compress(Bitmap.CompressFormat.PNG, 100, this)
            }
        } catch (_: Exception) {
            call.respond(HttpStatusCode.InternalServerError)
        }
    }
}