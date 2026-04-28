/*
 * LocalShare - Share files locally
 * Copyright (C) 2026 defname
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