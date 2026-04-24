package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.util.Log
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.json.toJsonString
import io.ktor.http.CacheControl
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.cacheControl
import io.ktor.server.response.header
import io.ktor.server.response.respondOutputStream
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

sealed class FileDelta {
    data class Added(val file: FileInfo) : FileDelta()
    data class Removed(val fileId: String) : FileDelta()
}

// Dieses Modul beobachtet dein bestehendes StateFlow
fun Flow<List<FileInfo>>.asDeltaEvents(): Flow<FileDelta> = flow {
    var oldList = emptyList<FileInfo>()
    this@asDeltaEvents.collect { newList ->
        // 1. Was ist neu? (In newList, aber nicht in oldList)
        val added = newList.filter { newItem -> oldList.none { it.id == newItem.id } }
        added.forEach { emit(FileDelta.Added(it)) }

        // 2. Was wurde gelöscht? (In oldList, aber nicht in newList)
        val removed = oldList.filter { oldItem -> newList.none { it.id == oldItem.id } }
        removed.forEach { emit(FileDelta.Removed(it.id)) }

        oldList = newList // Der neue Zustand wird zum 'oldList' für den nächsten Schritt
    }
}

fun Route.getEvents(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    connectionLogsRepository: ConnectionLogsRepository,
    context: Context
) {
    get ("/{token}/events") {

        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        call.response.cacheControl(CacheControl.NoCache(CacheControl.Visibility.Private))
        call.response.header(HttpHeaders.ContentType, ContentType.Text.EventStream.toString())
        call.response.header(HttpHeaders.Connection, "keep-alive")
        call.response.header("X-Accel-Buffering", "no")

        call.respondOutputStream {
            // Wir nutzen den OutputStream, um die Kontrolle über das Flushing zu haben
            val writer = bufferedWriter()

            // Standardmäßig gehen wir von einem Server-Shutdown aus
            var disconnectReason: DisconnectReason = DisconnectReason.ServerShutdown

            try {
                coroutineScope {

                    // 🔄 Event-Collector (reagiert auf Datei-Änderungen)
                    launch {
                        serviceRepository.fileList
                            .asDeltaEvents()
                            .drop(1)
                            .collect { delta ->
                                // Sofort-Check bei Datei-Event (falls IP gerade gebannt wurde)
                                if (!securityHandler.verifyAccess(call)) {
                                    disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                    this@coroutineScope.cancel("Access denied")
                                    return@collect
                                }

                                val eventName = when (delta) {
                                    is FileDelta.Added -> "add"
                                    is FileDelta.Removed -> "remove"
                                }

                                val data = when (delta) {
                                    is FileDelta.Added -> delta.file.toJsonString()
                                        .replace("\n", "")
                                        .replace("\r", "")

                                    is FileDelta.Removed -> delta.fileId
                                }

                                writer.write("event: $eventName\n")
                                writer.write("data: $data\n\n")
                                writer.flush()
                            }
                    }

                    // ❤️ Heartbeat Loop (prüft regelmäßig und hält Verbindung offen)
                    launch {
                        while (true) {
                            delay(1000)   // TODO Add to settings!
                            if (!securityHandler.verifyAccess(call)) {
                                disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                this@coroutineScope.cancel("Access denied")
                            }
                            writer.write(": heartbeat\n\n")
                            writer.flush()
                        }
                    }
                }
            } catch (e: Exception) {
                // Wenn es KEINE CancellationException ist (z.B. IOException weil Socket zu),
                // dann ist der Client weg.
                if (e !is kotlinx.coroutines.CancellationException) {
                    disconnectReason = DisconnectReason.Unexpected.ClientGone
                }
            } finally {
                val connectionId = call.attributes.getOrNull(CallAttributes.connectionId)
                if (connectionId != null) {
                    connectionLogsRepository.clientDisconnected(connectionId, disconnectReason)
                } else {
                    Log.d("FileServerService", "Client disconnected without connectionId")
                }
            }
        }
    }
}
