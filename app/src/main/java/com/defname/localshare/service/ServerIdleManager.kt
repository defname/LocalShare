// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service

import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ServerIdleManager(
    private val settingsRepository: SettingsRepository,
    private val connectionLogsRepository: ConnectionLogsRepository,
    private val scope: CoroutineScope
) {
    private var timeoutJob: Job? = null

    fun startMonitoring(onTimeout: () -> Unit) {
        scope.launch {
            combine(
                connectionLogsRepository.hasActiveConnections, // Flow<Boolean>
                settingsRepository.settingsFlow // Flow<Settings>
            ) { isActive, settings ->
                isActive to settings.serverIdleTimeoutSeconds
            }.distinctUntilChanged()
                .collect { (isActive, timeout) ->
                    timeoutJob?.cancel()
                    if (!isActive && timeout > 0) {
                        timeoutJob = launch {
                            delay(timeout * 1000L)
                            onTimeout()
                        }
                    }
                }
        }
    }

    fun stop() {
        timeoutJob?.cancel()
    }
}