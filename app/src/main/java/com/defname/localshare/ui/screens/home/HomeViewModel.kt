// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.home

import android.graphics.Bitmap
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.QrCodeProvider
import com.defname.localshare.data.RuntimeData
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServerUrlProvider
import com.defname.localshare.data.ServerUrls
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.ConnectionLogEntry
import com.defname.localshare.domain.model.NetworkInfo
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private data class InternalUiState(
    val logMenuId: String? = null,
    val filesToDelete: Set<Uri> = emptySet(),
    val ipAddressSelectorExpanded: Boolean = false,
    val qrCodeDialogVisible: Boolean = false,
    val qrCodeUrl: String = "",
    val qrCodeBitmap: Bitmap? = null
)

class HomeViewModel(
    private val addFilesUseCase: AddFilesUseCase,
    private val serviceRepository: ServiceRepository,
    private val connectionLogsRepository: ConnectionLogsRepository,
    private val settingsRepository: SettingsRepository,
    private val securityRepository: SecurityRepository,
    private val networkInfoProvider: NetworkInfoProvider,
    private val permissionRepository: PermissionRepository,
    private val manageServiceUseCase: ManageServiceUseCase,
    private val serverUrlProvider: ServerUrlProvider,
    private val qrCodeProvider: QrCodeProvider
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


    val state: StateFlow<HomeState> = combine(
        serviceRepository.runtimeState,
        connectionLogsRepository.connections,
        settingsState,
        securityRepository.blacklist,
        securityRepository.whitelist,
        _uiState,
        permissionRepository.hasNotificationPermission,
        serverUrlProvider.serverUrls,
        networkInfoProvider.localIpAddresses,
    ) { states ->

        val runtimeState = states[0] as RuntimeData
        val logs = states[1] as List<ConnectionLogEntry>
        val settings = states[2] as Settings
        val blacklist = states[3] as Set<String>
        val whitelist = states[4] as List<WhiteListEntry>
        val uiState = states[5] as InternalUiState
        val hasNotificationPermission = states[6] as Boolean
        val serverUrls = states[7] as ServerUrls
        val localIpAddresses = states[8] as List<NetworkInfo>


        val selectedIpAddressIsValid = (localIpAddresses + NetworkInfo("0.0.0.0", "any")).any { it.ip == settings.serverIp }

        HomeState(
            token = settings.token,
            selectedIpAddress = settings.serverIp,
            isSelectedIpAddressValid = selectedIpAddressIsValid,
            port = settings.serverPort,
            fileList = runtimeState.fileList,
            isRunning = runtimeState.serviceState == RuntimeState.RUNNING,
            serviceState = runtimeState.serviceState,
            blacklist = blacklist,
            whitelist = whitelist,
            logEntries = logs.toLogListEntries(securityRepository).take(logCount),
            logMenuOpenForId = uiState.logMenuId,
            filesToDelete = uiState.filesToDelete,
            localIpAddresses = localIpAddresses,
            ipAddressSelectorExpanded = uiState.ipAddressSelectorExpanded,
            ipAddressSelectorEnabled = runtimeState.serviceState == RuntimeState.STOPPED,
            showExpandLogsButton = logs.size > logCount,
            hasNotificationPermission = hasNotificationPermission,
            qrCodeDialogVisible = uiState.qrCodeDialogVisible,
            qrCodeUrl = uiState.qrCodeUrl,
            qrCodeBitmap = uiState.qrCodeBitmap,
            serverUrls = serverUrls.serverUrls,
            primaryServerUrl = serverUrls.defaultServerUrl
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = HomeState()
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
                ipAddressSelectorExpanded = !_uiState.value.ipAddressSelectorExpanded
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

    fun onStartServer() {
        manageServiceUseCase.startService()
    }

    fun onStopServer() {
        manageServiceUseCase.stopService()
    }

    fun onCloseQrCodeDialog() {
        _uiState.update { it.copy(qrCodeDialogVisible = false) }
    }

    fun onOpenQrCodeDialog(url: String) {
        _uiState.update {
            val qrCodeUrlChanged = it.qrCodeUrl != url
            if (qrCodeUrlChanged) {
                it.copy(
                    qrCodeDialogVisible = true,
                    qrCodeUrl = url,
                    qrCodeBitmap = null
                )
            } else {
                it.copy(qrCodeDialogVisible = true)
            }
        }

        if (_uiState.value.qrCodeBitmap == null) {
            viewModelScope.launch {
                val bitmap = withContext(Dispatchers.Default) {
                    qrCodeProvider.generateQRCode(url)
                }
                _uiState.update { it.copy(qrCodeBitmap = bitmap) }
            }
        }
    }

}