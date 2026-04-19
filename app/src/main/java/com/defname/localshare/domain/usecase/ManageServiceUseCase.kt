package com.defname.localshare.domain.usecase

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.defname.localshare.service.LocalShareService

class ManageServiceUseCase(
    private val context: Context
) {
    fun startService() {
        val intent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.ACTION_START
        }
        ContextCompat.startForegroundService(context, intent)
    }

    fun stopService() {
        val intent = Intent(context, LocalShareService::class.java)
        context.stopService(intent)
    }
}