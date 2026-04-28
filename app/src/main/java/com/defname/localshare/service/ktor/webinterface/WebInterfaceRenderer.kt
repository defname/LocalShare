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

package com.defname.localshare.service.ktor.webinterface

import android.content.Context
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ktor.json.toJsonString
import io.ktor.http.encodeURLPathPart

class WebInterfaceRenderer(
    private val context: Context,
    private val templateFile: String = "web/template.html"
){

    private fun rederFileStatic(token: String, file: FileInfo): String {
        val fileId = file.id.encodeURLPathPart()
        val hasThumbnail = file.filePreview != null
        val imgSrc = if (hasThumbnail) "/$token/thumbnail/$fileId"
        else "/$token/icon/${file.iconFile.encodeURLPathPart()}"
        val imgClass = if (!hasThumbnail) "icon" else ""

        return """
            <div class="file">
                <div class="image-wrapper">
                    <img src="$imgSrc" class="$imgClass" />
                </div>
                <span class="filename">${file.name}</span>
                <span>
                    <a href="/$token/download/$fileId"><button>Download</button></a>
                    <a href="/$token/stream/$fileId"><button>Open</button></a>
                </span>
            </div>
            """.trimIndent()

    }

    fun render(token: String, files: List<FileInfo> = emptyList()): String {

        // 1. Template aus Assets laden
        val template = context.assets.open(templateFile).bufferedReader().use { it.readText() }

        // 2. Den dynamischen Teil für die Dateien bauen (HTML-Schnipsel)

        val jsonFileList = files.map { it.toJsonString() }

        // 3. Platzhalter im Template ersetzen
        val finalHtml = template
            .replace("{{token}}", token)
            .replace("{{appname}}", context.applicationInfo.loadLabel(context.packageManager).toString())
            .replace("{{appurl}}", context.getString(R.string.app_url))
            .replace("{{filelist}}", "[" + jsonFileList.joinToString(",\n") + "]")

        return finalHtml
    }
}