// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.logs

import com.defname.localshare.ui.components.LogListEntry

data class LogsState(
    val entries: List<LogListEntry> = emptyList(),
    val menuOpenForId: String? = null
)