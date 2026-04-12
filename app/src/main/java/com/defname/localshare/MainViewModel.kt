package com.defname.localshare

import androidx.compose.runtime.Composable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
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

data class AppUiState(
    //val main: MainScreenState = MainScreenState(),
    val logs: LogListState = LogListState(),
    val isServerRunning: Boolean = false
)

class MainViewModel(private val repository: ServerRepository) : ViewModel() {

    // Lokale UI-States (Sachen, die nicht ins Repo gehören)
    private val _contextMenuFor = MutableStateFlow<LogEntry?>(null)


    // Der "Master-State", den alle Screens beobachten
    val uiState: StateFlow<AppUiState> = combine(
        repository.state,
        _contextMenuFor
    ) { repoState, menuEntry ->
        AppUiState(
            isServerRunning = repoState.isRunning,
            logs = LogListState(
                entries = repoState.logs
                    .map {
                        LogListEntry(
                            it,
                            repository.isWhitelisted(it.clientIp),
                            repository.isBlacklisted(it.clientIp)
                        ) }
                    .reversed(),
                contextMenuFor = menuEntry
            ),
            // settings = ...
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), AppUiState())

    // Aktionen (Events)
    fun openLogMenu(entry: LogEntry) { _contextMenuFor.value = entry }
    fun closeLogMenu() { _contextMenuFor.value = null }

    fun addToBlackList(ip: String) { repository.addToBlacklist(ip) }
    fun removeFromBlackList(ip: String) { repository.removeFromBlacklist(ip) }
    fun removeFromWhiteList(ip: String) { repository.removeFromWhitelist(ip) }

}