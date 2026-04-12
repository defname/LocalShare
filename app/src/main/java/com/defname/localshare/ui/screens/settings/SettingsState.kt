package com.defname.localshare.ui.screens.settings

import com.defname.localshare.ServerState
import com.defname.localshare.ui.components.LogListEntry

data class SettingsState(
    val server: ServerState = ServerState()
)