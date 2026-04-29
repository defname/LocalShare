// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.defname.localshare.R
import com.defname.localshare.data.RuntimeState

@Composable
fun ServerFab(
    serverState: RuntimeState,
    onStartServer: () -> Unit,
    onStopServer: () -> Unit
) {
    val isRunning = serverState == RuntimeState.RUNNING
    
    FloatingActionButton(
        onClick = {
            if (isRunning) onStopServer() else onStartServer()
        },
        containerColor = if (isRunning) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer,
        contentColor = if (isRunning) MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer
    ) {
        Icon(
            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
            contentDescription = if (isRunning) stringResource(R.string.main_server_button_stop) else stringResource(R.string.main_server_button_start)
        )
    }
}
