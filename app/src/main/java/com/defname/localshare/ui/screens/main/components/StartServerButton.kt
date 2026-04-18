package com.defname.localshare.ui.screens.main.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.defname.localshare.R


@Composable
fun StartServerButton(
    serverIsRunning: Boolean,
    hasNotificationPermission: Boolean,
    requestNotificationPermission: () -> Unit,
    startServer: () -> Unit,
    stopServer: () -> Unit
) {
    Surface(modifier = Modifier
        .fillMaxWidth(),
        color = MaterialTheme.colorScheme.surface
    ) {

        Box(modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)) {
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
                if (!serverIsRunning) {
                    // Button zum Starten
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { startServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primaryFixed,
                            contentColor = MaterialTheme.colorScheme.onPrimaryFixed
                        )
                    ) {
                        Icon(imageVector = Icons.Default.Send, contentDescription = stringResource(R.string.main_server_button_start))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.main_server_button_start))
                    }
                } else {
                    // Button zum Stoppen (wenn er schon läuft)
                    Button(
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { stopServer() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                    ) {
                        Icon(imageVector = Icons.Default.Block, contentDescription = stringResource(
                            R.string.main_server_button_stop
                        ))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(stringResource(R.string.main_server_button_stop))
                    }
                }
            }
        }
    }
}
