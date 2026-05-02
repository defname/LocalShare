package com.defname.localshare.data

import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

data class ServerUrls(
    val serverUrls: List<String>,
    val defaultServerUrl: String
)

class ServerUrlProvider(
    val settingsRepository: SettingsRepository,
    val networkInfoProvider: NetworkInfoProvider
) {
    val serverUrls: Flow<ServerUrls> = combine(
        settingsRepository.settingsFlow,
        networkInfoProvider.localIpAddresses
    ) { settings, localIpAddresses ->
        val token = settings.token
        val serverPort = settings.serverPort

        if (settings.serverIp != "0.0.0.0") {
            val serverIp = networkInfoProvider.getSmartDefaultIp(localIpAddresses)
            val serverUrl = getServerUrl(serverIp, serverPort, token)
            ServerUrls(
                listOf(serverUrl),
                serverUrl
            )
        }
        else {
            val defaultServerIp = networkInfoProvider.getSmartDefaultIp(localIpAddresses)
            val defaultServerUrl = getServerUrl(defaultServerIp, serverPort, token)
            ServerUrls(
                localIpAddresses.map {
                    getServerUrl(it.ip, serverPort, token)
                },
                defaultServerUrl
            )
        }
    }

    fun getServerUrl(serverIp: String, serverPort: Int, token: String): String {
        return "http://$serverIp:$serverPort/$token"
    }

    fun getFileUrl(serverIp: String, serverPort: Int, token: String,fileId: String, download: Boolean = false): String {
        return "${getServerUrl(serverIp, serverPort, token)}/file/$fileId${if (download) "?download" else ""}"
    }

    fun getZipUrl(serverIp: String, serverPort: Int, token: String): String {
        return "${getServerUrl(serverIp, serverPort, token)}?download"
    }
}