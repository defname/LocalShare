package com.defname.localshare.ui.screens.files

import android.net.Uri
import androidx.annotation.StringRes
import com.defname.localshare.R
import com.defname.localshare.domain.model.FileInfo

data class FilesState(
    val fileList: List<FileInfo> = emptyList(),
    val selectedFiles: Set<Uri> = emptySet(),
    val sortedBy: FilesSortType = FilesSortType.NAME,
    val sortedAscending: Boolean = true,
    val isSortingMenuOpen: Boolean = false,
    val searchQuery: String = ""
)

enum class FilesSortType(@StringRes val labelRes: Int) {
    NAME(R.string.sort_name),
    SIZE(R.string.sort_size),
    TYPE(R.string.sort_type)
}