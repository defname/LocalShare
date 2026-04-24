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
