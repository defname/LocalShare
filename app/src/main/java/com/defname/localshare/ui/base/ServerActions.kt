package com.defname.localshare.ui.base

import com.defname.localshare.ServerRepository

interface ServerAccessControlActions {
    fun addToBlacklist(ip: String) = ServerRepository.addToBlacklist(ip)
    fun removeFromBlacklist(ip: String) = ServerRepository.removeFromBlacklist(ip)
    fun removeFromWhitelist(ip: String) = ServerRepository.removeFromWhitelist(ip)
}