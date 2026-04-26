package com.defname.localshare.ui.screens.files

import android.net.Uri
import com.defname.localshare.domain.model.FileInfo

data class FilesState(
    val fileList: List<FileInfo> = emptyList(),
    val selectedFiles: Set<Uri> = emptySet(),
    val sortedBy: FilesSortType = FilesSortType.NAME,
    val searchQuery: String = ""
)

enum class FilesSortType {
    NAME,
    SIZE,
    TYPE
}