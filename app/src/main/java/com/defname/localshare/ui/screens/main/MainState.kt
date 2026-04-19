package com.defname.localshare.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector


data class MainState(
    val navigationItems: List<NavigationItem> = listOf(
        Screen.Main,
        Screen.Logs,
        Screen.Settings,
        Screen.Info
    ).map {
        NavigationItem(it.route, it.label, it.icon)
    },

    val isServerRunning: Boolean = false,
    val hasLogs: Boolean = false,
    val showQrDialog: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false
)

class NavigationItem(
    val route: String,
    val label: String,
    val icon: ImageVector
)

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Main : Screen("main", "Server", Icons.Default.Send)
    object Logs : Screen("logs", "Logs", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Info : Screen("info", "Info", Icons.Default.Info)

    companion object {
        fun fromRoute(route: String?): Screen {
            return when (route?.substringBefore("/")) {
                Main.route -> Main
                Settings.route -> Settings
                Logs.route -> Logs
                Info.route -> Info
                else -> Main
            }
        }
    }
}