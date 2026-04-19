package com.defname.localshare.domain.usecase

import android.net.Uri
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.ServiceRepository

class AddFilesUseCase(
    val fileInfoProvider: FileInfoProvider,
    val serviceRepository: ServiceRepository
) {
    operator fun invoke(uri: Uri) {
        val fileInfo = fileInfoProvider.getFileInfo(uri)
        serviceRepository.addFile(fileInfo)
    }
    operator fun invoke(uris: List<Uri>) {
        for (uri in uris) {
            invoke(uri)
        }
    }
}