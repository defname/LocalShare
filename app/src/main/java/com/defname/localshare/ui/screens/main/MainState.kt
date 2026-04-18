package com.defname.localshare.ui.screens.main

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.graphics.vector.ImageVector


data class MainState(
    val title: String = DrawerItem.Main.label,
    val drawerItems: List<DrawerItem> = listOf(
        DrawerItem.Main,
        DrawerItem.Logs,
        DrawerItem.Settings,
        DrawerItem.Info
    ),

    val isServerRunning: Boolean = false,
    val hasLogs: Boolean = false,
    val showQrDialog: Boolean = false,
    val isNotificationPermissionGranted: Boolean = false
)

sealed class DrawerItem(val route: String, val label: String, val icon: ImageVector) {
    object Main : DrawerItem("main", "Server", Icons.Default.Send)
    object Logs : DrawerItem("logs", "Logs", Icons.Default.List)
    object Settings : DrawerItem("settings", "Settings", Icons.Default.Settings)
    object Info : DrawerItem("info", "Info", Icons.Default.Info)
}