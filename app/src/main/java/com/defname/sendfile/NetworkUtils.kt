package com.defname.sendfile

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
            val iFaceAddresses = iface.inetAddresses
            for (addr in iFaceAddresses) {
                if (!addr.isLoopbackAddress && addr is java.net.Inet4Address) {
                    addresses.add(NetworkInfo(addr.hostAddress, iface.name))
                }
            }
        }
    } catch (e: Exception) {
        e.printStackTrace()
    }
    return addresses
}