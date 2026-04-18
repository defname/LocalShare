package com.defname.localshare.domain.model

import android.graphics.Bitmap
import android.net.Uri

data class FileInfo(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val iconFile: String,
    val filePreview: Bitmap? = null
)