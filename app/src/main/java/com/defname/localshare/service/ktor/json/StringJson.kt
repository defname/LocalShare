package com.defname.localshare.service.ktor.json

fun String.escapeJson(): String = this
    .replace("\\", "\\\\")
    .replace("\"", "\\\"")
    .replace("\n", "\\n")
    .replace("\r", "\\r")

fun String.toOneLine(): String = this.trimIndent()
    .replace("\n", "")
    .replace("\r", "")
