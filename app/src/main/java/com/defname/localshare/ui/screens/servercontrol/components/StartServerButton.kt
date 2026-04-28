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

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.defname.localshare.R
import com.defname.localshare.data.RuntimeState


@Composable
fun StartServerButton(
    serverState: RuntimeState,
    hasNotificationPermission: Boolean,
    requestNotificationPermission: () -> Unit,
    startServer: () -> Unit,
    stopServer: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(modifier = Modifier
        .fillMaxWidth()
        .then(modifier),
        color = Color.Transparent // MaterialTheme.colorScheme.surface
    ) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp, 4.dp, 16.dp, 12.dp)
        ) {
            if (!hasNotificationPermission) {
                Button(
                    modifier = Modifier.fillMaxWidth(),
                    onClick = { requestNotificationPermission() },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    ),
                ) {
                    Text(stringResource(R.string.main_server_button_request_notification_permission))
                }
            } else {
                when (serverState) {
                    RuntimeState.STOPPED -> {
                        // Button zum Starten
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primaryFixed,
                                contentColor = MaterialTheme.colorScheme.onPrimaryFixed
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.Send,
                                contentDescription = stringResource(R.string.main_server_button_start)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.main_server_button_start))
                        }
                    }

                    RuntimeState.RUNNING -> {
                        // Button zum Stoppen (wenn er schon läuft)
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { stopServer() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error,
                                contentColor = MaterialTheme.colorScheme.onError
                            ),
                        ) {
                            Icon(
                                imageVector = Icons.Default.Stop,
                                contentDescription = stringResource(
                                    R.string.main_server_button_stop
                                )
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.main_server_button_stop))
                        }
                    }

                    RuntimeState.STARTING -> {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startServer() },
                            enabled = false
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Farbe anpassen, damit sie zum deaktivierten Button passt
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.main_server_button_starting))
                        }
                    }

                    RuntimeState.STOPPING -> {
                        Button(
                            modifier = Modifier.fillMaxWidth(),
                            onClick = { startServer() },
                            enabled = false
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant // Farbe anpassen, damit sie zum deaktivierten Button passt
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(stringResource(R.string.main_server_button_stopping))
                        }
                    }
                }
            }
        }
    }
}
