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