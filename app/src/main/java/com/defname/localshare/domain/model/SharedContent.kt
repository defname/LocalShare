package com.defname.localshare.domain.model

import java.util.concurrent.atomic.AtomicInteger

sealed class SharedContent {
    val id: Int = nextId.getAndIncrement()
    abstract val mimeType: String

    companion object {
        private val nextId = AtomicInteger(0)
    }

    data class Text(val text: String) : SharedContent() {
        override val mimeType = "text/plain"
    }

    data class Other(
        val data: String, // content from EXTRA_TEXT
        override val mimeType: String,  // intent.type
        val label: String? = null  // optional from EXTRA_SUBJECT
    ) : SharedContent()
}