@file:Suppress("HardCodedStringLiteral")
package com.defname.localshare.service.ktor.json

import com.defname.localshare.domain.model.SharedContent

fun SharedContent.toJsonString() = when (this) {
    is SharedContent.Text -> this.toJsonString()
    is SharedContent.VCard -> this.toJsonString()
    is SharedContent.Other -> this.toJsonString()
}

fun SharedContent.Text.toJsonString() = """
    {
        "id": "${this.id}",
        "mimeType": "${this.mimeType.escapeJson()}",
        "text": "${this.text.escapeJson()}"
    }    
    """.toOneLine()

fun SharedContent.VCard.toJsonString() = """
    {
        "id": "${this.id}",
        "mimeType": "${this.mimeType.escapeJson()}"
    }    
    """.toOneLine()
fun SharedContent.Other.toJsonString() =
    """
    {
        "id": "${this.id}",
        "mimeType": "${this.mimeType.escapeJson()}",
        "text": "${this.data.escapeJson()}",
        "filename": "${this.label?.escapeJson() ?: ""}"
    }    
    """.toOneLine()
