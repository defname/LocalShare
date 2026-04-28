package com.defname.localshare.ui.screens.sharedcontent

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.usecase.AddSharedContentUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class SharedContentViewModel(
    val serviceRepository: ServiceRepository,
    val addSharedContentUseCase: AddSharedContentUseCase
) : ViewModel() {
    private val _selectedItems = MutableStateFlow<Set<Int>>(emptySet())
    private val _hasClipboardContent = MutableStateFlow(false)
    private val _expandedItem = MutableStateFlow<Int?>(null)

    val state = combine(
        serviceRepository.runtimeState,
        _selectedItems,
        _hasClipboardContent,
        _expandedItem
    ) { runtimeState, selectedItems, hasClipboardContent, expandedItem ->
        SharedContentState(
            sharedContentList = runtimeState.sharedContentList,
            selectedItems = selectedItems,
            hasClipboardContent = hasClipboardContent,
            expandedItem = expandedItem
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SharedContentState()
    )

    fun onClearSelection() {
        _selectedItems.update { emptySet() }
    }

    fun onSelectAll() {
        _selectedItems.update { state.value.sharedContentList.map { it.id }.toSet() }
    }

    fun onToggleSelection(id: Int) {
        _selectedItems.update { currentSelection ->
            if (id in currentSelection) {
                currentSelection - id
            } else {
                _expandedItem.update { null }
                currentSelection + id
            }
        }
    }

    fun onRemoveSelected() {
        serviceRepository.removeContent(_selectedItems.value)
        onClearSelection()
    }

    fun updateClipboardStatus(hasContent: Boolean) {
        _hasClipboardContent.update { hasContent }
    }

    fun addClipboardContent(text: String) {
        addSharedContentUseCase("text/plain", text, null)
    }

    fun onExpand(id: Int) {
        _expandedItem.update { if (it == id) null else id }
    }
}
