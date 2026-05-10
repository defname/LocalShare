// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service.ktor.json

// Escapes a string for safe embedding inside a JSON string value inside a <script> block.
// Escapes <, > and / to Unicode escapes to prevent XSS via script tag injection.
fun String.escapeJson(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")
    .replace("<", "\\u003C")
    .replace(">", "\\u003E")
    .replace("/", "\\u002F")

fun String.toOneLine(): String = this.trimIndent()
    .replace("\n", "")
    .replace("\r", "")
