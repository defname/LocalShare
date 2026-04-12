/*
 * LocalShare - Share files locally
 * Copyright (C) 2024 defname
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.defname.localshare

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.IntentCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.defname.localshare.ui.screens.logs.LogsScreen
import com.defname.localshare.ui.screens.servercontrol.ServerControlScreen
import com.defname.localshare.ui.screens.settings.SettingsScreen
import com.defname.localshare.ui.theme.LocalShareTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()

        super.onCreate(savedInstanceState)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            splashScreen.setOnExitAnimationListener { splashScreenView ->
                val iconView = splashScreenView

                iconView.animate()
                    .scaleX(3f)
                    .scaleY(3f)
                    .alpha(0f)
                    .setDuration(300L)
                    .withEndAction { splashScreenView.remove() }
                    .start()
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }

        // Prüfen, ob die App durch eine "Teilen"-Aktion gestartet wurde
        if (intent != null) {
            onNewIntent(intent)
        }

        ServerRepository.init(applicationContext)

        enableEdgeToEdge()
        setContent {
            LocalShareTheme {
                MainScreen()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent)

        if (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_SEND_MULTIPLE) {
            if (ServerRepository.state.value.clearFileListOnSendIntent) {
                ServerRepository.clearFiles()
            }
        }

        if (intent.action == Intent.ACTION_SEND) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                val uri = IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                uri?.let { ServerRepository.addFile(it) }
            }
        } else if (intent.action == Intent.ACTION_SEND_MULTIPLE) {
            if (intent.hasExtra(Intent.EXTRA_STREAM)) {
                val uris = IntentCompat.getParcelableArrayListExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)
                uris?.let { ServerRepository.addFiles(it) }
            }
        }
    }

}

fun hasNotificationPermission(context: Context): Boolean {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    } else {
        true
    }
}

fun requestNotificationPermission(context: Context, launcher: ActivityResultLauncher<String>) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        val status = ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
        if (status != PackageManager.PERMISSION_GRANTED) {
            launcher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            // Optional: Permission already granted, perform the notification logic
        }
    } else {
        // API < 33: Notifications are enabled by default, no runtime permission needed
    }
}


sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Main : Screen("main", "Server", Icons.Default.Send)
    object Logs : Screen("logs", "Logs", Icons.Default.List)
    object Settings : Screen("settings", "Settings", Icons.Default.Settings)
    object Info : Screen("info", "Info", Icons.Default.Info)
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val state by ServerRepository.state.collectAsState()
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Liste der Screens für die Sidebar
    val drawerItems = listOf(Screen.Main, Screen.Logs, Screen.Settings, Screen.Info)

    var showQrCodeDialog by remember { mutableStateOf<Boolean>(false) }

    if (showQrCodeDialog) {
        QrCodeDialog(
            onDismiss = { showQrCodeDialog = false }
        )
    }


    ModalNavigationDrawer(
        drawerState = drawerState,
        // Sidebar nur auf dem Main Screen per Wischgeste erlauben
        gesturesEnabled = currentRoute == Screen.Main.route,
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.fillMaxWidth(0.75f)
            ) {
                Spacer(Modifier.height(12.dp))
                Text(
                    stringResource(R.string.app_name),
                    modifier = Modifier.padding(16.dp),
                    style = MaterialTheme.typography.headlineSmall
                )
                HorizontalDivider()
                Spacer(Modifier.height(12.dp))

                drawerItems.forEach { screen ->
                    NavigationDrawerItem(
                        icon = { Icon(screen.icon, contentDescription = null) },
                        label = { Text(screen.label) },
                        selected = currentRoute == screen.route,
                        onClick = {
                            // Menü schließen und navigieren
                            scope.launch { drawerState.close() }
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Main.route) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
                    )
                }
            }
        }
    ) {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            when (currentRoute) {
                                Screen.Info.route -> stringResource(R.string.main_title_info)
                                Screen.Logs.route -> stringResource(R.string.main_title_logs)
                                Screen.Settings.route -> stringResource(R.string.main_title_settings)
                                else -> stringResource(R.string.app_name)
                            }
                        )
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


@Composable
fun StartServerButton() {
    val state by ServerRepository.state.collectAsState()
    val context = LocalContext.current
    var permissionGranted by remember { mutableStateOf(hasNotificationPermission(context)) }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                permissionGranted = hasNotificationPermission(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult (
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        permissionGranted = isGranted
    }


    Surface(modifier = Modifier
        .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
            if (!permissionGranted) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { requestNotificationPermission(context, permissionLauncher) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                ) {
                    Text(stringResource(R.string.main_server_button_request_notification_permission))
                }
            } else {
                if (!state.isRunning) {
                    // Button zum Starten
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { ServerRepository.startServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryFixed,
                            contentColor = MaterialTheme.colorScheme.onPrimaryFixed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(R.string.main_server_button_start))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.main_server_button_start))
                    }
                } else {
                    // Button zum Stoppen (wenn er schon läuft)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { ServerRepository.stopServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = stringResource(
                            R.string.main_server_button_stop
                        ))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.main_server_button_stop))
                    }
                }
            }
        }
    }
}
