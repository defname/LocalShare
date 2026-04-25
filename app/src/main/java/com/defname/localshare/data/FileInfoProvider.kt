package com.defname.localshare.data

import android.content.ContentResolver
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Size
import android.webkit.MimeTypeMap
import com.defname.localshare.IconMap
import com.defname.localshare.domain.model.FileInfo

class FileInfoProvider(private val contentResolver: ContentResolver) {
    fun getThumbnail(fileUri: Uri?): Bitmap? {
        if (fileUri == null) {
            return null
        }
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                contentResolver.loadThumbnail(fileUri, Size(512, 512), null)
            }
            else {
                null
            }
        } catch(e: Exception) {
            e.printStackTrace()
            null
        }
    }

    fun getMimeType(uri: Uri, fileName: String): String {
        val fromResolver = contentResolver.getType(uri)
        if (fromResolver != null) return fromResolver

        val ext = fileName.substringAfterLast('.', "").lowercase()
        val fromExt = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(ext)

        return fromExt ?: "application/octet-stream"
    }

    fun getFileInfo(uri: Uri): FileInfo {
        var name = ""
        var size = 0L

        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
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
        val mimeType = getMimeType(uri, name)
        val iconFile = IconMap.getIcon(mimeType)

        return FileInfo(
            id = generatedId,
            uri = uri,
            name = name,
            size = size,
            mimeType = mimeType,
            iconFile = iconFile,
            filePreview = getThumbnail(uri)
        )
    }

    fun isDuplicate(file1: FileInfo, file2: FileInfo): Boolean {
        return file1.uri == file2.uri ||
                (file1.name == file2.name && file1.size == file2.size)
    }

    fun hasAccess(uri: Uri): Boolean {
        return try {
            contentResolver.openFileDescriptor(uri, "r")?.close()
            true
        } catch (e: Exception) {
            false
        }
    }
}