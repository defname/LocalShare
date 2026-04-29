// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.json

import com.defname.localshare.domain.model.SharedContent

fun SharedContent.toJsonString() = when (this) {
    is SharedContent.Text -> this.toJsonString()
    is SharedContent.Other -> this.toJsonString()
}

fun SharedContent.Text.toJsonString() = """
    {
        "id": "${this.id}",
        "mimeType": "${this.mimeType.escapeJson()}",
        "text": "${this.text.escapeJson()}"
    }    
    """.toOneLine()

fun SharedContent.Other.toJsonString() =
    """
    {
        "id": "${this.id}",
        "mimeType": "${this.mimeType.escapeJson()}",
        "data": "${this.data.escapeJson()}",
        "label": "${this.label?.escapeJson() ?: ""}"
    }    
    """.toOneLine()
