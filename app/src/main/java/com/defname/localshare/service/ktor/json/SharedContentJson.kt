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

@file:Suppress("HardCodedStringLiteral")
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
