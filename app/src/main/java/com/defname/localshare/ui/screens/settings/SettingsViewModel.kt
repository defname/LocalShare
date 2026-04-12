package com.defname.localshare.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.ServerRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel : ViewModel() {
    val state: StateFlow<SettingsState> = ServerRepository.state
        .map { SettingsState(server = it) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SettingsState()
        )

    fun setPort(port: Int) {
        ServerRepository.setPort(port)
    }

    fun setIdleTimeoutSeconds(seconds: Int) {
        ServerRepository.setIdleTimeoutSeconds(seconds)
    }

    fun setRequireApproval(requireApproval: Boolean) {
        ServerRepository.setRequireApproval(requireApproval)
    }

    fun setWhiteListEntryTTLSeconds(seconds: Int) {
        ServerRepository.setWhiteListEntryTTLSeconds(seconds)
    }

    fun setClearFilesListOnSendIntent(clear: Boolean) {
        ServerRepository.setClearFilesListOnSendIntent(clear)
    }

    fun setKeepScreenOn(keepScreenOn: Boolean) {
        ServerRepository.setKeepScreenOn(keepScreenOn)
    }
}