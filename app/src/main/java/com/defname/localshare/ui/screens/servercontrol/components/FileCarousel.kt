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

package com.defname.localshare.ui.screens.servercontrol.components

import android.net.Uri
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.ui.components.FileIcon

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCarousel(
    fileList: List<FileInfo> = emptyList(),
    selectedFiles: Set<Uri> = emptySet(),
    onToggleSelection: (Uri) -> Unit = {}
) {
    val context = LocalContext.current

    HorizontalMultiBrowseCarousel(
        state = rememberCarouselState { fileList.size },
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
        val item = fileList[i]
        val isSelected = selectedFiles.contains(item.uri)

        Card(
            Modifier
                .maskClip(MaterialTheme.shapes.medium)
                .combinedClickable(
                    onClick = { if (selectedFiles.isNotEmpty()) onToggleSelection(item.uri) },
                    onLongClick = { onToggleSelection(item.uri) }
                )
        ) {
            FileIcon(item, isSelected = isSelected)
        }
    }
}