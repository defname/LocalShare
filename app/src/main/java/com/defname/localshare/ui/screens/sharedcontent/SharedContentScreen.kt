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

package com.defname.localshare.ui.screens.sharedcontent

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LibraryAddCheck
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.defname.localshare.R
import com.defname.localshare.ui.screens.sharedcontent.components.PasteFab
import com.defname.localshare.ui.screens.sharedcontent.components.SharedContentList
import com.defname.localshare.ui.theme.LocalShareTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedContentScreen(
    viewModel: SharedContentViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.state.collectAsState()


    val context = LocalContext.current
    val clipboardManager = remember { context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager }

    fun checkClipboard() {
        val hasText = clipboardManager.primaryClip?.let { clip ->
            clip.itemCount > 0 // && clip.description.hasMimeType(ClipDescription.MIMETYPE_TEXT_PLAIN)
        } ?: false
        viewModel.updateClipboardStatus(hasText)
    }

    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                checkClipboard()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }



    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    if (state.selectedItems.isNotEmpty()) {
                        Text("${state.selectedItems.size} selected")
                    } else {
                        Text(stringResource(R.string.sharedcontentscreen_title))
                    }
                },
                navigationIcon = {
                    if (state.selectedItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onClearSelection() }) {
                            Icon(Icons.Default.Close, contentDescription = "Cancel")
                        }
                    } else {
                        IconButton(onClick = onOpenDrawer) {
                            Icon(Icons.Default.Menu, contentDescription = null)
                        }
                    }
                },
                actions = {
                    if (state.selectedItems.isNotEmpty()) {
                        IconButton(onClick = { viewModel.onSelectAll() }) {
                            Icon(Icons.Default.LibraryAddCheck, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.onRemoveSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {

                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (state.hasClipboardContent) {
                PasteFab(onClick = {
                    val clipData = clipboardManager.primaryClip
                    if (clipData != null && clipData.itemCount > 0) {
                        val text = clipData.getItemAt(0).text?.toString()
                        if (!text.isNullOrBlank()) {
                            viewModel.addClipboardContent(text)
                        }
                    }
                })
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            SharedContentList(
                items = state.sharedContentList,
                selectedItems = state.selectedItems,
                onToggleSelection = { viewModel.onToggleSelection(it) },
                expandedItem = state.expandedItem,
                onExpand = { viewModel.onExpand(it) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun SharedContentScreen() {
    LocalShareTheme {
        SharedContentScreen(onOpenDrawer = {})
    }
}
