package com.defname.localshare.domain.model

import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

data class ConnectionRequest(
    val id: String = UUID.randomUUID().toString(),
    val clientIp: String,
    val deferred: CompletableDeferred<Boolean> = CompletableDeferred()
)