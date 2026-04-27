package com.defname.localshare.ui.screens.main

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.defname.localshare.ui.components.QRDialog
import com.defname.localshare.ui.screens.files.FilesScreen
import com.defname.localshare.ui.screens.files.SharedContentScreen
import com.defname.localshare.ui.screens.info.InfoScreen
import com.defname.localshare.ui.screens.logs.LogsScreen
import com.defname.localshare.ui.screens.main.components.MainMenu
import com.defname.localshare.ui.screens.servercontrol.ServerControlScreen
import com.defname.localshare.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel = koinViewModel()
) {
    val state by viewModel.state.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    if (state.showQrDialog) {
        QRDialog(state.qrFullLink, state.qrForStream, { viewModel.toggleQrForStream() }, { viewModel.hideQrDialog() })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        // Sidebar nur auf dem Main Screen per Wischgeste erlauben
        gesturesEnabled = currentRoute == Screen.Main.route,
        drawerContent = {
            MainMenu(
                drawerItems = state.navigationItems,
                onItemClick = { route ->
                    scope.launch {
                        drawerState.close()
                        navController.navigate(route) {
                            popUpTo(Screen.Main.route) { saveState = true }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                }
            )
        }
    ) {
        val onOpenDrawer: () -> Unit = {
            scope.launch { drawerState.open() }
        }

        NavHost(
            navController = navController,
            startDestination = Screen.Main.route,
            modifier = Modifier.fillMaxSize()
        ) {
            composable(Screen.Main.route) {
                ServerControlScreen(
                    onOpenDrawer = onOpenDrawer,
                    onNavigateToLogs = { navController.navigate(Screen.Logs.route) },
                    onShowQr = { viewModel.showQrDialog() },
                    serverState = state.serverState,
                    onStartServer = { viewModel.startServer() },
                    onStopServer = { viewModel.stopServer() }
                )
            }
            composable(Screen.Files.route) {
                FilesScreen(onOpenDrawer = onOpenDrawer)
            }
            composable(Screen.SharedContent.route) {
                SharedContentScreen(onOpenDrawer = onOpenDrawer)
            }
            composable(Screen.Settings.route) {
                SettingsScreen(
                    onOpenDrawer = onOpenDrawer,
                    serverState = state.serverState,
                    onStartServer = { viewModel.startServer() },
                    onStopServer = { viewModel.stopServer() }
                )
            }
            composable(Screen.Logs.route) {
                LogsScreen(onOpenDrawer = onOpenDrawer)
            }
            composable(Screen.Info.route) {
                InfoScreen(onOpenDrawer = onOpenDrawer)
            }
        }
    }
}
