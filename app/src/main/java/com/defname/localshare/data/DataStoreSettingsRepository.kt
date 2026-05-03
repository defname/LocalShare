// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.defname.localshare.domain.model.Settings
import com.defname.localshare.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DataStoreSettingsRepository(
    private val store: DataStore<Preferences>
) : SettingsRepository {
    private object Keys {
        val TOKEN = stringPreferencesKey("token")
        val PORT = intPreferencesKey("port")
        val SERVER_IP = stringPreferencesKey("serverIp")
        val SERVER_IDLE_TIMEOUT_SECONDS = intPreferencesKey("serverIdleTimeout")
        val REQUIRE_APPROVAL = booleanPreferencesKey("requireApproval")
        val APPROVAL_TIMEOUT_SECONDS = intPreferencesKey("approvalTimeout")
        val WHITELIST_ENTRY_TTL_SECONDS = intPreferencesKey("whitelistEntryTTL")
        val CLEAR_FILE_LIST_ON_SHARE_INTENT = booleanPreferencesKey("clearFileListOnShareIntent")
        val SSE_HEARTBEAT_PERIOD_SECONDS = intPreferencesKey("sseHeartbeatPeriod")
        val SHOW_WELCOME_MESSAGE = booleanPreferencesKey("showWelcomeMessage")
        val REGENERATE_TOKEN_AT_APP_START = booleanPreferencesKey("regenerateTokenAtAppStart")
    }

    override val settingsFlow: Flow<Settings> = store.data
        .map {
            Settings(
                token = it[Keys.TOKEN] ?: "",
                serverPort = it[Keys.PORT] ?: 8080,
                serverIp = it[Keys.SERVER_IP] ?: "0.0.0.0",
                serverIdleTimeoutSeconds = it[Keys.SERVER_IDLE_TIMEOUT_SECONDS] ?: 30,
                requireApproval = it[Keys.REQUIRE_APPROVAL] ?: true,
                approvalTimeoutSeconds = it[Keys.APPROVAL_TIMEOUT_SECONDS] ?: 30,
                whitelistEntryTTLSeconds = it[Keys.WHITELIST_ENTRY_TTL_SECONDS] ?: (60 * 60),
                clearFileListOnShareIntent = it[Keys.CLEAR_FILE_LIST_ON_SHARE_INTENT] ?: false,
                sseHeartbeatPeriodSeconds = it[Keys.SSE_HEARTBEAT_PERIOD_SECONDS] ?: 1,
                showWelcomeMessage = it[Keys.SHOW_WELCOME_MESSAGE] ?: true,
                regenerateTokenAtAppStart = it[Keys.REGENERATE_TOKEN_AT_APP_START] ?: true
            )
        }

    override suspend fun setToken(token: String) { store.edit { it[Keys.TOKEN] = token } }
    override suspend fun setPort(port: Int) { store.edit { it[Keys.PORT] = port } }
    override suspend fun setServerIp(serverIp: String) { store.edit { it[Keys.SERVER_IP] = serverIp } }
    override suspend fun setServerIdleTimeoutSeconds(serverIdleTimeout: Int) { store.edit { it[Keys.SERVER_IDLE_TIMEOUT_SECONDS] = serverIdleTimeout } }
    override suspend fun setRequireApproval(requireApproval: Boolean) { store.edit { it[Keys.REQUIRE_APPROVAL] = requireApproval } }
    override suspend fun setApprovalTimeoutSeconds(approvalTimeout: Int) { store.edit { it[Keys.APPROVAL_TIMEOUT_SECONDS] = approvalTimeout } }
    override suspend fun setWhitelistEntryTTLSeconds(whitelistEntryTTL: Int) { store.edit { it[Keys.WHITELIST_ENTRY_TTL_SECONDS] = whitelistEntryTTL } }
    override suspend fun setClearFileListOnShareIntent(clearFileListOnShareIntent: Boolean) { store.edit { it[Keys.CLEAR_FILE_LIST_ON_SHARE_INTENT] = clearFileListOnShareIntent } }
    override suspend fun setHeartbeatPeriodSeconds(heartbeatPeriod: Int) { store.edit { it[Keys.SSE_HEARTBEAT_PERIOD_SECONDS] = heartbeatPeriod } }
    override suspend fun setShowWelcomeMessage(showWelcomeMessage: Boolean) { store.edit { it[Keys.SHOW_WELCOME_MESSAGE] = showWelcomeMessage } }
    override suspend fun setRegenerateTokenAtAppStart(regenerateTokenAtAppStart: Boolean) { store.edit { it[Keys.REGENERATE_TOKEN_AT_APP_START] = regenerateTokenAtAppStart } }
}