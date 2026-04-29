// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.data

import com.defname.localshare.domain.model.NetworkInfo
import java.net.NetworkInterface

class NetworkInfoProvider {
    fun getLocalIpAddresses(): List<NetworkInfo> {
        val addresses = mutableListOf<NetworkInfo>()
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces()
            while (interfaces.hasMoreElements()) {
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