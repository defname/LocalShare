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

package com.defname.localshare.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LogsViewModel(
    private val serviceRepository: ServiceRepository,
    private val logsRepository: ConnectionLogsRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {
    private val _menuOpenForId = MutableStateFlow<String?>(null)

    val state: StateFlow<LogsState> = combine(
        serviceRepository.runtimeState,
        logsRepository.connections,
        _menuOpenForId
    ) { runtimeState, logs, menuOpenForId ->
        LogsState(
            entries = logs.toLogListEntries(securityRepository),
            menuOpenForId = menuOpenForId
    ) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogsState()
    )

    fun openMenu(id: String) { _menuOpenForId.value = id }
    fun closeMenu() { _menuOpenForId.value = null }
    fun addToBlacklist(it: String) { securityRepository.addToBlacklist(it) }
    fun removeFromBlacklist(it: String) { securityRepository.removeFromBlacklist(it) }
    fun removeFromWhitelist(it: String) { securityRepository.removeFromWhitelist(it) }

    fun clearLogs() {
        logsRepository.clearLogs()
    }
}