// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn

class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    val settings = settingsRepository.settingsFlow.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = Settings()
    )

    val runtimeState = serviceRepository.runtimeState

    suspend fun setPort(port: Int) {
        settingsRepository.setPort(port)
    }

    suspend fun setIdleTimeoutSeconds(seconds: Int) {
        settingsRepository.setServerIdleTimeoutSeconds(seconds)
    }

    suspend fun setRequireApproval(requireApproval: Boolean) {
        settingsRepository.setRequireApproval(requireApproval)
    }

    suspend fun setWhiteListEntryTTLSeconds(seconds: Int) {
        settingsRepository.setWhitelistEntryTTLSeconds(seconds)
    }

    suspend fun setClearFilesListOnSendIntent(clear: Boolean) {
        settingsRepository.setClearFileListOnShareIntent(clear)
    }

    suspend fun setHeartbeatPeriodSeconds(seconds: Int) {
        settingsRepository.setHeartbeatPeriodSeconds(seconds)
    }
}