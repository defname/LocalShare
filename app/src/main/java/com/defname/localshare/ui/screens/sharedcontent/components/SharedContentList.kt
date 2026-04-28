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