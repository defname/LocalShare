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

package com.defname.localshare.data

import com.defname.localshare.domain.model.ConnectionLogEntry
import com.defname.localshare.domain.model.DisconnectReason
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class ConnectionLogsRepository {
    private val _connections = MutableStateFlow<List<ConnectionLogEntry>>(emptyList())
    val connections: StateFlow<List<ConnectionLogEntry>> = _connections.asStateFlow()

    val hasActiveConnections = connections
        .map { list -> list.any { it.closedTimestamp == null } }
        .distinctUntilChanged()


    fun clearLogs() {
        _connections.update {
            it.filter { entry -> entry.closedTimestamp == null }
        }
    }

    fun clientConnected(
        method: String,
        path: String,
        clientIp: String
    ): String {
        val entry = ConnectionLogEntry(
            method = method,
            path = path,
            clientIp = clientIp
        )
        _connections.update { it + entry }
        return entry.id
    }

    fun clientDisconnected(
        id: String,
        result: DisconnectReason
    ) {
        _connections.update { logs ->
            logs.map { entry ->
                if (entry.id == id && entry.closedTimestamp == null) {
                    entry.copy(
                        closedTimestamp = System.currentTimeMillis(),
                        result = result
                    )
                } else {
                    entry
                }
            }
        }
    }

}
