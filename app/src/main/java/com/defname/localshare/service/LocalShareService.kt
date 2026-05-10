// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.net.Uri
import android.os.IBinder
import android.widget.Toast
import com.defname.localshare.R
import com.defname.localshare.data.ConnectionLogsRepository
import com.defname.localshare.data.FileInfoProvider
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ktor.configureServerModule
import com.defname.localshare.service.notification.NotificationHelper
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.android.ext.android.inject

class LocalShareService : Service() {
    private val notificationHelper: NotificationHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val serviceRepository: ServiceRepository by inject()
    private val logsRepository: ConnectionLogsRepository by inject()
    private val securityHandler: ServerSecurityHandler by inject()
    private val idleManager: ServerIdleManager by inject()
    private val fileInfoProvider: FileInfoProvider by inject()

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var server: EmbeddedServer<*, *>? = null

    companion object {
        const val ACTION_START = "com.defname.localshare.START_SERVICE"
        const val ACTION_STOP = "com.defname.localshare.STOP_SERVICE"
        // Legacy IP-based actions kept for any external callers
        const val APPROVE_IP = "com.defname.localshare.APPROVE_IP"
        const val DENY_IP = "com.defname.localshare.DENY_IP"
        // New session-based actions
        const val APPROVE_SESSION = "com.defname.localshare.APPROVE_SESSION"
        const val DENY_SESSION = "com.defname.localshare.DENY_SESSION"
        const val ACTION_GRANT_PERMISSION = "com.defname.localshare.GRANT_PERMISSION"
    }

    private val activeUriPermissions = mutableSetOf<Uri>()

    override fun onCreate() {
        super.onCreate()
        idleManager.startMonitoring {
            stopSelf(getString(R.string.service_stopped_timeout))
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ip = intent?.getStringExtra("ip") ?: ""
        val sessionId = intent?.getStringExtra("sessionId") ?: ""

        intent?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { activeUriPermissions.add(it) }
            }
        }

        when (intent?.action) {
            ACTION_GRANT_PERMISSION -> { /* URIs stored above */ }
            ACTION_START -> startHttpServer()
            ACTION_STOP -> stopSelf()

            APPROVE_SESSION -> {
                if (sessionId.isNotEmpty()) {
                    securityHandler.approveSession(sessionId)
                    cancelNotification(sessionId.hashCode())
                }
            }
            DENY_SESSION -> {
                if (sessionId.isNotEmpty()) {
                    securityHandler.denySession(sessionId)
                    cancelNotification(sessionId.hashCode())
                }
            }

            // Legacy fallbacks
            APPROVE_IP -> {
                securityHandler.approveIp(ip)
                cancelNotification(ip.hashCode())
            }
            DENY_IP -> {
                securityHandler.blockIp(ip)
                cancelNotification(ip.hashCode())
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification(id: Int) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(id)
    }

    override fun onBind(p0: Intent?): IBinder? = null

    private fun startHttpServer() {
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()
            serviceRepository.serverStarting()

            val notification = notificationHelper.buildBaseNotification(
                serverIp = settings.serverIp,
                port = settings.serverPort,
                isRunning = false
            )
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)

            if (server == null) {
                try {
                    val newServer = withContext(Dispatchers.IO) {
                        val engine = embeddedServer(
                            Netty,
                            port = settings.serverPort,
                            host = settings.serverIp
                        ) {
                            configureServerModule(
                                serviceRepository = serviceRepository,
                                connectionLogsRepository = logsRepository,
                                settingsRepository = settingsRepository,
                                securityHandler = securityHandler,
                                fileInfoProvider = fileInfoProvider,
                                context = this@LocalShareService.applicationContext
                            )
                        }
                        engine.start(wait = false)
                        serviceRepository.serverStarted()
                        engine
                    }

                    server = newServer

                    val runningNotification = notificationHelper.buildBaseNotification(
                        serverIp = settings.serverIp,
                        port = settings.serverPort,
                        isRunning = true
                    )
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NotificationHelper.NOTIFICATION_ID, runningNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                    server = null
                    stopSelf(getString(R.string.service_stopped_starting_error, e.localizedMessage))
                }
            }
        }
    }

    private fun stopHttpServer() {
        serviceRepository.serverStopping()
        idleManager.stop()
        server?.stop(1000, 2000)
        server = null
        serviceRepository.serverStopped()
    }

    suspend fun stopSelf(reason: String) {
        withContext(Dispatchers.Main) {
            Toast.makeText(this@LocalShareService, reason, Toast.LENGTH_LONG).show()
        }
        stopSelf()
    }

    override fun onDestroy() {
        stopHttpServer()
        serviceScope.cancel()
        super.onDestroy()
    }
}
