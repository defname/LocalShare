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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.RuntimeData
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.ConnectionLogEntry
import com.defname.localshare.domain.model.NetworkInfo
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class InternalUiState(
    val logMenuId: String? = null,
    val filesToDelete: Set<Uri> = emptySet(),
    val ipAddressSelectorExpanded: Boolean = false,
    val localIpAddresses: List<NetworkInfo> = emptyList()
)

class ServerControlViewModel(
    private val addFilesUseCase: AddFilesUseCase,
    private val serviceRepository: ServiceRepository,
    private val connectionLogsRepository: ConnectionLogsRepository,
    private val settingsRepository: SettingsRepository,
    private val securityRepository: SecurityRepository,
    private val networkInfoProvider: NetworkInfoProvider,
    private val permissionRepository: PermissionRepository
) : ViewModel() {
    /**
     * StateFlow for the log entry for which the context menu should be shown.
     */
    private val _uiState = MutableStateFlow(InternalUiState())
    private val logCount = 4

    private val settingsState = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )


    val state: StateFlow<ServerControlState> = combine(
        serviceRepository.runtimeState,
        connectionLogsRepository.connections,
        settingsState,
        securityRepository.blacklist,
        securityRepository.whitelist,
        _uiState,
        permissionRepository.hasNotificationPermission

    ) { states ->

        val runtimeState = states[0] as RuntimeData
        val logs = states[1] as List<ConnectionLogEntry>
        val settings = states[2] as Settings
        val blacklist = states[3] as Set<String>
        val whitelist = states[4] as List<WhiteListEntry>
        val uiState = states[5] as InternalUiState
        val hasNotificationPermission = states[6] as Boolean


        val localAddresses = networkInfoProvider.getLocalIpAddresses() + NetworkInfo("0.0.0.0", "any")

        ServerControlState(
            token = settings.token,
            selectedIpAddress = settings.serverIp,
            isSelectedIpAddressValid = localAddresses.any { it.ip == settings.serverIp },
            port = settings.serverPort,
            fileList = runtimeState.fileList,
            isRunning = runtimeState.serviceState == RuntimeState.RUNNING,
            serviceState = runtimeState.serviceState,
            blacklist = securityRepository.blacklist.value,
            whitelist = securityRepository.whitelist.value,
            logEntries = logs.toLogListEntries(securityRepository).take(logCount),
            logMenuOpenForId = uiState.logMenuId,
            filesToDelete = uiState.filesToDelete,
            localIpAddresses = uiState.localIpAddresses,
            ipAddressSelectorExpanded = uiState.ipAddressSelectorExpanded,
            ipAddressSelectorEnabled = runtimeState.serviceState == RuntimeState.STOPPED,
            showExpandLogsButton = logs.size > logCount,
            hasNotificationPermission = hasNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerControlState()
    )

    fun addFiles(files: List<Uri>) {
        if (files.isNotEmpty()) {
            addFilesUseCase(files)
        }
    }

    fun clearFilesToDelete() {
        _uiState.update { it.copy(filesToDelete = emptySet()) }
    }

    fun toggleFileToDelete(uri: Uri) {
        if (_uiState.value.filesToDelete.contains(uri)) {
            _uiState.update { it.copy(filesToDelete = it.filesToDelete - uri) }
        } else {
            _uiState.update { it.copy(filesToDelete = it.filesToDelete + uri) }
        }
    }

    fun removeFiles() {
        for (file in _uiState.value.filesToDelete) {
            serviceRepository.removeFile(file)
        }
        clearFilesToDelete()
    }

    fun clearFiles() {
        serviceRepository.clearFiles()
        clearFilesToDelete()
    }

    suspend fun setSeletectedIp(ip: String?) {
        settingsRepository.setServerIp(ip ?: "0.0.0.0")
    }

    fun collapseIpAddressSelector() {
        _uiState.update { it.copy(ipAddressSelectorExpanded = false) }
    }

    fun ipAddressSelectorExpandedChange() {
        val addresses = networkInfoProvider.getLocalIpAddresses()
        val currentSettingsIp = settingsState.value.serverIp

        // Falls die aktuelle IP nicht mehr existiert, korrigieren wir sie
        if (addresses.none { it.ip == currentSettingsIp } && currentSettingsIp != "0.0.0.0") {
            val fallbackIp = networkInfoProvider.getSmartDefaultIp(addresses)
            viewModelScope.launch {
                settingsRepository.setServerIp(fallbackIp)
            }
        }

        _uiState.update {
            it.copy(
                ipAddressSelectorExpanded = !_uiState.value.ipAddressSelectorExpanded,
                localIpAddresses = addresses
            )
        }
    }

    fun logMenuOpenForId(id: String) {
        _uiState.update { it.copy(logMenuId = id) }
    }

    fun logMenuClose() {
        _uiState.update { it.copy(logMenuId = null) }
    }

    suspend fun onTokenChange(token: String) {
        settingsRepository.setToken(token)
    }

    suspend fun onRandomTokenClick() {
        settingsRepository.setToken(SecurityRepository.generateRandomToken())
    }

    fun addToBlacklist(it: String) {
        securityRepository.addToBlacklist(it)
    }

    fun removeFromBlacklist(it: String) {
        securityRepository.removeFromBlacklist(it)
    }

    fun removeFromWhitelist(it: String) {
        securityRepository.removeFromWhitelist(it)
    }

    fun updatePermissionStatus() {
        permissionRepository.updatePermissionStatus()
    }
}