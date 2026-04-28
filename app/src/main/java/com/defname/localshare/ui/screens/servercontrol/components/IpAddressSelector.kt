/*
 * LocalShare - Share files locally
 * Copyright (C) 2026 defname
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

package com.defname.localshare.ui.screens.servercontrol.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.defname.localshare.R
import com.defname.localshare.domain.model.NetworkInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpAddressSelector(
    addresses: List<NetworkInfo> = emptyList(),
    selectedAddress: String? = null,
    isSelectedAddressValid: Boolean = true,
    expanded: Boolean = false,
    enabled: Boolean = false,
    onExpandedChange: () -> Unit = {},
    onAddressSelected: (String?) -> Unit = {},
    onDismiss: () -> Unit = {},
) {
    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { if (enabled) onExpandedChange() },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        OutlinedTextField(
            value = selectedAddress ?: "0.0.0.0",
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.servercontrolscreen_bind_server_input_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            isError = !isSelectedAddressValid,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
            supportingText = {
                if (!isSelectedAddressValid)
                    Text(stringResource(R.string.ipaddressseelector_selected_ip_is_not_available))
                else if (!enabled)
                    Text(stringResource(R.string.ipaddressselector_only_available_when_server_is_stopped))
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            // Option 1: Alle Interfaces
            DropdownMenuItem(
                text = { Text(stringResource(R.string.servercontrolscreen_bind_server_input_default)) },
                onClick = {
                    onAddressSelected(null)
                    onDismiss()
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )

            // Dynamische Optionen aus der IP-Liste
            addresses.forEach { netInfo ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(netInfo.ip)
                            Text(netInfo.interfaceName, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = {
                        onAddressSelected(netInfo.ip)
                        onDismiss()
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}