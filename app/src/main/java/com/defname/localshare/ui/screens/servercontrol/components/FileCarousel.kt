package com.defname.localshare.ui.screens.servercontrol.components

import android.net.Uri
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.carousel.HorizontalMultiBrowseCarousel
import androidx.compose.material3.carousel.rememberCarouselState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.defname.localshare.domain.model.FileInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileCarousel(
    fileList: List<FileInfo> = emptyList(),
    selectedFiles: Set<Uri> = emptySet(),
    onToggleSelection: (Uri) -> Unit = {}
) {
    val context = LocalContext.current
    val svgImageLoader = remember {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory())}
            .build()
    }

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