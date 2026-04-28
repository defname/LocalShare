/*
 * LocalShare - Share files locally
 * Copyright (C) 2026 defname
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
