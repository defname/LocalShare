package com.defname.localshare.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.defname.localshare.data.LogsRepository
import com.defname.localshare.data.ServiceRepository
import com.defname.localshare.domain.repository.SettingsRepository
import com.defname.localshare.service.ktor.configureServerModule
import com.defname.localshare.service.notification.NotificationHelper
import io.ktor.server.engine.EmbeddedServer
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class LocalShareService : Service() {
    private val notificationHelper: NotificationHelper by inject()
    private val settingsRepository: SettingsRepository by inject()
    private val serviceRepository: ServiceRepository by inject()
    private val logsRepository: LogsRepository by inject()
    private val securityHandler: ServerSecurityHandler by inject()
    private val idleManager: ServerIdleManager by inject()

    private var server: EmbeddedServer<*, *>? = null

    companion object {
        const val ACTION_START = "com.defname.localshare.START_SERVICE"
        const val ACTION_STOP = "com.defname.localshare.STOP_SERVICE"
        const val APPROVE_IP = "com.defname.localshare.APPROVE_IP"
        const val DENY_IP = "com.defname.localshare.DENY_IP"
    }

    override fun onCreate() {
        super.onCreate()
        idleManager.startMonitoring {
            stopSelf()
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val ip = intent?.getStringExtra("ip") ?: ""
        when (intent?.action) {
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
        val settings = runBlocking {
            settingsRepository.settingsFlow.first()
        }

        // 1.  Build notification and show it
        val notification = notificationHelper.buildBaseNotification(
            serverIp = settings.serverIp,
            port = settings.serverPort
        )

        startForeground(NotificationHelper.NOTIFICATION_ID, notification)

        // 2.  Start Ktor
        if (server == null) {
            server = embeddedServer(
                Netty,
                port = settings.serverPort,
                host = settings.serverIp
            ) {
                configureServerModule(
                    serviceRepository = serviceRepository,
                    logsRepository = logsRepository,
                    securityHandler = securityHandler,
                    context = this@LocalShareService.applicationContext
                )
            }
            server?.start(wait = false)
        }

        serviceRepository.serverStarted()
    }

    private fun stopHttpServer() {
        idleManager.stop()
        server?.stop(1000, 2000)
        serviceRepository.serverStopped()
        server = null
    }

    override fun onDestroy() {
        stopHttpServer()
        super.onDestroy()
    }
}