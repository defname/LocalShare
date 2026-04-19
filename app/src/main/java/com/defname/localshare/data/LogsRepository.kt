package com.defname.localshare.data

import com.defname.localshare.domain.model.LogEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class LogsRepository {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: StateFlow<List<LogEntry>> = _logs.asStateFlow()

    fun clearLogs() {
        _logs.update { emptyList() }
    }

    fun addLog(entry: LogEntry) {
        _logs.update { it + entry }
    }

    fun updateLogStatus(id: String, status: Int) {
        _logs.update { logs ->
            logs.map { log ->
                if (log.id == id) {
                    log.copy(status = status)
                } else {
                    log
                }
            }
        }
    }
}
