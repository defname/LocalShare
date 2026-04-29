// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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

fun FileInfo.sizeAsString(): String {
    var sizeDouble = this.size.toDouble()
    val units = listOf("Byte", "KB", "MB", "GB", "TB")
    var unitIndex = 0
    while (sizeDouble >= 1024 && unitIndex < units.size - 1) {
        sizeDouble /= 1024
        unitIndex++
    }
    return "%.2f %s".format(sizeDouble, units[unitIndex])
}