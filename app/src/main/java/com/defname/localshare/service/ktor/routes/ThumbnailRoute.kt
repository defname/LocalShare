package com.defname.localshare.service.ktor.routes

import android.content.Context
import android.graphics.Bitmap
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.ServerSecurityHandler
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondBytes
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import kotlinx.coroutines.flow.first
import java.io.ByteArrayOutputStream

fun Route.getThumbnail(
    securityHandler: ServerSecurityHandler,
    serviceRepository: ServiceRepository,
    context: Context
) {
    get("/{token}/thumbnail/{file}") {
        if (!securityHandler.verifyAccess(call)) {
            return@get call.respondText("No Access.", status = HttpStatusCode.Forbidden)
        }

        val fileList = serviceRepository.fileList.first()

        val requestedFile = call.parameters["file"]

        val fileInfo = fileList.find { it.id == requestedFile }
        if (fileInfo == null) {
            return@get call.respondText("File not found.\n", status = HttpStatusCode.NotFound)
        }

        val thumbnail = fileInfo.filePreview
        if (thumbnail == null) {
            return@get call.respondText(
                "No thumbnail found.\n",
                status = HttpStatusCode.NotFound
            )
        }
        val stream = ByteArrayOutputStream()
        thumbnail.compress(Bitmap.CompressFormat.PNG, 100, stream)
        call.respondBytes(stream.toByteArray(), ContentType.Image.PNG)
    }
}