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
            val serverAddress = formatAddressForUrl(settings.serverIp, settings.isServerIpv6)
            val serverUrl = getServerUrl(serverAddress, serverPort, token)
            ServerUrls(
                listOf(serverUrl),
                serverUrl
            )
        }
        else {
            val defaultServerAddress = networkInfoProvider.getSmartDefaultIp(localIpAddresses)
            val defaultServerAddressString = formatAddressForUrl(defaultServerAddress.address, defaultServerAddress.isIpv6Addr)
            val defaultServerUrl = getServerUrl(defaultServerAddressString, serverPort, token)
            ServerUrls(
                localIpAddresses.map {
                    getServerUrl(it.address, serverPort, token)
                },
                defaultServerUrl
            )
        }
    }

    fun formatAddressForUrl(address: String, isIpv6: Boolean) = if (isIpv6) "[$address]" else address

    fun getServerUrl(serverAddress: String, serverPort: Int, token: String): String {
        return "http://$serverAddress:$serverPort/$token"
    }

    fun getFileUrl(serverAddress: String, serverPort: Int, token: String, fileId: String, download: Boolean = false): String {
        return "${getServerUrl(serverAddress, serverPort, token)}/file/$fileId${if (download) "?download" else ""}"
    }

    fun getZipUrl(serverAddress: String, serverPort: Int, token: String): String {
        return "${getServerUrl(serverAddress, serverPort, token)}?download"
    }
}