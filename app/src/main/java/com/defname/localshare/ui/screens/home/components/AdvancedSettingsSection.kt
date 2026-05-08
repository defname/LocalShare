package com.defname.localshare.ui.screens.home.components

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.defname.localshare.ui.screens.home.HomeState
import com.defname.localshare.ui.screens.home.HomeViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

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
                    onAddressSelected = { scope.launch { viewModel.onIpAddressSelected(it) } },
                    onDismiss = { viewModel.collapseIpAddressSelector() }
                )
            }
        }
    }
}