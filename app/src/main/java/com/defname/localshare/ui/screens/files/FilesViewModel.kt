package com.defname.localshare.ui.screens.files

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.FileInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

class FilesViewModel(
    private val serviceRepository: ServiceRepository
) : ViewModel() {
    private val _selectedFiles = MutableStateFlow<Set<Uri>>(emptySet())
    private val _sortedBy = MutableStateFlow<FilesSortType>(FilesSortType.NAME)
    private val _searchQuery = MutableStateFlow<String>("")

    val state = combine(
        serviceRepository.fileList,
        _selectedFiles,
        _sortedBy,
        _searchQuery
    ) { fileList, selectedFiles, sortedBy, searchQuery ->
        FilesState(
            fileList = fileList,
            selectedFiles = selectedFiles,
            sortedBy = sortedBy,
            searchQuery = searchQuery
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = FilesState()
    )

    fun onToggleSelection(file: FileInfo) {
        val uri = file.uri
        _selectedFiles.update {
            if (it.contains(uri)) {
                it - uri
            } else {
                it + uri
            }
        }
    }
}