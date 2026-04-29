// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.files.components

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.defname.localshare.ui.screens.files.FilesSortType

@Composable
fun SortingButton(
    isMenuOpen: Boolean,
    sortedBy: FilesSortType,
    sortedAscending: Boolean,
    onToggleMenu: () -> Unit,
    onSortChanged: (FilesSortType) -> Unit
) {
    Box {
        IconButton(onClick = onToggleMenu) {
            Icon(Icons.Default.FilterList, contentDescription = "Sort")
        }
        DropdownMenu(
            expanded = isMenuOpen,
            onDismissRequest = onToggleMenu
        ) {
            FilesSortType.entries.forEach { sortType ->
                DropdownMenuItem(
                    text = { Text(stringResource(sortType.labelRes)) },
                    onClick = {
                        onSortChanged(sortType)
                        onToggleMenu()
                    },
                    trailingIcon = {
                        if (sortedBy == sortType) {
                            Icon(if (sortedAscending) Icons.Default.ArrowDropDown else Icons.Default.ArrowDropUp, contentDescription = null)
                        }
                    }
                )
            }
        }
    }
}