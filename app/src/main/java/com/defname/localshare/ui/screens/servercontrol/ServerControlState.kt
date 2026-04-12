package com.defname.localshare.ui.screens.servercontrol

import android.net.Uri
import com.defname.localshare.NetworkInfo
import com.defname.localshare.ServerState
import com.defname.localshare.ui.components.LogListEntry

data class ServerControlState(
    val server: ServerState = ServerState(),

    val logCount: Int = 4,
    val logEntries: List<LogListEntry> = emptyList(),
    val logMenuOpenForId: String? = null,

    val filesToDelete: Set<Uri> = emptySet(),

    val localIpAddresses: List<NetworkInfo> = emptyList(),

    val ipAddressSelectorExpanded: Boolean = false,
    val ipAddressSelectorEnabled: Boolean = false,

)