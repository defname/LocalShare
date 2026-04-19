package com.defname.localshare.service

import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.service.notification.NotificationHelper
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.routing.RoutingCall
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout

class ServerSecurityHandler(
    private val securityRepository: SecurityRepository,
    private val notificationHelper: NotificationHelper
) {
    suspend fun verifyAccess(call: RoutingCall): Boolean {
        val token = call.parameters["token"] ?: return false
        val ip = call.request.local.remoteHost
        if (!securityRepository.checkToken(token)) {
            return false
        }
        if (securityRepository.isBlacklisted(ip)) {
            return false
        }
        if (securityRepository.isWhitelisted(ip)) {
            return true
        }
        if (!securityRepository.isApprovalRequired()) {
            return true
        }

        notificationHelper.showApprovalNotification(ip)

        return try {
            withTimeout(30_000L) {
                securityRepository.whitelist.first() { list ->
                    list.any { it.ip == ip }
                }
                true
            }
        } catch (e: TimeoutCancellationException) {
            false
        }
    }

    fun mayContinueDownload(request: ApplicationRequest): Boolean {
        if (securityRepository.isBlacklisted(request.local.remoteHost)) {
            return false
        }
        if (securityRepository.isWhitelisted(request.local.remoteHost)) {
            return true
        }

        return !securityRepository.isApprovalRequired()
    }

    fun approveIp(ip: String) {
        securityRepository.addToWhitelist(ip)
    }

    fun blockIp(ip: String) {
        securityRepository.addToBlacklist(ip)
    }
}