package com.defname.localshare.service.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.defname.localshare.MainActivity
import com.defname.localshare.R
import com.defname.localshare.service.LocalShareService

class NotificationHelper(private val context: Context) {
    companion object {
        const val CHANNEL_ID = "local_share_server_channel"
        const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(channel)
    }

    fun buildBaseNotification(serverIp: String, port: Int): Notification {
        val mainActivityIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, mainActivityIntent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            context, 1, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.service_notification_title))
            .setContentText(context.getString(R.string.server_notification_text, serverIp, port))
            .setSmallIcon(R.drawable.ic_launcher_foreground) // System-Icon als Fallback
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.service_notification_stop_server), stopPendingIntent)
            .build()
    }


    fun showApprovalNotification(clientIp: String, filename: String = "files") {
        val approveIntent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.APPROVE_IP
            putExtra("ip", clientIp)
        }
        val approvePending = PendingIntent.getService(context, clientIp.hashCode(), approveIntent, PendingIntent.FLAG_IMMUTABLE)

        val denyIntent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.DENY_IP
            putExtra("ip", clientIp)
        }
        val denyPending = PendingIntent.getService(context, clientIp.hashCode(), denyIntent, PendingIntent.FLAG_IMMUTABLE)

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(context.getString(R.string.connection_request_notification_title))
            .setContentText(context.getString(R.string.connection_request_notification_text, clientIp, filename))
            // .setSmallIcon(R.drawable.ic_security)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // System-Icon als Fallback
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .addAction(0, context.getString(R.string.connection_request_notification_accept), approvePending)
            .addAction(0, context.getString(R.string.connection_request_notification_deny), denyPending)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(clientIp.hashCode(), notification)
    }
}