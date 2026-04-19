package com.defname.localshare.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update


class MainViewModel(
    private val logsRepository: LogsRepository,
    private val serviceRepository: ServiceRepository,
    private val permissionRepository: PermissionRepository,
    private val manageServiceUseCase: ManageServiceUseCase
) : ViewModel() {
    fun showQrDialog() {
        _showQrDialog.update { true }
    }

    fun hideQrDialog() {
        _showQrDialog.update { false }
    }

    fun clearLogs() {
        logsRepository.clearLogs()
    }

    fun updatePermissionStatus() {
        permissionRepository.updatePermissionStatus()
    }

    fun startServer() {
        manageServiceUseCase.startService()
    }

    fun stopServer() {
        manageServiceUseCase.stopService()
    }

    private val _showQrDialog = MutableStateFlow(false)
    val state = combine(
        _showQrDialog,
        logsRepository.logs,
        serviceRepository.runtimeState,
        permissionRepository.hasNotificationPermission
    ) { showQrDialog, logs, runtimeState, hasNotificationPermission ->
        MainState(
            showQrDialog = showQrDialog,
            hasLogs = logs.isNotEmpty(),
            isServerRunning = runtimeState.isRunning,
            isNotificationPermissionGranted = hasNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState()
    )
}