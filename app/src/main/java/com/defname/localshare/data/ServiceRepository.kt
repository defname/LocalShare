/*
 * LocalShare - Share files locally
 * Copyright (C) 2024 defname
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.defname.localshare.data

import android.net.Uri
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.SharedContent
import com.defname.localshare.domain.repository.FileProvider
import io.ktor.util.AttributeKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update

enum class RuntimeState {
    RUNNING,
    STARTING,
    STOPPING,
    STOPPED
}

object CallAttributes {
    val connectionId = AttributeKey<String>("connectionId")
}


data class RuntimeData(
    val fileList: List<FileInfo> = emptyList(),
    val sharedContentList: List<SharedContent> = emptyList(),
    val serviceState: RuntimeState = RuntimeState.STOPPED,
)

class ServiceRepository(
    repositoryScope: CoroutineScope
) : FileProvider {

    private val _runtimeState = MutableStateFlow(RuntimeData())
    val runtimeState = _runtimeState.asStateFlow()

    override val fileList: StateFlow<List<FileInfo>> = _runtimeState
        .map { it.fileList }
        .distinctUntilChanged()
        .stateIn(
            repositoryScope,
            started = SharingStarted.Eagerly,
            initialValue = emptyList()
        )

    fun addFile(file: FileInfo) {
        _runtimeState.update { state ->
            if (state.fileList.any { it.uri == file.uri }) {
                state
            } else {
                state.copy(fileList = state.fileList + file)
            }
        }
    }
    fun removeFile(uri: Uri) { _runtimeState.update { it.copy(fileList = it.fileList.filter { it.uri != uri }) } }
    fun removeFiles(uris: Set<Uri>) { _runtimeState.update { it.copy(fileList = it.fileList.filter { !uris.contains(it.uri) }) } }
    fun clearFiles() { _runtimeState.update { it.copy(fileList = emptyList()) } }

    fun addContent(content: SharedContent) {
        _runtimeState.update { it.copy(sharedContentList = it.sharedContentList + content) }
    }

    fun serverStarting() { _runtimeState.update { it.copy(serviceState = RuntimeState.STARTING) } }
    fun serverStarted() { _runtimeState.update { it.copy(serviceState = RuntimeState.RUNNING) } }
    fun serverStopping() { _runtimeState.update { it.copy(serviceState = RuntimeState.STOPPING) } }
    fun serverStopped() { _runtimeState.update { it.copy(serviceState = RuntimeState.STOPPED) } }

    fun serverRunning() = _runtimeState.value.serviceState == RuntimeState.RUNNING

}

