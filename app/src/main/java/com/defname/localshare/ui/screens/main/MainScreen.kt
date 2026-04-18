package com.defname.localshare.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.defname.localshare.QrCodeDialog
import com.defname.localshare.R
import com.defname.localshare.Screen
import com.defname.localshare.ServerRepository
import com.defname.localshare.StartServerButton
import com.defname.localshare.ui.screens.info.InfoScreen
import com.defname.localshare.ui.screens.logs.LogsScreen
import com.defname.localshare.ui.screens.main.components.MainMenu
import com.defname.localshare.ui.screens.servercontrol.ServerControlScreen
import com.defname.localshare.ui.screens.settings.SettingsScreen
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    viewModel: MainViewModel
) {
    val state = viewModel.state.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val scope = rememberCoroutineScope()

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)


    ModalNavigationDrawer(
        drawerState = drawerState,
        // Sidebar nur auf dem Main Screen per Wischgeste erlauben
        gesturesEnabled = state.value == Screen.Main.route,
        drawerContent = {
            MainMenu(
                drawerItems = state.value.drawerItems,
                onItemClick = {}
            )
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(state.value.title)
                    },
                    windowInsets = WindowInsets.statusBars,
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant,
                        titleContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    ),
                    navigationIcon = {
                        if (currentRoute == Screen.Main.route) {
                            IconButton(onClick = { scope.launch { drawerState.open() } }) {
                                Icon(Icons.Default.Menu,
                                    stringResource(R.string.main_nav_icon_descr_menu)
                                )
                            }
                        } else {
                            IconButton(onClick = { navController.popBackStack() }) {
                                Icon(Icons.Default.ArrowBack,
                                    stringResource(R.string.main_nav_icon_descr_back)
                                )
                            }
                        }
                    },
                    actions = {
                        when (currentRoute) {
                            Screen.Logs.route -> {
                                if (state.logs.isNotEmpty()) {
                                    IconButton(onClick = { ServerRepository.clearLogs() }) {
                                        Icon(Icons.Default.Clear,
                                            stringResource(R.string.main_action_button_descr_clear)
                                        )
                                    }
                                }
                            }
                            Screen.Main.route -> {
                                if (state.isRunning) {
                                    IconButton(onClick = { showQrCodeDialog = true}) {
                                        Icon(Icons.Default.QrCode2,
                                            stringResource(R.string.main_action_button_descr_qr_code)
                                        )
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                )
            },
            bottomBar = {
                StartServerButton()
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
                        ServerControlScreen(navController)
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
