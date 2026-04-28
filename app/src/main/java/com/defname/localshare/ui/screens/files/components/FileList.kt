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

