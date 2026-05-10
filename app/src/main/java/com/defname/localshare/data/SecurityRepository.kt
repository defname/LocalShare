// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.security.SecureRandom

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
            val settings = settingsRepository.settingsFlow.first()
            if (settings.token.isEmpty() || settings.regenerateTokenAtAppStart) {
                settingsRepository.setToken(generateRandomToken())
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

    fun checkToken(providedToken: String): Boolean {
        if (!validateTokenFormat(providedToken)) return false
        return providedToken == settings.value.token
    }

    fun isApprovalRequired(): Boolean = settings.value.requireApproval

    fun addToBlacklist(ip: String) {
        removeFromWhitelist(ip)
        _blacklist.update { it + ip }
    }
    fun removeFromBlacklist(ip: String) { _blacklist.update { it - ip } }
    fun isBlacklisted(ip: String): Boolean = _blacklist.value.contains(ip)

    fun addToWhitelist(ip: String) { _whitelist.update { it + WhiteListEntry(ip, System.currentTimeMillis()) } }
    fun removeFromWhitelist(ip: String) { _whitelist.update { it.filter { entry -> entry.ip != ip } } }
    fun isWhitelisted(ip: String): Boolean = _whitelist.value.any { it.ip == ip && it.isStillValid(settings.value.whitelistEntryTTLSeconds) }

    companion object {
        private val BASE36 = "0123456789abcdefghijklmnopqrstuvwxyz"
        private val rng = SecureRandom()

        /**
         * Token format: XXXX-XXXX-XXXX-CCC
         * 12 random base-36 chars (groups of 4) + 3 checksum chars.
         * Checksum: c0 = sum(group1) mod 36, c1 = sum(group2) mod 36, c2 = sum(group3) mod 36.
         */
        fun generateRandomToken(): String {
            val random = (0 until 12).map { BASE36[rng.nextInt(36)] }

            fun charVal(c: Char) = BASE36.indexOf(c)
            fun valChar(v: Int) = BASE36[v % 36]

            val c0 = valChar((0 until 4).sumOf { charVal(random[it]) })
            val c1 = valChar((4 until 8).sumOf { charVal(random[it]) })
            val c2 = valChar((8 until 12).sumOf { charVal(random[it]) })

            val p1 = random.subList(0, 4).joinToString("")
            val p2 = random.subList(4, 8).joinToString("")
            val p3 = random.subList(8, 12).joinToString("")

            return "$p1-$p2-$p3-$c0$c1$c2"
        }

        fun validateTokenFormat(token: String): Boolean {
            val parts = token.split("-")
            if (parts.size != 4) return false
            if (parts[0].length != 4 || parts[1].length != 4 || parts[2].length != 4 || parts[3].length != 3) return false
            if (token.any { it != '-' && it !in BASE36 }) return false

            val chars = (parts[0] + parts[1] + parts[2]).toList()
            fun charVal(c: Char) = BASE36.indexOf(c)
            fun valChar(v: Int) = BASE36[v % 36]

            val expectedC0 = valChar((0 until 4).sumOf { charVal(chars[it]) })
            val expectedC1 = valChar((4 until 8).sumOf { charVal(chars[it]) })
            val expectedC2 = valChar((8 until 12).sumOf { charVal(chars[it]) })

            return parts[3][0] == expectedC0 && parts[3][1] == expectedC1 && parts[3][2] == expectedC2
        }
    }
}
