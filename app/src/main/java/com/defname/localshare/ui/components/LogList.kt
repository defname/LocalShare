package com.defname.localshare.ui.components

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QuestionMark
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.defname.localshare.R
import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.domain.model.ConnectionLogEntry
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.ui.theme.LocalShareTheme


data class LogListEntry(
    val logEntry: ConnectionLogEntry,
    val isWhiteListed: Boolean,
    val isBlackListed: Boolean
)

val cmpConnectionLogEntries = Comparator<ConnectionLogEntry> { a, b ->
    if (a.closedTimestamp != null && b.closedTimestamp != null) {
        (a.closedTimestamp - b.closedTimestamp).toInt()
    }
    else if (a.closedTimestamp != null) -1
    else if (b.closedTimestamp != null) 1
    else (a.openTimestamp - b.openTimestamp).toInt()
}

fun List<ConnectionLogEntry>.toLogListEntries(securityRepository: SecurityRepository): List<LogListEntry> {
    return this
        .sortedWith(cmpConnectionLogEntries)
        .map { entry ->
            LogListEntry(
                entry,
                securityRepository.isWhitelisted(entry.clientIp),
                securityRepository.isBlacklisted(entry.clientIp)
            )
        }.reversed()
}


@Composable
fun DisconnectReasonText(
    disconnectReason: DisconnectReason
) {
    when (disconnectReason) {
        is DisconnectReason.Expected -> {
            Text(
                "${disconnectReason.statusCode}",
                color = if (disconnectReason.statusCode >= 400) Color.Red else Color.Green
            )
        }

        is DisconnectReason.ServerShutdown -> {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = "Server shutdown",
                tint = Color.Red
            )
        }

        is DisconnectReason.Unexpected -> {
            when (disconnectReason) {
                DisconnectReason.Unexpected.ClientGone -> Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Auth invalid",
                    tint = Color.Green
                )
                DisconnectReason.Unexpected.AuthInvalid -> Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Auth invalid",
                    tint = Color.Red
                )

                DisconnectReason.Unexpected.Unknown -> Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = "Unknown",
                    tint = Color.Red
                )

                is DisconnectReason.Unexpected.Error -> Icon(
                    imageVector = Icons.Default.QuestionMark,
                    contentDescription = disconnectReason.message ?: "Unknown",
                    tint = Color.Red
                )
            }
        }
    }
}

@Composable
fun LogList(
    entries: List<LogListEntry>,
    menuOpenForId: String? = null,

    onContextMenuOpen: (ConnectionLogEntry) -> Unit = {},
    onContextMenuClose: () -> Unit = {},

    onAddToBlackList: (String) -> Unit = {},
    onRemoveFromBlackList: (String) -> Unit = {},
    onRemoveFromWhiteList: (String) -> Unit = {},
) {
    Column {
        entries.forEach { entry ->
            Box {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(8.dp, 4.dp)
                        .combinedClickable(
                            onClick = { },
                            onLongClick = { onContextMenuOpen(entry.logEntry) }
                        ),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (entry.logEntry.closedTimestamp == null) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else if (entry.logEntry.result != null) {
                        DisconnectReasonText(entry.logEntry.result)
                    } else {
                        Text("UNKNOWN")
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(entry.logEntry.method)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        entry.logEntry.path,
                        modifier = Modifier.weight(1f),
                        //style = MaterialTheme.typography.bodyMedium,
                        maxLines = 1,
                        overflow = TextOverflow.StartEllipsis
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        entry.logEntry.clientIp,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (entry.isBlackListed) Color.Red else if (entry.isWhiteListed) Color.Green else MaterialTheme.colorScheme.outline
                    )
                }

                LogEntryContextMenu(
                    ipAddress = entry.logEntry.clientIp,
                    visible = menuOpenForId == entry.logEntry.id,
                    isWhiteListed = entry.isWhiteListed,
                    isBlackListed = entry.isBlackListed,
                    onDismiss = { onContextMenuClose() },
                    onRemoveFromWhiteList = { onRemoveFromWhiteList(entry.logEntry.clientIp) },
                    onAddToBlackList = { onAddToBlackList(entry.logEntry.clientIp) },
                    onRemoveFromBlackList = { onRemoveFromBlackList(entry.logEntry.clientIp) }
                )

            }
        }
    }
}

@Preview
@Composable
fun LogListPreview() {
    LocalShareTheme {
        val entries = listOf(
            LogListEntry(
                ConnectionLogEntry(
                    method = "GET",
                    path = "/file.txt",
                    clientIp = "192.168.0.1",
                    openTimestamp = 123
                ),
                isWhiteListed = true,
                isBlackListed = false
            ),
            LogListEntry(
                ConnectionLogEntry(
                    method = "GET",
                    path = "/file.txt",
                    clientIp = "192.168.0.2",
                    openTimestamp = 125,
                ),
                isWhiteListed = false,
                isBlackListed = true
            ),
            LogListEntry(
                ConnectionLogEntry(
                    method = "GET",
                    path = "/file.txt",
                    clientIp = "192.168.0.5",
                    openTimestamp = 126
                ),
                isWhiteListed = true,
                isBlackListed = false
            )
        )

        LogList(
            entries = entries
        )
    }
}


@Composable
fun LogEntryContextMenu(
    ipAddress: String,
    modifier: Modifier = Modifier,
    visible: Boolean = false,
    isWhiteListed: Boolean = false,
    isBlackListed: Boolean = false,
    onDismiss: () -> Unit = {},
    onRemoveFromWhiteList: () -> Unit = {},
    onAddToBlackList: () -> Unit = {},
    onRemoveFromBlackList: () -> Unit = {},
) {
    DropdownMenu(
        expanded = visible,
        onDismissRequest = { onDismiss() },
        modifier = modifier
    ) {
        if (isWhiteListed) {
            DropdownMenuItem(
                text = { Text(
                    stringResource(
                        R.string.servercontrolscreen_remove_from_whitelist,
                        ipAddress
                    )) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        //tint = Color.Red
                    )
                },
                onClick = {
                    onRemoveFromWhiteList()
                    onDismiss()
                }
            )
        }
        if (isBlackListed) {
            DropdownMenuItem(
                text = { Text(
                    stringResource(
                        R.string.servercontrolscreen_remove_from_blacklist,
                        ipAddress
                    )) },
                leadingIcon = {
                    Icon(Icons.Default.Undo, contentDescription = null, tint = Color.Green)
                },
                onClick = {
                    onRemoveFromBlackList()
                    onDismiss()
                }
            )
        }
        else {
            DropdownMenuItem(
                text = { Text(
                    stringResource(
                        R.string.servercontrolscreen_add_to_blacklist,
                        ipAddress
                    )) },
                leadingIcon = {
                    Icon(
                        Icons.Default.Block,
                        contentDescription = null,
                        tint = Color.Red
                    )
                },
                onClick = {
                    onAddToBlackList()
                    onDismiss()
                }
            )
        }
    }
}

@Preview
@Composable
fun LogEntryContextMenuPreview() {
    LocalShareTheme {
        LogEntryContextMenu(
            ipAddress = "192.168.0.1",
            visible = true,
            isWhiteListed = true
        )
    }
}