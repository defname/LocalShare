@file:Suppress("HardCodedStringLiteral")
package com.defname.localshare.service.ktor.json

import com.defname.localshare.domain.model.FileInfo

fun FileInfo.toJsonString() = """
    {
        "fileId": "${this.id}",
        "filename": "${this.name.escapeJson()}",
        "hasThumbnail": ${if (this.filePreview != null) "true" else "false"},
        "icon": "${this.iconFile}",
        "size": ${this.size},
        "mimeType": "${this.mimeType}"
    }    
    """.toOneLine()