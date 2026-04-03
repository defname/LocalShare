/*
 * LocalShare - Share files locally
 * Copyright (C) 2024 defname
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.defname.localshare

import java.net.NetworkInterface

data class NetworkInfo(
    val ip: String,
    val interfaceName: String
)

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
        "wlan", "wlp", "swlan",      // Standard WiFi & Modern Linux WiFi (wlp...)
        "ap", "softap", "wigig",     // Hotspots & High-Speed WiFi
        "eth", "enp", "eno", "ens",  // Ethernet (Legacy, PCI, Onboard, Slot)
        "p2p",                       // WiFi Direct
        "rndis", "usb", "lan",       // USB-Tethering & LAN-Adapter
        "rmnet", "ccmni", "ppp",     // Mobilfunk (verschiedene Hersteller)
        "wwan"                       // Wireless Wide Area Network
    )
    for (prefix in priorityPrefixes) {
        val found = addresses.find { it.interfaceName.lowercase().startsWith(prefix) }
        if (found != null) return found.ip
    }

    return addresses.first().ip
}
