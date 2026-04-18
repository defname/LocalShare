package com.defname.localshare.domain.usecase

import android.net.Uri
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.ServiceRepository

class AddFilesUseCase(
    val fileInfoProvider: FileInfoProvider,
    val serviceRepository: ServiceRepository
) {
    fun invoke(uri: Uri) {
        val fileInfo = fileInfoProvider.getFileInfo(uri)
        serviceRepository.addFile(fileInfo)
    }
}