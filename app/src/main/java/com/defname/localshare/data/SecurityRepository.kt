/*
 * LocalShare - Share files locally
 * Copyright (C) 2026 defname
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

package com.defname.localshare.data

import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.model.WhiteListEntry
import com.defname.localshare.domain.model.isStillValid
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

class SecurityRepository(
    private val settingsRepository: SettingsRepository,
    private val repositoryScope: CoroutineScope
) {
    private val settings = settingsRepository.settingsFlow
        .stateIn(repositoryScope, SharingStarted.Eagerly, Settings())


    private val _blacklist = MutableStateFlow(setOf<String>())
    val blacklist = _blacklist.asStateFlow()

    private val _whitelist = MutableStateFlow(listOf<WhiteListEntry>())
    val whitelist = _whitelist.asStateFlow()

    init {
        initializeToken()
        startCleaningJob()
    }

    private fun initializeToken() {
        repositoryScope.launch {
            settingsRepository.settingsFlow.collect { currentSettings ->
                if (currentSettings.token.isEmpty()) {
                    settingsRepository.setToken(generateRandomToken())
                }
            }
        }
    }

    private fun startCleaningJob() {
        repositoryScope.launch {
            while (isActive) {
                delay(60_000)
                cleanupExpiredWhitelistEntries()
            }
        }
    }

    private fun cleanupExpiredWhitelistEntries() {
        val now = System.currentTimeMillis()
        _whitelist.update { it.filter { entry -> entry.isStillValid(settings.value.whitelistEntryTTLSeconds, now) } }
    }

    fun checkToken(providedToken: String): Boolean { return providedToken == settings.value.token }

    fun isApprovalRequired(): Boolean { return settings.value.requireApproval }

    fun addToBlacklist(ip: String) {
        removeFromWhitelist(ip)
        _blacklist.update { it + ip }
    }
    fun removeFromBlacklist(ip: String) { _blacklist.update { it - ip } }
    fun isBlacklisted(ip: String): Boolean { return _blacklist.value.contains(ip) }

    fun addToWhitelist(ip: String) { _whitelist.update { it + WhiteListEntry(ip, System.currentTimeMillis()) } }
    fun removeFromWhitelist(ip: String) { _whitelist.update { it.filter { entry -> entry.ip != ip } } }
    fun isWhitelisted(ip: String): Boolean { return _whitelist.value.any { it.ip == ip && it.isStillValid(settings.value.whitelistEntryTTLSeconds)  } }
    
    companion object {
        fun generateRandomToken(): String {
            return UUID.randomUUID().toString().take(6)
        }
    }
}