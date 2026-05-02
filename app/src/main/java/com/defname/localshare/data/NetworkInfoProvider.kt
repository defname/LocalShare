// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import com.defname.localshare.domain.model.NetworkInfo
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.onStart
import java.net.NetworkInterface

class NetworkInfoProvider(context: Context) {
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    val localIpAddresses: Flow<List<NetworkInfo>> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(getLocalIpAddresses())
            }

            override fun onLost(network: Network) {
                trySend(getLocalIpAddresses())
            }

            override fun onLinkPropertiesChanged(network: Network, linkProperties: LinkProperties) {
                trySend(getLocalIpAddresses())
            }
        }

        connectivityManager.registerDefaultNetworkCallback(callback)
        
        // Initialer Wert
        trySend(getLocalIpAddresses())

        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.onStart { emit(getLocalIpAddresses()) }.distinctUntilChanged()

    fun getLocalIpAddresses(): List<NetworkInfo> {
        val addresses = mutableListOf<NetworkInfo>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces?.hasMoreElements() == true) {
                val iface = interfaces.nextElement()

                if (!iface.isUp || iface.isLoopback) continue

                val iFaceAddresses = iface.inetAddresses
                for (addr in iFaceAddresses) {
                    if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                        addresses.add(NetworkInfo(addr.hostAddress!!, iface.name))
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return addresses
    }

    fun getSmartDefaultIp(addresses: List<NetworkInfo>): String {
        if (addresses.isEmpty()) return "127.0.0.1"

        val priorityPrefixes = listOf(
            "wlan", "wlp", "swlan",      // Wi-Fi
            "ap", "softap", "wigig",     // Hotspot, High-Speed Wi-Fi
            "eth", "enp", "eno", "ens",  // Ethernet
            "p2p",                       // WiFi Direct
            "rndis", "usb", "lan",       // USB-Tethering & LAN-Adapter
            "rmnet", "ccmni", "ppp",     // mobil network
            "wwan"                       // Wireless Wide Area Network
        )
        for (prefix in priorityPrefixes) {
            val found = addresses.find { it.interfaceName.lowercase().startsWith(prefix) }
            if (found != null) return found.ip
        }

        return addresses.first().ip
    }
}
