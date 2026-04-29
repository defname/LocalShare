// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defname.localshare.R
import com.defname.localshare.data.RuntimeState
import com.defname.localshare.ui.theme.LocalShareTheme
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit,
    serverState: RuntimeState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    val settings by viewModel.settings.collectAsState()
    val runtimeState by viewModel.runtimeState.collectAsState()
    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.screen_settings)) },
                navigationIcon = {
                    IconButton(onClick = onOpenDrawer) {
                        Icon(Icons.Default.Menu, contentDescription = null)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Gruppe 1: Server Konfiguration
            SettingsGroup(title = stringResource(R.string.settings_server_title)) {
                // Port Einstellung
                SettingsRow {
                    TextField(
                        value = settings.serverPort.toString(),
                        enabled = runtimeState.serviceState == RuntimeState.STOPPED,
                        label = { Text(stringResource(R.string.settings_server_port_label)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            if (runtimeState.serviceState != RuntimeState.STOPPED) {
                                Text(stringResource(R.string.settings_server_port_hint_not_available))
                            } else {
                                Text(stringResource(R.string.settings_server_port_hint))
                            }
                        },
                        onValueChange = {
                            scope.launch { viewModel.setPort(it.toIntOrNull() ?: 8080) }
                        }
                    )
                }

                SettingsRow {
                    TextField(
                        value = settings.serverIdleTimeoutSeconds.toString(),
                        label = { Text(stringResource(R.string.settings_server_timeout_label)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(stringResource(R.string.settings_server_timeout_hint))
                        },
                        onValueChange = {
                            scope.launch { viewModel.setIdleTimeoutSeconds(it.toIntOrNull() ?: 30) }
                        }
                    )
                }
            }

            // Gruppe 2: Sicherheit
            SettingsGroup(title = stringResource(R.string.settings_security_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_security_require_approval_label),
                    subtitle = stringResource(R.string.settings_security_require_approval_hint),
                    checked = settings.requireApproval,
                    onCheckedChange = { scope.launch { viewModel.setRequireApproval(it) } }
                )

                if (settings.requireApproval) {
                    SettingsRow {
                        TextField(
                            value = settings.whitelistEntryTTLSeconds.toString(),
                            label = { Text(stringResource(R.string.settings_security_whitelist_ttl_label)) },
                            modifier = Modifier.weight(1f),
                            supportingText = {
                                Text(stringResource(R.string.settings_security_whitelist_ttl_hint))
                            },
                            onValueChange = {
                                scope.launch { viewModel.setWhiteListEntryTTLSeconds(it.toIntOrNull() ?: 30) }
                            }
                        )
                    }
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_webinterface_title)) {
                SettingsRow {
                    TextField(
                        value = settings.sseHeartbeatPeriodSeconds.toString(),
                        label = { Text(stringResource(R.string.settings_webinterface_heartbeat_label)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(stringResource(R.string.settings_webinterface_heartbeat_hint))
                        },
                        onValueChange = {
                            scope.launch { viewModel.setHeartbeatPeriodSeconds(it.toIntOrNull() ?: 1) }
                        }
                    )
                }
            }

            SettingsGroup(title = stringResource(R.string.settings_misc_title)) {
                SettingsSwitchRow(
                    title = stringResource(R.string.settings_misc_clear_file_list_on_share_label),
                    subtitle = stringResource(R.string.settings_misc_clear_file_list_on_share_hint),
                    checked = settings.clearFileListOnShareIntent,
                    onCheckedChange = { scope.launch { viewModel.setClearFilesListOnSendIntent(it) } }
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SettingsScreenPreview() {
    LocalShareTheme {
        SettingsScreen(
            onOpenDrawer = {},
            serverState = RuntimeState.STOPPED,
            onStartServer = {},
            onStopServer = {}
        )
    }
}

@Composable
fun SettingsGroup(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier = Modifier.padding(vertical = 8.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Column(content = content)
    }
}

@Composable
fun SettingsRow(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

@Composable
fun SettingsSwitchRow(title: String, subtitle: String, checked: Boolean, onCheckedChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(4.dp))
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
