// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.files

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.defname.localshare.R
import com.defname.localshare.ui.screens.files.components.AddFilesFab
import com.defname.localshare.ui.screens.files.components.FileList
import com.defname.localshare.ui.screens.files.components.SortingButton
import com.defname.localshare.ui.theme.LocalShareTheme
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FilesScreen(
    viewModel: FilesViewModel = koinViewModel(),
    onOpenDrawer: () -> Unit
) {
    val state by viewModel.state.collectAsState()

    val filePickerDialog = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        viewModel.addFiles(uris)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    if (state.selectedFiles.isNotEmpty()) {
                        Text("${state.selectedFiles.size} selected")
                    } else {
                        Text(stringResource(R.string.screen_files))
                    }
                },
                navigationIcon = {
                    if (state.selectedFiles.isNotEmpty()) {
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
                    if (state.selectedFiles.isNotEmpty()) {
                        IconButton(onClick = { viewModel.selectAll() }) {
                            Icon(Icons.Default.LibraryAddCheck, contentDescription = "Select All")
                        }
                        IconButton(onClick = { viewModel.onDeleteSelected() }) {
                            Icon(Icons.Default.Delete, contentDescription = "Remove", tint = MaterialTheme.colorScheme.error)
                        }
                    } else {
                        SortingButton(
                            isMenuOpen = state.isSortingMenuOpen,
                            sortedBy = state.sortedBy,
                            sortedAscending = state.sortedAscending,
                            onToggleMenu = { viewModel.onToggleSortingMenu() },
                            onSortChanged = { viewModel.onSortChanged(it) }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            )
        },
        floatingActionButton = {
            if (state.selectedFiles.isEmpty()) {
                AddFilesFab(onOpenAddFilesDialog = { filePickerDialog.launch("*/*") })
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            FileList(
                files = state.fileList,
                selectedFiles = state.selectedFiles,
                isSelectionMode = state.selectedFiles.isNotEmpty(),
                onFileSelected = { viewModel.onToggleSelection(it) }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun FilesScreenPreview() {
    LocalShareTheme {
        FilesScreen(onOpenDrawer = {})
    }
}
