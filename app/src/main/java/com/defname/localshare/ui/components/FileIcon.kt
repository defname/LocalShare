package com.defname.localshare.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.decode.SvgDecoder
import com.defname.localshare.domain.model.FileInfo

@Composable
fun FileIcon(
    file: FileInfo,
    isSelected: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val svgImageLoader = remember(context) {
        ImageLoader.Builder(context)
            .components { add(SvgDecoder.Factory()) }
            .build()
    }

    if (file.filePreview != null) {
        Image(
            modifier = Modifier
                .size(48.dp)
                .clip(MaterialTheme.shapes.small)
                .alpha(if (isSelected) 0.3f else 1f),
            bitmap = file.filePreview.asImageBitmap(),
            contentDescription = file.name,
            contentScale = ContentScale.Crop,
            colorFilter = if (isSelected) {
                ColorFilter.tint(MaterialTheme.colorScheme.error.copy(alpha = 0.5f), BlendMode.SrcAtop)
            } else null
        )
    } else {
        val iconFilename = file.iconFile
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
                contentDescription = file.name,
                modifier = Modifier.size(32.dp),
            )
        }
    }
}