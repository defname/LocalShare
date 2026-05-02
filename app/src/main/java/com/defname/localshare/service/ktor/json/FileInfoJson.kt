// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.json

import com.defname.localshare.domain.model.FileInfo

fun FileInfo.toJsonString() = """
    {
        "fileId": "${this.id}",
        "filename": "${this.name.escapeJson()}",
        "icon": "${this.iconFile}",
        "size": ${this.size},
        "mimeType": "${this.mimeType}",
        "hasThumbnail": ${mimeType.startsWith("image/") || mimeType.startsWith("video/")}
    }    
    """.toOneLine()