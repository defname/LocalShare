// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

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
        const val APPROVAL_CHANNEL_ID = "local_share_approval_channel"
        const val NOTIFICATION_ID = 1
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serverChannel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = context.getString(R.string.notification_channel_description)
        }
        manager.createNotificationChannel(serverChannel)

        val approvalChannel = NotificationChannel(
            APPROVAL_CHANNEL_ID,
            context.getString(R.string.connection_request_notification_title),
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            enableVibration(true)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(approvalChannel)
    }

    fun buildBaseNotification(serverIp: String, port: Int, isRunning: Boolean): Notification {
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
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .addAction(0, context.getString(R.string.service_notification_stop_server), stopPendingIntent)
            .build()
    }

    fun showApprovalNotification(clientIp: String, sessionId: String) {
        // Use sessionId hash as the unique notification ID so each session gets its own notification
        val notifId = sessionId.hashCode()

        val approveIntent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.APPROVE_SESSION
            putExtra("sessionId", sessionId)
            putExtra("ip", clientIp)
        }
        val approvePending = PendingIntent.getService(
            context,
            notifId,
            approveIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val denyIntent = Intent(context, LocalShareService::class.java).apply {
            action = LocalShareService.DENY_SESSION
            putExtra("sessionId", sessionId)
            putExtra("ip", clientIp)
        }
        val denyPending = PendingIntent.getService(
            context,
            notifId + 1,
            denyIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, APPROVAL_CHANNEL_ID)
            .setContentTitle(context.getString(R.string.connection_request_notification_title))
            .setContentText(context.getString(R.string.connection_request_notification_text, clientIp, ""))
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setDefaults(Notification.DEFAULT_ALL)
            .addAction(0, context.getString(R.string.connection_request_notification_accept), approvePending)
            .addAction(0, context.getString(R.string.connection_request_notification_deny), denyPending)
            .setAutoCancel(true)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(notifId, notification)
    }
}
