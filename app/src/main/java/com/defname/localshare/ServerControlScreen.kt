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

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.format.Formatter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Stream
import androidx.compose.material.icons.filled.Undo
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumInteractiveComponentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder

fun shareText(context: Context, text: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCarousel(
    selectedFiles: Set<Uri>,
    onToggleSelection: (Uri) -> Unit
) {
    val state by ServerRepository.state.collectAsState()
    val context = LocalContext.current
    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory())}
            .build()
    }

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { state.fileList.size },
        modifier = Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(top = 16.dp, bottom = 16.dp),
        preferredItemWidth = 48.dp,
        itemSpacing = 8.dp,
        maxSmallItemWidth = 48.dp,
        minSmallItemWidth = 48.dp,
        contentPadding = PaddingValues(horizontal = 8.dp)
    ) { i ->
        val item = state.fileList[i]
        val isSelected = selectedFiles.contains(item.uri)

        Card(
            Modifier
                .maskClip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = { if (selectedFiles.isNotEmpty()) onToggleSelection(item.uri) },
                    onLongClick = { onToggleSelection(item.uri) }
                )
        ) {
            if (item.filePreview != null) {
                Image(
                    modifier = Modifier
                        .size(48.dp)
                        .maskClip(MaterialTheme.shapes.small)
                        .alpha(if (isSelected) 0.3f else 1f),
                    bitmap = item.filePreview.asImageBitmap(),
                    contentDescription = item.name,
                    contentScale = ContentScale.Crop,
                    colorFilter = if (isSelected) {
                        ColorFilter.tint(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), BlendMode.SrcAtop)
                    } else null
                )
            } else {
                val iconFilename = item.iconFile
                val assetPath = "file:///android_asset/fileicons/$iconFilename"

                Box(
                    Modifier
                        .size(48.dp)
                        .background(
                            if (isSelected) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.surfaceVariant
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = assetPath,
                        imageLoader = svgImageLoader, // Hier wird der SVG-Decoder genutzt
                        contentDescription = item.name,
                        modifier = Modifier.size(32.dp),
                    )
                }
            }
        }
    }
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IpAddressSelector() {
    val state by ServerRepository.state.collectAsState()
    var expanded by remember { mutableStateOf(false) }

    // Wir zeigen die aktuell gewählte IP oder "All Interfaces" an
    val selectedOption = state.selectedIp

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = {
            expanded = !expanded
            if (it) {
                ServerRepository.updateLocalIpAddresses()
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp)
    ) {
        OutlinedTextField(
            value = selectedOption,
            onValueChange = {},
            readOnly = true, // Verhindert Tastatureingabe
            label = { Text(stringResource(R.string.servercontrolscreen_bind_server_input_label)) },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
            modifier = Modifier
                .menuAnchor()
                .fillMaxWidth(),
            enabled = !state.isRunning
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            // Option 1: Alle Interfaces
            DropdownMenuItem(
                text = { Text(stringResource(R.string.servercontrolscreen_bind_server_input_default)) },
                onClick = {
                    ServerRepository.setSelectedIp(null)
                    expanded = false
                },
                contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
            )

            // Dynamische Optionen aus der IP-Liste
            state.localIpAddresses.forEach { netInfo ->
                DropdownMenuItem(
                    text = {
                        Column {
                            Text(netInfo.ip)
                            Text(netInfo.interfaceName, style = MaterialTheme.typography.labelSmall)
                        }
                    },
                    onClick = {
                        ServerRepository.setSelectedIp(netInfo.ip)
                        expanded = false
                    },
                    contentPadding = ExposedDropdownMenuDefaults.ItemContentPadding
                )
            }
        }
    }
}

