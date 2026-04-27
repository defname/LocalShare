package com.defname.localshare.service.ktor.json

fun String.escapeJson(): String = this.replace("\"", "\\\"")
fun String.toOneLine(): String = this.trimIndent()
    .replace("\n", "")
    .replace("\r", "")
