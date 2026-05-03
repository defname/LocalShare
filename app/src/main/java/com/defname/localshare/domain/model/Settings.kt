// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.model

data class Settings(
    val token: String = "",
    val serverPort: Int = 8080,
    val serverIp: String = "0.0.0.0",
    val serverIdleTimeoutSeconds: Int = 30,
    val requireApproval: Boolean = true,
    val approvalTimeoutSeconds: Int = 30,
    val whitelistEntryTTLSeconds: Int = 60 * 60,
    val clearFileListOnShareIntent: Boolean = false,
    val sseHeartbeatPeriodSeconds: Int = 1,
    val showWelcomeMessage: Boolean = true,
    val regenerateTokenAtAppStart: Boolean = true,
)