package com.defname.localshare.service.ktor.routes

import android.content.Context
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.model.FileInfo
import com.defname.localshare.service.ServerSecurityHandler
import com.defname.localshare.service.ktor.json.toJsonString
import io.ktor.server.routing.Route
import io.ktor.server.sse.sse
import io.ktor.sse.ServerSentEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.flow

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
    context: Context
) {
    sse("/{token}/events") {

        val routingCall = call as? io.ktor.server.routing.RoutingCall

        if (routingCall == null || !securityHandler.verifyAccess(routingCall)) {
            return@sse
        }


        serviceRepository.fileList
            .asDeltaEvents()
            .drop(1)
            .collect { delta ->
                if (!securityHandler.verifyAccess(routingCall)) {
                    return@collect
                }
                when (delta) {
                    is FileDelta.Added -> {
                        send(ServerSentEvent(
                            data = delta.file.toJsonString(),
                            event = "add"
                        ))
                    }
                    is FileDelta.Removed -> {
                        send(ServerSentEvent(
                            data = delta.fileId,
                            event = "remove"
                        ))
                    }
                }
            }
    }
}
