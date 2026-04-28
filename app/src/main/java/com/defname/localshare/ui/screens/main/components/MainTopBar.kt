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