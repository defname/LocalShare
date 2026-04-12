package com.defname.localshare

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.defname.localshare.ui.components.LogListEntry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn


@Composable
inline fun <reified T : ViewModel> getViewModel(
    repository: ServerRepository = ServerRepository
): T {
    return viewModel(
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(repository) as T
            }
        }
    )
}

data class LogListState(
    val entries: List<LogListEntry> = emptyList(),
    val contextMenuFor: LogEntry? = null
)

data class ServerControlScreenUiState(
    val ipAddressSelectorExpanded: Boolean = false
)

data class AppUiState(
    //val main: MainScreenState = MainScreenState(),
    val server: ServerState = ServerState(),
    val logs: LogListState = LogListState(),
    val serverControlScreen: ServerControlScreenUiState = ServerControlScreenUiState()
)

class MainViewModel(private val repository: ServerRepository) : ViewModel() {

    // Lokale UI-States (Sachen, die nicht ins Repo gehören)
    private val _logsContextMenuFor = MutableStateFlow<LogEntry?>(null)
    private val _mainIpAddressSelectorExpanded = MutableStateFlow<Boolean>(false)

    // Der "Master-State", den alle Screens beobachten
    val uiState: StateFlow<AppUiState> = combine(
        repository.state,
        _logsContextMenuFor,
        _mainIpAddressSelectorExpanded
    ) { serverState, logsContextMenuFor, mainIpAddressSelectorExpanded ->
        AppUiState(
            logs = LogListState(
                entries = serverState.logs
                    .map {
                        LogListEntry(
                            it,
                            repository.isWhitelisted(it.clientIp),
                            repository.isBlacklisted(it.clientIp)
                        ) }
                    .reversed(),
                contextMenuFor = logsContextMenuFor
            ),
            serverControlScreen = ServerControlScreenUiState(
                mainIpAddressSelectorExpanded
            ),
            server = serverState
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUiState())

    // Aktionen (Events)
    fun openLogMenu(entry: LogEntry) { _logsContextMenuFor.value = entry }
    fun closeLogMenu() { _logsContextMenuFor.value = null }

    fun addToBlackList(ip: String) { repository.addToBlacklist(ip) }
    fun removeFromBlackList(ip: String) { repository.removeFromBlacklist(ip) }
    fun removeFromWhiteList(ip: String) { repository.removeFromWhitelist(ip) }

    fun toggleIpAddressSelectorExpanded() { _mainIpAddressSelectorExpanded.value = !_mainIpAddressSelectorExpanded.value }
}