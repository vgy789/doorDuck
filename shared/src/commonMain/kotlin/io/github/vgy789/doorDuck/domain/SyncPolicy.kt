package io.github.vgy789.doorDuck.domain

import io.github.vgy789.doorDuck.model.Defaults
import io.github.vgy789.doorDuck.platform.currentTimeMillis

object SyncPolicy {
    private val retryOffsetsMs = longArrayOf(
        1L * 60L * 60L * 1000L,
        3L * 60L * 60L * 1000L,
        6L * 60L * 60L * 1000L,
        12L * 60L * 60L * 1000L,
        24L * 60L * 60L * 1000L,
        48L * 60L * 60L * 1000L,
    )

    fun refreshAtMs(expiresAtMs: Long): Long {
        return expiresAtMs
    }

    fun nextRetryDelayMs(
        attempt: Int,
        minDelayMs: Long = 0L,
    ): Long {
        val scheduleMs = when (attempt.coerceAtLeast(0)) {
            0 -> 60L * 60L * 1000L
            1 -> 2L * 60L * 60L * 1000L
            2 -> 3L * 60L * 60L * 1000L
            3 -> 6L * 60L * 60L * 1000L
            4 -> 12L * 60L * 60L * 1000L
            else -> 24L * 60L * 60L * 1000L
        }
        return maxOf(scheduleMs, minDelayMs.coerceAtLeast(0L))
    }

    fun nextRetryAtMs(
        expiresAtMs: Long?,
        attempt: Int,
        nowMs: Long = currentTimeMillis(),
        minDelayMs: Long = 0L,
    ): Long {
        val minRetryAtMs = nowMs + minDelayMs.coerceAtLeast(0L)
        val baseRetryAtMs = expiresAtMs?.let { expiresAt ->
            val offsetMs = retryOffsetsMs.getOrElse(attempt.coerceAtLeast(0)) { retryOffsetsMs.last() }
            expiresAt + offsetMs
        } ?: (nowMs + nextRetryDelayMs(attempt = attempt, minDelayMs = 0L))
        return maxOf(baseRetryAtMs, minRetryAtMs)
    }

    fun shouldRefreshNow(
        autoRefreshEnabled: Boolean,
        localImagePath: String?,
        expiresAtMs: Long?,
        nextAutoRefreshAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        if (!autoRefreshEnabled) return false
        if (localImagePath.isNullOrBlank()) return false
        if (expiresAtMs == null) return false
        if (nowMs < refreshAtMs(expiresAtMs)) return false
        if (nextAutoRefreshAtMs != null && nowMs < nextAutoRefreshAtMs) return false
        return true
    }

    fun isExpired(
        expiresAtMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return expiresAtMs != null && nowMs >= expiresAtMs
    }

    fun isManualRefreshBlocked(
        blockedUntilMs: Long?,
        nowMs: Long = currentTimeMillis(),
    ): Boolean {
        return blockedUntilMs != null && nowMs < blockedUntilMs
    }

    fun nextManualRefreshAllowedAt(
        nowMs: Long = currentTimeMillis(),
    ): Long {
        return nowMs + Defaults.manualRefreshCooldownMillis
    }
}
