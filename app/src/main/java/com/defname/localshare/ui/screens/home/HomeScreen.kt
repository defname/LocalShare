// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.home

import android.Manifest
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.defname.localshare.ui.components.HintCard
import com.defname.localshare.ui.components.LogListEntry
import com.defname.localshare.ui.components.QrCodeDialog
import com.defname.localshare.ui.screens.home.components.AdvancedSettingsSection
import com.defname.localshare.ui.screens.home.components.FileCarousel
import com.defname.localshare.ui.screens.home.components.SharingHeroSection
import com.defname.localshare.ui.screens.home.components.StartServerButton
import com.defname.localshare.ui.theme.LocalShareTheme
import org.koin.androidx.compose.koinViewModel

fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit,
    onNavigateToLogs: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val filePickerDialog = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addFiles(uris)
    }

    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { _ ->
        viewModel.updatePermissionStatus()
    }

    if (state.qrCodeDialogVisible) {
        QrCodeDialog(
            url = state.qrCodeUrl,
            qrCodeBitmap = state.qrCodeBitmap,
            onDismiss = { viewModel.onCloseQrCodeDialog() }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("LocalShare") },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
                actions = { },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        bottomBar = {
            StartServerButton(
                serverState = state.serviceState,
                hasNotificationPermission = state.hasNotificationPermission,
                requestNotificationPermission = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                },
                startServer = { viewModel.onStartServer() },
                stopServer = { viewModel.onStopServer() },
                modifier = Modifier.navigationBarsPadding()
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // --- Hero Section ---
            SharingHeroSection(state, viewModel, context)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Shared Files Section ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    "Shared Files",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                if (state.fileList.isNotEmpty()) {
                    @OptIn(ExperimentalMaterial3Api::class)
                    CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                        IconButton(
                            onClick = { viewModel.clearFiles() },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = "Clear All",
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            if (state.fileList.isNotEmpty()) {
                Text(
                    "${state.fileList.size} files" + " • " + Formatter.formatFileSize(context, state.fileList.sumOf { it.size }),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                FileCarousel(
                    fileList = state.fileList,
                    selectedFiles = state.filesToDelete,
                    onToggleSelection = { viewModel.toggleFileToDelete(it) },
                )
            } else {
                HintCard(text = "No files added yet. Add files to start sharing.")
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row {
                Spacer(Modifier.weight(1f))
                if (state.filesToDelete.isNotEmpty()) {
                    Button(
                        modifier = Modifier
                            .padding(end = 8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        onClick = { viewModel.removeFiles() }
                    ) {
                        Icon(imageVector = Icons.Default.Delete, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Delete Selected")
                    }
                    Button(
                        onClick = { viewModel.clearFilesToDelete() }
                    ) {
                        Icon(imageVector = Icons.Default.Cancel, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Cancel")
                    }
                } else {
                    Button(
                        onClick = { filePickerDialog.launch("*/*") }
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Add Files")
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // --- Advanced Settings (Collapsible) ---
            AdvancedSettingsSection(state, viewModel, scope)

            Spacer(modifier = Modifier.height(24.dp))

            // --- Recent Activity Section ---
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 8.dp)
            ) {
                Text(
                    "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                @OptIn(ExperimentalMaterial3Api::class)
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    IconButton(
                        onClick = onNavigateToLogs,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "View Detailed Logs",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            if (state.logEntries.isNotEmpty()) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    state.logEntries.forEach {
                        LogListEntry(
                            entry = it,
                            menuIsOpen = state.logMenuOpenForId == it.logEntry.id,
                            onContextMenuOpen = { viewModel.logMenuOpenForId(it.logEntry.id) },
                            onContextMenuClose = { viewModel.logMenuClose() },
                            onRemoveFromWhiteList = { viewModel.removeFromWhitelist(it.logEntry.clientIp) },
                            onAddToBlackList = { viewModel.addToBlacklist(it.logEntry.clientIp) },
                            onRemoveFromBlackList = { viewModel.removeFromBlacklist(it.logEntry.clientIp) }
                        )
                    }
                }
            }
            else {
                HintCard(text = "No recent activity yet.")
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (state.blacklist.isNotEmpty()) {
                Text(
                    "Blocked Connections",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                state.blacklist.distinct().forEach { ip ->
                    Row(
                        Modifier
                            .padding(vertical = 4.dp)
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(ip, Modifier.weight(1f))
                        IconButton(onClick = { viewModel.removeFromBlacklist(ip) }) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = "Unblock",
                                tint = MaterialTheme.colorScheme.tertiary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    LocalShareTheme {
        HomeScreen(
            onOpenDrawer = {},
            onNavigateToLogs = {}
        )
    }
}
