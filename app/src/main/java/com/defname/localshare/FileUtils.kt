/*
 * LocalShare - Share files locally
 * Copyright (C) 2024 defname
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
package com.defname.localshare

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import android.webkit.MimeTypeMap

data class FileInfo(
    val id: String,
    val uri: Uri,
    val name: String,
    val size: Long,
    val mimeType: String,
    val iconFile: String,
    val filePreview: Bitmap? = null
)


fun getFileThumbnail(context: Context, fileUri: Uri?): Bitmap? {
    if (fileUri == null) {
        return null
    }
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(fileUri, Size(512, 512), null)
        }
        else {
            null
        }
    } catch(e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getMimeType(context: Context, uri: Uri, fileName: String): String {
    val fromResolver = context.contentResolver.getType(uri)
    if (fromResolver != null) return fromResolver

    val ext = fileName.substringAfterLast('.', "").lowercase()
    val fromExt = MimeTypeMap.getSingleton()
        .getMimeTypeFromExtension(ext)

    return fromExt ?: "application/octet-stream"
}

fun getFileInfo(context: Context, uri: Uri): FileInfo {

    var name = ""
    var size = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }

    val generatedId = Integer.toHexString(uri.toString().hashCode()).lowercase()
    val mimeType = getMimeType(context, uri, name)
    val iconFile = IconMap.getIcon(mimeType)

    return FileInfo(
        id = generatedId,
        uri = uri,
        name = name,
        size = size,
        mimeType = mimeType,
        iconFile = iconFile,
        filePreview = getFileThumbnail(context, uri)
    )
}
