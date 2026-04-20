package com.defname.localshare.ui.screens.main

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.ui.components.QRDialog
import com.defname.localshare.ui.screens.info.InfoScreen
import com.defname.localshare.ui.screens.logs.LogsScreen
import com.defname.localshare.ui.screens.main.components.MainMenu
import com.defname.localshare.ui.screens.main.components.MainTopBar
import com.defname.localshare.ui.screens.main.components.StartServerButton
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

    val currentScreen = Screen.fromRoute(currentRoute)
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.updatePermissionStatus()
    }

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
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                MainTopBar(
                    currentScreen = currentScreen,
                    isServerRunning = state.serverState == RuntimeState.RUNNING,
                    hasLogs = state.hasLogs,
                    onNavigationClick = { scope.launch { drawerState.open() } },
                    onQrClick = { viewModel.showQrDialog() },
                    onClearLogs = { viewModel.clearLogs() }
                )
            },
            bottomBar = {
                StartServerButton(
                    serverState = state.serverState,
                    hasNotificationPermission = state.isNotificationPermissionGranted,
                    requestNotificationPermission = {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                    },
                    startServer = { viewModel.startServer() },
                    stopServer = { viewModel.stopServer() }
                )
            }
        ) { innerPadding ->
            Box (
                modifier = Modifier
                    .padding(innerPadding)
                    .padding(horizontal = 16.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
            ) {
                NavHost(
                    navController = navController,
                    startDestination = Screen.Main.route,
                ) {
                    composable(Screen.Main.route) {
                        ServerControlScreen(
                            onNavigateToLogs = { scope.launch { navController.navigate(Screen.Logs.route) } }
                        )
                    }
                    composable(Screen.Settings.route) {
                        SettingsScreen()
                    }
                    composable(Screen.Logs.route) {
                        LogsScreen()
                    }
                    composable(Screen.Info.route) {
                        InfoScreen()
                    }
                }
            }
        }
    }
}
