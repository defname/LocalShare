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