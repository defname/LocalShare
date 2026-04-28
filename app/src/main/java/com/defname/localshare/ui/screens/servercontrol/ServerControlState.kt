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

package com.defname.localshare.ui.screens.servercontrol

import android.net.Uri
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
    val blacklist: Set<String> = emptySet(),
    val whitelist: List<WhiteListEntry> = emptyList(),

    val logEntries: List<LogListEntry> = emptyList(),
    val logMenuOpenForId: String? = null,

    val filesToDelete: Set<Uri> = emptySet(),

    val localIpAddresses: List<NetworkInfo> = emptyList(),

    val ipAddressSelectorExpanded: Boolean = false,
    val ipAddressSelectorEnabled: Boolean = false,

    val showExpandLogsButton: Boolean = false,

    val hasNotificationPermission: Boolean = false

    )