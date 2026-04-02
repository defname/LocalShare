package com.defname.sendfile

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.Size
import android.webkit.MimeTypeMap
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID



data class ServerState(
    val fileList: List<FileInfo> = emptyList(),
    val token: String = UUID.randomUUID().toString(),
    val isRunning: Boolean = false,
    val activeClients: List<String> = emptyList(),
    val bannedIps: Set<String> = emptySet(),
    val localIpAddresses: List<NetworkInfo> = emptyList(),
    val selectedIp: String = "0.0.0.0"
)

object ServerRepository {
    /** Private state flow for the UI to observe */
    private val _state = MutableStateFlow(ServerState())
    /** Public state flow (read-only) */
    val state: StateFlow<ServerState> = _state.asStateFlow()  //< public state flow (readonly)

    /* controlling functions for the UI */

    fun setSelectedIp(ip: String?) {
        _state.update { it.copy(selectedIp = ip ?: "0.0.0.0") }
    }

    fun addFile(context: Context, uri: Uri) {
        if (_state.value.fileList.find { it.uri == uri } != null) {
            return
        }
        _state.update { it.copy(
            fileList = it.fileList + getFileInfo(context, uri)
        )}
    }

    fun addFiles(context: Context, uris: List<Uri>) {
        uris.forEach { addFile(context, it) }
    }

    fun removeFile(uri: Uri) {
        _state.update { it.copy(fileList = it.fileList.filter { el -> el.uri != uri } ) }
    }

    fun clearFiles() {
        _state.update { it.copy(fileList = emptyList()) }
    }

    fun setToken(token: String) {
        if (token.isNotEmpty() && token.all { char -> char.isLetterOrDigit() || char == '-' || char == '_' }) {
            _state.update { it.copy(token = token) }
        }
    }

    fun setRandomToken() {
        _state.update { it.copy(token = UUID.randomUUID().toString()) }
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
