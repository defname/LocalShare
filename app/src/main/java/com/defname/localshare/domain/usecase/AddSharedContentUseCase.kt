package com.defname.localshare.domain.usecase

import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.SharedContent

class AddSharedContentUseCase(
    private val serviceRepository: ServiceRepository
) {
    operator fun invoke(
        mimeType: String?,
        text: String?,
        subject: String?
    ) {
        val content = when (mimeType) {
            "text/plain" -> SharedContent.Text(text ?: "")
            else -> SharedContent.Other(
                data = text ?: "",
                mimeType = mimeType ?: "",
                label = subject
            )
        }
        serviceRepository.addContent(content)
    }
}