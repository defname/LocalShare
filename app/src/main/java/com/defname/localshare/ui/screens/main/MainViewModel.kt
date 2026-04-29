// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.main

import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.NetworkInfoProvider
import com.defname.localshare.data.PermissionRepository
import com.defname.localshare.data.RuntimeData
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.ConnectionLogEntry
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.domain.usecase.ManageServiceUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private data class QrState(
    val showQrDialog: Boolean,
    val qrForStream: Boolean
)

class MainViewModel(
    private val logsRepository: ConnectionLogsRepository,
    private val serviceRepository: ServiceRepository,
    private val settingsRepository: SettingsRepository,
    private val permissionRepository: PermissionRepository,
    private val manageServiceUseCase: ManageServiceUseCase,
    private val networkInfoProvider: NetworkInfoProvider
) : ViewModel() {

    init {
        viewModelScope.launch {
            val showWelcomeMessage = settingsRepository.settingsFlow.first().showWelcomeMessage
            _welcomeMessageVisible.update { showWelcomeMessage }
        }
    }

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

    fun openInBrowser(context: Context, uri: String) {
        val intent = Intent(Intent.ACTION_VIEW, uri.toUri())
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    suspend fun onDismissWelcomeMessage(showNextTime: Boolean = false) {
        _welcomeMessageVisible.update { false }
        settingsRepository.setShowWelcomeMessage(showNextTime)
    }

    private val _qrState = MutableStateFlow(QrState(false, false))
    private val _welcomeMessageVisible = MutableStateFlow(true)

    private fun getQrLink(qrForStream: Boolean, token: String, ip: String, port: Int): String {
        return if (qrForStream) {
            "http://$ip:$port/$token"
        } else {
            "http://$ip:$port/$token?download"
        }
    }

    val state = combine(
        _qrState,
        settingsRepository.settingsFlow,
        logsRepository.connections,
        serviceRepository.runtimeState,
        permissionRepository.hasNotificationPermission,
        _welcomeMessageVisible
    ) { arr ->
        var qrState = arr[0] as QrState
        var settings = arr[1] as Settings
        var logs = arr[2] as List<ConnectionLogEntry>
        var runtimeState = arr[3] as RuntimeData
        var hasNotificationPermission = arr[4] as Boolean
        var welcomeMessageVisible = arr[5] as Boolean

        var qrCodeIp = settings.serverIp
        if (qrCodeIp == "0.0.0.0") {
            val addresses = networkInfoProvider.getLocalIpAddresses()
            qrCodeIp = networkInfoProvider.getSmartDefaultIp(addresses)
        }

        val helpLink = "http://$qrCodeIp:${settings.serverPort}/${settings.token}/static/docs/index.html"

        MainState(
            showQrDialog = qrState.showQrDialog,
            qrForStream = qrState.qrForStream,
            qrFullLink = getQrLink(
                qrState.qrForStream,
                settings.token,
                qrCodeIp,
                settings.serverPort
            ),
            helpLink = helpLink,
            hasLogs = logs.isNotEmpty(),
            serverState = runtimeState.serviceState,
            isNotificationPermissionGranted = hasNotificationPermission,
            welcomeMessageVisible = welcomeMessageVisible
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = MainState()
    )
}