package com.defname.localshare.ui.screens.servercontrol

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.NetworkInfo
import com.defname.localshare.ServerRepository
import com.defname.localshare.getLocalIpAddresses
import com.defname.localshare.ui.base.ServerAccessControlActions
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class ServerControlViewModel : ServerAccessControlActions, ViewModel() {
    /**
     * StateFlow for the log entry for which the context menu should be shown.
     */
    private val _logMenuOpenForId: MutableStateFlow<String?> = MutableStateFlow(null)
    private val logCount = 4

    /**
     * StateFLow for file that should be removed from filelist.
     */
    private val _filesToDelete: MutableStateFlow<Set<Uri>> = MutableStateFlow(emptySet())

    private val _ipAddressSelectorExpanded: MutableStateFlow<Boolean> = MutableStateFlow(false)
    private val _localIpAddresses: MutableStateFlow<List<NetworkInfo>> = MutableStateFlow(emptyList())


    val state: StateFlow<ServerControlState> = combine(
        ServerRepository.state,
        _logMenuOpenForId,
        _filesToDelete,
        _ipAddressSelectorExpanded,
        _localIpAddresses

    ) { serverState, logMenuOpenForId, filesToDelete, ipAddressSelectorExpanded, localIpAddresses ->
        ServerControlState(
            server = serverState,
            logMenuOpenForId = logMenuOpenForId,
            logEntries = serverState.logs.toLogListEntries().take(logCount),
            logCount = logCount,
            filesToDelete = filesToDelete,
            ipAddressSelectorEnabled = !serverState.isRunning,
            ipAddressSelectorExpanded = ipAddressSelectorExpanded,
            localIpAddresses = localIpAddresses
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ServerControlState()
    )

    fun addFiles(files: List<Uri>) {
        if (files.isNotEmpty()) {
            ServerRepository.addFiles(files)
        }
    }

    fun clearFilesToDelete() {
        _filesToDelete.value = emptySet()
    }

    fun toggleFileToDelete(uri: Uri) {
        if (_filesToDelete.value.contains(uri)) {
            _filesToDelete.value -=  uri
        } else {
            _filesToDelete.value +=  uri
        }
    }

    fun removeFiles() {
        _filesToDelete.value.forEach { uri -> ServerRepository.removeFile(uri) }
        clearFilesToDelete()
    }

    fun clearFiles() {
        ServerRepository.clearFiles()
        clearFilesToDelete()
    }

    fun setSeletectedIp(ip: String?) {
        ServerRepository.setSelectedIp(ip)
    }

    fun collapseIpAddressSelector() {
        _ipAddressSelectorExpanded.value = false
    }

    fun ipAddressSelectorExpandedChange() {
        _localIpAddresses.value = getLocalIpAddresses()
        _ipAddressSelectorExpanded.value = !_ipAddressSelectorExpanded.value
    }

    fun logMenuOpenForId(id: String) {
        _logMenuOpenForId.value = id
    }

    fun logMenuClose() {
        _logMenuOpenForId.value = null
    }
}