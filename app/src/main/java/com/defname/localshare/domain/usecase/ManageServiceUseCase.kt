// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.domain.usecase

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.service.LocalShareService

class ManageServiceUseCase(
    private val context: Context,
    private val serviceRepository: ServiceRepository
) {
    fun startService() {
        // Korrekter Zugriff auf die Dateiliste im Repository
        val uris = serviceRepository.runtimeState.value.fileList.map { it.uri }
        
        val intent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.ACTION_START
            
            if (uris.isNotEmpty()) {
                clipData = ClipData.newRawUri("Shared Files", uris.first()).apply {
                    for (i in 1 until uris.size) {
                        addItem(ClipData.Item(uris[i]))
                    }
                }
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService() {
        val intent = Intent(context, LocalShareService::class.java)
        context.stopService(intent)
    }
}
