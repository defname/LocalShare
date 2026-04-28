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

package com.defname.localshare.service.ktor.responses

import android.content.Context
import android.util.Log
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ktor.webinterface.WebInterfaceRenderer
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

suspend fun ApplicationCall.sendFileListing(
    files: List<FileInfo>,
    token: String,
    context: Context
) {
    try {
        val renderer = WebInterfaceRenderer(context)
        val finalHtml = renderer.render(token, files)
        respondText(finalHtml, ContentType.Text.Html)

    } catch (e: Exception) {
        Log.e("FileServerService", "Error loading template", e)
        respond(HttpStatusCode.InternalServerError, "Template Error")
    }
}