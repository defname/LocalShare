// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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