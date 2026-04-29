// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.ui.screens.sharedcontent

import com.defname.localshare.domain.model.SharedContent

data class SharedContentState(
    val sharedContentList: List<SharedContent> = emptyList(),
    val selectedItems: Set<Int> = emptySet(),
    val hasClipboardContent: Boolean = false,
    val expandedItem: Int? = null
)