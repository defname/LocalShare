package com.defname.localshare.ui.screens.settings

import com.defname.localshare.ServerState

data class SettingsState(
    val server: ServerState = ServerState()
)