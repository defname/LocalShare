package com.defname.localshare.domain.repository

import com.defname.localshare.domain.model.Settings
import kotlinx.coroutines.flow.Flow

interface SettingsRepository {
    val settingsFlow: Flow<Settings>

    suspend fun setToken(token: String)
    suspend fun setPort(port: Int)
    suspend fun setServerIp(serverIp: String)
    suspend fun setServerIdleTimeoutSeconds(serverIdleTimeout: Int)
    suspend fun setRequireApproval(requireApproval: Boolean)
    suspend fun setApprovalTimeoutSeconds(approvalTimeout: Int)
    suspend fun setWhitelistEntryTTLSeconds(whitelistEntryTTL: Int)
    suspend fun setClearFileListOnShareIntent(clearFileListOnShareIntent: Boolean)
}