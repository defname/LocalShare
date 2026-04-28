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

package com.defname.localshare.ui.screens.sharedcontent.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.defname.localshare.domain.model.SharedContent

@Composable
fun SharedContentList(
    items: List<SharedContent>,
    selectedItems: Set<Int>,
    expandedItem: Int?,
    onToggleSelection: (Int) -> Unit,
    modifier: Modifier = Modifier,
    onExpand: (Int) -> Unit
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(items, key = { it.id }) { item ->
            SharedContentItem(
                item = item,
                isSelected = item.id in selectedItems,
                isSelectionMode = selectedItems.isNotEmpty(),
                isExpanded = expandedItem == item.id,
                onToggleSelection = { onToggleSelection(item.id) },
                onExpand = { onExpand(item.id) }
            )
        }
    }
}