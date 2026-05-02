// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.compose.SubcomposeAsyncImage
import coil.compose.SubcomposeAsyncImageContent
import coil.decode.SvgDecoder
import coil.decode.VideoFrameDecoder
import coil.request.ImageRequest
import coil.request.videoFrameMillis
import com.defname.localshare.domain.model.FileInfo

@Composable
fun FileIcon(
    file: FileInfo,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val imageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components {
                add(VideoFrameDecoder.Factory())
                add(SvgDecoder.Factory())
            }
            .build()
    }

    val iconFilename = file.iconFile
    val assetPath = "file:///android_asset/fileicons/$iconFilename"

    // Wir versuchen zuerst das Thumbnail/Bild über die URI zu laden
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(context)
            .data(file.uri)
            .videoFrameMillis(1000) // Nimm den Frame bei 1 Sekunde
            .build(),
        imageLoader = imageLoader,
        contentDescription = file.name,
        modifier = modifier
            .size(48.dp)
            .clip(MaterialTheme.shapes.small)
            .alpha(if (isSelected) 0.3f else 1f),
        contentScale = ContentScale.Crop,
        colorFilter = if (isSelected) {
            ColorFilter.tint(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), BlendMode.SrcAtop)
        } else null
    ) {
        when (painter.state) {
            is AsyncImagePainter.State.Success -> {
                SubcomposeAsyncImageContent()
            }
            is AsyncImagePainter.State.Loading -> {
                // Während des Ladens zeigen wir das Icon leicht ausgegraut
                IconFallback(file, isSelected, imageLoader, alpha = 0.5f)
            }
            else -> {
                // Fehlerfall (z.B. keine Videodatei oder kein Bild)
                IconFallback(file, isSelected, imageLoader)
            }
        }
    }
}

@Composable
private fun IconFallback(
    file: FileInfo,
    isSelected: Boolean,
    imageLoader: ImageLoader,
    alpha: Float = 1f
) {
    val assetPath = "file:///android_asset/fileicons/${file.iconFile}"
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(alpha)
            .background(
                if (isSelected) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                else MaterialTheme.colorScheme.surfaceVariant
            ),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = assetPath,
            imageLoader = imageLoader,
            contentDescription = null,
            modifier = Modifier.size(32.dp)
        )
    }
}
