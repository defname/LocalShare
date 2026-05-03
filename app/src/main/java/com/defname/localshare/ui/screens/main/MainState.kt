// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentPaste
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector
import com.defname.localshare.data.RuntimeState


data class MainState(
    val navigationItems: List<NavigationItem> = listOf(
        Screen.Main,
        Screen.Files,
        Screen.SharedContent,
        Screen.Logs,
        Screen.Settings,
        Screen.Info
    ).map {
        NavigationItem(it.route, it.label, it.icon)
    },

    val helpLink: String = "",
    val welcomeMessageVisible: Boolean = false,
    val serverState: RuntimeState = RuntimeState.STOPPED
)

class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Main : Screen("main", "Overview", Icons.Default.Send)
    object Files : Screen("files", "Shared Files", Icons.Default.ListAlt)
    object SharedContent : Screen("sharedcontent", "Shared Text", Icons.Default.ContentPaste)
    object Logs : Screen("logs", "Logs", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Info : Screen("info", "Info", Icons.Default.Info)

    companion object {
        fun fromRoute(route: String?): Screen {
            return when (route?.substringBefore("/")) {
                Main.route -> Main
                Files.route -> Files
                SharedContent.route -> SharedContent
                Settings.route -> Settings
                Logs.route -> Logs
                Info.route -> Info
                else -> Main
            }
        }
    }
}