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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
fun SettingsScreen() {
    val state by ServerRepository.state.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxWidth()
        // Das Scrollen kommt ja bereits vom MainScreen (MainActivity)
    ) {
        // Gruppe 1: Server Konfiguration
        SettingsGroup(title = stringResource(R.string.settings_server_title)) {
            // Port Einstellung
            SettingsRow {
                TextField(
                    value = state.port.toString(),
                    enabled = !state.isRunning,
                    label = { Text(stringResource(R.string.settings_server_port_label)) },
                    modifier = Modifier.weight(1f),
                    supportingText = {
                        if (state.isRunning) {
                            Text(stringResource(R.string.settings_server_port_hint_not_available))
                        }
                        else {
                            Text(stringResource(R.string.settings_server_port_hint))
                        }
                    },
                    onValueChange = {
                        ServerRepository.setPort(it.toIntOrNull() ?: 8080)
                    }
                )
            }

            SettingsRow {
                TextField(
                    value = state.idleTimeoutSeconds.toString(),
                    label = { Text(stringResource(R.string.settings_server_timeout_label)) },
                    modifier = Modifier.weight(1f),
                    supportingText = {
                        Text(stringResource(R.string.settings_server_timeout_hint))
                    },
                    onValueChange = {
                        ServerRepository.setIdleTimeoutSeconds(it.toIntOrNull() ?: 30)
                    }
                )
            }
        }

        // Gruppe 2: Sicherheit
        SettingsGroup(title = stringResource(R.string.settings_security_title)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_security_require_approval_label),
                subtitle = stringResource(R.string.settings_security_require_approval_hint),
                checked = state.requireApproval,
                onCheckedChange = { ServerRepository.setRequireApproval(it) }
            )

            if (state.requireApproval) {
                SettingsRow{
                    TextField(
                        value = state.whiteListEntryTTLSeconds.toString(),
                        label = { Text(stringResource(R.string.settings_security_whitelist_ttl_label)) },
                        modifier = Modifier.weight(1f),
                        supportingText = {
                            Text(stringResource(R.string.settings_security_whitelist_ttl_hint))
                        },
                        onValueChange = {
                            ServerRepository.setWhiteListEntryTTLSeconds(it.toIntOrNull() ?: 30)
                        }
                    )
                }
            }
        }

        SettingsGroup(title = stringResource(R.string.settings_misc_title)) {
            SettingsSwitchRow(
                title = stringResource(R.string.settings_misc_clear_file_list_on_share_label),
                subtitle = stringResource(R.string.settings_misc_clear_file_list_on_share_hint),
                checked = state.clearFileListOnSendIntent,
                onCheckedChange = { ServerRepository.setClearFilesListOnSendIntent(it) }
            )

            SettingsSwitchRow(
                title = stringResource(R.string.settings_misc_keep_screen_on_label),
                subtitle = stringResource(R.string.settings_misc_keep_screen_on_hint),
                checked = state.keepScreenOn,
                onCheckedChange = { ServerRepository.setKeepScreenOn(it) }
            )
        }
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