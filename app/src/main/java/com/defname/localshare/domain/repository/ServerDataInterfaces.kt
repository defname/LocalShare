// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.repository

import com.defname.localshare.domain.model.FileInfo
import kotlinx.coroutines.flow.StateFlow

interface FileProvider {
    val fileList: StateFlow<List<FileInfo>>
}
