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