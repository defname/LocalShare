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