package com.defname.localshare.ui.screens.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

private data class QrState(
    val showQrDialog: Boolean,
    val qrForStream: Boolean
)

class MainViewModel(
    private val logsRepository: LogsRepository,
    private val serviceRepository: ServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
    private val manageServiceUseCase: ManageServiceUseCase,
    private val networkInfoProvider: NetworkInfoProvider
) : ViewModel() {
    fun showQrDialog() {
        _qrState.update { it.copy(showQrDialog = true) }
    }

    fun hideQrDialog() {
        _qrState.update { it.copy(showQrDialog = false) }
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

    fun toggleQrForStream() {
        _qrState.update { it.copy(qrForStream = !it.qrForStream) }
    }

    private val _qrState = MutableStateFlow(QrState(false, false))

    private fun getQrLink(qrForStream: Boolean, token: String, ip: String, port: Int): String {
        return if (qrForStream) {
            "http://$ip:$port/stream/$token"
        } else {
            "http://$ip:$port/download/$token"
        }
    }

    val state = combine(
        _qrState,
        settingsRepository.settingsFlow,
        logsRepository.logs,
        serviceRepository.runtimeState,
        permissionRepository.hasNotificationPermission
    ) { qrState, settings, logs, runtimeState, hasNotificationPermission ->
        var qrCodeIp = settings.serverIp
        if (qrCodeIp == "0.0.0.0") {
            val addresses = networkInfoProvider.getLocalIpAddresses()
            qrCodeIp = networkInfoProvider.getSmartDefaultIp(addresses)
        }

        MainState(
            showQrDialog = qrState.showQrDialog,
            qrForStream = qrState.qrForStream,
            qrFullLink = getQrLink(
                qrState.qrForStream,
                settings.token,
                qrCodeIp,
                settings.serverPort
            ),
            hasLogs = logs.isNotEmpty(),
            serverState = runtimeState.serviceState,
            isNotificationPermissionGranted = hasNotificationPermission
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState()
    )
}