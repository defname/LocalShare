package com.defname.localshare.ui.screens.servercontrol

import android.net.Uri
import com.defname.localshare.data.Connection
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.NetworkInfo
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.ui.components.LogListEntry

data class ServerControlState(
    val token: String = "",

    val selectedIpAddress: String = "0.0.0.0",
    val isSelectedIpAddressValid: Boolean = true,
    val port: Int = 8080,

    val fileList: List<FileInfo> = emptyList(),
    val isRunning: Boolean = false,
    val serviceState: RuntimeState = RuntimeState.STOPPED,
    val activeConnections: Set<Connection> = emptySet(),
    val blacklist: Set<String> = emptySet(),
    val whitelist: List<WhiteListEntry> = emptyList(),

    val logEntries: List<LogListEntry> = emptyList(),
    val logMenuOpenForId: String? = null,

    val filesToDelete: Set<Uri> = emptySet(),

    val localIpAddresses: List<NetworkInfo> = emptyList(),

    val ipAddressSelectorExpanded: Boolean = false,
    val ipAddressSelectorEnabled: Boolean = false,

    val showExpandLogsButton: Boolean = false

    )