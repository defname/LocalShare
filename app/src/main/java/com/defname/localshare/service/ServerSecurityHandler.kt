// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

package com.defname.localshare.service

import com.defname.localshare.data.SecurityRepository
import com.defname.localshare.service.notification.NotificationHelper
import io.ktor.server.request.ApplicationRequest
import io.ktor.server.request.cookies
import io.ktor.server.routing.RoutingCall
import io.ktor.util.collections.ConcurrentSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

enum class SessionState { PENDING, APPROVED, DENIED, REVOKED }

data class DeviceSession(
    val sessionId: String,
    val ip: String,
    @Volatile var state: SessionState = SessionState.PENDING,
    val createdAt: Long = System.currentTimeMillis()
)

class ServerSecurityHandler(
    private val securityRepository: SecurityRepository,
    private val notificationHelper: NotificationHelper
) {
    private val pendingVerificationRequests = ConcurrentSet<String>()
    private val sessions = ConcurrentHashMap<String, DeviceSession>()

    private val _sessionEvents = MutableSharedFlow<Pair<String, SessionState>>(extraBufferCapacity = 64)
    val sessionEvents = _sessionEvents.asSharedFlow()

    private val rateLimitMap = ConcurrentHashMap<String, Pair<Int, Long>>()
    private val MAX_FAILURES = 20
    private val RATE_LIMIT_WINDOW_MS = 60_000L
    private val RATE_LIMIT_BAN_MS = 300_000L

    // ─── Rate Limiting ────────────────────────────────────────────────────────

    private fun recordFailedAttempt(ip: String) {
        val now = System.currentTimeMillis()
        val current = rateLimitMap[ip]
        if (current == null || now - current.second > RATE_LIMIT_WINDOW_MS) {
            rateLimitMap[ip] = Pair(1, now)
        } else {
            rateLimitMap[ip] = Pair(current.first + 1, current.second)
        }
    }

    fun isRateLimited(ip: String): Boolean {
        val entry = rateLimitMap[ip] ?: return false
        val now = System.currentTimeMillis()
        if (now - entry.second > RATE_LIMIT_BAN_MS) {
            rateLimitMap.remove(ip)
            return false
        }
        return entry.first >= MAX_FAILURES
    }

    // ─── Session Management ───────────────────────────────────────────────────

    fun createSession(ip: String): String {
        val sessionId = UUID.randomUUID().toString().replace("-", "")
        sessions[sessionId] = DeviceSession(sessionId = sessionId, ip = ip)
        return sessionId
    }

    fun getSession(sessionId: String): DeviceSession? = sessions[sessionId]

    fun approveSession(sessionId: String) {
        val session = sessions[sessionId] ?: return
        session.state = SessionState.APPROVED
        _sessionEvents.tryEmit(Pair(sessionId, SessionState.APPROVED))
        pendingVerificationRequests.remove(session.ip)
    }

    fun denySession(sessionId: String) {
        val session = sessions[sessionId] ?: return
        session.state = SessionState.DENIED
        _sessionEvents.tryEmit(Pair(sessionId, SessionState.DENIED))
        pendingVerificationRequests.remove(session.ip)
    }

    fun revokeSession(sessionId: String) {
        val session = sessions[sessionId] ?: return
        session.state = SessionState.REVOKED
        _sessionEvents.tryEmit(Pair(sessionId, SessionState.REVOKED))
        sessions.remove(sessionId)
    }

    fun getPendingSessionIdForIp(ip: String): String? =
        sessions.values.firstOrNull { it.ip == ip && it.state == SessionState.PENDING }?.sessionId

    fun cleanupSessions() {
        val cutoff = System.currentTimeMillis() - 24 * 60 * 60 * 1000L
        sessions.entries.removeIf { it.value.createdAt < cutoff }
    }

    // ─── Device Check ─────────────────────────────────────────────────────────

    private suspend fun isSameDevice(remoteHost: String, localHost: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val remoteAddr = InetAddress.getByName(remoteHost)
                val localAddr = InetAddress.getByName(localHost)
                if (remoteAddr.isLoopbackAddress) {
                    return@withContext localAddr.isLoopbackAddress
                }
                NetworkInterface.getByInetAddress(remoteAddr) != null
            } catch (e: Exception) {
                (remoteHost == "127.0.0.1" && localHost == "127.0.0.1") ||
                        (remoteHost == "::1" && localHost == "::1")
            }
        }

    // ─── Token Check ──────────────────────────────────────────────────────────

    fun checkToken(token: String): Boolean = securityRepository.checkToken(token)

    // ─── Landing Access ───────────────────────────────────────────────────────

    sealed class LandingAccess {
        object Allowed : LandingAccess()
        data class NeedsApproval(val sessionId: String) : LandingAccess()
        object Forbidden : LandingAccess()
        object RateLimited : LandingAccess()
    }

    suspend fun checkLandingAccess(call: RoutingCall): LandingAccess {
        val token = call.parameters["token"] ?: return LandingAccess.Forbidden
        val remoteHost = call.request.local.remoteHost
        val localHost = call.request.local.localHost

        if (!securityRepository.checkToken(token)) {
            recordFailedAttempt(remoteHost)
            return LandingAccess.Forbidden
        }
        if (isRateLimited(remoteHost)) return LandingAccess.RateLimited
        if (securityRepository.isBlacklisted(remoteHost)) return LandingAccess.Forbidden
        if (isSameDevice(remoteHost, localHost)) return LandingAccess.Allowed
        if (!securityRepository.isApprovalRequired()) return LandingAccess.Allowed

        val sessionId = call.request.cookies()["ls_session"]
        if (sessionId != null) {
            val session = sessions[sessionId]
            if (session != null && session.ip == remoteHost) {
                return when (session.state) {
                    SessionState.APPROVED -> LandingAccess.Allowed
                    SessionState.PENDING -> LandingAccess.NeedsApproval(sessionId)
                    SessionState.DENIED, SessionState.REVOKED -> LandingAccess.Forbidden
                }
            }
        }

        val existingPending = getPendingSessionIdForIp(remoteHost)
        if (existingPending != null) return LandingAccess.NeedsApproval(existingPending)

        val newSessionId = createSession(remoteHost)
        pendingVerificationRequests.add(remoteHost)
        notificationHelper.showApprovalNotification(remoteHost, newSessionId)
        return LandingAccess.NeedsApproval(newSessionId)
    }

    // ─── General Route Access Verification ───────────────────────────────────

    suspend fun verifyAccess(call: RoutingCall): Boolean {
        val token = call.parameters["token"] ?: return false
        val remoteHost = call.request.local.remoteHost
        val localHost = call.request.local.localHost

        if (!securityRepository.checkToken(token)) {
            recordFailedAttempt(remoteHost)
            return false
        }
        if (isRateLimited(remoteHost)) return false
        if (securityRepository.isBlacklisted(remoteHost)) return false
        if (isSameDevice(remoteHost, localHost)) return true
        if (!securityRepository.isApprovalRequired()) return true

        val sessionId = call.request.cookies()["ls_session"] ?: return false
        val session = sessions[sessionId] ?: return false
        return session.state == SessionState.APPROVED && session.ip == remoteHost
    }

    // ─── Non-suspending In-Flight Check (for SSE heartbeat loops) ────────────

    fun isStillAllowed(call: RoutingCall): Boolean {
        val remoteHost = call.request.local.remoteHost
        if (securityRepository.isBlacklisted(remoteHost)) return false
        if (!securityRepository.isApprovalRequired()) return true
        val sessionId = call.request.cookies()["ls_session"] ?: return true
        val session = sessions[sessionId] ?: return false
        return session.state == SessionState.APPROVED && session.ip == remoteHost
    }

    // ─── Download Mid-Transfer Check ──────────────────────────────────────────

    fun mayContinueDownload(request: ApplicationRequest): Boolean {
        val remoteHost = request.local.remoteHost
        return !securityRepository.isBlacklisted(remoteHost)
    }

    // ─── Backwards-Compat for LocalShareService ───────────────────────────────

    fun approveIp(ip: String) {
        val sessionId = getPendingSessionIdForIp(ip) ?: run {
            securityRepository.addToWhitelist(ip)
            return
        }
        approveSession(sessionId)
    }

    fun blockIp(ip: String) {
        securityRepository.addToBlacklist(ip)
        sessions.values.filter { it.ip == ip && it.state == SessionState.PENDING }
            .forEach { denySession(it.sessionId) }
    }
}
