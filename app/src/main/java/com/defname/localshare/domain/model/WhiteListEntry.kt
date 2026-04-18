package com.defname.localshare.domain.model

data class WhiteListEntry(
    val ip: String,
    val timestamp: Long = System.currentTimeMillis()
)

fun WhiteListEntry.isStillValid(ttlSeconds: Int = 60 * 60, now: Long = System.currentTimeMillis()): Boolean {
    return timestamp + ttlSeconds * 1000L >= now
}
