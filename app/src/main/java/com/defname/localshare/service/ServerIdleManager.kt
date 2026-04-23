package com.defname.localshare.service

import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

class ServerIdleManager(
    private val serviceRepository: ServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val scope: CoroutineScope
) {
    private var timeoutJob: Job? = null

    fun startMonitoring(onTimeout: () -> Unit) {
        scope.launch {
            combine(
                serviceRepository.runtimeState, // Flow<Set<String>>
                settingsRepository.settingsFlow // Flow<Settings>
            ) { runtimeState, settings ->
                runtimeState.activeConnections.isEmpty() to settings.serverIdleTimeoutSeconds
            }.distinctUntilChanged()
                .collect { (isEmpty, timeout) ->
                    timeoutJob?.cancel()
                    if (isEmpty && timeout > 0) {
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