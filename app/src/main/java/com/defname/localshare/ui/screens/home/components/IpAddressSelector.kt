// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.home.components

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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.res.stringResource
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
            value = selectedAddress ?: stringResource(R.string.ipaddressselector_all_networks),
            onValueChange = {},
            readOnly = true,
            label = { Text(stringResource(R.string.ipaddressselector_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            isError = !isSelectedAddressValid,
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = enabled,
            supportingText = {
                if (!isSelectedAddressValid)
                    Text(stringResource(R.string.ipaddressselector_connection_lost))
                else if (!enabled)
                    Text(stringResource(R.string.ipaddressselector_stop_sharing_to_change))
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { onDismiss() }
        ) {
            // Option 1: All Networks
            DropdownMenuItem(
                text = {
                    Column {
                        Text(stringResource(R.string.ipaddressselector_all_networks), style = MaterialTheme.typography.bodyLarge)
                        Text(stringResource(R.string.ipaddressselector_all_networks_description), style = MaterialTheme.typography.labelSmall)
                    }
                },
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
                            Text(netInfo.ip, style = MaterialTheme.typography.bodyLarge)
                            Text(stringResource(R.string.ipaddressselector_interface_label, netInfo.interfaceName), style = MaterialTheme.typography.labelSmall)
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