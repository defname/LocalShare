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
