package com.defname.localshare.ui.screens.logs

import com.defname.localshare.ui.components.LogListEntry

data class LogsState(
    val entries: List<LogListEntry> = emptyList(),
    val menuOpenForId: String? = null
)