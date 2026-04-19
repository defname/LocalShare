package com.defname.localshare.domain.model

import java.util.UUID

data class LogEntry (
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val status: Int = 0,
    val clientIp: String,
    val id: String = UUID.randomUUID().toString()
)