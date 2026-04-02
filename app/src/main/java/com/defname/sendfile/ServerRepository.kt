package com.defname.sendfile

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID

data class LogEntry (
    val timestamp: Long = System.currentTimeMillis(),
    val method: String,
    val path: String,
    val status: Int,
    val clientIp: String
)

data class ConnectionRequest(
    val id: String = UUID.randomUUID().toString(),
    val clientIp: String,
    val deferred: CompletableDeferred<Boolean> = CompletableDeferred()
)

data class ServerState(
    val fileList: List<FileInfo> = emptyList(),
    val token: String = UUID.randomUUID().toString(),
    val isRunning: Boolean = false,
    val activeClients: List<String> = emptyList(),
    val blacklist: Set<String> = emptySet(),
    val whitelist: Set<String> = emptySet(),
    val localIpAddresses: List<NetworkInfo> = emptyList(),
    val selectedIp: String = "0.0.0.0",
    val logs: List<LogEntry> = emptyList(),
    val pendingRequests: List<ConnectionRequest> = emptyList()
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

    fun addLog(entry: LogEntry) {
        Log.d("ServerRepository", "Adding log: $entry")
        _state.update { it.copy(logs = it.logs + entry) }
    }

    fun clearLogs() {
        _state.update { it.copy(logs = emptyList()) }
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

    fun addToBlacklist(ip: String) {
        removeFromWhitelist(ip)
        _state.update { it.copy(blacklist = it.blacklist + ip) }
        Log.d("ServerRepository", "add to blacklist $ip")
    }

    fun removeFromBlacklist(ip: String) {
        _state.update {
            val newList = it.blacklist.toMutableSet()
            newList.remove(ip)
            it.copy(blacklist = newList)
        }
        Log.d("ServerRepository", "remove from blacklist $ip")
    }

    fun isBlacklisted(ip: String): Boolean {
        return _state.value.blacklist.contains(ip)
    }

    fun addToWhitelist(ip: String) {
        removeFromBlacklist(ip)
        _state.update { it.copy(whitelist = it.whitelist + ip) }
        Log.d("ServerRepository", "add to whitelist $ip")
    }

    fun removeFromWhitelist(ip: String) {
        _state.update {
            val newList = it.whitelist.toMutableSet()
            newList.remove(ip)
            it.copy(whitelist = newList)
        }
        Log.d("ServerRepository", "remove from whitelist $ip")
    }

    fun isWhitelisted(ip: String): Boolean {
        return _state.value.whitelist.contains(ip)
    }

    fun updateLocalIpAddresses() {
        _state.update { it.copy(localIpAddresses = getLocalIpAddresses()) }
    }

    fun askForPermission(clientIp: String): CompletableDeferred<Boolean> {
        val request = ConnectionRequest(clientIp = clientIp)
        _state.update { it.copy(pendingRequests = it.pendingRequests + request) }
        return request.deferred
    }

    fun approvaRequestIp(ip: String, approved: Boolean) {
        val request = _state.value.pendingRequests.find { it.clientIp == ip }
        if (request != null) {
            approveRequest(request.id, approved)
        }
    }

    fun approveRequest(clientIp: String, approved: Boolean) {
        val request = _state.value.pendingRequests.find { it.clientIp == clientIp }

        if (request != null) {
            request.deferred.complete(approved)
            if (approved) {
                addToWhitelist(clientIp)
            }
            else {
                addToBlacklist(clientIp)
            }
            // Aus den ausstehenden Anfragen entfernen
            _state.update { it.copy(pendingRequests = it.pendingRequests.filter { it.clientIp != clientIp }) }
        } else {
            Log.e("ServerRepository", "No pending request found for IP: $clientIp")
            // Falls approved, trotzdem whitelisten (Sicherheitsnetz)
            if (approved) addToWhitelist(clientIp)
        }
    }
}
