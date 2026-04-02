package com.defname.sendfile

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