@Composable
fun ServerControlScreen(navController: NavController) {
    val state by ServerRepository.state.collectAsState()
    val context = LocalContext.current

    var filesToDelete by remember { mutableStateOf(setOf<Uri>()) }
    val toggleFileSelection = { uri: Uri ->
        filesToDelete = if (filesToDelete.contains(uri)) {
            filesToDelete - uri
        } else {
            filesToDelete + uri
        }
    }

    val filePickerDialog = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) {
            ServerRepository.addFiles(context, uris)
        }
    }

    Column (
        modifier = Modifier.fillMaxSize()
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row (verticalAlignment = Alignment.CenterVertically){
            Text(
                stringResource(R.string.servercontrolscreen_shared_files_caption),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .weight(1f)
            )
            if (state.fileList.isNotEmpty()) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    IconButton(
                        onClick = { ServerRepository.clearFiles(); filesToDelete = emptySet() },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = stringResource(R.string.servercontrollscreen_filelist_icon_descr_clear),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (state.fileList.isNotEmpty()) {
            Text(
                stringResource(
                    R.string.servercontrolscreen_shared_files_number,
                    state.fileList.size
                ), style = MaterialTheme.typography.bodySmall)
            Text(
                stringResource(
                    R.string.servercontrolscreen_shared_files_total_size,
                    Formatter.formatFileSize(context, state.fileList.sumOf { it.size })
                ), style = MaterialTheme.typography.bodySmall)
            FileCarousel(filesToDelete, toggleFileSelection)
        }
        Row {
            Spacer(Modifier.weight(1f))
            if (filesToDelete.isNotEmpty()) {
                Button(
                    modifier = Modifier
                        .padding(end = 8.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    onClick = { filesToDelete.forEach { uri -> ServerRepository.removeFile(uri) }; filesToDelete = emptySet() }
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = stringResource(R.string.servercontrolscreen_shared_files_delete_file))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.servercontrolscreen_shared_files_delete_file))
                }
                Button(
                    onClick = { filesToDelete = emptySet() }
                ) {
                    Icon(imageVector = Icons.Default.Cancel, contentDescription = stringResource(R.string.servercontrolscreen_shared_files_delete_file_cancel))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.servercontrolscreen_shared_files_delete_file_cancel))
                }
            }
            else {
                Button(
                    onClick = { filePickerDialog.launch("*/*") }
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = stringResource(R.string.servercontrolscreen_add_files_icon_description_add))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.servercontrolsscreen_add_files_button_caption))
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.servercontrolscreen_server_settings_caption),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        Row(Modifier.padding(8.dp)) {
            OutlinedTextField(
                value = state.token,
                label = { Text(stringResource(R.string.servercontrolscreen_token_input_caption)) },
                singleLine = true,
                onValueChange = { ServerRepository.setToken(it) },
                enabled = true,
                modifier = Modifier
                    .weight(2f)
                    .fillMaxWidth(),
                trailingIcon = { IconButton(onClick = { ServerRepository.setRandomToken() }) {
                    Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.servercontrolscreen_generate_random_token_icon_description))
                } }
            )
        }

        IpAddressSelector()

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            stringResource(R.string.servercontrollscreen_serverurls_caption),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))

        val usedAddresses: List<String> = remember(state.selectedIp, state.localIpAddresses) {
            if (state.selectedIp != "0.0.0.0") {
                listOf(state.selectedIp)
            } else {
                state.localIpAddresses.map { it.ip }
            }
        }

        for (ipAddress in usedAddresses) {
            val baseUrl = "http://$ipAddress:${state.port}/${state.token}"
            for (action in listOf("stream", "download")) {
                val url = "$baseUrl/$action"
                Card(modifier = Modifier
                    .padding(horizontal = 8.dp)
                    .padding(bottom = 8.dp)) {
                    Row(Modifier.padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (action == "stream") Icons.Default.Stream else Icons.Default.Download,
                            contentDescription = if (action == "stream") stringResource(R.string.servercontrollscreen_server_urls_icon_descr_stream) else stringResource(
                                R.string.servercontrollscreen_server_urls_icon_descr_download
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            url,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        IconButton(onClick = { shareText(context, url) }) {
                            Icon(imageVector = Icons.Default.Share, contentDescription = stringResource(
                                R.string.servercontrollscreen_serverurls_icon_descr_share
                            ))
                        }
                    }
                }
            }
        }



        Spacer(modifier = Modifier.height(16.dp))
        Row (verticalAlignment = Alignment.CenterVertically){
            Text(
                stringResource(R.string.servercontrollscreen_logs_caption),

                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .weight(1f)
            )
            if (state.logs.size > 4) {
                CompositionLocalProvider(LocalMinimumInteractiveComponentSize provides Dp.Unspecified) {
                    IconButton(
                        onClick = { navController.navigate(Screen.Logs.route) },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = stringResource(R.string.servercontrollscreen_logs_expand),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
        Spacer(modifier = Modifier.height(8.dp))

        // LogList(4)


        if (state.isRunning) {

            if (state.activeClients.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))

                Text(
                    stringResource(
                        R.string.servercontrollscreen_active_clients_caption,
                        state.activeClients.size
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .padding(top = 16.dp)
                )

                state.activeClients.distinct().forEach { ip ->
                    Row(
                        Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ip, Modifier
                                .weight(2f)
                                .padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = { ServerRepository.addToBlacklist(ip) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = stringResource(R.string.servercontrollscreen_active_clients_ban),
                                tint = Color.Red
                            )
                        }
                    }
                }
            }

            if (state.blacklist.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    stringResource(
                        R.string.servercontrollscreen_banned_ips_caption,
                        state.blacklist.size
                    ),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                )

                state.blacklist.distinct().forEach { ip ->
                    Row(
                        Modifier
                            .padding(8.dp)
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.background)
                            .border(
                                width = 1.dp,
                                color = MaterialTheme.colorScheme.outline,
                                MaterialTheme.shapes.medium
                            ),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            ip, Modifier
                                .weight(2f)
                                .padding(start = 8.dp)
                        )
                        IconButton(
                            onClick = { ServerRepository.removeFromBlacklist(ip) }
                        ) {
                            Icon(
                                imageVector = Icons.Default.Undo,
                                contentDescription = stringResource(R.string.servercontrollscreen_icon_descr_unban),
                                tint = Color.Green
                            )
                        }
                    }
                }
            }
        }

    }
}
