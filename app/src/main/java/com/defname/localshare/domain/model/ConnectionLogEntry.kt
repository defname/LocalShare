package com.defname.localshare.domain.model

import java.util.UUID

sealed class DisconnectReason {

    data class Expected(val statusCode: Int) : DisconnectReason()

    object ServerShutdown : DisconnectReason()

    sealed class Unexpected : DisconnectReason() {
        object ClientGone : Unexpected()
        object AuthInvalid : Unexpected()
        object Unknown: Unexpected()
        data class Error(val message: String?) : Unexpected()
    }
}

data class ConnectionLogEntry (
    val openTimestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val clientIp: String,
    val closedTimestamp: Long? = null,
    val result: DisconnectReason? = null,
    val id: String = UUID.randomUUID().toString()
)