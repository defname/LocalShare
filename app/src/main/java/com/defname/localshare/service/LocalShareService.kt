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
        const val APPROVE_IP = "com.defname.localshare.APPROVE_IP"
        const val DENY_IP = "com.defname.localshare.DENY_IP"
        const val ACTION_GRANT_PERMISSION = "com.defname.localshare.GRANT_PERMISSION"
    }

    private val activeUriPermissions = mutableSetOf<Uri>()

    override fun onCreate() {
        super.onCreate()
        idleManager.startMonitoring {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ip = intent?.getStringExtra("ip") ?: ""
        
        // Delegierte Berechtigungen für den gesamten Prozess verankern
        intent?.clipData?.let { clipData ->
            for (i in 0 until clipData.itemCount) {
                clipData.getItemAt(i).uri?.let { activeUriPermissions.add(it) }
            }
        }

        when (intent?.action) {
            ACTION_GRANT_PERMISSION -> {
                // Die URIs wurden bereits am Anfang der Funktion in activeUriPermissions
                // gespeichert. Da ACTION_GRANT_PERMISSION nur aufgerufen wird, wenn der 
                // Server bereits läuft, ist keine weitere Aktion nötig.
            }
            ACTION_START -> startHttpServer()
            ACTION_STOP -> stopSelf()
            APPROVE_IP -> {
                securityHandler.approveIp(ip)
                cancelNotification(ip)
            }
            DENY_IP -> {
                securityHandler.blockIp(ip)
                cancelNotification(ip)
            }
        }
        return START_NOT_STICKY
    }

    private fun cancelNotification(ip: String) {
        val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(ip.hashCode())
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startHttpServer() {
        serviceScope.launch {
            val settings = settingsRepository.settingsFlow.first()

            // 1. Status auf STARTING setzen (UI zeigt jetzt den Spinner)
            serviceRepository.serverStarting()

            // 2. Basis-Notification bauen und zeigen
            val notification = notificationHelper.buildBaseNotification(
                serverIp = settings.serverIp,
                port = settings.serverPort,
                isRunning = false // Spinner in der Notification, kein Stop-Button
            )
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)

            // 3. Ktor Start
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

                    // 4. Notification auf "Running" updaten (Stop-Button zeigen)
                    val runningNotification = notificationHelper.buildBaseNotification(
                        serverIp = settings.serverIp,
                        port = settings.serverPort,
                        isRunning = true
                    )
                    val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
                    manager.notify(NotificationHelper.NOTIFICATION_ID, runningNotification)
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            this@LocalShareService,
                            "Error starting server: ${e.localizedMessage}",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                    server = null
                    stopSelf()
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

    override fun onDestroy() {
        stopHttpServer()
        serviceScope.cancel()
        super.onDestroy()
    }
}