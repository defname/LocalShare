package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.util.Log
import com.defname.localshare.data.CallAttributes
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.DisconnectReason
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.domain.model.SharedContent
import com.defname.localshare.domain.repository.SettingsRepository
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
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.BufferedWriter

sealed class FlowDelta<T>(val obj: T) {
    class Added<T>(obj: T) : FlowDelta<T>(obj)
    class Removed<T>(obj: T) : FlowDelta<T>(obj)
}

fun <T, K>Flow<List<T>>.asDeltaEvents(key: (T) -> K): Flow<FlowDelta<T>> = flow {
    var oldList = emptyList<T>()
    this@asDeltaEvents.collect { newList ->
        // 1. Was ist neu? (In newList, aber nicht in oldList)
        val added = newList.filter { newItem -> oldList.none { key(it) == key(newItem) } }
        added.forEach { emit(FlowDelta.Added(it)) }

        // 2. Was wurde gelöscht? (In oldList, aber nicht in newList)
        val removed = oldList.filter { oldItem -> newList.none { key(it) == key(oldItem) } }
        removed.forEach { emit(FlowDelta.Removed(it)) }

        oldList = newList
    }
}

@JvmName("asFileInfoDeltaEvents")
fun Flow<List<FileInfo>>.asDeltaEvents() = asDeltaEvents { it.id }
@JvmName("asSharedContentDeltaEvents")
fun Flow<List<SharedContent>>.asDeltaEvents() = asDeltaEvents { it.id }


fun BufferedWriter.writeEvent(eventName: String, data: String) {
    write("event: $eventName\n")
    write("data: $data\n\n")
    flush()
}

fun BufferedWriter.writeHeartbeat() {
    write(": heartbeat\n\n")
    flush()
}

fun Route.getEvents(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    settingsRepository: SettingsRepository,
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
                    writer.writeEvent("init", "connected")

                    val sharedContentList = serviceRepository.runtimeState.map { it.sharedContentList }
                        .stateIn(
                            this@coroutineScope,
                            started = kotlinx.coroutines.flow.SharingStarted.Eagerly,
                            initialValue = emptyList()
                        )

                    // 🔄 Event-Collector (reagiert auf Datei-Änderungen)
                    launch {
                        serviceRepository.fileList
                            .asDeltaEvents()
                            .collect { delta ->
                                // Sofort-Check bei Datei-Event (falls IP gerade gebannt wurde)
                                if (!securityHandler.verifyAccess(call)) {
                                    disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                    this@coroutineScope.cancel("Access denied")
                                    return@collect
                                }

                                val eventName = when (delta) {
                                    is FlowDelta.Added -> "add"
                                    is FlowDelta.Removed -> "remove"
                                }

                                val data = when (delta) {
                                    is FlowDelta.Added -> delta.obj.toJsonString()

                                    is FlowDelta.Removed -> delta.obj.id
                                }

                                writer.writeEvent(eventName, data)
                            }
                    }

                    launch {
                        sharedContentList
                            .asDeltaEvents()
                            .collect { delta ->
                                // Sofort-Check bei Datei-Event (falls IP gerade gebannt wurde)
                                if (!securityHandler.verifyAccess(call)) {
                                    disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                    this@coroutineScope.cancel("Access denied")
                                    return@collect
                                }

                                val eventName = when (delta) {
                                    is FlowDelta.Added -> "addSharedContent"
                                    is FlowDelta.Removed -> "removeSharedContent"
                                }

                                val data = when (delta) {
                                    is FlowDelta.Added -> delta.obj.toJsonString()
                                    is FlowDelta.Removed -> delta.obj.id.toString()
                                }

                                writer.writeEvent(eventName, data)
                            }
                    }

                    // ❤️ Heartbeat Loop (prüft regelmäßig und hält Verbindung offen)
                    launch {
                        while (true) {
                            delay(settingsRepository.settingsFlow.first().sseHeartbeatPeriodSeconds * 1000L)
                            if (!securityHandler.verifyAccess(call)) {
                                disconnectReason = DisconnectReason.Unexpected.AuthInvalid
                                this@coroutineScope.cancel("Access denied")
                            }
                            writer.writeHeartbeat()
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
