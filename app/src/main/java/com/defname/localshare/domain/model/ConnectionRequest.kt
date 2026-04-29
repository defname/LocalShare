// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.model

import kotlinx.coroutines.CompletableDeferred
import java.util.UUID

data class ConnectionRequest(
    val id: String = UUID.randomUUID().toString(),
    val clientIp: String,
    val deferred: CompletableDeferred<Boolean> = CompletableDeferred()
)