package com.defname.localshare.domain.model

import com.defname.localshare.ConnectionRequest
import com.defname.localshare.FileInfo
import com.defname.localshare.WhiteListEntry
import java.util.UUID

data class ServiceState(
    val fileList: List<FileInfo> = emptyList(),
    val token: String = UUID.randomUUID().toString(),
    val isRunning: Boolean = false,
    val activeClients: List<String> = emptyList(),
    val blacklist: Set<String> = emptySet(),
    val whitelist: List<WhiteListEntry> = emptyList(),
    val whiteListEntryTTLSeconds: Int = 60 * 60,
    val pendingRequests: List<ConnectionRequest> = emptyList(),
    val port: Int = 8080,
    val idleTimeoutSeconds: Int = 300,
    val requireApproval: Boolean = true,
    val approvalTimeoutSeconds: Int = 30,
    val keepScreenOn: Boolean = true,
    val clearFileListOnSendIntent: Boolean = false
)