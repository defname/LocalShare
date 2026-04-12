package com.defname.localshare.ui.screens.logs

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.ServerRepository
import com.defname.localshare.ui.base.ServerAccessControlActions
import com.defname.localshare.ui.components.LogListEntry
import com.defname.localshare.ui.components.toLogListEntries
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class LogsViewModel() : ServerAccessControlActions, ViewModel() {
    private val _menuOpenForId = MutableStateFlow<String?>(null)

    val state: StateFlow<LogsState> = combine(
        ServerRepository.state,
        _menuOpenForId
    ) { serverState, menuOpenForId ->
        LogsState(
            entries = serverState.logs.toLogListEntries(),
            menuOpenForId = menuOpenForId
    ) }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = LogsState()
    )

    fun openMenu(id: String) { _menuOpenForId.value = id }
    fun closeMenu() { _menuOpenForId.value = null }

}