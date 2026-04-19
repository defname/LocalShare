package com.defname.localshare.ui.screens.servercontrol

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.NetworkInfo
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.RuntimeData
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.LogEntry
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.AddFilesUseCase
import com.defname.localshare.getLocalIpAddresses
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private data class InternalUiState(
    val logMenuId: String? = null,
    val filesToDelete: Set<Uri> = emptySet(),
    val ipAddressSelectorExpanded: Boolean = false,
    val localIpAddresses: List<NetworkInfo> = emptyList()
)

class ServerControlViewModel(
    private val addFilesUseCase: AddFilesUseCase,
    private val serviceRepository: ServiceRepository,
    private val logsRepository: LogsRepository,
    private val settingsRepository: SettingsRepository,
    private val securityRepository: SecurityRepository
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
        logsRepository.logs,
        settingsState,
        securityRepository.blacklist,
        securityRepository.whitelist,
        _uiState

    ) { states ->

        val runtimeState = states[0] as RuntimeData
        val logs = states[1] as List<LogEntry>
        val settings = states[2] as Settings
        val blacklist = states[3] as Set<String>
        val whitelist = states[4] as List<WhiteListEntry>
        val uiState = states[5] as InternalUiState

        ServerControlState(
            token = settings.token,
            selectedIpAdress = settings.serverIp,
            port = settings.serverPort,
            fileList = runtimeState.fileList,
            isRunning = runtimeState.isRunning,
            activeClients = runtimeState.activeClients,
            blacklist = securityRepository.blacklist.value,
            whitelist = securityRepository.whitelist.value,
            logEntries = logs.toLogListEntries(securityRepository).takeLast(logCount),
            logMenuOpenForId = uiState.logMenuId,
            filesToDelete = uiState.filesToDelete,
            localIpAddresses = uiState.localIpAddresses,
            ipAddressSelectorExpanded = uiState.ipAddressSelectorExpanded,
            ipAddressSelectorEnabled = !runtimeState.isRunning,
            showExpandLogsButton = logs.size > logCount
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
        _uiState.update {
            it.copy(
                ipAddressSelectorExpanded = !_uiState.value.ipAddressSelectorExpanded,
                localIpAddresses = getLocalIpAddresses()
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
        settingsRepository.setToken(securityRepository.generateRandomToken())
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
}