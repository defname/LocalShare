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
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.defname.localshare.ui.components.LogListEntry
import com.defname.localshare.ui.components.QrCodeDialog
import com.defname.localshare.ui.screens.home.components.FileCarousel
import com.defname.localshare.ui.screens.home.components.IpAddressSelector
import com.defname.localshare.ui.screens.home.components.StartServerButton
import com.defname.localshare.ui.theme.LocalShareTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
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

@Composable
fun HintCard(
    modifier: Modifier? = null,
    text: String? = null,
    content: @Composable () -> Unit = {}
) {
    Card(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp).then(modifier ?: Modifier),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))
    ) {
        if (text != null) {
            Text(
                text,
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
        content()
    }
}

@Composable
fun SharingHeroSection(state: HomeState, viewModel: HomeViewModel, context: Context) {
    val isRunning = state.isRunning
    val primaryUrl = state.primaryServerUrl

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isRunning) MaterialTheme.colorScheme.primaryContainer
            else MaterialTheme.colorScheme.surfaceVariant
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isRunning) 4.dp else 0.dp
        ),
        border = if (!isRunning) BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant) else null
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = if (isRunning) Icons.Default.Share else Icons.Default.Cancel,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = if (isRunning) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = if (isRunning) "Sharing is Active" else "Sharing is Off",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = if (isRunning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (isRunning && primaryUrl != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Anyone on your Wi-Fi can access files at:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(12.dp))
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = primaryUrl,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(12.dp).fillMaxWidth(),
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(24.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Button(
                        onClick = { shareText(context, primaryUrl) },
                        modifier = Modifier.weight(1f),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Link")
                    }
                    Button(
                        onClick = { viewModel.onOpenQrCodeDialog(primaryUrl) },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        contentPadding = PaddingValues(vertical = 12.dp)
                    ) {
                        Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(8.dp))
                        Text("QR Code")
                    }
                }
            } else if (!isRunning) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Start sharing to make your files available to other devices on this network.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun AdvancedSettingsSection(
    state: HomeState,
    viewModel: HomeViewModel,
    scope: CoroutineScope
) {
    var expanded by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { expanded = !expanded }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                "Technical Details",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                contentDescription = null
            )
        }

        if (expanded) {
            Column(modifier = Modifier.padding(top = 8.dp, bottom = 16.dp)) {
                OutlinedTextField(
                    value = state.token,
                    label = { Text("Security Token") },
                    placeholder = { Text("Optional password") },
                    singleLine = true,
                    onValueChange = { scope.launch { viewModel.onTokenChange(it) } },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        IconButton(onClick = { scope.launch { viewModel.onRandomTokenClick() } }) {
                            Icon(Icons.Default.Refresh, contentDescription = "Generate New")
                        }
                    }
                )

                Spacer(modifier = Modifier.height(16.dp))
                
                IpAddressSelector(
                    addresses = state.localIpAddresses,
                    selectedAddress = state.selectedIpAddress,
                    isSelectedAddressValid = state.isSelectedIpAddressValid,
                    expanded = state.ipAddressSelectorExpanded,
                    enabled = state.ipAddressSelectorEnabled,
                    onExpandedChange = { viewModel.ipAddressSelectorExpandedChange() },
                    onAddressSelected = { scope.launch { viewModel.setSeletectedIp(it) } },
                    onDismiss = { viewModel.collapseIpAddressSelector() }
                )
            }
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
