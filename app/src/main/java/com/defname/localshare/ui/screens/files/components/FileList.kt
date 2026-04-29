// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.files.components

import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.sizeAsString
import com.defname.localshare.ui.components.FileIcon

@Composable
fun FileList(
    files: List<FileInfo>,
    isSelectionMode: Boolean = false,
    selectedFiles: Set<Uri>,
    onFileSelected: (FileInfo) -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        for (file in files) {
            val isSelected = selectedFiles.contains(file.uri)
            FileListRow(
                file = file,
                isSelected = isSelected,
                isSelectionMode = isSelectionMode,
                onToggleSelection = { onFileSelected(file) },
                onClick = {})
        }
    }
}

@Composable
fun FileListRow(
    file: FileInfo,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    onToggleSelection: () -> Unit,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = { if (isSelectionMode) onToggleSelection() else onClick() },
                onLongClick = { onToggleSelection() }
            ),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface
    ) {
        ListItem(
            headlineContent = { Text(file.name, overflow = TextOverflow.Ellipsis) },
            supportingContent = { Text(file.sizeAsString()) },
            leadingContent = {
                FileIcon(file)
            },
            trailingContent = {
                if (isSelectionMode) {
                    Checkbox(checked = isSelected, onCheckedChange = { onToggleSelection() })
                }
            }
        )
    }
}

