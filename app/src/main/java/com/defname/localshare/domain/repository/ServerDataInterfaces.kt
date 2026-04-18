package com.defname.localshare.domain.repository

import com.defname.localshare.domain.model.FileInfo
import kotlinx.coroutines.flow.StateFlow

interface FileProvider {
    val fileList: StateFlow<List<FileInfo>>
}
