package com.defname.localshare.ui.screens.sharedcontent

import com.defname.localshare.domain.model.SharedContent

data class SharedContentState(
    val sharedContentList: List<SharedContent> = emptyList(),
    val selectedItems: Set<Int> = emptySet(),
    val hasClipboardContent: Boolean = false
)