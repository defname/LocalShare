package com.defname.sendfile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class FileInfo(
    val uri: Uri,
    val name: String,
    val size: Long = 0L,
    val filePreview: Bitmap? = null
)

data class ServerState(
    val fileList: List<FileInfo> = emptyList(),
    val deliverAsStream: Boolean = false,
    val useCustomToken: Boolean = false,
    val token: String? = null,
    val customToken: String? = null,
    val isRunning: Boolean = false,
    val activeClients: List<String> = emptyList(),
    val bannedIps: Set<String> = emptySet(),
    val localIpAddresses: List<NetworkInfo> = emptyList()
)

object ServerRepository {
    /** Private state flow for the UI to observe */
    private val _state = MutableStateFlow(ServerState())
    /** Public state flow (read-only) */
    val state: StateFlow<ServerState> = _state.asStateFlow()  //< public state flow (readonly)

    /* controlling functions for the UI */
    /** fileUri setter */
    fun addFile(context: Context, uri: Uri) {
        _state.update { it.copy(
            fileList = it.fileList + getFileInfo(context, uri)
        )}
    }

    fun addFiles(context: Context, uris: List<Uri>) {
        uris.forEach { addFile(context, it) }
    }

    fun removeFile(idx: Int) {
        _state.update { it.copy(fileList = it.fileList.filterIndexed { index, _ -> index != idx }) }
    }

    fun removeFile(uri: Uri) {
        _state.update { it.copy(fileList = it.fileList.filter {u -> u != uri }) }
    }

    fun clearFiles() {
        _state.update { it.copy(fileList = emptyList()) }
    }

    fun setUseCustomToken(context: Context, useCustomToken: Boolean) {
        _state.update { it.copy(useCustomToken = useCustomToken) }
    }

    fun setDeliverAsStream(deliverAsStream: Boolean) {
        _state.update { it.copy(deliverAsStream = deliverAsStream) }
    }

    fun setCustomToken(context: Context, token: String) {
        _state.update { it.copy(customToken = token) }
    }

    fun getToken(): String {
        return (if (_state.value.useCustomToken) _state.value.customToken else _state.value.token) ?: ""
    }

    fun startServer(context: Context) {
        val intent = Intent(context, FileServerService::class.java).apply {
            action = "START_SERVER"
        }
        context.startForegroundService(intent)
    }

    fun stopServer(context: Context) {
        val intent = Intent(context, FileServerService::class.java).apply {
            action = "STOP_SERVER"
        }
        context.stopService(intent)
    }

    /* setting state for for the service */
    fun onServerStarted() {
        _state.update { it.copy(isRunning = true) }
        Log.d("ServerRepository", "Server started")
    }

    fun onServerStopped() {
        _state.update { it.copy(isRunning = false) }
        Log.d("ServerRepository", "Server stopped")
    }

    fun onClientConnected(ip: String) {
        _state.update { it.copy(activeClients = it.activeClients + ip) }
        Log.d("ServerRepository", "Client connected: $ip")
    }

    fun onClientDisconnected(ip: String) {
        _state.update {
            val newList = it.activeClients.toMutableList()
            newList.remove(ip)
            it.copy(activeClients = newList)
        }
        Log.d("ServerRepository", "Client disconnected: $ip")
    }

    fun banIp(ip: String) {
        _state.update { it.copy(bannedIps = it.bannedIps + ip) }
        Log.d("ServerRepository", "IP banned: $ip")
    }

    fun unbanIp(ip: String) {
        _state.update {
            val newList = it.bannedIps.toMutableSet()
            newList.remove(ip)
            it.copy(bannedIps = newList)
        }
        Log.d("ServerRepository", "IP unbanned: $ip")
    }

    fun updateLocalIpAddresses() {
        _state.update { it.copy(localIpAddresses = getLocalIpAddresses()) }
    }
}

fun getFileThumbnail(context: Context, fileUri: Uri?): Bitmap? {
    if (fileUri == null) {
        return null
    }
    return try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            context.contentResolver.loadThumbnail(fileUri, Size(512, 512), null)
        }
        else {
            null
        }
    } catch(e: Exception) {
        e.printStackTrace()
        null
    }
}

fun getFileInfo(context: Context, uri: Uri): FileInfo {

    var name: String = ""
    var size: Long = 0L

    context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
        val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
        if (cursor.moveToFirst()) {
            name = cursor.getString(nameIndex)
        }
        val sizeIndex = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE)
        if (cursor.moveToFirst()) {
            size = cursor.getLong(sizeIndex)
        }
    }

    return FileInfo(uri, name, size, getFileThumbnail(context, uri))
}
