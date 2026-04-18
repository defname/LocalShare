package com.defname.localshare.data

import com.defname.localshare.domain.model.LogEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class LogsRepository {
    private val _logs = MutableStateFlow<List<LogEntry>>(emptyList())
    val logs: Flow<List<LogEntry>> = _logs.asStateFlow()

}