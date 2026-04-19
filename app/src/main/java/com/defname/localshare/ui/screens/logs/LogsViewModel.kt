package com.defname.localshare.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LogsViewModel(
    private val serviceRepository: ServiceRepository,
    private val logsRepository: LogsRepository,
    private val securityRepository: SecurityRepository
) : ViewModel() {
    private val _menuOpenForId = MutableStateFlow<String?>(null)

    val state: StateFlow<LogsState> = combine(
        serviceRepository.runtimeState,
        logsRepository.logs,
        _menuOpenForId
    ) { runtimeState, logs, menuOpenForId ->
        LogsState(
            entries = logs.toLogListEntries(securityRepository),
            menuOpenForId = menuOpenForId
    ) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogsState()
    )

    fun openMenu(id: String) { _menuOpenForId.value = id }
    fun closeMenu() { _menuOpenForId.value = null }
    fun addToBlacklist(it: String) { securityRepository.addToBlacklist(it) }
    fun removeFromBlacklist(it: String) { securityRepository.removeFromBlacklist(it) }
    fun removeFromWhitelist(it: String) { securityRepository.removeFromWhitelist(it) }

}