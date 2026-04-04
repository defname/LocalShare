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
package com.defname.localshare

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.util.Log
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.util.UUID
import androidx.core.content.edit

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

data class WhiteListEntry(
    val ip: String,
    val timestamp: Long = System.currentTimeMillis()
)

data class ServerState(
    val fileList: List<FileInfo> = emptyList(),
    val token: String = UUID.randomUUID().toString(),
    val isRunning: Boolean = false,
    val activeClients: List<String> = emptyList(),
    val blacklist: Set<String> = emptySet(),
    val whitelist: List<WhiteListEntry> = emptyList(),
    val whiteListEntryTTLSeconds: Int = 60 * 60,
    val localIpAddresses: List<NetworkInfo> = emptyList(),
    val selectedIp: String = "0.0.0.0",
    val logs: List<LogEntry> = emptyList(),
    val pendingRequests: List<ConnectionRequest> = emptyList(),
    val port: Int = 8080,
    val idleTimeoutSeconds: Int = 300,
    val requireApproval: Boolean = true,
    val approvalTimeoutSeconds: Int = 30,
    val keepScreenOn: Boolean = true,
    val clearFileListOnSendIntent: Boolean = false
)

object ServerRepository {
    /** Private state flow for the UI to observe */
    private val _state = MutableStateFlow(ServerState())
    private lateinit var _sharedPrefs: SharedPreferences

    /** Public state flow (read-only) */
    val state: StateFlow<ServerState> = _state.asStateFlow()  //< public state flow (readonly)


    fun init(context: Context) {
        updateLocalIpAddresses()
        _sharedPrefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)
        val sharedToken = _sharedPrefs.getString("token", UUID.randomUUID().toString())
        val sharedSelectedIp = _sharedPrefs.getString("selectedIp", "0.0.0.0")

        _state.update{ it.copy(
            token = sharedToken ?: UUID.randomUUID().toString(),
            selectedIp = _state.value.localIpAddresses.find { it.ip == sharedSelectedIp }?.ip ?: "0.0.0.0",
            port = _sharedPrefs.getInt("port", 8080),
            idleTimeoutSeconds = _sharedPrefs.getInt("idleTimeoutSeconds", 30),
            requireApproval = _sharedPrefs.getBoolean("requireApproval", true),
            keepScreenOn = _sharedPrefs.getBoolean("keepScreenOn", true),
            whiteListEntryTTLSeconds = _sharedPrefs.getInt("whiteListEntryTTL", 60 * 60),
            approvalTimeoutSeconds = _sharedPrefs.getInt("approvalTimeoutSeconds", 30),
            clearFileListOnSendIntent = _sharedPrefs.getBoolean("clearFileListOnSendIntent", false)
        )}
    }

    fun setSelectedIp(ip: String?) {
        _state.update { it.copy(selectedIp = ip ?: "0.0.0.0") }
        _sharedPrefs.edit { putString("selectedIp", ip ?: "0.0.0.0") }

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
            _sharedPrefs.edit { putString("token", token) }
        }
    }

    fun setRandomToken() {
        setToken(UUID.randomUUID().toString())
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

    /* setting state for the service */
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
        _state.update { it.copy(whitelist = it.whitelist + WhiteListEntry(ip, System.currentTimeMillis())) }
        Log.d("ServerRepository", "add to whitelist $ip")
        Log.d("ServerRepository", "whitelist: ${_state.value.whitelist}")
    }

    fun removeFromWhitelist(ip: String) {
        _state.update {
            it.copy(whitelist = it.whitelist.filter{ e -> e.ip != ip })
        }
        Log.d("ServerRepository", "remove from whitelist $ip")
    }

    fun isWhitelisted(ip: String): Boolean {
        val entry = _state.value.whitelist.find{ e -> e.ip == ip }
        if (entry == null) {
            return false
        }
        if (entry.timestamp + _state.value.whiteListEntryTTLSeconds * 1000 < System.currentTimeMillis()) {
            removeFromWhitelist(ip)
            return false
        }
        return true
    }

    fun setWhiteListEntryTTLSeconds(seconds: Int) {
        if (seconds < 0) {
            return
        }
        _state.update { it.copy(whiteListEntryTTLSeconds = seconds) }
        _sharedPrefs.edit {
            putInt("whiteListEntryTTL", seconds)
        }
    }

    fun updateLocalIpAddresses() {
        _state.update { it.copy(localIpAddresses = getLocalIpAddresses()) }
    }

    fun askForPermission(clientIp: String): CompletableDeferred<Boolean> {
        val request = ConnectionRequest(clientIp = clientIp)
        _state.update { it.copy(pendingRequests = it.pendingRequests + request) }
        return request.deferred
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

    fun setIdleTimeoutSeconds(seconds: Int) {
        if (seconds < 0) {
            return
        }
        _state.update { it.copy(idleTimeoutSeconds = seconds) }
        _sharedPrefs.edit {
            putInt("idleTimeoutSeconds", seconds)
        }
    }

    fun setPort(port: Int) {
        if (port < 1024 || port > 65535) {
            return
        }
        _state.update { it.copy(port = port) }
        _sharedPrefs.edit {
            putInt("port", port)
        }
    }

    fun setRequireApproval(requireApproval: Boolean) {
        _state.update { it.copy(requireApproval = requireApproval) }
        _sharedPrefs.edit {
            putBoolean("requireApproval", requireApproval)
        }
    }

    fun setKeepScreenOn(keepScreenOn: Boolean) {
        _state.update { it.copy(keepScreenOn = keepScreenOn) }
        _sharedPrefs.edit {
            putBoolean("keepScreenOn", keepScreenOn)
        }
    }

    fun setClearFilesListOnSendIntent(clearFileListOnSendIntent: Boolean)    {
        _state.update { it.copy(clearFileListOnSendIntent = clearFileListOnSendIntent) }
        _sharedPrefs.edit {
            putBoolean("clearFileListOnSendIntent", clearFileListOnSendIntent)
        }
    }

    fun getServerAdress(fileId: String? = null, download: Boolean = false): String {
        val s = _state.value
        val displayIp = if (s.selectedIp == "0.0.0.0") {
            getSmartDefaultIp(s.localIpAddresses)
        } else s.selectedIp

        val baseUrl = "http://${displayIp}:${s.port}/${s.token}"
        val filePart = if (fileId != null) "/$fileId" else ""

        return "$baseUrl/${if (download) "download" else "stream"}$filePart"
    }
}
