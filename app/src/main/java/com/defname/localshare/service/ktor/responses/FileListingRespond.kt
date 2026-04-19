package com.defname.localshare.service.ktor.responses

import android.content.Context
import android.util.Log
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLPathPart
import io.ktor.server.application.ApplicationCall
import io.ktor.server.response.respond
import io.ktor.server.response.respondText

suspend fun ApplicationCall.sendFileListing(
    files: List<FileInfo>,
    token: String,
    context: Context
) {
    try {
        // 1. Template aus Assets laden
        val template = context.assets.open("listing.html").bufferedReader().use { it.readText() }

        // 2. Den dynamischen Teil für die Dateien bauen (HTML-Schnipsel)
        val fileEntriesHtml = StringBuilder()
        val fileItems = mutableListOf<String>()
        files.forEach { file ->
            val fileId = file.id.encodeURLPathPart()
            val hasThumbnail = file.filePreview != null
            val imgSrc = if (hasThumbnail) "/$token/thumbnail/$fileId"
            else "/$token/icon/${file.iconFile.encodeURLPathPart()}"
            val imgClass = if (!hasThumbnail) "icon" else ""

            fileEntriesHtml.append("""
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
                """.trimIndent())
            fileItems += ("""
                {
                    fileId: "$fileId",
                    filename: "${file.name}",
                    hasThumbnail: $hasThumbnail,
                    icon: "${file.iconFile}",
                    size: ${file.size},
                    mimeType: "${file.mimeType}"
                }    
                """.trimIndent())
        }

        // 3. Platzhalter im Template ersetzen
        val finalHtml = template
            .replace("{{token}}", token)
            .replace("{{noscript}}", fileEntriesHtml.toString())
            .replace("{{appname}}", context.applicationInfo.loadLabel(context.packageManager).toString())
            .replace("{{appurl}}", context.getString(R.string.app_url))
            .replace("{{filelist}}", "[" + fileItems.joinToString(",\n") + "]")


        // 4. Antwort senden
        respondText(finalHtml, ContentType.Text.Html)

    } catch (e: Exception) {
        Log.e("FileServerService", "Error loading template", e)
        respond(HttpStatusCode.InternalServerError, "Template Error")
    }
}