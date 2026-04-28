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