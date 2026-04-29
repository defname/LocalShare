// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.main.components

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.defname.localshare.R
import com.defname.localshare.ui.screens.main.Screen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTopBar(
    currentScreen: Screen,
    isServerRunning: Boolean,
    hasLogs: Boolean,
    onNavigationClick: () -> Unit,
    onQrClick: () -> Unit,
    onClearLogs: () -> Unit
) {
    TopAppBar(
        title = {
            Text(currentScreen.label)
        },
        windowInsets = WindowInsets.statusBars,
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        navigationIcon = {
            IconButton(onClick = onNavigationClick) {
                Icon(
                    imageVector = Icons.Default.Menu,
                    contentDescription = null
                )
            }
        },
        actions = {
            when (currentScreen) {
                Screen.Logs -> {
                    if (hasLogs) {
                        IconButton(onClick = onClearLogs) {
                            Icon(Icons.Default.Clear,
                                stringResource(R.string.main_action_button_descr_clear)
                            )
                        }
                    }
                }
                Screen.Main -> {
                    if (isServerRunning) {
                        IconButton(onClick = onQrClick ) {
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
}