// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.model

data class NetworkInfo(
    val address: String,
    val interfaceName: String,
    val priority: Int,
    val isIpv6Addr: Boolean = false
)

val allNetworksNetworkInfo = NetworkInfo(
    address = "0.0.0.0",
    interfaceName = "all",
    isIpv6Addr = false,
    priority = -1,
)