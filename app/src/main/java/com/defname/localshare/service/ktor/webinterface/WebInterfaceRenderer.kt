// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.webinterface

import android.content.Context
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ktor.json.toJsonString

class WebInterfaceRenderer(
    private val context: Context,
    private val templateFile: String = "web/template.html"
){
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